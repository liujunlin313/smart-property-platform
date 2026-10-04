[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$PidFile,
    [Parameter(Mandatory = $true)][string]$RunPath
)

$ErrorActionPreference = 'Continue'
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
. (Join-Path $scriptRoot 'run-session.ps1')
$config = Get-Content -Raw -LiteralPath (Join-Path $scriptRoot 'observability.config.json') | ConvertFrom-Json
$PID | Set-Content -NoNewline -LiteralPath $PidFile
$lastSecuritySignature = ''

function Write-JsonLine([string]$path, [object]$record) {
    Add-Content -LiteralPath $path -Value ($record | ConvertTo-Json -Compress -Depth 6) -Encoding utf8
}

function Remove-ExpiredLogs {
    # Historical run directories are immutable during normal collection. Retention is a
    # separately approved archival operation, never an implicit worker-side delete.
    return
}

function Write-SecuritySummary([string]$NginxAccess, [string]$AgentRoot) {
    $today = Join-Path $nginxAccess 'access.jsonl'
    if (-not (Test-Path -LiteralPath $today)) { return }
    $cutoff = (Get-Date).AddMinutes(-[int]$config.securityAlertWindowMinutes)
    $matches = @()
    foreach ($line in Get-Content -LiteralPath $today -Tail 250 -ErrorAction SilentlyContinue) {
        try {
            $row = $line | ConvertFrom-Json
            $time = [datetimeoffset]::Parse($row.timestamp).LocalDateTime
            $isSuspicious = $false
            foreach ($pattern in $config.suspiciousPathPatterns) {
                if ($row.uri -like "*$pattern*") { $isSuspicious = $true; break }
            }
            if ($time -ge $cutoff -and ($isSuspicious -or [int]$row.status -ge 400)) { $matches += $row }
        } catch { }
    }
    if ($matches.Count -lt [int]$config.securityAlertThreshold) { return }
    $signature = ($matches | ForEach-Object { "{0}:{1}" -f $_.request_id, $_.status }) -join ','
    if ($signature -eq $lastSecuritySignature) { return }
    $lastSecuritySignature = $signature
    $summary = [ordered]@{
        timestamp = (Get-Date).ToString('o')
        event = 'SECURITY_ALERT_SUMMARY'
        window_minutes = [int]$config.securityAlertWindowMinutes
        event_count = $matches.Count
        sample_request_ids = @($matches | Select-Object -First 10 -ExpandProperty request_id)
        reasons = @($matches | ForEach-Object { if ([int]$_.status -ge 400) { "HTTP_$($_.status)" } else { 'SUSPICIOUS_PATH' } } | Select-Object -Unique)
    }
    Write-JsonLine (Join-Path $agentRoot 'security-alerts.jsonl') $summary
}

try {
    while ($true) {
        # A collector reconnect is an event. Resolve the current runtime for every
        # cycle; tests retain that runtime and cannot switch this collection target.
        $current = Get-ActivePublicDemoRunSession -ProjectRoot $projectRoot
        $metricsRoot = Join-Path $current.RunPath 'ops\metrics'
        $agentRoot = Join-Path $current.RunPath 'ops\agent'
        $nginxAccess = Join-Path $current.RunPath 'nginx\access'
        New-Item -ItemType Directory -Force -Path $metricsRoot, $agentRoot | Out-Null
        $metricsFile = Join-Path $metricsRoot ("metrics-{0}.jsonl" -f (Get-Date -Format 'yyyyMMdd'))
        $rows = @(& docker stats --no-stream --format '{{json .}}' 2>$null)
        foreach ($row in $rows) {
            if ($row) {
                Write-JsonLine $metricsFile ([ordered]@{ timestamp = (Get-Date).ToString('o'); source = 'docker-stats'; raw = $row })
            }
        }
        $disk = @(& docker system df --format '{{json .}}' 2>$null)
        foreach ($row in $disk) {
            if ($row) {
                Write-JsonLine $metricsFile ([ordered]@{ timestamp = (Get-Date).ToString('o'); source = 'docker-system-df'; raw = $row })
            }
        }
        Write-SecuritySummary $nginxAccess $agentRoot
        Remove-ExpiredLogs
        Start-Sleep -Seconds ([int]$config.metricsIntervalSeconds)
    }
} finally {
    Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
}
