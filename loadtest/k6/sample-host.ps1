param(
    [int]$IntervalSec = 5,
    [int]$DumpEvery = 3,
    [int]$DurationSec = 150,
    [string]$OutDir = '',
    [int]$JavaPid = 0,
    [string]$MysqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
    [string]$MysqlUser = 'root',
    [string]$MysqlPassword = 'root',
    [string]$MysqlDb = 'box',
    [string]$JdkBin = 'D:\yangleduo\Develop\OpenJDK-21.0.11\bin',
    [string]$RedisCli = ''
)

$ErrorActionPreference = 'Continue'
if (-not $OutDir) {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $OutDir = Join-Path $PSScriptRoot "results\samples\$stamp"
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$log = Join-Path $OutDir 'sampler.log'

function Write-Log($msg) {
    $line = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') $msg"
    Add-Content -Path $log -Value $line
    Write-Host $line
}

if ($JavaPid -le 0) {
    $java = Get-Process java -ErrorAction SilentlyContinue |
        Sort-Object WorkingSet64 -Descending |
        Select-Object -First 1
    if (-not $java) { throw 'no java process found' }
    $JavaPid = $java.Id
}

$jcmd = Join-Path $JdkBin 'jcmd.exe'
$jstat = Join-Path $JdkBin 'jstat.exe'
$jstack = Join-Path $JdkBin 'jstack.exe'
if (-not $RedisCli) {
    $RedisCli = 'wsl -d Ubuntu-22.04 -- bash -lc ''$HOME/.local/bin/redis-cli -a admin123'''
}

Write-Log "out=$OutDir pid=$JavaPid interval=${IntervalSec}s duration=${DurationSec}s"
"javaPid=$JavaPid" | Set-Content (Join-Path $OutDir 'meta.txt')
Get-Process -Id $JavaPid | Format-List Id, ProcessName, StartTime, WorkingSet64, PeakWorkingSet64 |
    Out-String | Add-Content (Join-Path $OutDir 'meta.txt')

$jfr = Join-Path $OutDir 'box-loadtest.jfr'
if (Test-Path $jcmd) {
    & $jcmd $JavaPid JFR.start name=boxk6 settings=profile dumponexit=true filename=$jfr 2>> $log | Out-File (Join-Path $OutDir 'jfr-start.txt')
}

$end = (Get-Date).AddSeconds($DurationSec)
$i = 0
$series = Join-Path $OutDir 'series.csv'
'ts,java_ws_mb,java_cpu,mysql_threads_connected,mysql_threads_running,mysql_questions,redis_connected_clients,redis_ops,redis_used_mb,est_8080' |
    Set-Content $series

function Invoke-Mysql([string]$sql, [string]$file) {
    if (-not (Test-Path $MysqlBin)) { return }
    & $MysqlBin --host=127.0.0.1 --port=3306 --user=$MysqlUser --password=$MysqlPassword --protocol=TCP --database=$MysqlDb -N -e $sql 2>$null |
        Out-File -FilePath $file -Encoding utf8
}

function Invoke-Redis([string]$cmd) {
    wsl -d Ubuntu-22.04 -- bash -lc "`$HOME/.local/bin/redis-cli -a admin123 --no-auth-warning $cmd" 2>$null
}

while ((Get-Date) -lt $end) {
    $i++
    $ts = Get-Date -Format 'yyyy-MM-ddTHH:mm:ss'
    $tick = Join-Path $OutDir ('tick-{0:D3}' -f $i)
    New-Item -ItemType Directory -Force -Path $tick | Out-Null
    $ts | Set-Content (Join-Path $tick 'ts.txt')

    $proc = Get-Process -Id $JavaPid -ErrorAction SilentlyContinue
    $wsMb = if ($proc) { [math]::Round($proc.WorkingSet64 / 1MB, 1) } else { 0 }
    $cpu = if ($proc) { [math]::Round($proc.CPU, 2) } else { 0 }
    if ($proc) {
        $proc | Format-List Id, CPU, WorkingSet64, PagedMemorySize64, Handles, Threads |
            Out-String | Set-Content (Join-Path $tick 'java-process.txt')
    }

    if (Test-Path $jstat) {
        & $jstat -gc $JavaPid 2>$null | Out-File (Join-Path $tick 'jstat-gc.txt')
        & $jstat -gcutil $JavaPid 2>$null | Out-File (Join-Path $tick 'jstat-gcutil.txt')
    }
    if (Test-Path $jcmd) {
        & $jcmd $JavaPid GC.heap_info 2>$null | Out-File (Join-Path $tick 'heap-info.txt')
        if (($i % $DumpEvery) -eq 1) {
            & $jcmd $JavaPid Thread.print -l 2>$null | Out-File (Join-Path $tick 'thread-dump.txt')
        }
    } elseif ((Test-Path $jstack) -and (($i % $DumpEvery) -eq 1)) {
        & $jstack -l $JavaPid 2>$null | Out-File (Join-Path $tick 'thread-dump.txt')
    }

    Invoke-Mysql "SHOW GLOBAL STATUS WHERE Variable_name IN ('Threads_connected','Threads_running','Questions','Slow_queries','Innodb_row_lock_waits','Innodb_buffer_pool_wait_free','Aborted_clients','Aborted_connects','Max_used_connections','Connection_errors_max_connections');" (Join-Path $tick 'mysql-status.txt')
    Invoke-Mysql "SHOW PROCESSLIST;" (Join-Path $tick 'mysql-processlist.txt')
    Invoke-Mysql "SELECT COUNT(*) FROM information_schema.processlist;" (Join-Path $tick 'mysql-process-count.txt')

    Invoke-Redis 'INFO stats' | Out-File (Join-Path $tick 'redis-stats.txt')
    Invoke-Redis 'INFO clients' | Out-File (Join-Path $tick 'redis-clients.txt')
    Invoke-Redis 'INFO memory' | Out-File (Join-Path $tick 'redis-memory.txt')
    Invoke-Redis 'SLOWLOG GET 16' | Out-File (Join-Path $tick 'redis-slowlog.txt')

    $est = 0
    netstat -ano | Select-String ':8080' | Measure-Object | ForEach-Object { $est = $_.Count }
    "est_8080=$est" | Set-Content (Join-Path $tick 'net.txt')

    $mysqlConnected = 0; $mysqlRunning = 0; $mysqlQ = 0
    Get-Content (Join-Path $tick 'mysql-status.txt') -ErrorAction SilentlyContinue | ForEach-Object {
        $p = $_ -split '\s+'
        if ($p[0] -eq 'Threads_connected') { $mysqlConnected = $p[1] }
        if ($p[0] -eq 'Threads_running') { $mysqlRunning = $p[1] }
        if ($p[0] -eq 'Questions') { $mysqlQ = $p[1] }
    }
    $redisClients = 0; $redisOps = 0; $redisMem = 0
    Get-Content (Join-Path $tick 'redis-clients.txt') -ErrorAction SilentlyContinue | ForEach-Object {
        if ($_ -match '^connected_clients:(\d+)') { $redisClients = $Matches[1] }
    }
    Get-Content (Join-Path $tick 'redis-stats.txt') -ErrorAction SilentlyContinue | ForEach-Object {
        if ($_ -match '^instantaneous_ops_per_sec:(\d+)') { $redisOps = $Matches[1] }
    }
    Get-Content (Join-Path $tick 'redis-memory.txt') -ErrorAction SilentlyContinue | ForEach-Object {
        if ($_ -match '^used_memory:(\d+)') { $redisMem = [math]::Round([int64]$Matches[1] / 1MB, 2) }
    }

    Add-Content $series "$ts,$wsMb,$cpu,$mysqlConnected,$mysqlRunning,$mysqlQ,$redisClients,$redisOps,$redisMem,$est"

    $dump = Join-Path $tick 'thread-dump.txt'
    if (Test-Path $dump) {
        $text = Get-Content $dump -Raw -ErrorAction SilentlyContinue
        $runnable = ([regex]::Matches($text, 'java.lang.Thread.State: RUNNABLE')).Count
        $blocked = ([regex]::Matches($text, 'java.lang.Thread.State: BLOCKED')).Count
        $waiting = ([regex]::Matches($text, 'java.lang.Thread.State: WAITING')).Count
        $timed = ([regex]::Matches($text, 'java.lang.Thread.State: TIMED_WAITING')).Count
        $http = ([regex]::Matches($text, 'http-nio-8080-exec-')).Count
        "runnable=$runnable blocked=$blocked waiting=$waiting timed_waiting=$timed http_nio_exec=$http" |
            Set-Content (Join-Path $tick 'thread-summary.txt')
    }

    Write-Log "tick $i ws=${wsMb}MB mysql_conn=$mysqlConnected running=$mysqlRunning redis_ops=$redisOps est8080=$est"
    Start-Sleep -Seconds $IntervalSec
}

if (Test-Path $jcmd) {
    & $jcmd $JavaPid JFR.dump name=boxk6 filename=$jfr 2>> $log | Out-File (Join-Path $OutDir 'jfr-dump.txt')
    & $jcmd $JavaPid JFR.stop name=boxk6 2>> $log | Out-File (Join-Path $OutDir 'jfr-stop.txt')
}

Write-Log "done ticks=$i"
Write-Host "samples: $OutDir"
