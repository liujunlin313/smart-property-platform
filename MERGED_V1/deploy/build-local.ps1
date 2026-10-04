[CmdletBinding()]
param([switch]$SkipNpmInstall)

$ErrorActionPreference = 'Stop'
$deployDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDirectory = (Resolve-Path (Join-Path $deployDirectory '..')).Path
$frontendDirectory = Join-Path $projectDirectory 'frontend'
$backendDirectory = Join-Path $projectDirectory 'backend'

$javaHome = 'C:\Program Files\Android\openjdk\jdk-21.0.8'
$maven = 'C:\Users\17737\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd'
if (-not (Test-Path -LiteralPath (Join-Path $javaHome 'bin\java.exe'))) {
    $java = Get-Command java.exe -ErrorAction Stop
    $javaHome = Split-Path -Parent (Split-Path -Parent $java.Source)
}
if (-not (Test-Path -LiteralPath $maven)) {
    $maven = (Get-Command mvn.cmd -ErrorAction Stop).Source
}
$npm = (Get-Command npm.cmd -ErrorAction Stop).Source

if (-not $SkipNpmInstall) {
    Push-Location $frontendDirectory
    try { & $npm ci --no-audit --no-fund; if ($LASTEXITCODE -ne 0) { throw 'npm ci failed.' } }
    finally { Pop-Location }
}

Push-Location $frontendDirectory
try { & $npm run build; if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' } }
finally { Pop-Location }

$env:JAVA_HOME = $javaHome
Push-Location $backendDirectory
try { & $maven -DskipTests package; if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' } }
finally { Pop-Location }

Write-Host 'Build PASS: frontend/dist and backend executable JAR are ready.'
