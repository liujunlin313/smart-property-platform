[CmdletBinding()]
param(
    [string]$ConfigFile = '',
    [switch]$ManagedByApplication,
    [int]$OwnerPid = 0,
    [string]$OwnerToken = '',
    [string]$RunId = '',
    [switch]$MachineReadable
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')

$ConfigFile = Resolve-LocalConfigFile $ConfigFile
$properties = Import-LocalProperties $ConfigFile
$endpoint = Get-LocalProperty $properties 'MINIO_ENDPOINT' 'http://127.0.0.1:9000'
$endpointUri = [Uri]$endpoint
$apiPort = $endpointUri.Port
$consolePort = [int](Get-LocalProperty $properties 'MINIO_CONSOLE_PORT' '9001')
$executable = Get-LocalProperty $properties 'MINIO_EXECUTABLE' 'C:\Program Files\minio\minio.exe'
$dataDirectory = Get-LocalProperty $properties 'MINIO_DATA_DIRECTORY' 'deploy\runtime\minio-data'
$rootUser = Get-LocalProperty $properties 'MINIO_ROOT_USER' (Get-LocalProperty $properties 'MINIO_ACCESS_KEY')
$rootPassword = Get-LocalProperty $properties 'MINIO_ROOT_PASSWORD' (Get-LocalProperty $properties 'MINIO_SECRET_KEY')
$healthUrl = $endpoint.TrimEnd('/') + '/minio/health/live'

if (-not [System.IO.Path]::IsPathRooted($dataDirectory)) {
    $dataDirectory = Join-Path (Resolve-ProjectDirectory) $dataDirectory
}

Write-DevStatus TITLE 'MINIO'
if (Wait-HttpReady -Url $healthUrl -Attempts 1 -DelayMilliseconds 1) {
    $existingPid = Get-PortOwnerProcessId $apiPort
    Write-DevStatus REUSE ("Existing MinIO API ready at {0}" -f $endpoint)
    Write-MachineResult @{ STATUS = 'OK'; MODE = 'EXTERNAL_REUSED'; PID = $existingPid; DETAIL = 'API_READY' }
    exit 0
}

if (-not (Test-Path -LiteralPath $executable)) {
    Write-DevStatus WARN 'MinIO is unavailable and MINIO_EXECUTABLE was not found; file features will be disabled.'
    Write-MachineResult @{ STATUS = 'DEGRADED'; MODE = 'UNAVAILABLE'; DETAIL = 'FILE_APIS_503' }
    exit 10
}
if ([string]::IsNullOrWhiteSpace($rootUser) -or [string]::IsNullOrWhiteSpace($rootPassword)) {
    Write-DevStatus WARN 'MinIO credentials are missing; file features will be disabled.'
    Write-MachineResult @{ STATUS = 'DEGRADED'; MODE = 'MISSING_CONFIG'; DETAIL = 'FILE_APIS_503' }
    exit 10
}

New-Item -ItemType Directory -Path $dataDirectory -Force | Out-Null
$runtime = Get-DevRuntimeDirectory
$stdout = Join-Path $runtime 'minio.stdout.log'
$stderr = Join-Path $runtime 'minio.stderr.log'
$env:MINIO_ROOT_USER = $rootUser
$env:MINIO_ROOT_PASSWORD = $rootPassword
$env:MINIO_BROWSER = 'on'

Write-DevStatus START ("Starting MinIO API {0}" -f $endpoint)
$arguments = @('server', $dataDirectory, "--address=$($endpointUri.Host):$apiPort", "--console-address=$($endpointUri.Host):$consolePort")
$process = Start-Process -FilePath $executable -ArgumentList $arguments -PassThru -WindowStyle Hidden `
    -RedirectStandardOutput $stdout -RedirectStandardError $stderr
if (-not (Wait-HttpReady -Url $healthUrl -Attempts 40 -DelayMilliseconds 500)) {
    if (-not $process.HasExited) { Stop-Process -Id $process.Id -ErrorAction SilentlyContinue }
    Write-DevStatus WARN ("MinIO failed to become ready; inspect {0}" -f $stderr)
    Write-MachineResult @{ STATUS = 'DEGRADED'; MODE = 'START_FAILED'; DETAIL = 'FILE_APIS_503' }
    exit 10
}

if ($ManagedByApplication) {
    if ([string]::IsNullOrWhiteSpace($RunId)) { throw 'Managed MinIO requires RunId.' }
    Save-ManagedProcessRecord -Service 'minio' -ProcessId $process.Id -Executable $executable `
        -OwnerPid $OwnerPid -OwnerToken $OwnerToken -RunId $RunId -Ports @($apiPort, $consolePort) | Out-Null
    Write-DevStatus OK ("MinIO API ready; managed PID {0}" -f $process.Id)
    Write-MachineResult @{ STATUS = 'OK'; MODE = 'OWNED_BY_RUN'; PID = $process.Id; DETAIL = 'API_READY' }
    exit 0
}

Write-DevStatus READY ("MinIO ready; PID {0}. Press Ctrl+C to stop." -f $process.Id)
$recordPath = Save-ManagedProcessRecord -Service 'minio' -ProcessId $process.Id -Executable $executable `
    -OwnerPid $PID -OwnerToken ("standalone-{0}" -f $PID) -RunId ("standalone-{0}" -f $PID) -Ports @($apiPort, $consolePort)
try {
    Wait-Process -Id $process.Id
}
finally {
    Stop-ExactManagedProcess -RecordPath $recordPath
}
