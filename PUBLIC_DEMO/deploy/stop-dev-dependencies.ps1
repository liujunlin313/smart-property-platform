[CmdletBinding()]
param([Parameter(Mandatory)] [string]$RunId)

$deployDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
& (Join-Path $deployDirectory 'scripts\dev\stop-managed-dependencies.ps1') -RunId $RunId
