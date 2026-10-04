[CmdletBinding()]
param(
    [string]$CloudflaredExecutable = '',
    [ValidateRange(1024, 65535)] [int]$LocalPort = 8080
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($CloudflaredExecutable)) {
    $command = Get-Command cloudflared.exe -ErrorAction SilentlyContinue
    if (-not $command) { throw 'cloudflared.exe was not found. Install it before starting a temporary public demo.' }
    $CloudflaredExecutable = $command.Source
}
if (-not (Test-Path -LiteralPath $CloudflaredExecutable)) { throw 'cloudflared.exe path is invalid.' }
$localUrl = "http://127.0.0.1:$LocalPort"
$health = Invoke-WebRequest -Uri "$localUrl/actuator/health" -UseBasicParsing -TimeoutSec 3
if ([int]$health.StatusCode -ne 200) { throw 'The local application is not healthy.' }

Write-Host 'Starting a temporary public URL. Do not share administrator credentials.'
Write-Host 'Stop the tunnel with Ctrl+C immediately after the demonstration.'
& $CloudflaredExecutable tunnel --url $localUrl --protocol http2 --no-autoupdate
exit $LASTEXITCODE
