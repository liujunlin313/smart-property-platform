[CmdletBinding()]
param([string]$EnvironmentFile = '')

$ErrorActionPreference = 'Stop'
$deployDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDirectory = (Resolve-Path (Join-Path $deployDirectory '..')).Path
if ([string]::IsNullOrWhiteSpace($EnvironmentFile)) {
    $EnvironmentFile = Join-Path $deployDirectory 'local.env.properties'
}
if (-not (Test-Path -LiteralPath $EnvironmentFile)) {
    throw "Missing local configuration: $EnvironmentFile"
}

$version = '5.0.14.1'
$expectedSha256 = '018EA18A35876383CBB5F4CD0258ADFC87747CF9D619BCE1CF73A2E36F720CCF'
$downloadDirectory = Join-Path $deployDirectory 'runtime\downloads'
$toolDirectory = Join-Path $deployDirectory "runtime\tools\redis-$version"
$archive = Join-Path $downloadDirectory "Redis-x64-$version.zip"
$server = Join-Path $toolDirectory 'redis-server.exe'
$cli = Join-Path $toolDirectory 'redis-cli.exe'
New-Item -ItemType Directory -Path $downloadDirectory -Force | Out-Null

if (-not (Test-Path -LiteralPath $archive)) {
    Write-Host "Downloading pinned Redis for Windows $version..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri "https://github.com/tporadowski/redis/releases/download/v$version/Redis-x64-$version.zip" `
        -OutFile $archive -UseBasicParsing
}
$actualSha256 = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash
if ($actualSha256 -ne $expectedSha256) {
    throw "Redis archive checksum mismatch. Expected $expectedSha256 but received $actualSha256."
}
if (-not (Test-Path -LiteralPath $server)) {
    New-Item -ItemType Directory -Path $toolDirectory -Force | Out-Null
    Expand-Archive -LiteralPath $archive -DestinationPath $toolDirectory -Force
}
if (-not (Test-Path -LiteralPath $server) -or -not (Test-Path -LiteralPath $cli)) {
    throw 'Redis extraction did not produce the expected executables.'
}

$lines = Get-Content -LiteralPath $EnvironmentFile -Encoding UTF8
$updates = [ordered]@{
    REDIS_EXECUTABLE = $server
    REDIS_CLI_EXECUTABLE = $cli
    REDIS_DATA_DIRECTORY = 'deploy\runtime\redis-data'
}
foreach ($entry in $updates.GetEnumerator()) {
    $prefix = $entry.Key + '='
    $found = $false
    for ($index = 0; $index -lt $lines.Count; $index++) {
        if ($lines[$index].StartsWith($prefix, [StringComparison]::Ordinal)) {
            $lines[$index] = $prefix + $entry.Value
            $found = $true
            break
        }
    }
    if (-not $found) { $lines += $prefix + $entry.Value }
}
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllLines($EnvironmentFile, $lines, $utf8NoBom)
Write-Host "Redis local runtime ready: $server" -ForegroundColor Green
Write-Host 'Only executable paths were updated; no credential value was printed.' -ForegroundColor Green
