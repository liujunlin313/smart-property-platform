[CmdletBinding()]
param(
    [switch]$Foreground,
    [string]$RunPath
)

$ErrorActionPreference = 'Stop'
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
. (Join-Path $scriptRoot 'run-session.ps1')
if ([string]::IsNullOrWhiteSpace($RunPath)) { $RunPath = (Get-ActivePublicDemoRunSession -ProjectRoot $projectRoot).RunPath }
$agentLog = Get-PublicDemoAgentControlRoot $projectRoot
New-Item -ItemType Directory -Force -Path $agentLog | Out-Null

function Start-Worker([string]$name, [string]$file) {
    $pidFile = Join-Path $agentLog "$name.pid"
    if (Test-Path -LiteralPath $pidFile) {
        $oldPid = [int](Get-Content -Raw -LiteralPath $pidFile)
        if (Get-Process -Id $oldPid -ErrorAction SilentlyContinue) {
            Write-Host "[INFO] $name is already running (PID $oldPid)."
            return
        }
        Remove-Item -LiteralPath $pidFile -Force
    }
    $workerArguments = "-NoProfile -ExecutionPolicy Bypass -File `"$file`" -PidFile `"$pidFile`" -RunPath `"$RunPath`""
    if ($Foreground) {
        & powershell.exe $workerArguments
    } else {
        $process = Start-Process -FilePath powershell.exe -ArgumentList $workerArguments -WindowStyle Hidden -PassThru
        $process.Id | Set-Content -NoNewline -LiteralPath $pidFile
        Write-Host "[OK] Started $name (PID $($process.Id))."
    }
}

Start-Worker 'events-worker' (Join-Path $scriptRoot 'events-worker.ps1')
Start-Worker 'metrics-worker' (Join-Path $scriptRoot 'metrics-worker.ps1')
Write-Host "[OK] Observability agent is running. Run: $RunPath"
