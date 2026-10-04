[CmdletBinding()]
param(
    [string]$EnvironmentFile = '',
    [switch]$SkipInstall
)

$ErrorActionPreference = 'Stop'
$deployDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDirectory = (Resolve-Path (Join-Path $deployDirectory '..')).Path
. (Join-Path $deployDirectory 'scripts\dev\common.ps1')

foreach ($port in @(8080, 5173)) {
    $occupied = Get-PortOwnerProcessId $port
    if ($occupied -gt 0) {
        Write-DevStatus ERROR ("Port {0} is already occupied by {1}. Nothing was stopped." -f `
                $port, (Get-ProcessDisplayName $occupied))
        throw "Development startup requires free port $port."
    }
}

if ([string]::IsNullOrWhiteSpace($EnvironmentFile)) {
    $EnvironmentFile = Join-Path $deployDirectory 'local.env.properties'
}
if (-not (Test-Path -LiteralPath $EnvironmentFile)) {
    throw 'Create deploy\local.env.properties before starting development.'
}

$runId = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$ownerToken = [guid]::NewGuid().ToString()
$runDirectory = Get-DevRunDirectory $runId
$logDirectory = Join-Path $projectDirectory ("logs\run-{0}" -f $runId)
New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
$env:APP_RUN_ID = $runId
$env:APP_ORCHESTRATOR_OWNER_TOKEN = $ownerToken

$backendOut = Join-Path $logDirectory 'backend-wrapper.out.log'
$backendErr = Join-Path $logDirectory 'backend-wrapper.err.log'
$frontendOut = Join-Path $logDirectory 'frontend-wrapper.out.log'
$frontendErr = Join-Path $logDirectory 'frontend-wrapper.err.log'
foreach ($path in @($backendOut, $backendErr, $frontendOut, $frontendErr)) {
    New-Item -ItemType File -Path $path -Force | Out-Null
}

function Wait-PortReady {
    param([int]$Port, [System.Diagnostics.Process]$Process, [int]$Seconds = 45)
    $deadline = [DateTime]::UtcNow.AddSeconds($Seconds)
    while ([DateTime]::UtcNow -lt $deadline) {
        if ($Process.HasExited) { return $false }
        if ((Get-PortOwnerProcessId $Port) -gt 0) { return $true }
        Start-Sleep -Milliseconds 250
    }
    return $false
}

function Open-SharedReader {
    param([string]$Path)
    $stream = New-Object System.IO.FileStream($Path, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::ReadWrite)
    return New-Object System.IO.StreamReader($stream, [Text.Encoding]::UTF8)
}

function Write-NewLines {
    param([System.IO.StreamReader]$Reader, [string]$Prefix)
    while (-not $Reader.EndOfStream) {
        $line = $Reader.ReadLine()
        if ($null -ne $line) { Write-Host ("[{0}] {1}" -f $Prefix, $line) }
    }
}

$backendWrapper = $null
$frontendWrapper = $null
$backendIdentity = $null
$frontendIdentity = $null
$readers = @()
$failure = $null

function Stop-CapturedIdentity {
    param([object]$Identity)
    if ($null -eq $Identity) { return }
    if (-not (Test-ProcessIdentity -ProcessId $Identity.ProcessId `
            -ExpectedExecutable $Identity.Executable `
            -ExpectedStartTimeUtcTicks $Identity.StartTimeUtcTicks)) {
        return
    }
    Write-DevStatus STOP ("Stopping captured project-owned {0} PID {1}" -f $Identity.Service, $Identity.ProcessId)
    Stop-Process -Id $Identity.ProcessId -ErrorAction Stop
    Wait-Process -Id $Identity.ProcessId -Timeout 8 -ErrorAction SilentlyContinue
    if (Get-Process -Id $Identity.ProcessId -ErrorAction SilentlyContinue) {
        Stop-Process -Id $Identity.ProcessId -Force -ErrorAction Stop
        Wait-Process -Id $Identity.ProcessId -Timeout 5 -ErrorAction SilentlyContinue
    }
    if (Get-Process -Id $Identity.ProcessId -ErrorAction SilentlyContinue) {
        throw ("Captured {0} PID {1} is still running after exact cleanup." -f `
                $Identity.Service, $Identity.ProcessId)
    }
    Write-DevStatus OK ("Stopped captured {0} PID {1}" -f $Identity.Service, $Identity.ProcessId)
}

try {
    Write-DevStatus TITLE ("SMART PROPERTY DEVELOPMENT RUN {0}" -f $runId)
    $backendArguments = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File',
        (Join-Path $deployDirectory 'start-backend-dev.ps1'), '-EnvironmentFile', $EnvironmentFile)
    $backendWrapper = Start-Process powershell.exe -ArgumentList $backendArguments -PassThru -WindowStyle Hidden `
        -WorkingDirectory $projectDirectory -RedirectStandardOutput $backendOut -RedirectStandardError $backendErr
    if (-not (Wait-PortReady -Port 8080 -Process $backendWrapper -Seconds 60)) {
        throw 'Backend did not become ready on port 8080. Inspect backend-wrapper logs.'
    }
    $backendPid = Get-PortOwnerProcessId 8080
    $backendProcess = Get-Process -Id $backendPid -ErrorAction Stop
    $backendIdentity = [pscustomobject]@{
        Service = 'backend'
        ProcessId = $backendPid
        Executable = $backendProcess.Path
        StartTimeUtcTicks = $backendProcess.StartTime.ToUniversalTime().Ticks
    }
    $backendRecord = Save-ManagedProcessRecord -Service 'backend' -ProcessId $backendPid `
        -Executable $backendProcess.Path -OwnerPid $PID -OwnerToken $ownerToken -RunId $runId -Ports @(8080)

    $frontendArguments = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File',
        (Join-Path $deployDirectory 'start-frontend-dev.ps1'))
    if ($SkipInstall) { $frontendArguments += '-SkipInstall' }
    $frontendWrapper = Start-Process powershell.exe -ArgumentList $frontendArguments -PassThru -WindowStyle Hidden `
        -WorkingDirectory $projectDirectory -RedirectStandardOutput $frontendOut -RedirectStandardError $frontendErr
    if (-not (Wait-PortReady -Port 5173 -Process $frontendWrapper -Seconds 30)) {
        throw 'Frontend did not become ready on port 5173. Inspect frontend-wrapper logs.'
    }
    $frontendPid = Get-PortOwnerProcessId 5173
    $frontendProcess = Get-Process -Id $frontendPid -ErrorAction Stop
    $frontendIdentity = [pscustomobject]@{
        Service = 'frontend'
        ProcessId = $frontendPid
        Executable = $frontendProcess.Path
        StartTimeUtcTicks = $frontendProcess.StartTime.ToUniversalTime().Ticks
    }
    $frontendRecord = Save-ManagedProcessRecord -Service 'frontend' -ProcessId $frontendPid `
        -Executable $frontendProcess.Path -OwnerPid $PID -OwnerToken $ownerToken -RunId $runId -Ports @(5173)

    $readers = @(
        @{ Reader = Open-SharedReader $backendOut; Prefix = 'BACKEND' },
        @{ Reader = Open-SharedReader $backendErr; Prefix = 'BACKEND ERROR' },
        @{ Reader = Open-SharedReader $frontendOut; Prefix = 'FRONTEND' },
        @{ Reader = Open-SharedReader $frontendErr; Prefix = 'FRONTEND ERROR' }
    )
    Write-DevStatus READY 'Web UI http://127.0.0.1:5173'
    Write-DevStatus READY ("RUN_ID={0}; press Ctrl+C to stop this run only" -f $runId)

    while ($true) {
        foreach ($entry in $readers) { Write-NewLines $entry.Reader $entry.Prefix }
        if ($backendWrapper.HasExited) { throw 'Backend wrapper exited unexpectedly.' }
        if ($frontendWrapper.HasExited) { throw 'Frontend wrapper exited unexpectedly.' }
        Start-Sleep -Milliseconds 200
    }
}
catch {
    $failure = $_
}
finally {
    foreach ($entry in $readers) {
        Write-NewLines $entry.Reader $entry.Prefix
        $entry.Reader.Dispose()
    }
    foreach ($service in @('frontend', 'backend')) {
        $record = Join-Path $runDirectory ($service + '.json')
        Stop-ExactManagedProcess -RecordPath $record -OwnerToken $ownerToken
    }
    # The JVM shutdown hook can remove its record while Maven still leaves the
    # child process alive.  Retain the verified PID/start-time identity in this
    # orchestrator so a missing record never turns into an orphaned 8080.
    Stop-CapturedIdentity $frontendIdentity
    Stop-CapturedIdentity $backendIdentity
    foreach ($wrapper in @($frontendWrapper, $backendWrapper)) {
        if ($null -ne $wrapper -and -not $wrapper.HasExited) {
            Stop-Process -Id $wrapper.Id -ErrorAction SilentlyContinue
        }
    }
    & (Join-Path $deployDirectory 'scripts\dev\stop-managed-dependencies.ps1') `
        -RunId $runId -OwnerToken $ownerToken
    Write-DevStatus OK ("RUN_ID={0} stopped; 8080 and 5173 cleanup completed" -f $runId)
}

if ($null -ne $failure) { throw $failure }
