[CmdletBinding()]
param([string]$ConfigFile = '')

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')

$ConfigFile = Resolve-LocalConfigFile $ConfigFile
$properties = Import-LocalProperties $ConfigFile
$jdbcUrl = Get-LocalProperty $properties 'DB_URL'
$username = Get-LocalProperty $properties 'DB_USERNAME'
$password = Get-LocalProperty $properties 'DB_PASSWORD'
$mysql = Get-LocalProperty $properties 'MYSQL_EXECUTABLE' 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'

Write-DevStatus TITLE 'MYSQL'
if ($jdbcUrl -notmatch '^jdbc:mysql://([^:/?]+)(?::([0-9]+))?/([^?]+)') {
    Write-DevStatus ERROR 'DB_URL is not a supported MySQL JDBC URL.'
    exit 20
}
$databaseHost = $Matches[1]
$databasePort = if ([string]::IsNullOrWhiteSpace($Matches[2])) { 3306 } else { [int]$Matches[2] }
$databaseName = $Matches[3]
if (-not (Test-Path -LiteralPath $mysql)) {
    $command = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($null -ne $command) { $mysql = $command.Source }
}
if (-not (Test-Path -LiteralPath $mysql)) {
    Write-DevStatus ERROR 'mysql.exe was not found; set MYSQL_EXECUTABLE in local.env.properties.'
    exit 20
}

$startInfo = New-Object System.Diagnostics.ProcessStartInfo
$startInfo.FileName = $mysql
$startInfo.Arguments = @(
    '--protocol=tcp', "--host=$databaseHost", "--port=$databasePort", "--user=$username",
    "--database=$databaseName", '--batch', '--skip-column-names', '--default-character-set=utf8mb4',
    '--execute="SELECT DATABASE(), COALESCE((SELECT MAX(version) FROM flyway_schema_history), ''NONE'');"'
) -join ' '
$startInfo.UseShellExecute = $false
$startInfo.CreateNoWindow = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
$startInfo.EnvironmentVariables['MYSQL_PWD'] = $password
$process = New-Object System.Diagnostics.Process
$process.StartInfo = $startInfo
try {
    [void]$process.Start()
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) {
        Write-DevStatus ERROR ("MySQL connection failed at {0}:{1}/{2}: {3}" -f $databaseHost, $databasePort, $databaseName, $stderr.Trim())
        exit 20
    }
    $parts = $stdout.Trim() -split "`t"
    $schemaVersion = if ($parts.Count -gt 1) { $parts[1] } else { 'UNKNOWN' }
    Write-DevStatus OK ("{0}:{1}" -f $databaseHost, $databasePort)
    Write-DevStatus OK ("database = {0}" -f $databaseName)
    Write-DevStatus OK ("frozen schema baseline = V{0}" -f $schemaVersion)
    exit 0
}
finally {
    $process.Dispose()
}
