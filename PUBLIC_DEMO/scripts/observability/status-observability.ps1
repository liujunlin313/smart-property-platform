[CmdletBinding()]
param()

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
. (Join-Path $scriptRoot 'run-session.ps1')
$activeRun = Get-ActivePublicDemoRunSession -ProjectRoot $projectRoot
$agentLog = Get-PublicDemoAgentControlRoot $projectRoot
Write-Host "active run       $($activeRun.RunId)"
foreach ($name in @('bootstrap')) {
    $pidFile = Join-Path $agentLog "$name.pid"
    $pidValue = if (Test-Path -LiteralPath $pidFile) { [int](Get-Content -Raw -LiteralPath $pidFile) } else { $null }
    $running = $pidValue -and (Get-Process -Id $pidValue -ErrorAction SilentlyContinue)
    Write-Host ("{0,-16} {1}" -f $name, ($(if ($running) { "RUNNING (PID $pidValue)" } else { 'STOPPED' })))
}
foreach ($name in @('events-worker', 'metrics-worker')) {
    $pidFile = Join-Path $agentLog "$name.pid"
    $pidValue = if (Test-Path -LiteralPath $pidFile) { [int](Get-Content -Raw -LiteralPath $pidFile) } else { $null }
    $running = $pidValue -and (Get-Process -Id $pidValue -ErrorAction SilentlyContinue)
    Write-Host ("{0,-16} {1}" -f $name, ($(if ($running) { "RUNNING (PID $pidValue)" } else { 'STOPPED' })))
}
