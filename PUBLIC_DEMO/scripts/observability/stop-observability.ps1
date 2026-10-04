[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
. (Join-Path $scriptRoot 'run-session.ps1')
$agentRoots = @(
    (Get-PublicDemoAgentControlRoot $projectRoot),
    (Join-Path (Get-PublicDemoLogsRoot $projectRoot) 'agent-control')
) | Select-Object -Unique

foreach ($agentLog in $agentRoots) {
    foreach ($name in @('bootstrap', 'events-worker', 'metrics-worker')) {
        $pidFile = Join-Path $agentLog "$name.pid"
        if (-not (Test-Path -LiteralPath $pidFile)) { continue }
        try { $managedPid = [int](Get-Content -Raw -LiteralPath $pidFile) } catch { $managedPid = $null }
        $process = if ($managedPid) { Get-Process -Id $managedPid -ErrorAction SilentlyContinue } else { $null }
        if ($process) {
            Stop-Process -Id $managedPid -ErrorAction Stop
            Write-Host "[OK] Stopped $name (PID $managedPid)."
        }
        Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
    }
}

# Older Stage 2.5 workers stored their PID files inside individual historical
# runs. Reclaim only PowerShell processes whose command line is this project's
# observability folder; never touch unrelated PowerShell processes.
$observabilityPath = (Join-Path $projectRoot 'scripts\observability\').ToLowerInvariant()
$legacyProcesses = Get-CimInstance Win32_Process -Filter "Name='powershell.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.ProcessId -ne $PID -and $_.CommandLine -and $_.CommandLine.ToLowerInvariant().Contains($observabilityPath) }
foreach ($legacy in $legacyProcesses) {
    $process = Get-Process -Id $legacy.ProcessId -ErrorAction SilentlyContinue
    if ($process) {
        Stop-Process -Id $legacy.ProcessId -ErrorAction Stop
        Write-Host "[OK] Stopped legacy project observability process (PID $($legacy.ProcessId))."
    }
}
