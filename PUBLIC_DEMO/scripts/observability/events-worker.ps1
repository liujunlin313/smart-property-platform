[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$PidFile,
    [Parameter(Mandatory = $true)][string]$RunPath
)

$ErrorActionPreference = 'Continue'
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
. (Join-Path $scriptRoot 'run-session.ps1')
$PID | Set-Content -NoNewline -LiteralPath $PidFile

function Write-EventLine([string]$raw) {
    # Events are lifecycle evidence. They follow the active run and never create a run.
    $current = Get-ActivePublicDemoRunSession -ProjectRoot $projectRoot
    $eventRoot = Join-Path $current.RunPath 'ops\docker-events'
    New-Item -ItemType Directory -Force -Path $eventRoot | Out-Null
    $file = Join-Path $eventRoot ("events-{0}.jsonl" -f (Get-Date -Format 'yyyyMMdd'))
    [ordered]@{
        timestamp = (Get-Date).ToString('o')
        source = 'docker-events'
        run_id = $current.RunId
        raw = $raw
    } | ConvertTo-Json -Compress | Add-Content -LiteralPath $file -Encoding utf8
}

try {
    while ($true) {
        Write-EventLine 'EVENT_STREAM_CONNECTED'
        try {
            & docker events --filter type=container --format '{{json .}}' 2>&1 | ForEach-Object {
                if ($_ -and ($_ -notmatch '^Error response from daemon')) { Write-EventLine $_.ToString() }
            }
        } catch {
            Write-EventLine ('EVENT_STREAM_ERROR: ' + $_.Exception.Message)
        }
        Start-Sleep -Seconds 5
    }
} finally {
    Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
}
