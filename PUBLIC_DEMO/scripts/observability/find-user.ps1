[CmdletBinding()]
param([Parameter(Mandatory = $true)][string]$Username)

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
$config = Get-Content -Raw -LiteralPath (Join-Path $scriptRoot 'observability.config.json') | ConvertFrom-Json
$logRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot $config.logRoot))
$pattern = '"username":"' + [regex]::Escape($Username) + '"'
Get-ChildItem -LiteralPath (Join-Path $logRoot 'audit') -File -Filter *.jsonl -ErrorAction SilentlyContinue |
    Select-String -Pattern $pattern -ErrorAction SilentlyContinue |
    Select-Object Path, LineNumber, Line
