[CmdletBinding()]
param()

$ErrorActionPreference = 'Continue'
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
. (Join-Path $scriptRoot 'run-session.ps1')
$config = Get-Content -Raw -LiteralPath (Join-Path $scriptRoot 'observability.config.json') | ConvertFrom-Json
$logRoot = Get-PublicDemoLogsRoot $projectRoot
$hostSession = Ensure-PublicDemoHostSession $projectRoot
if ($hostSession.Created) {
    $activeRun = $hostSession.Run
} else {
    $activeRun = Get-PublicDemoRunState $projectRoot
    if (-not $activeRun) { $activeRun = Get-LatestPublicDemoRunSession $projectRoot }
    if (-not $activeRun) { $activeRun = $hostSession.Run }
}
Set-PublicDemoRunState $projectRoot $activeRun -AllowHostBoot:$hostSession.Created
$agentRoot = Get-PublicDemoAgentControlRoot $projectRoot
$lifecyclePid = Join-Path $agentRoot 'bootstrap.pid'
$mutexName = 'Local\SmartPropertyPublicDemoObservability'
New-Item -ItemType Directory -Force -Path $agentRoot | Out-Null

function Write-Lifecycle([string]$event, [string]$detail = '') {
    $file = Join-Path $agentRoot ("agent-lifecycle-{0}.log" -f (Get-Date -Format 'yyyyMMdd'))
    $suffix = if ([string]::IsNullOrWhiteSpace($detail)) { '' } else { " $detail" }
    Add-Content -LiteralPath $file -Value ("{0} {1}{2}" -f (Get-Date).ToString('o'), $event, $suffix) -Encoding utf8
    # Bootstrap/reconnect is lifecycle evidence, not a run classifier. Keep the
    # run selected at bootstrap start; do not invoke a fallback that can replace it.
    $current = $script:activeRun
    $lifecycleRoot = Join-Path $current.RunPath 'ops\lifecycle'
    New-Item -ItemType Directory -Force -Path $lifecycleRoot | Out-Null
    [ordered]@{
        timestamp = (Get-Date).ToString('o')
        event = $event
        detail = $detail
        host_session_id = Get-PublicDemoHostSessionId
        run_id = $current.RunId
    } | ConvertTo-Json -Compress | Add-Content -LiteralPath (Join-Path $lifecycleRoot ("lifecycle-{0}.jsonl" -f (Get-Date -Format 'yyyyMMdd'))) -Encoding utf8
}

function Test-DockerReady {
    $serverVersion = @(& docker version --format '{{.Server.Version}}' 2>$null)
    return $LASTEXITCODE -eq 0 -and $serverVersion.Count -gt 0 -and -not [string]::IsNullOrWhiteSpace($serverVersion[0])
}

function Get-WorkerRunning([string]$name) {
    $pidFile = Join-Path $agentRoot "$name.pid"
    if (-not (Test-Path -LiteralPath $pidFile)) { return $false }
    try {
        $workerPid = [int](Get-Content -Raw -LiteralPath $pidFile)
        return $null -ne (Get-Process -Id $workerPid -ErrorAction SilentlyContinue)
    } catch { return $false }
}

$mutex = New-Object System.Threading.Mutex($false, $mutexName)
if (-not $mutex.WaitOne(0, $false)) {
    Write-Lifecycle 'ALREADY_RUNNING' 'bootstrap_mutex_held'
    exit 0
}

$PID | Set-Content -NoNewline -LiteralPath $lifecyclePid
$wasReady = $false
$attempt = 0
$started = Get-Date
try {
    Write-Lifecycle 'AGENT_START' 'scheduled_or_manual_bootstrap'
    while ($true) {
        if (-not (Test-DockerReady)) {
            $attempt++
            if ($wasReady) { Write-Lifecycle 'DOCKER_CONNECTION_LOST' "attempt=$attempt" }
            else { Write-Lifecycle 'WAITING_FOR_DOCKER' "attempt=$attempt" }
            $wasReady = $false
            $elapsedMinutes = ((Get-Date) - $started).TotalMinutes
            $delay = if ($elapsedMinutes -ge [int]$config.dockerReadySlowAfterMinutes) { [int]$config.dockerReadySlowRetrySeconds } else { [int]$config.dockerReadyRetrySeconds }
            Start-Sleep -Seconds $delay
            continue
        }

        if (-not $wasReady) {
            $event = if ($attempt -eq 0) { 'DOCKER_READY' } else { 'DOCKER_CONNECTION_RESTORED' }
            Write-Lifecycle $event "attempt=$attempt"
            $attempt = 0
        }
        $wasReady = $true

        if (-not (Test-Path -LiteralPath (Join-Path $projectRoot 'compose.yaml')) -or -not (Test-Path -LiteralPath (Join-Path $projectRoot '.env.docker'))) {
            Write-Lifecycle 'AGENT_ERROR' 'compose_or_env_file_missing'
            Start-Sleep -Seconds ([int]$config.dockerReadyRetrySeconds)
            continue
        }
        & docker compose --project-directory $projectRoot --env-file (Join-Path $projectRoot '.env.docker') ps *> $null
        if ($LASTEXITCODE -ne 0) {
            Write-Lifecycle 'AGENT_ERROR' 'compose_project_not_readable'
            Start-Sleep -Seconds ([int]$config.dockerReadyRetrySeconds)
            continue
        }

        $eventsRunning = Get-WorkerRunning 'events-worker'
        $metricsRunning = Get-WorkerRunning 'metrics-worker'
        if ($eventsRunning -and $metricsRunning) {
            Write-Lifecycle 'ALREADY_RUNNING' 'collectors_reused'
        } else {
            $event = if ($eventsRunning -or $metricsRunning) { 'COLLECTOR_RESTART' } else { 'COLLECTOR_START' }
            Write-Lifecycle $event 'starting_existing_collectors'
            & (Join-Path $scriptRoot 'start-observability.ps1') -RunPath $activeRun.RunPath *> $null
            if ($LASTEXITCODE -ne 0) { Write-Lifecycle 'AGENT_ERROR' 'collector_start_failed' }
        }
        Start-Sleep -Seconds 30
    }
} catch {
    Write-Lifecycle 'AGENT_ERROR' $_.Exception.Message
} finally {
    Write-Lifecycle 'AGENT_STOP'
    Remove-Item -LiteralPath $lifecyclePid -Force -ErrorAction SilentlyContinue
    $mutex.ReleaseMutex() | Out-Null
    $mutex.Dispose()
}
