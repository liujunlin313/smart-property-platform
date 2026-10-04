[CmdletBinding()]
param(
    [Parameter(Mandatory)] [int]$OwnerPid,
    [Parameter(Mandatory)] [string]$OwnerToken,
    [Parameter(Mandatory)] [string]$RunId
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')

# Independent safety net for IDEA's red Stop button. It never searches by name
# and only cleans records bearing the exact random token created by this JVM.
while ($null -ne (Get-Process -Id $OwnerPid -ErrorAction SilentlyContinue)) {
    Start-Sleep -Milliseconds 500
}

Write-DevStatus STOP ("Application PID {0} ended; running exact managed-dependency cleanup." -f $OwnerPid)
& (Join-Path $PSScriptRoot 'stop-managed-dependencies.ps1') -RunId $RunId -OwnerToken $OwnerToken
