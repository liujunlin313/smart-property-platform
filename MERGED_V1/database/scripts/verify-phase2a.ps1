[CmdletBinding()]
param(
    [ValidateRange(1024, 65535)]
    [int]$DatabasePort = 33307,

    [ValidateRange(1024, 65535)]
    [int]$MinioPort = 39000,

    [ValidateRange(1024, 65535)]
    [int]$ApplicationPort = 38080,

    [string]$MinioExecutable = '',

    [switch]$SkipBuild,
    [switch]$KeepRuntime,
    [switch]$AllowLegacyIsolatedDatabase
)

$ErrorActionPreference = 'Stop'
if (-not $AllowLegacyIsolatedDatabase) {
    throw 'FROZEN: this legacy script creates an isolated database. Current development uses only community_db. Run verify-current-development.ps1 instead.'
}

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$databaseDirectory = (Resolve-Path (Join-Path $scriptDirectory '..')).Path
$projectDirectory = (Resolve-Path (Join-Path $databaseDirectory '..')).Path
$migrationDirectory = Join-Path $databaseDirectory 'migrations'
$sampleDataFile = Join-Path $databaseDirectory 'sample-data\01_demo_data.sql'
$flywayDirectory = Join-Path $databaseDirectory 'flyway'
$backendDirectory = Join-Path $projectDirectory 'backend'
$frontendDirectory = Join-Path $projectDirectory 'frontend'
$evidenceDirectory = Join-Path $databaseDirectory 'evidence-phase2a'

$mysqlBaseDirectory = 'C:\Program Files\MySQL\MySQL Server 8.0'
$mysqlExecutable = Join-Path $mysqlBaseDirectory 'bin\mysql.exe'
$mysqlAdminExecutable = Join-Path $mysqlBaseDirectory 'bin\mysqladmin.exe'
$mysqlServerExecutable = Join-Path $mysqlBaseDirectory 'bin\mysqld.exe'
$javaHomeDirectory = 'C:\Program Files\Android\openjdk\jdk-21.0.8'
$mavenExecutable = 'C:\Users\17737\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd'

function Resolve-RequiredExecutable {
    param(
        [Parameter(Mandatory)] [AllowEmptyString()] [string]$PreferredPath,
        [Parameter(Mandatory)] [string]$CommandName,
        [Parameter(Mandatory)] [string]$FailureMessage
    )
    if (-not [string]::IsNullOrWhiteSpace($PreferredPath) -and (Test-Path -LiteralPath $PreferredPath)) {
        return (Resolve-Path -LiteralPath $PreferredPath).Path
    }
    $command = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }
    throw $FailureMessage
}

$mysqlExecutable = Resolve-RequiredExecutable $mysqlExecutable 'mysql.exe' 'MySQL 8 client was not found.'
$mysqlAdminExecutable = Resolve-RequiredExecutable $mysqlAdminExecutable 'mysqladmin.exe' 'MySQL 8 mysqladmin was not found.'
$mysqlServerExecutable = Resolve-RequiredExecutable $mysqlServerExecutable 'mysqld.exe' 'MySQL 8 server was not found.'
$mysqlBaseDirectory = Split-Path -Parent (Split-Path -Parent $mysqlServerExecutable)
$mavenExecutable = Resolve-RequiredExecutable $mavenExecutable 'mvn.cmd' 'Maven 3.x was not found.'
$npmExecutable = Resolve-RequiredExecutable '' 'npm.cmd' 'Node.js/npm was not found.'
$javaExecutable = Join-Path $javaHomeDirectory 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExecutable)) {
    $javaExecutable = Resolve-RequiredExecutable '' 'java.exe' 'Java 21 was not found.'
    $javaHomeDirectory = Split-Path -Parent (Split-Path -Parent $javaExecutable)
}
if ([string]::IsNullOrWhiteSpace($MinioExecutable)) {
    $MinioExecutable = Join-Path $projectDirectory 'deploy\tools\minio.exe'
}
$MinioExecutable = Resolve-RequiredExecutable $MinioExecutable 'minio.exe' 'MinIO was not found. Run deploy\download-minio.ps1 or pass -MinioExecutable.'

New-Item -ItemType Directory -Path $evidenceDirectory -Force | Out-Null

$runtimeDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ("smart-property-phase2a-{0}" -f $PID)
$mysqlDataDirectory = Join-Path $runtimeDirectory 'mysql-data'
$minioDataDirectory = Join-Path $runtimeDirectory 'minio-data'
$mysqlErrorLog = Join-Path $runtimeDirectory 'mysql-error.log'
$backendOutputLog = Join-Path $evidenceDirectory '05_backend_stdout.txt'
$backendErrorLog = Join-Path $evidenceDirectory '05_backend_stderr.txt'
$minioOutputLog = Join-Path $evidenceDirectory '04_minio_stdout.txt'
$minioErrorLog = Join-Path $evidenceDirectory '04_minio_stderr.txt'

$databaseName = 'smart_property_phase2a'
$databaseUser = 'smart_property_phase2a_app'
$databasePassword = 'Db-' + [Guid]::NewGuid().ToString('N') + '-Aa1!'
$minioAccessKey = 'p2a' + [Guid]::NewGuid().ToString('N').Substring(0, 16)
$minioSecretKey = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
$adminUsername = 'phase2a_admin'
$adminPassword = 'Admin-' + [Guid]::NewGuid().ToString('N') + '-Aa1!'
$viewerUsername = 'phase2a_viewer'
$viewerPassword = 'Viewer-' + [Guid]::NewGuid().ToString('N') + '-Aa1!'
$jwtBytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($jwtBytes)
$jwtSecret = [Convert]::ToBase64String($jwtBytes)
$jdbcUrl = "jdbc:mysql://127.0.0.1:$DatabasePort/$databaseName`?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
$applicationBaseUrl = "http://127.0.0.1:$ApplicationPort"

$mysqlServerProcess = $null
$minioProcess = $null
$backendProcess = $null
$verificationSucceeded = $false

function Invoke-LoggedProcess {
    param(
        [Parameter(Mandatory)] [string]$Executable,
        [Parameter(Mandatory)] [string[]]$Arguments,
        [Parameter(Mandatory)] [string]$WorkingDirectory,
        [Parameter(Mandatory)] [string]$LogPath
    )
    $stdoutPath = $LogPath + '.stdout.tmp'
    $stderrPath = $LogPath + '.stderr.tmp'
    $process = Start-Process -FilePath $Executable -ArgumentList $Arguments -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $stdoutPath -RedirectStandardError $stderrPath -PassThru -Wait -WindowStyle Hidden
    $parts = @()
    if (Test-Path -LiteralPath $stdoutPath) {
        $parts += Get-Content -LiteralPath $stdoutPath -Raw -ErrorAction SilentlyContinue
    }
    if (Test-Path -LiteralPath $stderrPath) {
        $parts += Get-Content -LiteralPath $stderrPath -Raw -ErrorAction SilentlyContinue
    }
    ($parts -join [Environment]::NewLine) | Set-Content -LiteralPath $LogPath -Encoding UTF8
    Remove-Item -LiteralPath $stdoutPath, $stderrPath -Force -ErrorAction SilentlyContinue
    if ($process.ExitCode -ne 0) {
        throw "Command failed with exit code $($process.ExitCode). See $LogPath"
    }
}

function Invoke-MysqlText {
    param(
        [Parameter(Mandatory)] [string]$SqlText,
        [Parameter(Mandatory)] [string]$User,
        [string]$Password,
        [string]$Database,
        [switch]$Batch
    )
    $arguments = @(
        '--protocol=tcp', '--host=127.0.0.1', "--port=$DatabasePort",
        "--user=$User", '--default-character-set=utf8mb4'
    )
    if ($Database) {
        $arguments += "--database=$Database"
    }
    if ($Batch) {
        $arguments += @('--batch', '--skip-column-names')
    }

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $mysqlExecutable
    $startInfo.Arguments = $arguments -join ' '
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    if ($Password) {
        $startInfo.EnvironmentVariables['MYSQL_PWD'] = $Password
    }

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    try {
        if (-not $process.Start()) {
            throw 'Failed to start MySQL client.'
        }
        $outputTask = $process.StandardOutput.ReadToEndAsync()
        $errorTask = $process.StandardError.ReadToEndAsync()
        $bytes = $utf8NoBom.GetBytes($SqlText)
        $process.StandardInput.BaseStream.Write($bytes, 0, $bytes.Length)
        $process.StandardInput.Close()
        $process.WaitForExit()
        $output = $outputTask.Result
        $errorText = $errorTask.Result
        if ($process.ExitCode -ne 0) {
            throw "MySQL failed with exit code $($process.ExitCode): $errorText"
        }
        return $output.Trim()
    }
    finally {
        $process.Dispose()
    }
}

function Assert-MysqlError {
    param(
        [Parameter(Mandatory)] [string]$SqlText,
        [Parameter(Mandatory)] [int]$ExpectedCode,
        [Parameter(Mandatory)] [string]$Label,
        [Parameter(Mandatory)] [string]$User,
        [Parameter(Mandatory)] [string]$Password,
        [Parameter(Mandatory)] [string]$Database
    )
    $failedAsExpected = $false
    try {
        Invoke-MysqlText -SqlText $SqlText -User $User -Password $Password -Database $Database | Out-Null
    }
    catch {
        $message = $_.Exception.Message
        if ($message -notmatch ("\bERROR\s+{0}\b" -f $ExpectedCode)) {
            throw "$Label returned an unexpected MySQL error: $message"
        }
        $failedAsExpected = $true
    }
    if (-not $failedAsExpected) {
        throw "$Label should have failed with MySQL ERROR $ExpectedCode, but the statement succeeded."
    }
}

function Wait-HttpReady {
    param(
        [Parameter(Mandatory)] [string]$Url,
        [Parameter(Mandatory)] [int]$Attempts,
        [Parameter(Mandatory)] [string]$FailureMessage
    )
    for ($attempt = 1; $attempt -le $Attempts; $attempt++) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
            if ([int]$response.StatusCode -ge 200 -and [int]$response.StatusCode -lt 500) {
                return
            }
        }
        catch {
            Start-Sleep -Milliseconds 500
        }
    }
    throw $FailureMessage
}

function Assert-StatusCode {
    param(
        [Parameter(Mandatory)] [scriptblock]$Action,
        [Parameter(Mandatory)] [int]$Expected,
        [Parameter(Mandatory)] [string]$Label
    )
    $actual = $null
    try {
        $response = & $Action
        $actual = [int]$response.StatusCode
    }
    catch {
        if ($_.Exception.Response) {
            $actual = [int]$_.Exception.Response.StatusCode
        }
        else {
            throw
        }
    }
    if ($actual -ne $Expected) {
        throw "$Label expected HTTP $Expected but received $actual"
    }
}

function Invoke-Json {
    param(
        [Parameter(Mandatory)] [string]$Method,
        [Parameter(Mandatory)] [string]$Url,
        [Parameter(Mandatory)] [string]$Token,
        [object]$Body,
        [Microsoft.PowerShell.Commands.WebRequestSession]$WebSession
    )
    $parameters = @{
        Method = $Method
        Uri = $Url
        Headers = @{ Authorization = "Bearer $Token" }
        UseBasicParsing = $true
    }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json; charset=utf-8'
        $parameters.Body = $Body | ConvertTo-Json -Depth 8
    }
    if ($WebSession) {
        $parameters.WebSession = $WebSession
    }
    try {
        return Invoke-RestMethod @parameters
    }
    catch {
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            try {
                $responseBody = $reader.ReadToEnd()
            }
            finally {
                $reader.Dispose()
            }
            throw "JSON request $Method $Url failed with HTTP $([int]$_.Exception.Response.StatusCode): $responseBody"
        }
        throw
    }
}

function Invoke-FileUpload {
    param(
        [Parameter(Mandatory)] [string]$Url,
        [Parameter(Mandatory)] [string]$Token,
        [Parameter(Mandatory)] [string]$FilePath
    )
    Add-Type -AssemblyName System.Net.Http
    $client = New-Object System.Net.Http.HttpClient
    $multipart = New-Object System.Net.Http.MultipartFormDataContent
    try {
        $client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer', $Token)
        $bytes = [System.IO.File]::ReadAllBytes($FilePath)
        $fileContent = New-Object System.Net.Http.ByteArrayContent -ArgumentList @(, $bytes)
        $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse('image/png')
        $multipart.Add($fileContent, 'file', 'phase2a-floor-plan.png')
        $response = $client.PostAsync($Url, $multipart).Result
        $text = $response.Content.ReadAsStringAsync().Result
        if (-not $response.IsSuccessStatusCode) {
            throw "File upload failed with HTTP $([int]$response.StatusCode): $text"
        }
        return $text | ConvertFrom-Json
    }
    finally {
        $multipart.Dispose()
        $client.Dispose()
    }
}

function Invoke-FileDownload {
    param(
        [Parameter(Mandatory)] [string]$Url,
        [Parameter(Mandatory)] [string]$Token
    )
    Add-Type -AssemblyName System.Net.Http
    $client = New-Object System.Net.Http.HttpClient
    try {
        $client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer', $Token)
        $response = $client.GetAsync($Url).Result
        if (-not $response.IsSuccessStatusCode) {
            throw "File download failed with HTTP $([int]$response.StatusCode)"
        }
        return $response.Content.ReadAsByteArrayAsync().Result
    }
    finally {
        $client.Dispose()
    }
}

$environmentNames = @(
    'JAVA_HOME', 'FLYWAY_URL', 'FLYWAY_USER', 'FLYWAY_PASSWORD', 'FLYWAY_LOCATIONS',
    'FLYWAY_VALIDATE_MIGRATION_NAMING', 'DB_URL', 'DB_USERNAME', 'DB_PASSWORD',
    'SERVER_PORT', 'APP_JWT_SECRET_BASE64', 'APP_ALLOWED_ORIGINS',
    'APP_BOOTSTRAP_ADMIN_USERNAME', 'APP_BOOTSTRAP_ADMIN_PASSWORD',
    'APP_BOOTSTRAP_VIEWER_USERNAME', 'APP_BOOTSTRAP_VIEWER_PASSWORD',
    'MINIO_ENDPOINT', 'MINIO_ACCESS_KEY', 'MINIO_SECRET_KEY', 'MINIO_PROPERTY_BUCKET',
    'MINIO_ROOT_USER', 'MINIO_ROOT_PASSWORD', 'MINIO_CI_CD', 'MINIO_BROWSER'
)
$previousEnvironment = @{}
foreach ($name in $environmentNames) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

try {
    New-Item -ItemType Directory -Path $mysqlDataDirectory, $minioDataDirectory -Force | Out-Null

    if (-not $SkipBuild) {
        Invoke-LoggedProcess -Executable $npmExecutable -Arguments @('run', 'build') `
            -WorkingDirectory $frontendDirectory -LogPath (Join-Path $evidenceDirectory '00_frontend_build.txt')
        $env:JAVA_HOME = $javaHomeDirectory
        Invoke-LoggedProcess -Executable $mavenExecutable -Arguments @('-DskipTests', 'package') `
            -WorkingDirectory $backendDirectory -LogPath (Join-Path $evidenceDirectory '01_backend_build.txt')
    }

    Invoke-LoggedProcess -Executable $mysqlServerExecutable -Arguments @(
        '--no-defaults', '--initialize-insecure',
        ('--basedir="{0}"' -f $mysqlBaseDirectory), ('--datadir="{0}"' -f $mysqlDataDirectory),
        ('--log-error="{0}"' -f $mysqlErrorLog)
    ) -WorkingDirectory $runtimeDirectory -LogPath (Join-Path $evidenceDirectory '02_mysql_initialize.txt')

    $mysqlServerProcess = Start-Process -FilePath $mysqlServerExecutable -ArgumentList @(
        '--no-defaults',
        ('--basedir="{0}"' -f $mysqlBaseDirectory), ('--datadir="{0}"' -f $mysqlDataDirectory),
        "--port=$DatabasePort", '--bind-address=127.0.0.1', '--skip-log-bin',
        '--character-set-server=utf8mb4', '--collation-server=utf8mb4_0900_ai_ci',
        ('--log-error="{0}"' -f $mysqlErrorLog)
    ) -PassThru -WindowStyle Hidden

    $mysqlReady = $false
    for ($attempt = 1; $attempt -le 60; $attempt++) {
        $pingProcess = Start-Process -FilePath $mysqlAdminExecutable -ArgumentList @(
            '--protocol=tcp', '--host=127.0.0.1', "--port=$DatabasePort", '--user=root', 'ping'
        ) -PassThru -Wait -WindowStyle Hidden
        if ($pingProcess.ExitCode -eq 0) {
            $mysqlReady = $true
            break
        }
        Start-Sleep -Milliseconds 500
    }
    if (-not $mysqlReady) {
        throw "Isolated MySQL did not become ready. See $mysqlErrorLog"
    }

    $bootstrapSql = @"
CREATE DATABASE $databaseName CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER '$databaseUser'@'127.0.0.1' IDENTIFIED BY '$databasePassword';
GRANT ALL PRIVILEGES ON $databaseName.* TO '$databaseUser'@'127.0.0.1';
FLUSH PRIVILEGES;
"@
    Invoke-MysqlText -SqlText $bootstrapSql -User 'root' | Out-Null

    $env:JAVA_HOME = $javaHomeDirectory
    $env:FLYWAY_URL = $jdbcUrl
    $env:FLYWAY_USER = $databaseUser
    $env:FLYWAY_PASSWORD = $databasePassword
    $env:FLYWAY_LOCATIONS = 'filesystem:' + $migrationDirectory.Replace('\', '/')
    $env:FLYWAY_VALIDATE_MIGRATION_NAMING = 'true'
    Invoke-LoggedProcess -Executable $mavenExecutable -Arguments @('flyway:migrate') `
        -WorkingDirectory $flywayDirectory -LogPath (Join-Path $evidenceDirectory '03_flyway_migrate.txt')

    $sampleSql = Get-Content -LiteralPath $sampleDataFile -Raw -Encoding UTF8
    Invoke-MysqlText -SqlText $sampleSql -User $databaseUser -Password $databasePassword -Database $databaseName | Out-Null

    $migrationVersion = Invoke-MysqlText -SqlText 'SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success = 1;' `
        -User $databaseUser -Password $databasePassword -Database $databaseName -Batch
    if ($migrationVersion -ne '3') {
        throw "Expected Flyway schema version 3, actual $migrationVersion"
    }

    Assert-MysqlError -ExpectedCode 3819 -Label 'partial property ownership' `
        -User $databaseUser -Password $databasePassword -Database $databaseName -SqlText @"
INSERT INTO ownership (
    person_id, property_unit_id, ownership_share, start_date, end_date, certificate_no
) VALUES (1, 4, 0.500000, '2026-08-21', NULL, 'P2A-INVALID-PARTIAL-SHARE');
"@
    Assert-MysqlError -ExpectedCode 1062 -Label 'second current property owner' `
        -User $databaseUser -Password $databasePassword -Database $databaseName -SqlText @"
INSERT INTO ownership (
    person_id, property_unit_id, ownership_share, start_date, end_date, certificate_no
) VALUES (2, 1, 1.000000, '2026-08-21', NULL, 'P2A-INVALID-SECOND-OWNER');
"@
    @(
        'PASS ownership_share must equal 1.000000 (ERROR 3819 captured)',
        'PASS one current owner per property (ERROR 1062 captured)'
    ) | Set-Content -LiteralPath (Join-Path $evidenceDirectory '03b_phase2a_constraint_results.txt') -Encoding UTF8

    $env:MINIO_ROOT_USER = $minioAccessKey
    $env:MINIO_ROOT_PASSWORD = $minioSecretKey
    $env:MINIO_CI_CD = '1'
    $env:MINIO_BROWSER = 'off'
    $minioProcess = Start-Process -FilePath $MinioExecutable -ArgumentList @(
        'server', ('"{0}"' -f $minioDataDirectory), "--address=127.0.0.1:$MinioPort",
        "--console-address=127.0.0.1:$($MinioPort + 1)"
    ) -WorkingDirectory $runtimeDirectory -RedirectStandardOutput $minioOutputLog `
        -RedirectStandardError $minioErrorLog -PassThru -WindowStyle Hidden
    Wait-HttpReady -Url "http://127.0.0.1:$MinioPort/minio/health/live" -Attempts 60 `
        -FailureMessage "MinIO did not become ready. See $minioErrorLog"

    $backendJar = Get-ChildItem -LiteralPath (Join-Path $backendDirectory 'target') `
        -Filter 'smart-property-backend-*.jar' -File | Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
    if (-not $backendJar) {
        throw 'Backend executable JAR was not found.'
    }

    $env:DB_URL = $jdbcUrl
    $env:DB_USERNAME = $databaseUser
    $env:DB_PASSWORD = $databasePassword
    $env:SERVER_PORT = [string]$ApplicationPort
    $env:APP_JWT_SECRET_BASE64 = $jwtSecret
    $env:APP_ALLOWED_ORIGINS = 'http://localhost:5173'
    $env:APP_BOOTSTRAP_ADMIN_USERNAME = $adminUsername
    $env:APP_BOOTSTRAP_ADMIN_PASSWORD = $adminPassword
    $env:APP_BOOTSTRAP_VIEWER_USERNAME = $viewerUsername
    $env:APP_BOOTSTRAP_VIEWER_PASSWORD = $viewerPassword
    $env:MINIO_ENDPOINT = "http://127.0.0.1:$MinioPort"
    $env:MINIO_ACCESS_KEY = $minioAccessKey
    $env:MINIO_SECRET_KEY = $minioSecretKey
    $env:MINIO_PROPERTY_BUCKET = 'property-assets'

    $backendProcess = Start-Process -FilePath $javaExecutable -ArgumentList @('-jar', ('"{0}"' -f $backendJar.FullName)) `
        -WorkingDirectory $backendDirectory -RedirectStandardOutput $backendOutputLog `
        -RedirectStandardError $backendErrorLog -PassThru -WindowStyle Hidden
    Wait-HttpReady -Url "$applicationBaseUrl/actuator/health" -Attempts 120 `
        -FailureMessage "Backend did not become ready. See $backendOutputLog and $backendErrorLog"

    $results = New-Object System.Collections.Generic.List[string]
    $results.Add('PASS single-owner database constraints reject partial and duplicate current ownership')
    $results.Add('PASS frontend bundle served by backend')
    $rootPage = Invoke-WebRequest -Uri "$applicationBaseUrl/" -UseBasicParsing
    if ($rootPage.Content -notmatch '<div id="app"></div>') {
        throw 'Backend root did not serve the Vue production bundle.'
    }
    $loginPage = Invoke-WebRequest -Uri "$applicationBaseUrl/login" -UseBasicParsing
    if ([int]$loginPage.StatusCode -ne 200) {
        throw 'SPA login history route was not refreshable.'
    }

    Assert-StatusCode -Expected 401 -Label 'anonymous API access' -Action {
        Invoke-WebRequest -Uri "$applicationBaseUrl/api/v1/dashboard/summary" -UseBasicParsing
    }
    $results.Add('PASS anonymous API request rejected with 401')

    $loginBody = @{ username = $adminUsername; password = $adminPassword } | ConvertTo-Json
    $adminLogin = Invoke-RestMethod -Method Post -Uri "$applicationBaseUrl/api/v1/auth/login" `
        -ContentType 'application/json' -Body $loginBody -SessionVariable adminSession -UseBasicParsing
    if (-not $adminLogin.accessToken -or $adminLogin.account.roles -notcontains 'SYSTEM_ADMIN') {
        throw 'Administrator login did not return the expected JWT role.'
    }
    $adminToken = $adminLogin.accessToken
    $results.Add('PASS administrator JWT login')

    $viewerLoginBody = @{ username = $viewerUsername; password = $viewerPassword } | ConvertTo-Json
    $viewerLogin = Invoke-RestMethod -Method Post -Uri "$applicationBaseUrl/api/v1/auth/login" `
        -ContentType 'application/json' -Body $viewerLoginBody -SessionVariable viewerSession -UseBasicParsing
    if ($viewerLogin.account.roles -notcontains 'DEMO_VIEWER') {
        throw 'Viewer login did not return the expected role.'
    }
    $viewerToken = $viewerLogin.accessToken
    $results.Add('PASS read-only viewer JWT login')

    $community = Invoke-Json -Method Post -Url "$applicationBaseUrl/api/v1/communities" -Token $adminToken -Body @{
        communityCode = 'P2A-TEST'; communityName = '二期链路测试小区'; addressLine = '北京市海淀区测试路1号'
        longitude = 116.3255000; latitude = 39.9901000; completionYear = 2020; status = 'ACTIVE'; changeReason = 'Phase 2A 自动验证'
    }
    $building = Invoke-Json -Method Post -Url "$applicationBaseUrl/api/v1/buildings" -Token $adminToken -Body @{
        communityId = $community.communityId; buildingNo = 'T1'; buildingName = '测试一号楼'; totalFloors = 18
        longitude = 116.3257000; latitude = 39.9903000; status = 'ACTIVE'; changeReason = 'Phase 2A 自动验证'
    }
    $buildingUnit = Invoke-Json -Method Post -Url "$applicationBaseUrl/api/v1/building-units" -Token $adminToken -Body @{
        buildingId = $building.buildingId; unitNo = '1'; unitName = '一单元'; status = 'ACTIVE'; changeReason = 'Phase 2A 自动验证'
    }
    $property = Invoke-Json -Method Post -Url "$applicationBaseUrl/api/v1/properties" -Token $adminToken -Body @{
        buildingUnitId = $buildingUnit.buildingUnitId; propertyNo = '1801'; floorNo = 18
        grossArea = $null; usableArea = $null; propertyUse = 'RESIDENTIAL'; status = 'ACTIVE'
        occupancyStatus = 'UNKNOWN'; changeReason = '面积暂未填写，应强制进入草稿'
    }
    if ($property.status -ne 'DRAFT' -or $null -ne $property.grossArea) {
        throw 'A property without gross area was not forced into DRAFT state.'
    }
    $results.Add('PASS Community-Building-Unit-Property write chain and NULL-area draft rule')

    $viewerProperty = Invoke-Json -Method Get -Url "$applicationBaseUrl/api/v1/properties/$($property.propertyUnitId)" -Token $viewerToken
    if ($viewerProperty.propertyUnitId -ne $property.propertyUnitId) {
        throw 'Viewer could not read the created property.'
    }
    Assert-StatusCode -Expected 403 -Label 'viewer write access' -Action {
        Invoke-WebRequest -Method Put -Uri "$applicationBaseUrl/api/v1/properties/$($property.propertyUnitId)" `
            -Headers @{ Authorization = "Bearer $viewerToken" } -ContentType 'application/json' `
            -Body (@{ buildingUnitId = $buildingUnit.buildingUnitId; propertyNo = '1801'; version = 0 } | ConvertTo-Json) -UseBasicParsing
    }
    $results.Add('PASS viewer read allowed and write rejected with 403')

    $updatedProperty = Invoke-Json -Method Put -Url "$applicationBaseUrl/api/v1/properties/$($property.propertyUnitId)" -Token $adminToken -Body @{
        buildingUnitId = $buildingUnit.buildingUnitId; propertyNo = '1801'; floorNo = 18
        grossArea = 88.50; usableArea = 70.00; propertyUse = 'RESIDENTIAL'; status = 'ACTIVE'
        occupancyStatus = 'VACANT'; version = 0; changeReason = '补录实测面积'
    }
    if ($updatedProperty.status -ne 'ACTIVE' -or [int]$updatedProperty.version -ne 1) {
        throw 'Valid property update did not activate the property or increment version.'
    }
    Assert-StatusCode -Expected 409 -Label 'stale optimistic-lock update' -Action {
        Invoke-WebRequest -Method Put -Uri "$applicationBaseUrl/api/v1/properties/$($property.propertyUnitId)" `
            -Headers @{ Authorization = "Bearer $adminToken" } -ContentType 'application/json' `
            -Body (@{
                buildingUnitId = $buildingUnit.buildingUnitId; propertyNo = '1801'; floorNo = 18
                grossArea = 89.00; usableArea = 70.00; propertyUse = 'RESIDENTIAL'; status = 'ACTIVE'
                occupancyStatus = 'VACANT'; version = 0; changeReason = '故意提交旧版本'
            } | ConvertTo-Json) -UseBasicParsing
    }
    $results.Add('PASS optimistic locking rejects stale writes with 409')

    $imagePath = Join-Path $runtimeDirectory 'phase2a-floor-plan.png'
    $pngBase64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='
    [System.IO.File]::WriteAllBytes($imagePath, [Convert]::FromBase64String($pngBase64))
    $upload = Invoke-FileUpload -Url "$applicationBaseUrl/api/v1/properties/$($property.propertyUnitId)/floor-plan" `
        -Token $adminToken -FilePath $imagePath
    if (-not $upload.fileObjectId -or $upload.contentType -ne 'image/png') {
        throw 'Floor-plan upload did not return the expected file metadata.'
    }
    $downloaded = Invoke-FileDownload -Url "$applicationBaseUrl/api/v1/properties/$($property.propertyUnitId)/floor-plan" -Token $viewerToken
    if ($downloaded.Length -ne ([System.IO.FileInfo]$imagePath).Length) {
        throw 'Downloaded floor-plan bytes did not match the uploaded file length.'
    }
    $results.Add('PASS MinIO floor-plan upload, metadata persistence and authenticated download')

    $refreshed = Invoke-RestMethod -Method Post -Uri "$applicationBaseUrl/api/v1/auth/refresh" `
        -WebSession $adminSession -UseBasicParsing
    if (-not $refreshed.accessToken -or $refreshed.accessToken -eq $adminToken) {
        throw 'Refresh-token rotation did not issue a new access token.'
    }
    Invoke-RestMethod -Method Post -Uri "$applicationBaseUrl/api/v1/auth/logout" `
        -Headers @{ Authorization = "Bearer $($refreshed.accessToken)" } -WebSession $adminSession -UseBasicParsing | Out-Null
    $results.Add('PASS refresh-token rotation and logout')

    $databaseAssertions = Invoke-MysqlText -SqlText @"
SELECT CONCAT(
  (SELECT COUNT(*) FROM app_account), '|',
  (SELECT COUNT(*) FROM asset_change_audit), '|',
  (SELECT COUNT(*) FROM file_object WHERE status = 'ACTIVE'), '|',
  (SELECT COUNT(*) FROM property_floor_plan), '|',
  (SELECT COUNT(*) FROM property_unit WHERE status = 'DRAFT' AND gross_area IS NULL)
);
"@ -User $databaseUser -Password $databasePassword -Database $databaseName -Batch
    $assertionParts = $databaseAssertions -split '\|'
    if ([int]$assertionParts[0] -lt 2 -or [int]$assertionParts[1] -lt 5 -or [int]$assertionParts[2] -ne 1 -or [int]$assertionParts[3] -ne 1) {
        throw "Database integration assertions failed: $databaseAssertions"
    }
    $results.Add('PASS database account, audit and file-metadata assertions')

    $results | Set-Content -LiteralPath (Join-Path $evidenceDirectory '06_http_chain_results.txt') -Encoding UTF8
    $summary = @(
        'Phase 2A full-chain verification: PASS',
        "Verified at: $([DateTimeOffset]::Now.ToString('yyyy-MM-dd HH:mm:ss zzz'))",
        'Isolation: temporary MySQL and loopback-only MinIO; runtime removed after verification',
        'Flyway schema version: 3',
        'Frontend production build: PASS',
        'Spring Boot executable build: PASS',
        'JWT admin/viewer authorization: PASS',
        'Asset CRUD and optimistic lock: PASS',
        'Single-owner database constraints: PASS',
        'MinIO floor-plan upload/download: PASS',
        'Database metadata/audit assertions: PASS'
    )
    $summary | Set-Content -LiteralPath (Join-Path $evidenceDirectory 'VERIFICATION_SUMMARY.txt') -Encoding UTF8
    $summary | ForEach-Object { Write-Host $_ }
    $verificationSucceeded = $true
}
finally {
    if ($backendProcess -and -not $backendProcess.HasExited) {
        Stop-Process -Id $backendProcess.Id -Force -ErrorAction SilentlyContinue
        $backendProcess.WaitForExit(10000) | Out-Null
    }
    if ($minioProcess -and -not $minioProcess.HasExited) {
        Stop-Process -Id $minioProcess.Id -Force -ErrorAction SilentlyContinue
        $minioProcess.WaitForExit(10000) | Out-Null
    }
    if ($mysqlServerProcess -and -not $mysqlServerProcess.HasExited) {
        Start-Process -FilePath $mysqlAdminExecutable -ArgumentList @(
            '--protocol=tcp', '--host=127.0.0.1', "--port=$DatabasePort", '--user=root', 'shutdown'
        ) -Wait -WindowStyle Hidden -ErrorAction SilentlyContinue
        $mysqlServerProcess.WaitForExit(10000) | Out-Null
        if (-not $mysqlServerProcess.HasExited) {
            Stop-Process -Id $mysqlServerProcess.Id -Force -ErrorAction SilentlyContinue
            $mysqlServerProcess.WaitForExit(10000) | Out-Null
        }
    }

    foreach ($name in $environmentNames) {
        [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process')
    }

    if (-not $KeepRuntime -and (Test-Path -LiteralPath $runtimeDirectory)) {
        $resolvedRuntime = (Resolve-Path -LiteralPath $runtimeDirectory).Path
        $resolvedTemp = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath()).TrimEnd('\')
        if (-not $resolvedRuntime.StartsWith($resolvedTemp + '\smart-property-phase2a-', [StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove unexpected runtime path: $resolvedRuntime"
        }
        $runtimeRemoved = $false
        for ($removeAttempt = 1; $removeAttempt -le 10; $removeAttempt++) {
            try {
                Remove-Item -LiteralPath $resolvedRuntime -Recurse -Force
                $runtimeRemoved = $true
                break
            }
            catch {
                if ($removeAttempt -lt 10) {
                    Start-Sleep -Milliseconds 500
                }
                elseif ($verificationSucceeded) {
                    throw
                }
                else {
                    Write-Warning "Verification failed and the isolated runtime could not be removed: $resolvedRuntime"
                }
            }
        }
    }
}
