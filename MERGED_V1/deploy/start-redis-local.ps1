[CmdletBinding()]
param([string]$EnvironmentFile = '')

$ErrorActionPreference = 'Stop'
$deployDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
if ([string]::IsNullOrWhiteSpace($EnvironmentFile)) {
    $EnvironmentFile = Join-Path $deployDirectory 'local.env.properties'
}
if (-not (Test-Path -LiteralPath $EnvironmentFile) -and (Test-Path -LiteralPath (Join-Path $deployDirectory 'local.env.ps1'))) {
    & (Join-Path $deployDirectory 'convert-local-env.ps1') -TargetFile $EnvironmentFile
}
& (Join-Path $deployDirectory 'scripts\dev\start-redis.ps1') -ConfigFile $EnvironmentFile
exit $LASTEXITCODE
