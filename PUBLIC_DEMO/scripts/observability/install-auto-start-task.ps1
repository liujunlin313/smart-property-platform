[CmdletBinding()]
param(
    [switch]$Remove
)

$ErrorActionPreference = 'Stop'
$taskName = 'SmartPropertyPublicDemoObservability'
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$bootstrap = Join-Path $scriptRoot 'observability-bootstrap.ps1'
if ($Remove) {
    Unregister-ScheduledTask -TaskName $taskName -Confirm:$false -ErrorAction SilentlyContinue
    Write-Host "[OK] Removed user task $taskName."
    exit 0
}
if (-not (Test-Path -LiteralPath $bootstrap)) { throw 'Observability bootstrap script is missing.' }

$currentUser = "{0}\{1}" -f $env:USERDOMAIN, $env:USERNAME
$powershell = Join-Path $env:WINDIR 'System32\WindowsPowerShell\v1.0\powershell.exe'
$arguments = "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$bootstrap`""
$action = New-ScheduledTaskAction -Execute $powershell -Argument $arguments
$trigger = New-ScheduledTaskTrigger -AtLogOn -User $currentUser
$principal = New-ScheduledTaskPrincipal -UserId $currentUser -LogonType Interactive -RunLevel Limited
$settings = New-ScheduledTaskSettingsSet -MultipleInstances IgnoreNew -ExecutionTimeLimit ([TimeSpan]::Zero) -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
$task = New-ScheduledTask -Action $action -Trigger $trigger -Principal $principal -Settings $settings -Description 'Starts only the Smart Property PUBLIC_DEMO observability bootstrap after this user logs on.'
Register-ScheduledTask -TaskName $taskName -InputObject $task -Force | Out-Null
Write-Host "[OK] Registered current-user logon task $taskName."
