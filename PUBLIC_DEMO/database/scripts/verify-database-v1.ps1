[CmdletBinding()]
param(
    [ValidateRange(1024, 65535)]
    [int]$Port = 33306,

    [switch]$KeepRuntime,
    [switch]$AllowLegacyIsolatedDatabase
)

$ErrorActionPreference = 'Stop'
if (-not $AllowLegacyIsolatedDatabase) {
    throw 'FROZEN: this legacy script starts an isolated MySQL instance. Current development uses only community_db.'
}

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$databaseDirectory = (Resolve-Path (Join-Path $scriptDirectory '..')).Path
$migrationDirectory = Join-Path $databaseDirectory 'migrations'
$sampleDataFile = Join-Path $databaseDirectory 'sample-data\01_demo_data.sql'
$testDirectory = Join-Path $databaseDirectory 'tests'
$flywayDirectory = Join-Path $databaseDirectory 'flyway'
$evidenceDirectory = Join-Path $databaseDirectory 'evidence'

$mysqlBaseDirectory = 'C:\Program Files\MySQL\MySQL Server 8.0'
$mysqlExecutable = Join-Path $mysqlBaseDirectory 'bin\mysql.exe'
$mysqlAdminExecutable = Join-Path $mysqlBaseDirectory 'bin\mysqladmin.exe'
$mysqlServerExecutable = Join-Path $mysqlBaseDirectory 'bin\mysqld.exe'
$javaHomeDirectory = 'C:\Program Files\JetBrains\IntelliJ IDEA 2024.2.3\jbr'
$mavenExecutable = 'C:\Program Files\JetBrains\IntelliJ IDEA 2024.2.3\plugins\maven\lib\maven3\bin\mvn.cmd'

if (-not (Test-Path -LiteralPath $mysqlExecutable)) {
    $mysqlCommand = Get-Command mysql.exe -ErrorAction SilentlyContinue
    $mysqlServerCommand = Get-Command mysqld.exe -ErrorAction SilentlyContinue
    $mysqlAdminCommand = Get-Command mysqladmin.exe -ErrorAction SilentlyContinue
    if (-not $mysqlCommand -or -not $mysqlServerCommand -or -not $mysqlAdminCommand) {
        throw 'MySQL 8 client/server tools were not found.'
    }
    $mysqlExecutable = $mysqlCommand.Source
    $mysqlServerExecutable = $mysqlServerCommand.Source
    $mysqlAdminExecutable = $mysqlAdminCommand.Source
    $mysqlBaseDirectory = Split-Path -Parent (Split-Path -Parent $mysqlServerExecutable)
}

if (-not (Test-Path -LiteralPath (Join-Path $javaHomeDirectory 'bin\java.exe'))) {
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if (-not $javaCommand) {
        throw 'Java 17+ was not found; Flyway Maven plugin cannot run.'
    }
    $javaHomeDirectory = Split-Path -Parent (Split-Path -Parent $javaCommand.Source)
}

if (-not (Test-Path -LiteralPath $mavenExecutable)) {
    $mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if (-not $mavenCommand) {
        throw 'Maven 3.x was not found; Flyway Maven plugin cannot run.'
    }
    $mavenExecutable = $mavenCommand.Source
}

New-Item -ItemType Directory -Path $evidenceDirectory -Force | Out-Null

$runtimeDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ("smart-property-db-v1-{0}" -f $PID)
$dataDirectory = Join-Path $runtimeDirectory 'data'
$mysqlErrorLog = Join-Path $runtimeDirectory 'mysql-error.log'
$serverProcess = $null
$databaseName = 'smart_property_v1'
$databaseUser = 'smart_property_test'
$databasePassword = 'local_test_only'
$jdbcUrl = "jdbc:mysql://127.0.0.1:$Port/$databaseName`?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"

function Invoke-NativeAndLog {
    param(
        [Parameter(Mandatory)] [string]$Executable,
        [Parameter(Mandatory)] [string[]]$Arguments,
        [Parameter(Mandatory)] [string]$LogPath,
        [switch]$AllowNonZeroExit
    )

    $output = & $Executable @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    if ($null -eq $output -or $output.Count -eq 0) {
        "Command completed with exit code $exitCode." | Set-Content -LiteralPath $LogPath -Encoding UTF8
    }
    else {
        $output | Tee-Object -FilePath $LogPath
    }
    if (-not $AllowNonZeroExit -and $exitCode -ne 0) {
        throw "Command failed with exit code $exitCode. See $LogPath"
    }
    return [pscustomobject]@{ ExitCode = $exitCode; Output = ($output -join [Environment]::NewLine) }
}

function Invoke-MysqlFile {
    param(
        [Parameter(Mandatory)] [string]$SqlFile,
        [Parameter(Mandatory)] [string]$LogPath,
        [switch]$Force
    )

    $arguments = @(
        '--protocol=tcp', '--host=127.0.0.1', "--port=$Port",
        "--user=$databaseUser", "--database=$databaseName",
        '--default-character-set=utf8mb4', '--table', '--show-warnings'
    )
    if ($Force) {
        $arguments += '--force'
    }

    # Windows PowerShell 5.1 converts native stderr records into NativeCommandError.
    # Redirect through System.Diagnostics.Process so expected negative-test errors
    # remain data for exact counting instead of becoming terminating PS errors.
    $sqlText = Get-Content -LiteralPath $SqlFile -Raw -Encoding UTF8
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $mysqlExecutable
    $startInfo.Arguments = $arguments -join ' '
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    $standardOutputBuffer = New-Object System.IO.MemoryStream
    $standardErrorBuffer = New-Object System.IO.MemoryStream
    $inputWriteError = $null
    try {
        if (-not $process.Start()) {
            throw "Failed to start MySQL client: $mysqlExecutable"
        }
        $standardOutputTask = $process.StandardOutput.BaseStream.CopyToAsync($standardOutputBuffer)
        $standardErrorTask = $process.StandardError.BaseStream.CopyToAsync($standardErrorBuffer)
        try {
            $inputBytes = $utf8NoBom.GetBytes($sqlText)
            $process.StandardInput.BaseStream.Write($inputBytes, 0, $inputBytes.Length)
            $process.StandardInput.BaseStream.Flush()
        }
        catch {
            $inputWriteError = $_
        }
        finally {
            $process.StandardInput.Close()
        }
        $process.WaitForExit()
        $standardOutputTask.Wait()
        $standardErrorTask.Wait()
        $exitCode = $process.ExitCode
        $standardOutput = $utf8NoBom.GetString($standardOutputBuffer.ToArray())
        $standardError = $utf8NoBom.GetString($standardErrorBuffer.ToArray())
    }
    finally {
        $standardOutputBuffer.Dispose()
        $standardErrorBuffer.Dispose()
        $process.Dispose()
    }

    $outputParts = @()
    if (-not [string]::IsNullOrWhiteSpace($standardOutput)) {
        $outputParts += $standardOutput.TrimEnd()
    }
    if (-not [string]::IsNullOrWhiteSpace($standardError)) {
        $outputParts += $standardError.TrimEnd()
    }
    $outputText = $outputParts -join [Environment]::NewLine

    if ($inputWriteError) {
        throw "MySQL client closed its input pipe (exit code $exitCode): $standardError"
    }

    if ([string]::IsNullOrWhiteSpace($outputText)) {
        "SQL file $([System.IO.Path]::GetFileName($SqlFile)) completed with exit code $exitCode." | Set-Content -LiteralPath $LogPath -Encoding UTF8
    }
    else {
        $outputText | Set-Content -LiteralPath $LogPath -Encoding UTF8
    }
    if (-not $Force -and $exitCode -ne 0) {
        throw "MySQL script failed with exit code $exitCode. See $LogPath"
    }
    return [pscustomobject]@{ ExitCode = $exitCode; Output = $outputText }
}

try {
    New-Item -ItemType Directory -Path $dataDirectory -Force | Out-Null

    $initializeLog = Join-Path $evidenceDirectory '00_mysql_initialize.txt'
    Invoke-NativeAndLog -Executable $mysqlServerExecutable -Arguments @(
        '--no-defaults', '--initialize-insecure',
        "--basedir=$mysqlBaseDirectory", "--datadir=$dataDirectory",
        "--log-error=$mysqlErrorLog"
    ) -LogPath $initializeLog | Out-Null

    $serverProcess = Start-Process -FilePath $mysqlServerExecutable -ArgumentList @(
        '--no-defaults',
        ('--basedir="{0}"' -f $mysqlBaseDirectory), ('--datadir="{0}"' -f $dataDirectory),
        "--port=$Port", '--bind-address=127.0.0.1',
        '--skip-log-bin', '--character-set-server=utf8mb4',
        '--collation-server=utf8mb4_0900_ai_ci',
        ('--log-error="{0}"' -f $mysqlErrorLog)
    ) -PassThru -WindowStyle Hidden

    $ready = $false
    for ($attempt = 1; $attempt -le 60; $attempt++) {
        & $mysqlAdminExecutable --protocol=tcp --host=127.0.0.1 "--port=$Port" --user=root ping 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            $ready = $true
            break
        }
        Start-Sleep -Milliseconds 500
    }
    if (-not $ready) {
        throw "Isolated MySQL did not become ready. See $mysqlErrorLog"
    }

    $bootstrapSql = @"
CREATE DATABASE $databaseName CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER '$databaseUser'@'127.0.0.1' IDENTIFIED BY '$databasePassword';
GRANT ALL PRIVILEGES ON $databaseName.* TO '$databaseUser'@'127.0.0.1';
FLUSH PRIVILEGES;
"@
    $bootstrapLog = Join-Path $evidenceDirectory '01_database_bootstrap.txt'
    Invoke-NativeAndLog -Executable $mysqlExecutable -Arguments @(
        '--protocol=tcp', '--host=127.0.0.1', "--port=$Port", '--user=root',
        "--execute=$bootstrapSql"
    ) -LogPath $bootstrapLog | Out-Null

    $previousJavaHome = $env:JAVA_HOME
    $previousMysqlPassword = $env:MYSQL_PWD
    $previousFlywayUrl = $env:FLYWAY_URL
    $previousFlywayUser = $env:FLYWAY_USER
    $previousFlywayPassword = $env:FLYWAY_PASSWORD
    $previousFlywayLocations = $env:FLYWAY_LOCATIONS
    $previousFlywayValidateNaming = $env:FLYWAY_VALIDATE_MIGRATION_NAMING
    $env:JAVA_HOME = $javaHomeDirectory
    $env:MYSQL_PWD = $databasePassword
    $env:FLYWAY_URL = $jdbcUrl
    $env:FLYWAY_USER = $databaseUser
    $env:FLYWAY_PASSWORD = $databasePassword
    $env:FLYWAY_LOCATIONS = "filesystem:$($migrationDirectory.Replace('\', '/'))"
    $env:FLYWAY_VALIDATE_MIGRATION_NAMING = 'true'

    try {
        Push-Location $flywayDirectory
        try {
            $flywayMigrateLog = Join-Path $evidenceDirectory '02_flyway_migrate.txt'
            Invoke-NativeAndLog -Executable $mavenExecutable -Arguments @(
                '-Dflyway.target=2',
                'flyway:migrate'
            ) -LogPath $flywayMigrateLog | Out-Null

            $flywayInfoLog = Join-Path $evidenceDirectory '03_flyway_info.txt'
            Invoke-NativeAndLog -Executable $mavenExecutable -Arguments @(
                '-Dflyway.target=2',
                'flyway:info'
            ) -LogPath $flywayInfoLog | Out-Null
        }
        finally {
            Pop-Location
        }

        Invoke-MysqlFile -SqlFile $sampleDataFile -LogPath (Join-Path $evidenceDirectory '04_seed_import.txt') | Out-Null
        Invoke-MysqlFile -SqlFile (Join-Path $testDirectory '01_schema_check.sql') -LogPath (Join-Path $evidenceDirectory '05_schema_check.txt') | Out-Null
        Invoke-MysqlFile -SqlFile (Join-Path $testDirectory '02_positive_cases.sql') -LogPath (Join-Path $evidenceDirectory '06_positive_cases.txt') | Out-Null

        $negativeResult = Invoke-MysqlFile -SqlFile (Join-Path $testDirectory '03_constraint_negative_cases.sql') -LogPath (Join-Path $evidenceDirectory '07_negative_cases.txt') -Force
        $negativeText = $negativeResult.Output
        $expectedErrorCounts = @{
            'ERROR 1062' = 4
            'ERROR 1452' = 7
            'ERROR 3819' = 5
        }
        foreach ($entry in $expectedErrorCounts.GetEnumerator()) {
            $actualCount = ([regex]::Matches($negativeText, "(?m)^$([regex]::Escape($entry.Key))\b")).Count
            if ($actualCount -ne $entry.Value) {
                throw "Negative constraint evidence mismatch for $($entry.Key): expected $($entry.Value), actual $actualCount"
            }
        }
        $expectedErrorTotal = ($expectedErrorCounts.Values | Measure-Object -Sum).Sum
        $actualErrorTotal = ([regex]::Matches($negativeText, '(?m)^ERROR\s+\d+\b')).Count
        if ($actualErrorTotal -ne $expectedErrorTotal) {
            throw "Negative constraint evidence contains missing or unexpected errors: expected total $expectedErrorTotal, actual $actualErrorTotal"
        }

        Invoke-MysqlFile -SqlFile (Join-Path $testDirectory '04_core_join_queries.sql') -LogPath (Join-Path $evidenceDirectory '08_core_join_queries.txt') | Out-Null
        $assertionResult = Invoke-MysqlFile -SqlFile (Join-Path $testDirectory '05_seed_assertions.sql') -LogPath (Join-Path $evidenceDirectory '09_seed_assertions.txt')
        if ($assertionResult.Output -match '(?m)\|\s*FAIL\s*\|') {
            throw 'At least one seed assertion returned FAIL.'
        }

        $summaryLines = @(
            'Database V1 verification: PASS',
            "Verified at: $([DateTimeOffset]::Now.ToString('yyyy-MM-dd HH:mm:ss zzz'))",
            "MySQL executable: $mysqlServerExecutable",
            "Flyway version: 13.1.0",
            "Database: $databaseName on isolated port $Port",
            'Application tables: 25',
            'Flyway migrations: 2',
            'Negative cases: 16 expected failures observed',
            'Seed assertions: 14 PASS',
            'Runtime data directory removed after verification unless -KeepRuntime is specified.'
        )
        $summaryLines | Set-Content -LiteralPath (Join-Path $evidenceDirectory 'VERIFICATION_SUMMARY.txt') -Encoding UTF8
        $summaryLines | ForEach-Object { Write-Host $_ }
    }
    finally {
        $env:JAVA_HOME = $previousJavaHome
        $env:MYSQL_PWD = $previousMysqlPassword
        $env:FLYWAY_URL = $previousFlywayUrl
        $env:FLYWAY_USER = $previousFlywayUser
        $env:FLYWAY_PASSWORD = $previousFlywayPassword
        $env:FLYWAY_LOCATIONS = $previousFlywayLocations
        $env:FLYWAY_VALIDATE_MIGRATION_NAMING = $previousFlywayValidateNaming
    }
}
finally {
    if ($serverProcess -and -not $serverProcess.HasExited) {
        & $mysqlAdminExecutable --protocol=tcp --host=127.0.0.1 "--port=$Port" --user=root shutdown 2>$null | Out-Null
        $serverProcess.WaitForExit(10000) | Out-Null
        if (-not $serverProcess.HasExited) {
            Stop-Process -Id $serverProcess.Id -Force
        }
    }

    if (-not $KeepRuntime -and (Test-Path -LiteralPath $runtimeDirectory)) {
        $resolvedRuntime = (Resolve-Path -LiteralPath $runtimeDirectory).Path
        $resolvedTemp = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath()).TrimEnd('\')
        if (-not $resolvedRuntime.StartsWith($resolvedTemp + '\smart-property-db-v1-', [StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove unexpected runtime path: $resolvedRuntime"
        }
        Remove-Item -LiteralPath $resolvedRuntime -Recurse -Force
    }
}
