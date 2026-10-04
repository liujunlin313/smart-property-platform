[CmdletBinding()]
param(
    [ValidateRange(1, 100)][int]$ConcurrentUsers = 0,
    [switch]$NoPause
)

$ErrorActionPreference = 'Stop'
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
$scriptFailed = $false

try {
    . (Join-Path $projectRoot 'scripts\observability\run-session.ps1')

    $users = $ConcurrentUsers
    if ($users -eq 0) {
        do {
            $raw = Read-Host '并发虚拟用户数（1-100）'
        } while (-not [int]::TryParse($raw, [ref]$users) -or $users -lt 1 -or $users -gt 100)
    }

    # A load test is evidence beneath the live runtime, never a lifecycle transition.
    $runtime = Get-RequiredPublicDemoRunSession -ProjectRoot $projectRoot
    $test = New-PublicDemoTestResult -ProjectRoot $projectRoot -TestType 'system-load' -Trigger 'system-load-test.ps1'
    $loadId = "load-$($test.TestId)"
    $jsonReport = Join-Path $test.TestPath 'system-load-report.json'
    $htmlReport = Join-Path $test.TestPath 'system-load-report.html'

    & python (Join-Path $scriptRoot 'loadtest_runner.py') `
        --env-file (Join-Path $projectRoot '.env.docker') `
        --report $jsonReport --scenario system --users $users --duration 4 --load-id $loadId
    if ($LASTEXITCODE -ne 0) { throw 'System load test did not complete.' }

    & python (Join-Path $scriptRoot 'render_report.py') `
        --input $jsonReport --output $htmlReport --title 'System load test report'
    if ($LASTEXITCODE -ne 0) { throw 'System load-test HTML report could not be rendered.' }

    $activeAfter = Get-RequiredPublicDemoRunSession -ProjectRoot $projectRoot
    if ($activeAfter.RunId -ne $runtime.RunId) {
        throw "Test contract violation: active runtime changed from $($runtime.RunId) to $($activeAfter.RunId)."
    }

    Write-Host "[OK] System load test completed under active runtime $($runtime.RunId)."
    Write-Host "[OK] JSON result: $jsonReport"
    Write-Host "[OK] Offline HTML report: $htmlReport"
} catch {
    $scriptFailed = $true
    Write-Host "[ERROR] $($_.Exception.Message)" -ForegroundColor Red
} finally {
    if (-not $NoPause) { [void](Read-Host 'Press ENTER to close...') }
}

if ($scriptFailed) { exit 1 }

