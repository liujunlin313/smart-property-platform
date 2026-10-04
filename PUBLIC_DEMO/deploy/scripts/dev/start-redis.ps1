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

function Invoke-RedisCommand {
    param([string]$HostName, [int]$Port, [string]$Password)
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $async = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $async.AsyncWaitHandle.WaitOne(1200, $false)) { return $false }
        $client.EndConnect($async)
        $stream = $client.GetStream()
        $stream.ReadTimeout = 1200
        $stream.WriteTimeout = 1200
        $encoding = New-Object System.Text.UTF8Encoding($false)
        if (-not [string]::IsNullOrWhiteSpace($Password)) {
            $auth = "*2`r`n`$4`r`nAUTH`r`n`$$($encoding.GetByteCount($Password))`r`n$Password`r`n"
            $bytes = $encoding.GetBytes($auth)
            $stream.Write($bytes, 0, $bytes.Length)
            $buffer = New-Object byte[] 256
            $count = $stream.Read($buffer, 0, $buffer.Length)
            $reply = $encoding.GetString($buffer, 0, $count)
            if (-not $reply.StartsWith('+OK')) { return $false }
        }
        $ping = $encoding.GetBytes("*1`r`n`$4`r`nPING`r`n")
        $stream.Write($ping, 0, $ping.Length)
        $buffer = New-Object byte[] 256
        $count = $stream.Read($buffer, 0, $buffer.Length)
        return $encoding.GetString($buffer, 0, $count).StartsWith('+PONG')
    }
    catch {
        return $false
    }
    finally {
        $client.Dispose()
    }
}

$ConfigFile = Resolve-LocalConfigFile $ConfigFile
$properties = Import-LocalProperties $ConfigFile
$redisHost = Get-LocalProperty $properties 'REDIS_HOST' '127.0.0.1'
$redisPort = [int](Get-LocalProperty $properties 'REDIS_PORT' '6379')
$redisPassword = Get-LocalProperty $properties 'REDIS_PASSWORD'
$redisExecutable = Get-LocalProperty $properties 'REDIS_EXECUTABLE'
$redisArguments = Get-LocalProperty $properties 'REDIS_ARGUMENTS'
$redisDataDirectory = Get-LocalProperty $properties 'REDIS_DATA_DIRECTORY' 'deploy\runtime\redis-data'
if (-not [System.IO.Path]::IsPathRooted($redisDataDirectory)) {
    $redisDataDirectory = Join-Path (Resolve-ProjectDirectory) $redisDataDirectory
}

Write-DevStatus TITLE 'REDIS'
if (Invoke-RedisCommand $redisHost $redisPort $redisPassword) {
    $existingPid = Get-PortOwnerProcessId $redisPort
    Write-DevStatus REUSE ("Existing Redis at {0}:{1}; PING = PONG" -f $redisHost, $redisPort)
    Write-MachineResult @{ STATUS = 'OK'; MODE = 'EXTERNAL_REUSED'; PID = $existingPid; DETAIL = 'PING=PONG' }
    exit 0
}

if ([string]::IsNullOrWhiteSpace($redisExecutable) -or -not (Test-Path -LiteralPath $redisExecutable)) {
    Write-DevStatus WARN 'Redis is unavailable and REDIS_EXECUTABLE was not found; cache mode will be BYPASS.'
    Write-MachineResult @{ STATUS = 'DEGRADED'; MODE = 'UNAVAILABLE'; DETAIL = 'CACHE_BYPASS' }
    exit 10
}

$runtime = Get-DevRuntimeDirectory
$null = New-Item -ItemType Directory -Path $redisDataDirectory -Force
$stdout = Join-Path $runtime 'redis.stdout.log'
$stderr = Join-Path $runtime 'redis.stderr.log'
$arguments = @()
if (-not [string]::IsNullOrWhiteSpace($redisArguments)) {
    $arguments += $redisArguments
}
else {
    $arguments += @('--bind', $redisHost, '--port', [string]$redisPort)
    if (-not [string]::IsNullOrWhiteSpace($redisPassword)) {
        $arguments += @('--requirepass', $redisPassword)
    }
}

Write-DevStatus START ("Starting Redis on {0}:{1}" -f $redisHost, $redisPort)
$process = Start-Process -FilePath $redisExecutable -ArgumentList $arguments -PassThru -WindowStyle Hidden `
    -WorkingDirectory $redisDataDirectory `
    -RedirectStandardOutput $stdout -RedirectStandardError $stderr
for ($attempt = 1; $attempt -le 30; $attempt++) {
    if (Invoke-RedisCommand $redisHost $redisPort $redisPassword) { break }
    if ($process.HasExited) { break }
    Start-Sleep -Milliseconds 400
}
if (-not (Invoke-RedisCommand $redisHost $redisPort $redisPassword)) {
    if (-not $process.HasExited) { Stop-Process -Id $process.Id -ErrorAction SilentlyContinue }
    Write-DevStatus WARN ("Redis failed to become ready; inspect {0}" -f $stderr)
    Write-MachineResult @{ STATUS = 'DEGRADED'; MODE = 'START_FAILED'; DETAIL = 'CACHE_BYPASS' }
    exit 10
}

if ($ManagedByApplication) {
    if ([string]::IsNullOrWhiteSpace($RunId)) { throw 'Managed Redis requires RunId.' }
    Save-ManagedProcessRecord -Service 'redis' -ProcessId $process.Id -Executable $redisExecutable `
        -OwnerPid $OwnerPid -OwnerToken $OwnerToken -RunId $RunId -Ports @($redisPort) | Out-Null
    Write-DevStatus OK ("Redis ready; PING = PONG; managed PID {0}" -f $process.Id)
    Write-MachineResult @{ STATUS = 'OK'; MODE = 'OWNED_BY_RUN'; PID = $process.Id; DETAIL = 'PING=PONG' }
    exit 0
}

Write-DevStatus READY ("Redis ready; PING = PONG; PID {0}. Press Ctrl+C to stop." -f $process.Id)
$recordPath = Save-ManagedProcessRecord -Service 'redis' -ProcessId $process.Id -Executable $redisExecutable `
    -OwnerPid $PID -OwnerToken ("standalone-{0}" -f $PID) -RunId ("standalone-{0}" -f $PID) -Ports @($redisPort)
try {
    Wait-Process -Id $process.Id
}
finally {
    Stop-ExactManagedProcess -RecordPath $recordPath
}
