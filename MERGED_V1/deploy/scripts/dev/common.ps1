Set-StrictMode -Version 2.0

function Resolve-ProjectDirectory {
    $devScripts = Split-Path -Parent $PSScriptRoot
    $scripts = Split-Path -Parent $devScripts
    return (Resolve-Path (Join-Path $scripts '..')).Path
}

function Resolve-LocalConfigFile {
    param([string]$ConfigFile)
    $projectDirectory = Resolve-ProjectDirectory
    if ([string]::IsNullOrWhiteSpace($ConfigFile)) {
        $ConfigFile = Join-Path $projectDirectory 'deploy\local.env.properties'
    }
    if (-not (Test-Path -LiteralPath $ConfigFile)) {
        throw "Missing local configuration: $ConfigFile"
    }
    return (Resolve-Path -LiteralPath $ConfigFile).Path
}

function Import-LocalProperties {
    param([Parameter(Mandatory)] [string]$ConfigFile)
    $result = @{}
    foreach ($line in Get-Content -LiteralPath $ConfigFile -Encoding UTF8) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith('#') -or $trimmed.StartsWith('!')) {
            continue
        }
        $separator = $trimmed.IndexOf('=')
        if ($separator -lt 1) {
            continue
        }
        $key = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1).Trim()
        $result[$key] = $value
    }
    return $result
}

function Get-LocalProperty {
    param(
        [Parameter(Mandatory)] [hashtable]$Properties,
        [Parameter(Mandatory)] [string]$Name,
        [string]$DefaultValue = ''
    )
    if ($Properties.ContainsKey($Name)) {
        return [string]$Properties[$Name]
    }
    return $DefaultValue
}

function Write-DevStatus {
    param(
        [Parameter(Mandatory)] [ValidateSet('TITLE','OK','READY','REUSE','START','WARN','ERROR','STOP','OFFLINE')] [string]$Level,
        [Parameter(Mandatory)] [string]$Message
    )
    if ((Get-Variable -Name MachineReadable -Scope Script -ErrorAction SilentlyContinue) `
            -and $script:MachineReadable) {
        return
    }
    $color = switch ($Level) {
        'TITLE' { 'Cyan' }
        'OK' { 'Green' }
        'READY' { 'Green' }
        'REUSE' { 'Yellow' }
        'START' { 'Cyan' }
        'WARN' { 'Yellow' }
        'ERROR' { 'Red' }
        'STOP' { 'Yellow' }
        'OFFLINE' { 'Yellow' }
    }
    Write-Host ("[{0}] {1}" -f $Level, $Message) -ForegroundColor $color
}

function Write-MachineResult {
    param([Parameter(Mandatory)] [hashtable]$Values)
    if (-not ((Get-Variable -Name MachineReadable -Scope Script -ErrorAction SilentlyContinue) `
            -and $script:MachineReadable)) {
        return
    }
    foreach ($key in ($Values.Keys | Sort-Object)) {
        Write-Output ("{0}={1}" -f $key, [string]$Values[$key])
    }
}

function Get-DevRuntimeDirectory {
    $projectDirectory = Resolve-ProjectDirectory
    $runtime = Join-Path $projectDirectory 'deploy\runtime\dev-processes'
    New-Item -ItemType Directory -Path $runtime -Force | Out-Null
    return $runtime
}

function Get-DevRunDirectory {
    param([Parameter(Mandatory)] [string]$RunId)
    if ($RunId -notmatch '^[A-Za-z0-9._-]{6,80}$') {
        throw "Invalid development RUN_ID: $RunId"
    }
    $directory = Join-Path (Get-DevRuntimeDirectory) ("runs\{0}" -f $RunId)
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
    return $directory
}

function Get-PortOwnerProcessId {
    param([Parameter(Mandatory)] [int]$Port)
    try {
        $connection = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($null -ne $connection) { return [int]$connection.OwningProcess }
    }
    catch {
        # Windows PowerShell may deny the NetTCPConnection provider. netstat is
        # available on every supported Windows installation and is read-only.
    }

    $pattern = '^\s*TCP\s+\S+:' + [regex]::Escape([string]$Port) + '\s+\S+\s+LISTENING\s+(\d+)\s*$'
    foreach ($line in (& netstat.exe -ano -p tcp 2>$null)) {
        if ($line -match $pattern) { return [int]$Matches[1] }
    }
    return 0
}

function Get-ProcessDisplayName {
    param([Parameter(Mandatory)] [int]$ProcessId)
    $process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if ($null -eq $process) { return "PID $ProcessId" }
    return ("{0} (PID {1})" -f $process.ProcessName, $ProcessId)
}

function Test-HttpResponseContains {
    param(
        [Parameter(Mandatory)] [string]$Url,
        [Parameter(Mandatory)] [string]$Marker
    )
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
        return ([string]$response.Content).IndexOf($Marker, [StringComparison]::OrdinalIgnoreCase) -ge 0
    }
    catch {
        return $false
    }
}

function Test-ProcessIdentity {
    param(
        [Parameter(Mandatory)] [int]$ProcessId,
        [Parameter(Mandatory)] [string]$ExpectedExecutable,
        [long]$ExpectedStartTimeUtcTicks = 0
    )
    $process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if ($null -eq $process) { return $false }
    if ([string]::IsNullOrWhiteSpace($ExpectedExecutable)) { return $true }
    try {
        $actual = (Resolve-Path -LiteralPath $process.Path).Path
        $expected = (Resolve-Path -LiteralPath $ExpectedExecutable).Path
        if (-not $actual.Equals($expected, [StringComparison]::OrdinalIgnoreCase)) { return $false }
        if ($ExpectedStartTimeUtcTicks -gt 0 -and $process.StartTime.ToUniversalTime().Ticks -ne $ExpectedStartTimeUtcTicks) { return $false }
        return $true
    }
    catch {
        return $false
    }
}

function Save-ManagedProcessRecord {
    param(
        [Parameter(Mandatory)] [string]$Service,
        [Parameter(Mandatory)] [int]$ProcessId,
        [Parameter(Mandatory)] [string]$Executable,
        [Parameter(Mandatory)] [int]$OwnerPid,
        [Parameter(Mandatory)] [string]$OwnerToken,
        [Parameter(Mandatory)] [string]$RunId,
        [Parameter(Mandatory)] [int[]]$Ports
    )
    $record = [ordered]@{
        service = $Service
        processId = $ProcessId
        executable = (Resolve-Path -LiteralPath $Executable).Path
        processStartTimeUtcTicks = (Get-Process -Id $ProcessId -ErrorAction Stop).StartTime.ToUniversalTime().Ticks
        ownerPid = $OwnerPid
        ownerToken = $OwnerToken
        runId = $RunId
        ports = $Ports
        startedAt = [DateTimeOffset]::Now.ToString('o')
    }
    $path = Join-Path (Get-DevRunDirectory $RunId) ($Service + '.json')
    $record | ConvertTo-Json | Set-Content -LiteralPath $path -Encoding UTF8
    return $path
}

function Claim-Or-ReuseManagedProcess {
    param(
        [Parameter(Mandatory)] [string]$Service,
        [Parameter(Mandatory)] [int]$ProcessId,
        [Parameter(Mandatory)] [string]$Executable,
        [Parameter(Mandatory)] [int]$OwnerPid,
        [Parameter(Mandatory)] [string]$OwnerToken,
        [Parameter(Mandatory)] [string]$RunId,
        [Parameter(Mandatory)] [int[]]$Ports
    )
    $recordPath = Join-Path (Get-DevRunDirectory $RunId) ($Service + '.json')
    if (-not (Test-Path -LiteralPath $recordPath)) {
        return $false
    }
    try {
        $record = Get-Content -LiteralPath $recordPath -Raw | ConvertFrom-Json
        if ([int]$record.processId -ne $ProcessId) { return $false }
        if ($null -eq $record.processStartTimeUtcTicks) { return $false }
        if (-not (Test-ProcessIdentity -ProcessId $ProcessId -ExpectedExecutable ([string]$record.executable) `
                -ExpectedStartTimeUtcTicks ([long]$record.processStartTimeUtcTicks))) {
            return $false
        }
        Save-ManagedProcessRecord -Service $Service -ProcessId $ProcessId -Executable $Executable `
            -OwnerPid $OwnerPid -OwnerToken $OwnerToken -RunId $RunId -Ports $Ports | Out-Null
        return $true
    }
    catch {
        return $false
    }
}

function Wait-HttpReady {
    param(
        [Parameter(Mandatory)] [string]$Url,
        [int]$Attempts = 40,
        [int]$DelayMilliseconds = 500
    )
    for ($attempt = 1; $attempt -le $Attempts; $attempt++) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
            if ([int]$response.StatusCode -ge 200 -and [int]$response.StatusCode -lt 300) {
                return $true
            }
        }
        catch {
            # Readiness retries are expected while the process is starting.
        }
        Start-Sleep -Milliseconds $DelayMilliseconds
    }
    return $false
}

function Stop-ExactManagedProcess {
    param(
        [Parameter(Mandatory)] [string]$RecordPath,
        [string]$OwnerToken = ''
    )
    if (-not (Test-Path -LiteralPath $RecordPath)) { return }
    try {
        $record = Get-Content -LiteralPath $RecordPath -Raw | ConvertFrom-Json
        if (-not [string]::IsNullOrWhiteSpace($OwnerToken) -and $record.ownerToken -ne $OwnerToken) {
            return
        }
        $processId = [int]$record.processId
        $startTicks = if ($null -eq $record.processStartTimeUtcTicks) { 0 } else { [long]$record.processStartTimeUtcTicks }
        if (Test-ProcessIdentity -ProcessId $processId -ExpectedExecutable ([string]$record.executable) `
                -ExpectedStartTimeUtcTicks $startTicks) {
            Write-DevStatus STOP ("Stopping project-owned {0} PID {1}" -f $record.service, $processId)
            Stop-Process -Id $processId -ErrorAction Stop
            Wait-Process -Id $processId -Timeout 8 -ErrorAction SilentlyContinue
            if (Get-Process -Id $processId -ErrorAction SilentlyContinue) {
                Stop-Process -Id $processId -Force -ErrorAction Stop
                Wait-Process -Id $processId -Timeout 5 -ErrorAction SilentlyContinue
            }
            if (Get-Process -Id $processId -ErrorAction SilentlyContinue) {
                throw "Managed process PID $processId is still running after exact cleanup."
            }
            Write-DevStatus OK ("Stopped {0} PID {1}" -f $record.service, $processId)
        }
    }
    catch {
        Write-DevStatus WARN ("Could not stop managed process from {0}: {1}" -f $RecordPath, $_.Exception.Message)
    }
    finally {
        Remove-Item -LiteralPath $RecordPath -Force -ErrorAction SilentlyContinue
    }
}
