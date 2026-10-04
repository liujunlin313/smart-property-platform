[CmdletBinding()]
param(
    [string]$EnvironmentFile = '',
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [switch]$AllowDegraded,
    [switch]$SkipEdge
)

$ErrorActionPreference = 'Stop'
$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDirectory = (Resolve-Path (Join-Path $scriptDirectory '..\..')).Path
. (Join-Path $projectDirectory 'deploy\scripts\dev\common.ps1')
if ([string]::IsNullOrWhiteSpace($EnvironmentFile)) {
    $EnvironmentFile = Join-Path $projectDirectory 'deploy\local.env.properties'
}
$EnvironmentFile = Resolve-LocalConfigFile $EnvironmentFile
$properties = Import-LocalProperties $EnvironmentFile

if ((Get-LocalProperty $properties 'APP_FLYWAY_ENABLED' 'false') -ne 'false') {
    throw 'APP_FLYWAY_ENABLED must remain false in the current one-database development workflow.'
}
$databaseUrl = Get-LocalProperty $properties 'DB_URL'
if ($databaseUrl -notmatch '/community_db(?:\?|$)') {
    throw 'DB_URL must target the single community_db database.'
}

Write-Host '[BLOCK A] community_db and frozen schema preflight' -ForegroundColor Cyan
& (Join-Path $projectDirectory 'deploy\scripts\dev\check-mysql.ps1') -ConfigFile $EnvironmentFile
if ($LASTEXITCODE -ne 0) { throw 'MySQL/community_db preflight failed.' }

$api = $BaseUrl.TrimEnd('/') + '/api/v1'
$heartbeat = Invoke-RestMethod -Uri "$api/system/heartbeat" -Method Get -UseBasicParsing
if (-not $heartbeat.mysql.available -or $heartbeat.mysql.name -ne 'community_db') {
    throw 'Heartbeat did not confirm community_db.'
}
if (-not $AllowDegraded -and $heartbeat.status -ne 'UP') {
    throw "Dependency gate expected UP but received $($heartbeat.status)."
}

Write-Host '[BLOCK B] JWT rotation and role boundary' -ForegroundColor Cyan
$adminSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$viewerSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$adminToken = $null
$viewerToken = $null
try {
    $adminBody = @{ username = Get-LocalProperty $properties 'APP_BOOTSTRAP_ADMIN_USERNAME'; password = Get-LocalProperty $properties 'APP_BOOTSTRAP_ADMIN_PASSWORD' } | ConvertTo-Json
    $adminLogin = Invoke-RestMethod -Uri "$api/auth/login" -Method Post -ContentType 'application/json' -Body $adminBody -WebSession $adminSession -UseBasicParsing
    $adminToken = $adminLogin.accessToken
    if ($adminLogin.account.roles -notcontains 'SYSTEM_ADMIN') { throw 'Admin role regression failed.' }
    $summary = Invoke-RestMethod -Uri "$api/dashboard/summary" -Method Get -Headers @{ Authorization = "Bearer $adminToken" } -UseBasicParsing
    if ($null -eq $summary.propertyCount) { throw 'Admin summary read regression failed.' }
    $refreshed = Invoke-RestMethod -Uri "$api/auth/refresh" -Method Post -WebSession $adminSession -UseBasicParsing
    if ($refreshed.accessToken -eq $adminToken) { throw 'JWT refresh did not rotate the access token.' }
    $adminToken = $refreshed.accessToken

    $viewerBody = @{ username = Get-LocalProperty $properties 'APP_BOOTSTRAP_VIEWER_USERNAME'; password = Get-LocalProperty $properties 'APP_BOOTSTRAP_VIEWER_PASSWORD' } | ConvertTo-Json
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        $wrongStatus = 0
        $wrongBody = @{ username = Get-LocalProperty $properties 'APP_BOOTSTRAP_VIEWER_USERNAME'; password = '__phase2a_expected_wrong_password__' } | ConvertTo-Json
        try {
            Invoke-WebRequest -Uri "$api/auth/login" -Method Post -ContentType 'application/json' -Body $wrongBody -UseBasicParsing -ErrorAction Stop | Out-Null
        }
        catch { $wrongStatus = [int]$_.Exception.Response.StatusCode }
        if ($wrongStatus -ne 401) { throw "Wrong-password attempt expected HTTP 401 but received $wrongStatus." }
    }
    $viewerLogin = Invoke-RestMethod -Uri "$api/auth/login" -Method Post -ContentType 'application/json' -Body $viewerBody -WebSession $viewerSession -UseBasicParsing
    $viewerToken = $viewerLogin.accessToken
    if ($viewerLogin.account.roles -notcontains 'DEMO_VIEWER') { throw 'Viewer role regression failed.' }
    [void](Invoke-RestMethod -Uri "$api/dashboard/summary" -Method Get -Headers @{ Authorization = "Bearer $viewerToken" } -UseBasicParsing)
    $writeStatus = 0
    try {
        Invoke-WebRequest -Uri "$api/communities" -Method Post -ContentType 'application/json' -Body '{}' `
            -Headers @{ Authorization = "Bearer $viewerToken" } -UseBasicParsing -ErrorAction Stop | Out-Null
    }
    catch {
        $writeStatus = [int]$_.Exception.Response.StatusCode
    }
    if ($writeStatus -ne 403) { throw "Viewer write expected HTTP 403 but received $writeStatus." }
}
finally {
    if ($adminToken) {
        try { Invoke-RestMethod -Uri "$api/auth/logout" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -WebSession $adminSession -UseBasicParsing | Out-Null } catch { }
    }
    if ($viewerToken) {
        try { Invoke-RestMethod -Uri "$api/auth/logout" -Method Post -Headers @{ Authorization = "Bearer $viewerToken" } -WebSession $viewerSession -UseBasicParsing | Out-Null } catch { }
    }
}

if (-not $SkipEdge) {
    Write-Host '[BLOCK C] Microsoft Edge route and UI-action regression' -ForegroundColor Cyan
    Push-Location (Join-Path $projectDirectory 'frontend')
    try {
        & npm.cmd run test:edge
        if ($LASTEXITCODE -ne 0) { throw 'Microsoft Edge regression failed.' }
    }
    finally { Pop-Location }
}

Write-Host '[PASS] CURRENT DEVELOPMENT REGRESSION' -ForegroundColor Green
Write-Host ("database=community_db schema=V3 flyway=FROZEN status={0} cache={1} minio={2}" -f `
    $heartbeat.status, $heartbeat.cacheMode, $heartbeat.minio.available) -ForegroundColor Green
