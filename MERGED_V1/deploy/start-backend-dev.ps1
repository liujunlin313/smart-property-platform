[CmdletBinding()]
param(
    [string]$EnvironmentFile = '',
    [ValidateRange(1024, 65535)] [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'
$deployDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDirectory = (Resolve-Path (Join-Path $deployDirectory '..')).Path
$backendDirectory = Join-Path $projectDirectory 'backend'
. (Join-Path $deployDirectory 'scripts\dev\common.ps1')

# Check the application port before Java claims Redis/MinIO. This avoids a
# duplicate backend killing dependencies that belong to the already-running one.
$existingPid = Get-PortOwnerProcessId $Port
if ($existingPid -gt 0) {
    $existingProcess = Get-ProcessDisplayName $existingPid
    Write-DevStatus ERROR ("Port {0} is already occupied by {1}. It was not reused or stopped." -f $Port, $existingProcess)
    throw "Backend port $Port is unavailable."
}
if ([string]::IsNullOrWhiteSpace($EnvironmentFile)) {
    $EnvironmentFile = Join-Path $deployDirectory 'local.env.properties'
}
if (-not (Test-Path -LiteralPath $EnvironmentFile)) {
    $legacy = Join-Path $deployDirectory 'local.env.ps1'
    if (Test-Path -LiteralPath $legacy) {
        & (Join-Path $deployDirectory 'convert-local-env.ps1') -SourceFile $legacy -TargetFile $EnvironmentFile
    }
}
if (-not (Test-Path -LiteralPath $EnvironmentFile)) {
    throw 'Create deploy\local.env.properties from local.env.example.properties first.'
}

$javaHome = 'C:\Program Files\Android\openjdk\jdk-21.0.8'
$maven = 'C:\Users\17737\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd'
if (-not (Test-Path -LiteralPath (Join-Path $javaHome 'bin\java.exe'))) {
    $java = Get-Command java.exe -ErrorAction Stop
    $javaHome = Split-Path -Parent (Split-Path -Parent $java.Source)
}
if (-not (Test-Path -LiteralPath $maven)) {
    $maven = (Get-Command mvn.cmd -ErrorAction Stop).Source
}

# Thin wrapper: Java owns configuration, dependency bootstrap and cleanup.
$env:JAVA_HOME = $javaHome
$env:APP_LOCAL_CONFIG = (Resolve-Path -LiteralPath $EnvironmentFile).Path
$env:SERVER_PORT = [string]$Port
$env:SERVER_ADDRESS = '127.0.0.1'
Write-Host 'Starting the same SmartPropertyApplication main class used by IDEA.' -ForegroundColor Cyan
Write-Host 'Press Ctrl+C to stop Spring Boot and reclaim only dependency PIDs started by this run.' -ForegroundColor Cyan
Push-Location $backendDirectory
try {
    & $maven spring-boot:run
    $backendExitCode = $LASTEXITCODE
    if ($backendExitCode -ne 0) {
        throw "Backend development process exited with code $backendExitCode."
    }
}
finally {
    Pop-Location
}
