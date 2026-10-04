[CmdletBinding()]
param(
    [Parameter(Mandatory)] [string]$RunId,
    [string]$OwnerToken = ''
)

$ErrorActionPreference = 'Continue'
. (Join-Path $PSScriptRoot 'common.ps1')

$runtime = Get-DevRunDirectory $RunId
foreach ($service in @('minio', 'redis')) {
    $recordPath = Join-Path $runtime ($service + '.json')
    Stop-ExactManagedProcess -RecordPath $recordPath -OwnerToken $OwnerToken
}
