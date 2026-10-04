[CmdletBinding()]
param([string]$Version = 'RELEASE.2025-09-07T16-13-09Z')

$ErrorActionPreference = 'Stop'
$deployDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$toolsDirectory = Join-Path $deployDirectory 'tools'
New-Item -ItemType Directory -Path $toolsDirectory -Force | Out-Null
$binaryPath = Join-Path $toolsDirectory 'minio.exe'
$checksumPath = Join-Path $toolsDirectory 'minio.sha256sum'
$baseUrl = 'https://dl.min.io/server/minio/release/windows-amd64/archive'

Invoke-WebRequest -Uri "$baseUrl/minio.$Version.sha256sum" -OutFile $checksumPath
Invoke-WebRequest -Uri "$baseUrl/minio.$Version" -OutFile $binaryPath

$expected = ((Get-Content -LiteralPath $checksumPath -Raw).Trim() -split '\s+')[0].ToLowerInvariant()
$actual = (Get-FileHash -LiteralPath $binaryPath -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actual -ne $expected) {
    throw "MinIO checksum mismatch. Expected $expected but received $actual"
}
Write-Host "MinIO download PASS: $binaryPath"
