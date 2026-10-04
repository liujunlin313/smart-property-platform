[CmdletBinding()]
param(
    [ValidateRange(1, 100)][int]$ConcurrentUsers = 0,
    [switch]$NoPause
)

$ErrorActionPreference = 'Stop'
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptRoot)
$envFile = Join-Path $projectRoot '.env.docker'
$scriptFailed = $false

function Get-DatabaseCounters {
    param([string]$ProjectRoot, [string]$EnvFile)
    # Emits only selected aggregate counters and row counts; no business values or credentials.
    $query = @'
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -N -uroot -e "SHOW GLOBAL STATUS WHERE Variable_name IN ('Com_select','Com_insert','Com_update','Com_commit','Com_rollback','Innodb_rows_read','Innodb_rows_inserted','Innodb_rows_updated','Innodb_row_lock_time','Slow_queries','Threads_connected','Max_used_connections','Innodb_deadlocks'); SELECT 'table:property_unit', COUNT(*) FROM community_db.property_unit UNION ALL SELECT 'table:person', COUNT(*) FROM community_db.person UNION ALL SELECT 'table:app_account', COUNT(*) FROM community_db.app_account UNION ALL SELECT 'table:bill', COUNT(*) FROM community_db.bill;"
'@
    # Windows PowerShell 5.1 can split nested shell quotes before Docker receives
    # them. Base64 carries this fixed, non-secret command as one simple argument.
    $encodedQuery = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($query))
    $containerCommand = "echo $encodedQuery | base64 -d | sh"
    $lines = & docker compose --project-directory $ProjectRoot --env-file $EnvFile exec -T mysql sh -lc $containerCommand
    if ($LASTEXITCODE -ne 0) { throw 'Could not read selected MySQL aggregate counters.' }
    $result = @{}
    foreach ($line in $lines) {
        $pair = ([string]$line).Trim() -split '\s+', 2
        if ($pair.Count -eq 2) { $result[$pair[0]] = [int64]$pair[1] }
    }
    return $result
}

function Get-CounterValue {
    param([hashtable]$Counters, [string]$Name)
    if ($Counters.ContainsKey($Name) -and $null -ne $Counters[$Name]) { return [int64]$Counters[$Name] }
    return [int64]0
}

function Get-CounterDelta {
    param([hashtable]$Before, [hashtable]$After, [string]$Name)
    return (Get-CounterValue $After $Name) - (Get-CounterValue $Before $Name)
}

try {
    . (Join-Path $projectRoot 'scripts\observability\run-session.ps1')

    $users = $ConcurrentUsers
    if ($users -eq 0) {
        do {
            $raw = Read-Host '并发虚拟用户数（1-100）'
        } while (-not [int]::TryParse($raw, [ref]$users) -or $users -lt 1 -or $users -gt 100)
    }

    # Tests are stored under the existing runtime and cannot replace its active-run pointer.
    $runtime = Get-RequiredPublicDemoRunSession -ProjectRoot $projectRoot
    $test = New-PublicDemoTestResult -ProjectRoot $projectRoot -TestType 'database-load' -Trigger 'database-load-test.ps1'
    $loadId = "load-$($test.TestId)"
    $jsonReport = Join-Path $test.TestPath 'database-load-report.json'
    $htmlReport = Join-Path $test.TestPath 'database-load-report.html'
    $before = Get-DatabaseCounters -ProjectRoot $projectRoot -EnvFile $envFile

    & python (Join-Path $scriptRoot 'loadtest_runner.py') `
        --env-file $envFile --report $jsonReport --scenario database --users $users --duration 4 --load-id $loadId
    if ($LASTEXITCODE -ne 0) { throw 'Database load test did not complete.' }

    Start-Sleep -Seconds 1
    $after = Get-DatabaseCounters -ProjectRoot $projectRoot -EnvFile $envFile

    # Database telemetry is primary evidence. Snapshots stay in memory and become safe deltas only.
    $operationFiles = @(Get-ChildItem -LiteralPath (Join-Path $runtime.RunPath 'database') -Filter '*.jsonl' -File -ErrorAction SilentlyContinue)
    $operations = foreach ($file in $operationFiles) {
        foreach ($line in Get-Content -LiteralPath $file.FullName) {
            try {
                $event = $line | ConvertFrom-Json
                if ($event.event -eq 'DATABASE_OPERATION' -and $event.load_test_id -eq $loadId) { $event }
            } catch { }
        }
    }
    if (@($operations).Count -eq 0) {
        throw "No recorded database operations were found for $loadId in the active runtime telemetry."
    }

    $operationSummary = @(
        $operations | Group-Object {
            "$($_.operation)|$((@($_.tables) -join ', '))|$((@($_.changed_fields) -join ', '))"
        } | ForEach-Object {
            $sample = $_.Group | Select-Object -First 1
            $durations = @($_.Group | ForEach-Object { if ($null -ne $_.duration_ms) { [double]$_.duration_ms } })
            $affected = @($_.Group | ForEach-Object { if ($null -ne $_.affected_rows) { [int64]$_.affected_rows } })
            $returned = @($_.Group | ForEach-Object { if ($null -ne $_.returned_rows) { [int64]$_.returned_rows } })
            [ordered]@{
                operation = $sample.operation
                tables = @($sample.tables)
                changed_fields = @($sample.changed_fields)
                count = $_.Count
                average_duration_ms = if ($durations.Count -gt 0) { [math]::Round((($durations | Measure-Object -Average).Average), 2) } else { 0 }
                affected_rows = if ($affected.Count -gt 0) { [int64](($affected | Measure-Object -Sum).Sum) } else { [int64]0 }
                returned_rows = if ($returned.Count -gt 0) { [int64](($returned | Measure-Object -Sum).Sum) } else { [int64]0 }
            }
        }
    )

    $result = Get-Content -Raw $jsonReport | ConvertFrom-Json
    $databaseMetrics = [ordered]@{
        select_delta = Get-CounterDelta $before $after 'Com_select'
        insert_delta = Get-CounterDelta $before $after 'Com_insert'
        update_delta = Get-CounterDelta $before $after 'Com_update'
        commit_delta = Get-CounterDelta $before $after 'Com_commit'
        rollback_delta = Get-CounterDelta $before $after 'Com_rollback'
        rows_read_delta = Get-CounterDelta $before $after 'Innodb_rows_read'
        rows_inserted_delta = Get-CounterDelta $before $after 'Innodb_rows_inserted'
        rows_updated_delta = Get-CounterDelta $before $after 'Innodb_rows_updated'
        row_lock_time_ms_delta = Get-CounterDelta $before $after 'Innodb_row_lock_time'
        slow_queries_delta = Get-CounterDelta $before $after 'Slow_queries'
        deadlocks_delta = Get-CounterDelta $before $after 'Innodb_deadlocks'
        connections_current = Get-CounterValue $after 'Threads_connected'
        connections_peak = Get-CounterValue $after 'Max_used_connections'
    }
    $integrity = @(
        'property_unit', 'person', 'app_account', 'bill' | ForEach-Object {
            $key = "table:$_"
            $beforeCount = Get-CounterValue $before $key
            $afterCount = Get-CounterValue $after $key
            [ordered]@{ table = $_; before = $beforeCount; after = $afterCount; result = if ($beforeCount -eq $afterCount) { 'UNCHANGED' } else { 'CHANGED' } }
        }
    )
    $result | Add-Member -NotePropertyName database_metrics -NotePropertyValue $databaseMetrics
    $result | Add-Member -NotePropertyName database_operations -NotePropertyValue $operationSummary
    $result | Add-Member -NotePropertyName integrity -NotePropertyValue $integrity
    $result | Add-Member -NotePropertyName runtime_run_id -NotePropertyValue $runtime.RunId
    $result | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $jsonReport -Encoding utf8

    & python (Join-Path $scriptRoot 'render_report.py') `
        --input $jsonReport --output $htmlReport --title 'Database load test report'
    if ($LASTEXITCODE -ne 0) { throw 'Database load-test HTML report could not be rendered.' }

    $activeAfter = Get-RequiredPublicDemoRunSession -ProjectRoot $projectRoot
    if ($activeAfter.RunId -ne $runtime.RunId) {
        throw "Test contract violation: active runtime changed from $($runtime.RunId) to $($activeAfter.RunId)."
    }

    Write-Host "[OK] Database load test completed under active runtime $($runtime.RunId)."
    Write-Host "[OK] Recorded database operation groups: $($operationSummary.Count)"
    Write-Host "[OK] JSON result: $jsonReport"
    Write-Host "[OK] Offline HTML report: $htmlReport"
} catch {
    $scriptFailed = $true
    Write-Host "[ERROR] $($_.Exception.Message)" -ForegroundColor Red
} finally {
    if (-not $NoPause) { [void](Read-Host 'Press ENTER to close...') }
}

if ($scriptFailed) { exit 1 }




