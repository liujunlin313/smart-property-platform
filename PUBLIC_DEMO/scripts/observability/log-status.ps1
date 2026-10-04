[CmdletBinding()]
param()

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
$config = Get-Content -Raw -LiteralPath (Join-Path $scriptRoot 'observability.config.json') | ConvertFrom-Json
$logRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot $config.logRoot))
Get-ChildItem -LiteralPath $logRoot -Recurse -File -ErrorAction SilentlyContinue |
    Select-Object FullName, Length, LastWriteTime |
    Sort-Object FullName
