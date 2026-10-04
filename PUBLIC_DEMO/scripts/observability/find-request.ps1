[CmdletBinding()]
param([Parameter(Mandatory = $true)][string]$RequestId)

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
$config = Get-Content -Raw -LiteralPath (Join-Path $scriptRoot 'observability.config.json') | ConvertFrom-Json
$logRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot $config.logRoot))
Write-Host "Request timeline: $RequestId"
Get-ChildItem -LiteralPath $logRoot -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Extension -in '.log', '.jsonl' } |
    Select-String -SimpleMatch -Pattern $RequestId -ErrorAction SilentlyContinue |
    Select-Object Path, LineNumber, Line
