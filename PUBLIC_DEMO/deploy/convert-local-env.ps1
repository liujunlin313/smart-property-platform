[CmdletBinding()]
param(
    [string]$SourceFile = '',
    [string]$TargetFile = '',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$deployDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
if ([string]::IsNullOrWhiteSpace($SourceFile)) { $SourceFile = Join-Path $deployDirectory 'local.env.ps1' }
if ([string]::IsNullOrWhiteSpace($TargetFile)) { $TargetFile = Join-Path $deployDirectory 'local.env.properties' }
if (-not (Test-Path -LiteralPath $SourceFile)) { throw "Legacy configuration was not found: $SourceFile" }
if ((Test-Path -LiteralPath $TargetFile) -and -not $Force) {
    Write-Host "Shared local configuration already exists: $TargetFile" -ForegroundColor Yellow
    exit 0
}

# Phase 2A Stabilization - one-time migration. Values are never printed.
. $SourceFile
$values = [ordered]@{
    APP_DEV_BOOTSTRAP = 'true'
    APP_DEV_CLIENT_EVENTS_ENABLED = 'true'
    APP_FLYWAY_ENABLED = 'false'
    SPRING_PROFILES_ACTIVE = 'local'
    DB_URL = $env:DB_URL
    DB_USERNAME = $env:DB_USERNAME
    DB_PASSWORD = $env:DB_PASSWORD
    MYSQL_EXECUTABLE = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
    REDIS_HOST = '127.0.0.1'
    REDIS_PORT = '6379'
    REDIS_PASSWORD = ''
    REDIS_EXECUTABLE = ''
    REDIS_CLI_EXECUTABLE = ''
    REDIS_ARGUMENTS = ''
    REDIS_DATA_DIRECTORY = 'deploy\runtime\redis-data'
    MINIO_ENDPOINT = $env:MINIO_ENDPOINT
    MINIO_CONSOLE_PORT = '9001'
    MINIO_ROOT_USER = $env:MINIO_ACCESS_KEY
    MINIO_ROOT_PASSWORD = $env:MINIO_SECRET_KEY
    MINIO_ACCESS_KEY = $env:MINIO_ACCESS_KEY
    MINIO_SECRET_KEY = $env:MINIO_SECRET_KEY
    MINIO_EXECUTABLE = 'C:\Program Files\minio\minio.exe'
    MINIO_DATA_DIRECTORY = $(if ($env:MINIO_DATA_DIRECTORY) { $env:MINIO_DATA_DIRECTORY } else { 'deploy\runtime\minio-data' })
    MINIO_PROPERTY_BUCKET = $(if ($env:MINIO_PROPERTY_BUCKET) { $env:MINIO_PROPERTY_BUCKET } else { 'property-assets' })
    APP_BOOTSTRAP_ADMIN_USERNAME = $env:APP_BOOTSTRAP_ADMIN_USERNAME
    APP_BOOTSTRAP_ADMIN_PASSWORD = $env:APP_BOOTSTRAP_ADMIN_PASSWORD
    APP_BOOTSTRAP_VIEWER_USERNAME = $env:APP_BOOTSTRAP_VIEWER_USERNAME
    APP_BOOTSTRAP_VIEWER_PASSWORD = $env:APP_BOOTSTRAP_VIEWER_PASSWORD
    APP_BOOTSTRAP_RECONCILE_LOCAL = 'true'
    APP_JWT_SECRET_BASE64 = $env:APP_JWT_SECRET_BASE64
    APP_REFRESH_COOKIE_SECURE = $(if ($env:APP_REFRESH_COOKIE_SECURE) { $env:APP_REFRESH_COOKIE_SECURE } else { 'false' })
    APP_ALLOWED_ORIGINS = $(if ($env:APP_ALLOWED_ORIGINS) { $env:APP_ALLOWED_ORIGINS } else { 'http://127.0.0.1:5173,http://localhost:5173' })
}

$required = @('DB_URL','DB_USERNAME','DB_PASSWORD','MINIO_ENDPOINT','MINIO_ROOT_USER','MINIO_ROOT_PASSWORD',
    'APP_BOOTSTRAP_ADMIN_USERNAME','APP_BOOTSTRAP_ADMIN_PASSWORD','APP_BOOTSTRAP_VIEWER_USERNAME','APP_BOOTSTRAP_VIEWER_PASSWORD')
foreach ($name in $required) {
    $value = [string]$values[$name]
    if ([string]::IsNullOrWhiteSpace($value) -or $value.Contains('<CHANGE_ME')) {
        throw "Legacy configuration value $name is missing or still contains a placeholder."
    }
}

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add('# Generated from local.env.ps1 by deploy/convert-local-env.ps1.')
$lines.Add('# Local secrets: ignored by Git. Do not share this file.')
foreach ($entry in $values.GetEnumerator()) {
    $value = if ($null -eq $entry.Value) { '' } else { [string]$entry.Value }
    if ($value.Contains("`r") -or $value.Contains("`n")) { throw "Configuration value $($entry.Key) cannot contain a newline." }
    $lines.Add("$($entry.Key)=$value")
}
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllLines($TargetFile, $lines, $utf8NoBom)
Write-Host "Created shared local configuration: $TargetFile" -ForegroundColor Green
Write-Host 'No secret value was printed. The legacy file was left unchanged as a backup.' -ForegroundColor Green
