function Get-PublicDemoLogsRoot([string]$ProjectRoot) {
    $root = Join-Path $ProjectRoot 'logs'
    foreach ($directory in @($root, (Join-Path $root '_runtime'), (Join-Path $root '_legacy'))) {
        New-Item -ItemType Directory -Force -Path $directory | Out-Null
    }
    return $root
}

function Get-PublicDemoRuntimeRoot([string]$ProjectRoot) {
    $runtime = Join-Path (Get-PublicDemoLogsRoot $ProjectRoot) '_runtime'
    foreach ($directory in @($runtime, (Join-Path $runtime 'agent-control'))) {
        New-Item -ItemType Directory -Force -Path $directory | Out-Null
    }
    return $runtime
}

function Get-PublicDemoAgentControlRoot([string]$ProjectRoot) {
    return Join-Path (Get-PublicDemoRuntimeRoot $ProjectRoot) 'agent-control'
}

function Get-PublicDemoHostSessionId {
    $sessionId = (Get-Process -Id $PID).SessionId
    try {
        $boot = (Get-CimInstance -ClassName Win32_OperatingSystem -ErrorAction Stop).LastBootUpTime
        $bootStamp = ([datetime]$boot).ToUniversalTime().ToString('yyyyMMddHHmmssZ')
    } catch {
        $bootStamp = 'boot-time-unavailable'
    }
    $seed = '{0}|{1}|{2}|{3}|{4}' -f $env:COMPUTERNAME, $env:USERDOMAIN, $env:USERNAME, $sessionId, $bootStamp
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $hash = $sha.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($seed))
        return 'hs-' + (($hash | ForEach-Object { $_.ToString('x2') }) -join '').Substring(0, 20)
    } finally {
        $sha.Dispose()
    }
}

function Get-PublicDemoRunState([string]$ProjectRoot) {
    $root = Get-PublicDemoLogsRoot $ProjectRoot
    $runtimeState = Join-Path (Get-PublicDemoRuntimeRoot $ProjectRoot) 'active-run.json'
    $legacyState = Join-Path $root 'active-run.json'
    foreach ($candidate in @($runtimeState, $legacyState)) {
        if (-not (Test-Path -LiteralPath $candidate)) { continue }
        try {
            $state = Get-Content -Raw -LiteralPath $candidate | ConvertFrom-Json
            if ($state.run_id -and $state.run_path -and (Test-Path -LiteralPath $state.run_path)) {
                return [pscustomobject]@{ RunId = $state.run_id; RunPath = $state.run_path; Root = $root }
            }
        } catch { }
    }
    return $null
}

function Get-LatestPublicDemoRunSession([string]$ProjectRoot) {
    $root = Get-PublicDemoLogsRoot $ProjectRoot
    $candidates = foreach ($directory in Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue) {
        if ($directory.Name -notmatch '^run-') { continue }
        $metaPath = Join-Path $directory.FullName 'meta.json'
        if (-not (Test-Path -LiteralPath $metaPath)) { continue }
        try {
            $meta = Get-Content -Raw -LiteralPath $metaPath | ConvertFrom-Json
            [pscustomobject]@{ RunId = $directory.Name; RunPath = $directory.FullName; Root = $root; StartedAt = [datetimeoffset]$meta.started_at }
        } catch { }
    }
    return $candidates | Sort-Object StartedAt -Descending | Select-Object -First 1
}

function Set-PublicDemoRunState([string]$ProjectRoot, [object]$Run, [switch]$AllowHostBoot) {
    if ($Run.RunId -match '-host-boot$' -and -not $AllowHostBoot) {
        # Lifecycle code must never demote an explicit deploy/load-test run to HOST_BOOT.
        return
    }
    $state = [ordered]@{ run_id = $Run.RunId; run_path = $Run.RunPath; updated_at = (Get-Date).ToString('o') }
    $state | ConvertTo-Json | Set-Content -LiteralPath (Join-Path (Get-PublicDemoRuntimeRoot $ProjectRoot) 'active-run.json') -Encoding utf8
    Initialize-PublicDemoRunLogSinks $Run
}

function Initialize-PublicDemoRunLogSinks([object]$Run) {
    # Only a real runtime transition prepares its concrete host bind-mount target.
    # Tests never call this function and therefore cannot redirect live services.
    foreach ($directory in @('backend\application', 'backend\http', 'backend\trace', 'database', 'audit', 'security', 'nginx\access', 'nginx\error')) {
        New-Item -ItemType Directory -Force -Path (Join-Path $Run.RunPath $directory) | Out-Null
    }
}

function New-PublicDemoRunSession {
    param(
        [Parameter(Mandatory = $true)][string]$ProjectRoot,
        [Parameter(Mandatory = $true)][string]$Reason,
        [string]$Trigger = 'manual',
        [string]$DeploymentId = '',
        [string]$Service = '',
        [string]$HostSessionId = (Get-PublicDemoHostSessionId),
        [string]$PreviousRunId = ''
    )
    $root = Get-PublicDemoLogsRoot $ProjectRoot
    if ([string]::IsNullOrWhiteSpace($PreviousRunId)) {
        $previous = Get-PublicDemoRunState $ProjectRoot
        if ($previous) { $PreviousRunId = $previous.RunId }
    }
    $slug = ($Reason.ToLowerInvariant() -replace '[^a-z0-9]+', '-') -replace '(^-|-$)', ''
    $runId = 'run-{0}-{1}' -f (Get-Date -Format 'yyyyMMdd-HHmmss'), $slug
    $runPath = Join-Path $root $runId
    if (Test-Path -LiteralPath $runPath) { $runId = "$runId-$([Guid]::NewGuid().ToString('N').Substring(0, 6))"; $runPath = Join-Path $root $runId }
    # A runtime begins with metadata only. Streams create their directories when they
    # actually have a record to write; no decorative empty directory tree is created.
    New-Item -ItemType Directory -Force -Path $runPath | Out-Null
    $meta = [ordered]@{
        run_id = $runId
        started_at = (Get-Date).ToString('o')
        reason = $Reason
        trigger = $Trigger
        deployment_id = if ($DeploymentId) { $DeploymentId } else { $null }
        service = if ($Service) { $Service } else { $null }
        host_session_id = $HostSessionId
        previous_run_id = if ($PreviousRunId) { $PreviousRunId } else { $null }
        host = $env:COMPUTERNAME
        context = 'PUBLIC_DEMO_LOCALHOST'
    }
    $meta | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $runPath 'meta.json') -Encoding utf8
    $run = [pscustomobject]@{ RunId = $runId; RunPath = $runPath; Root = $root }
    Set-PublicDemoRunState $ProjectRoot $run -AllowHostBoot:($Reason -eq 'HOST_BOOT')
    return $run
}

function Get-PublicDemoHostSessionMarker([string]$ProjectRoot) {
    $marker = Join-Path (Get-PublicDemoRuntimeRoot $ProjectRoot) 'host-session.json'
    if (-not (Test-Path -LiteralPath $marker)) { return $null }
    try { return Get-Content -Raw -LiteralPath $marker | ConvertFrom-Json } catch { return $null }
}

function Set-PublicDemoHostSessionMarker([string]$ProjectRoot, [object]$Run, [string]$HostSessionId, [string]$Origin) {
    [ordered]@{
        host_session_id = $HostSessionId
        host_boot_run_id = $Run.RunId
        host_boot_run_path = $Run.RunPath
        established_at = (Get-Date).ToString('o')
        origin = $Origin
    } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path (Get-PublicDemoRuntimeRoot $ProjectRoot) 'host-session.json') -Encoding utf8
}

function Ensure-PublicDemoHostSession([string]$ProjectRoot) {
    $hostSessionId = Get-PublicDemoHostSessionId
    $marker = Get-PublicDemoHostSessionMarker $ProjectRoot
    if ($marker -and $marker.host_session_id -eq $hostSessionId -and $marker.host_boot_run_path -and (Test-Path -LiteralPath $marker.host_boot_run_path)) {
        return [pscustomobject]@{ Run = [pscustomobject]@{ RunId = $marker.host_boot_run_id; RunPath = $marker.host_boot_run_path; Root = Get-PublicDemoLogsRoot $ProjectRoot }; Created = $false; Adopted = $false }
    }
    if (-not $marker) {
        $historical = Get-ChildItem -LiteralPath (Get-PublicDemoLogsRoot $ProjectRoot) -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '^run-\d{8}-\d{6}-host-boot' } | Sort-Object Name | Select-Object -First 1
        if ($historical) {
            $adopted = [pscustomobject]@{ RunId = $historical.Name; RunPath = $historical.FullName; Root = Get-PublicDemoLogsRoot $ProjectRoot }
            Set-PublicDemoHostSessionMarker $ProjectRoot $adopted $hostSessionId 'historical-host-boot-adoption'
            return [pscustomobject]@{ Run = $adopted; Created = $false; Adopted = $true }
        }
    }
    $run = New-PublicDemoRunSession -ProjectRoot $ProjectRoot -Reason 'HOST_BOOT' -Trigger 'windows-logon-session' -HostSessionId $hostSessionId
    Set-PublicDemoHostSessionMarker $ProjectRoot $run $hostSessionId 'new-windows-logon-session'
    return [pscustomobject]@{ Run = $run; Created = $true; Adopted = $false }
}

function Get-ActivePublicDemoRunSession {
    param([Parameter(Mandatory = $true)][string]$ProjectRoot)
    $active = Get-PublicDemoRunState $ProjectRoot
    if ($active) { return $active }
    $hostSession = Ensure-PublicDemoHostSession $ProjectRoot
    Set-PublicDemoRunState $ProjectRoot $hostSession.Run -AllowHostBoot
    return $hostSession.Run
}

function Get-RequiredPublicDemoRunSession {
    param([Parameter(Mandatory = $true)][string]$ProjectRoot)
    $active = Get-PublicDemoRunState $ProjectRoot
    if (-not $active) { throw 'No active runtime run is available. A test must not create or switch a runtime run.' }
    return $active
}

function New-PublicDemoTestResult {
    param(
        [Parameter(Mandatory = $true)][string]$ProjectRoot,
        [Parameter(Mandatory = $true)][ValidateSet('system-load', 'database-load')][string]$TestType,
        [string]$Trigger = 'manual'
    )
    $runtime = Get-RequiredPublicDemoRunSession $ProjectRoot
    $testId = '{0}-{1}' -f $TestType, (Get-Date -Format 'yyyyMMdd-HHmmss')
    $testPath = Join-Path $runtime.RunPath (Join-Path 'tests' $testId)
    if (Test-Path -LiteralPath $testPath) { $testPath = "$testPath-$([Guid]::NewGuid().ToString('N').Substring(0, 6))" }
    New-Item -ItemType Directory -Force -Path $testPath | Out-Null
    [ordered]@{
        test_id = Split-Path -Leaf $testPath
        test_type = $TestType
        runtime_run_id = $runtime.RunId
        started_at = (Get-Date).ToString('o')
        trigger = $Trigger
    } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $testPath 'meta.json') -Encoding utf8
    return [pscustomobject]@{ Runtime = $runtime; TestId = Split-Path -Leaf $testPath; TestPath = $testPath }
}

function Get-PublicDemoBootstrapRunSession {
    param([Parameter(Mandatory = $true)][string]$ProjectRoot)
    $hostSession = Ensure-PublicDemoHostSession $ProjectRoot
    if ($hostSession.Created) { return $hostSession.Run }
    $active = Get-PublicDemoRunState $ProjectRoot
    if ($active) { return $active }
    Set-PublicDemoRunState $ProjectRoot $hostSession.Run -AllowHostBoot
    return $hostSession.Run
}
