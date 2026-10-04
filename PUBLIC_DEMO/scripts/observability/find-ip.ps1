[CmdletBinding()]
param([Parameter(Mandatory = $true)][string]$Ip)

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
$config = Get-Content -Raw -LiteralPath (Join-Path $scriptRoot 'observability.config.json') | ConvertFrom-Json
$logRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot $config.logRoot))
Get-ChildItem -LiteralPath (Join-Path $logRoot 'nginx\access') -File -Filter *.jsonl -ErrorAction SilentlyContinue |
    Select-String -SimpleMatch -Pattern $Ip -ErrorAction SilentlyContinue |
    Select-Object Path, LineNumber, Line
