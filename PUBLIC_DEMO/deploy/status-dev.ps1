[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$deployDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $deployDirectory 'scripts\dev\common.ps1')
$runtime = Get-DevRuntimeDirectory

Write-DevStatus TITLE 'SMART PROPERTY DEVELOPMENT STATUS'
foreach ($port in @(8080, 5173, 6379, 9000, 9001)) {
    $processId = Get-PortOwnerProcessId $port
    if ($processId -le 0) {
        Write-Host ("{0,-5} FREE" -f $port)
        continue
    }
    $ownership = if ($port -in @(6379, 9000, 9001)) { 'EXTERNAL' } else { 'UNKNOWN' }
    $runId = '-'
    foreach ($recordPath in Get-ChildItem -LiteralPath (Join-Path $runtime 'runs') `
            -Filter '*.json' -File -Recurse -ErrorAction SilentlyContinue) {
        try {
            $record = Get-Content -LiteralPath $recordPath.FullName -Raw | ConvertFrom-Json
            if ([int]$record.processId -eq $processId) {
                $ownership = 'OWNED'
                $runId = [string]$record.runId
                break
            }
        }
        catch { }
    }
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    $name = if ($null -eq $process) { '?' } else { $process.ProcessName }
    $commandLine = try { (Get-CimInstance Win32_Process -Filter "ProcessId=$processId").CommandLine } catch { '?' }
    Write-Host ("{0,-5} PID={1} PROCESS={2} OWNER={3} RUN_ID={4}" -f $port, $processId, $name, $ownership, $runId)
    Write-Host ("      COMMAND={0}" -f $commandLine)
}
