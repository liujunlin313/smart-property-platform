[CmdletBinding()]
param([switch]$SkipInstall)

$ErrorActionPreference = 'Stop'
$deployDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDirectory = (Resolve-Path (Join-Path $deployDirectory '..')).Path
$frontendDirectory = Join-Path $projectDirectory 'frontend'
. (Join-Path $deployDirectory 'scripts\dev\common.ps1')
$npm = (Get-Command npm.cmd -ErrorAction Stop).Source

$existingPid = Get-PortOwnerProcessId 5173
if ($existingPid -gt 0) {
    $existingProcess = Get-ProcessDisplayName $existingPid
    Write-DevStatus ERROR ("Port 5173 is already occupied by {0}. It was not reused or stopped." -f $existingProcess)
    throw 'Frontend port 5173 is unavailable.'
}

Push-Location $frontendDirectory
try {
    if (-not $SkipInstall -and -not (Test-Path -LiteralPath (Join-Path $frontendDirectory 'node_modules'))) {
        Write-Host 'Installing the locked frontend dependencies for the first development run...'
        & $npm ci --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) { throw 'npm ci failed.' }
    }
    Write-Host 'Frontend development server: http://127.0.0.1:5173'
    Write-Host 'Vite will hot-update the page when frontend source files change.'
    Write-Host 'API requests are proxied to http://127.0.0.1:8080.'
    & $npm run dev
    $frontendExitCode = $LASTEXITCODE
    if ($frontendExitCode -ne 0) {
        throw "Frontend development process exited with code $frontendExitCode."
    }
}
finally {
    Pop-Location
}
