[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$deployDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $deployDirectory 'scripts\dev\common.ps1')
$runtime = Get-DevRuntimeDirectory
$runsDirectory = Join-Path $runtime 'runs'

function Find-RecordForProcess {
    param([int]$ProcessId)
    foreach ($path in Get-ChildItem -LiteralPath $runsDirectory -Filter '*.json' `
            -File -Recurse -ErrorAction SilentlyContinue) {
        try {
            $record = Get-Content -LiteralPath $path.FullName -Raw | ConvertFrom-Json
            if ([int]$record.processId -eq $ProcessId) { return $path.FullName }
        }
        catch { }
    }
    return $null
}

function Stop-ListenerSafely {
    param([string]$Service, [int]$Port)
    $processId = Get-PortOwnerProcessId $Port
    if ($processId -le 0) {
        Write-DevStatus OK ("{0}: port {1} is free" -f $Service, $Port)
        return
    }
    $recordPath = Find-RecordForProcess $processId
    if ($null -ne $recordPath) {
        Stop-ExactManagedProcess -RecordPath $recordPath
        return
    }
    $process = Get-Process -Id $processId -ErrorAction Stop
    $commandLine = try { (Get-CimInstance Win32_Process -Filter "ProcessId=$processId").CommandLine } catch { '?' }
    Write-DevStatus WARN ("{0} port {1} has no valid RUN_ID ownership record" -f $Service, $Port)
    Write-Host ("PID={0} PROCESS={1}" -f $processId, $process.ProcessName)
    Write-Host ("COMMAND={0}" -f $commandLine)
    $answer = Read-Host 'Stop this exact process? Type YES to confirm'
    if ($answer -cne 'YES') {
        Write-DevStatus WARN ("{0} PID {1} was not stopped" -f $Service, $processId)
        return
    }
    $expectedPath = $process.Path
    $expectedTicks = $process.StartTime.ToUniversalTime().Ticks
    if (-not (Test-ProcessIdentity -ProcessId $processId -ExpectedExecutable $expectedPath `
            -ExpectedStartTimeUtcTicks $expectedTicks)) {
        throw "Process identity changed; $Service was not stopped."
    }
    Stop-Process -Id $processId -ErrorAction Stop
    Wait-Process -Id $processId -Timeout 10 -ErrorAction SilentlyContinue
    Write-DevStatus OK ("Stopped confirmed {0} PID {1}" -f $Service, $processId)
}

Write-DevStatus TITLE 'SMART PROPERTY DEVELOPMENT STOP'
Stop-ListenerSafely -Service 'frontend' -Port 5173
Stop-ListenerSafely -Service 'backend' -Port 8080

foreach ($path in Get-ChildItem -LiteralPath $runsDirectory -Filter '*.json' `
        -File -Recurse -ErrorAction SilentlyContinue) {
    try {
        $record = Get-Content -LiteralPath $path.FullName -Raw | ConvertFrom-Json
        if ($record.service -in @('redis', 'minio')) {
            Stop-ExactManagedProcess -RecordPath $path.FullName
        }
    }
    catch { }
}

Write-DevStatus READY 'Known Smart Property development processes are stopped. MySQL was not touched.'
