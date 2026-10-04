[CmdletBinding()]
param([int]$Tail = 100)

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
$config = Get-Content -Raw -LiteralPath (Join-Path $scriptRoot 'observability.config.json') | ConvertFrom-Json
$logRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot $config.logRoot))
Get-ChildItem -LiteralPath $logRoot -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Extension -in '.log', '.jsonl' } |
    Select-String -Pattern 'ERROR|WARN|"status":(4|5)[0-9]{2}|LOGIN_FAILURE|ACCESS_DENIED' -ErrorAction SilentlyContinue |
    Select-Object -Last $Tail Path, LineNumber, Line
