[CmdletBinding()]
param(
    [string]$EnvironmentFile = '',
    [ValidateRange(1024, 65535)] [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'
$deployDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDirectory = (Resolve-Path (Join-Path $deployDirectory '..')).Path
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
    throw "Missing $EnvironmentFile. Copy local.env.example.properties and fill local development values."
}

$env:APP_LOCAL_CONFIG = (Resolve-Path -LiteralPath $EnvironmentFile).Path
$env:SERVER_PORT = [string]$Port
$env:SERVER_ADDRESS = '127.0.0.1'
$jar = Get-ChildItem -LiteralPath (Join-Path $projectDirectory 'backend\target') `
    -Filter 'smart-property-backend-*.jar' -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
if (-not $jar) {
    throw 'Backend JAR is missing. Run deploy\build-local.ps1 first.'
}

$java = Join-Path 'C:\Program Files\Android\openjdk\jdk-21.0.8' 'bin\java.exe'
if (-not (Test-Path -LiteralPath $java)) {
    $java = (Get-Command java.exe -ErrorAction Stop).Source
}
Write-Host "Starting packaged application on http://127.0.0.1:$Port"
Write-Host 'Java uses the same local config and exact-PID dependency cleanup as IDEA.'
& $java -jar $jar.FullName
exit $LASTEXITCODE
