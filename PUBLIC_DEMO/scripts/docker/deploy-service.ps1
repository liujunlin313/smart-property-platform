[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidateSet('backend', 'web')][string]$Service,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
. (Join-Path $projectRoot 'scripts\observability\run-session.ps1')
$envFile = Join-Path $projectRoot '.env.docker'
if (-not (Test-Path -LiteralPath $envFile)) { throw 'Missing .env.docker; deployment aborted.' }

$envLines = Get-Content -LiteralPath $envFile
$deploymentId = 'D{0}-{1}' -f (Get-Date -Format 'yyyyMMdd-HHmmss'), $Service.ToUpperInvariant()
$reason = if ($Service -eq 'backend') { 'MANUAL_DEPLOY_BACKEND' } else { 'MANUAL_DEPLOY_WEB' }
$run = New-PublicDemoRunSession -ProjectRoot $projectRoot -Reason $reason -Trigger 'deploy-service.ps1' -DeploymentId $deploymentId -Service $Service
$observability = Join-Path $projectRoot 'scripts\observability'
& (Join-Path $observability 'stop-observability.ps1') *> $null
try { Stop-ScheduledTask -TaskName 'SmartPropertyPublicDemoObservability' -ErrorAction SilentlyContinue } catch { }
try { Start-ScheduledTask -TaskName 'SmartPropertyPublicDemoObservability' -ErrorAction Stop } catch { Write-Warning 'Observability scheduled task could not be restarted; deployment continues.' }
$journalDirectory = Join-Path $run.RunPath 'ops\deployments'
New-Item -ItemType Directory -Force -Path $journalDirectory | Out-Null

$oldContainerId = (& docker compose --env-file $envFile ps -q $Service 2>$null)
$oldImageId = if ($oldContainerId) { & docker inspect --format '{{.Image}}' $oldContainerId 2>$null } else { '' }
$started = Get-Date

function Write-Journal([string]$event, [hashtable]$extra) {
    $record = [ordered]@{ timestamp = (Get-Date).ToString('o'); event = $event; service = $Service; deployment_id = $deploymentId; old_container_id = $oldContainerId; old_image_id = $oldImageId }
    foreach ($key in $extra.Keys) { $record[$key] = $extra[$key] }
    Add-Content -LiteralPath (Join-Path $journalDirectory 'deployments.jsonl') -Value ($record | ConvertTo-Json -Compress) -Encoding utf8
}

Write-Journal 'DEPLOYMENT_STARTED' @{}
$deploymentReplacement = $false
$runReplacement = $false
$activeRunPathReplacement = $false
$updated = foreach ($line in $envLines) {
    if ($line -match '^PUBLIC_DEMO_LOG_RUN=') { $runReplacement = $true; "PUBLIC_DEMO_LOG_RUN=$($run.RunId)" }
    elseif ($line -match '^PUBLIC_DEMO_ACTIVE_RUN_LOG_ROOT=') { $activeRunPathReplacement = $true; "PUBLIC_DEMO_ACTIVE_RUN_LOG_ROOT=$($run.RunPath)" }
    elseif ($line -match '^PUBLIC_DEMO_DEPLOYMENT_ID=' -and $Service -eq 'backend') { $deploymentReplacement = $true; "PUBLIC_DEMO_DEPLOYMENT_ID=$deploymentId" }
    else { $line }
}
if (-not $runReplacement) { $updated += "PUBLIC_DEMO_LOG_RUN=$($run.RunId)" }
if (-not $activeRunPathReplacement) { $updated += "PUBLIC_DEMO_ACTIVE_RUN_LOG_ROOT=$($run.RunPath)" }
if ($Service -eq 'backend' -and -not $deploymentReplacement) { $updated += "PUBLIC_DEMO_DEPLOYMENT_ID=$deploymentId" }
Set-Content -LiteralPath $envFile -Value $updated -Encoding utf8

try {
    if ($SkipBuild) {
        Write-Journal 'DEPLOYMENT_REUSE_EXISTING_IMAGE' @{}
    } else {
        & docker compose --env-file $envFile build $Service
        if ($LASTEXITCODE -ne 0) { throw "Docker build failed for $Service." }
    }
    # The active runtime is a concrete bind mount. Restart the companion once as
    # part of this same planned deployment so backend and nginx both keep writing
    # to this one runtime; this is not a second lifecycle session.
    $servicesToRefresh = if ($Service -eq 'backend') { @('backend', 'web') } else { @('web', 'backend') }
    foreach ($targetService in $servicesToRefresh) {
        & docker compose --env-file $envFile up -d --no-deps $targetService
        if ($LASTEXITCODE -ne 0) { throw "Docker compose up failed for $targetService." }
    }
    $deadline = (Get-Date).AddMinutes(3)
    do {
        Start-Sleep -Seconds 3
        $candidateContainerId = (& docker compose --env-file $envFile ps -q $Service 2>$null)
        $health = if ($candidateContainerId) { & docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' $candidateContainerId 2>$null } else { 'missing' }
    } while ($health -ne 'healthy' -and (Get-Date) -lt $deadline)
    $newContainerId = (& docker compose --env-file $envFile ps -q $Service)
    $newImageId = (& docker inspect --format '{{.Image}}' $newContainerId)
    $downtimeMs = [int]((Get-Date) - $started).TotalMilliseconds
    Write-Journal 'DEPLOYMENT_COMPLETED' @{ health = $health; new_container_id = $newContainerId; new_image_id = $newImageId; elapsed_ms = $downtimeMs }
    if ($health -ne 'healthy') { throw "$Service did not become healthy within 3 minutes." }
    Write-Host "[OK] $Service deployment $deploymentId healthy; run $($run.RunId) recorded."
} catch {
    Write-Journal 'DEPLOYMENT_FAILED' @{ error = $_.Exception.Message }
    throw
}
