param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$Users = 100,
    [int]$SpawnRate = 20,
    [string]$Duration = "5m",

    [string]$LocustFile = ".\scripts\perf\locust_hot_read_v2.py",

    [string]$MySqlHost = "127.0.0.1",
    [int]$MySqlPort = 3306,
    [string]$MySqlUser = "root",
    [string]$MySqlPassword = "666",

    [string]$RedisHost = "127.0.0.1",
    [int]$RedisPort = 6379,
    [string]$RedisPassword = "",
    [string]$RedisCliPath = "redis-cli",
    [string]$RedisContainer = "cozy-redis"
)

$ErrorActionPreference = "Stop"

function Invoke-MySqlStatus {
    param(
        [string]$Sql,
        [string]$OutputFile
    )

    $oldPwd = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $MySqlPassword
        & mysql `
          "-h$MySqlHost" `
          "-P$MySqlPort" `
          "-u$MySqlUser" `
          "--default-character-set=utf8mb4" `
          "-N" `
          "-e" $Sql 2>&1 | Out-File -FilePath $OutputFile -Encoding utf8
    }
    finally {
        $env:MYSQL_PWD = $oldPwd
    }
}

function Get-RedisInfo {
    if ($RedisPassword -ne "") {
        docker exec $RedisContainer redis-cli -a $RedisPassword INFO stats
    }
    else {
        docker exec $RedisContainer redis-cli INFO stats
    }
}

function Test-CommandExists([string]$Name) {
    return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

$ts = Get-Date -Format "yyyyMMdd_HHmmss"
$outDir = ".\perf_with_redis_$ts"
New-Item -ItemType Directory -Path $outDir | Out-Null

Write-Host "===> Output directory: $outDir"

$enableRedisMetrics = Test-CommandExists "docker"
if (-not $enableRedisMetrics) {
    Write-Warning "docker not found. Continue test but skip Redis metrics."
}

$counterFile = Join-Path $outDir "system_counters.csv"
$counterJob = Start-Job -ScriptBlock {
    param($file)
    $counters = @(
        "\Processor(_Total)\% Processor Time",
        "\Memory\Available MBytes",
        "\Process(java)\% Processor Time",
        "\Process(java)\Working Set - Private"
    )
    while ($true) {
        $sample = Get-Counter -Counter $counters
        $line = [PSCustomObject]@{
            Timestamp = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
            CpuTotal = ($sample.CounterSamples | Where-Object { $_.Path -like "*Processor(_Total)*" } | Select-Object -First 1).CookedValue
            MemAvailMB = ($sample.CounterSamples | Where-Object { $_.Path -like "*Memory*Available MBytes*" } | Select-Object -First 1).CookedValue
            JavaCpu = ($sample.CounterSamples | Where-Object { $_.Path -like "*Process(java)*% Processor Time*" } | Measure-Object CookedValue -Sum).Sum
            JavaWorkingSetPrivate = ($sample.CounterSamples | Where-Object { $_.Path -like "*Process(java)*Working Set - Private*" } | Measure-Object CookedValue -Sum).Sum
        }
        $line | Export-Csv -Path $file -NoTypeInformation -Append
        Start-Sleep -Seconds 1
    }
} -ArgumentList $counterFile

$redisInfoFile = Join-Path $outDir "redis_stats.csv"
$redisJob = $null
if ($enableRedisMetrics) {
    "Timestamp,keyspace_hits,keyspace_misses,hit_rate_percent,evicted_keys,expired_keys,used_memory_human,connected_clients" | Out-File -FilePath $redisInfoFile -Encoding utf8

    $redisJob = Start-Job -ScriptBlock {
        param($file, $redisContainer, $redisPassword)

        function Parse-Field([string]$Text, [string]$Name) {
            $pattern = "^{0}:" -f [regex]::Escape($Name)
            $line = ($Text -split "`n") | Where-Object { $_ -match $pattern } | Select-Object -First 1
            if (-not $line) { return "0" }
            return ($line -split ":")[1].Trim()
        }

        while ($true) {
            if ($redisPassword -ne "") {
                $stats = docker exec $redisContainer redis-cli -a $redisPassword INFO stats | Out-String
                $memory = docker exec $redisContainer redis-cli -a $redisPassword INFO memory | Out-String
                $clients = docker exec $redisContainer redis-cli -a $redisPassword INFO clients | Out-String
            }
            else {
                $stats = docker exec $redisContainer redis-cli INFO stats | Out-String
                $memory = docker exec $redisContainer redis-cli INFO memory | Out-String
                $clients = docker exec $redisContainer redis-cli INFO clients | Out-String
            }

            $hits = [double](Parse-Field $stats "keyspace_hits")
            $misses = [double](Parse-Field $stats "keyspace_misses")
            $evicted = Parse-Field $stats "evicted_keys"
            $expired = Parse-Field $stats "expired_keys"
            $usedMemoryHuman = Parse-Field $memory "used_memory_human"
            $connectedClients = Parse-Field $clients "connected_clients"

            $total = $hits + $misses
            $hitrate = 0
            if ($total -gt 0) {
                $hitrate = [math]::Round(($hits / $total) * 100, 2)
            }

            "$((Get-Date).ToString("yyyy-MM-dd HH:mm:ss")),$hits,$misses,$hitrate,$evicted,$expired,$usedMemoryHuman,$connectedClients" | Out-File -FilePath $file -Encoding utf8 -Append
            Start-Sleep -Seconds 1
        }
    } -ArgumentList $redisInfoFile, $RedisContainer, $RedisPassword
}

$preFile = Join-Path $outDir "mysql_status_pre.txt"
$mysqlCmdPre = @"
SHOW GLOBAL STATUS WHERE Variable_name IN (
'Questions','Com_select','Com_insert','Com_update',
'Threads_connected','Threads_running','Innodb_rows_read',
'Innodb_row_lock_time','Slow_queries'
);
SHOW GLOBAL VARIABLES LIKE 'max_connections';
"@
Invoke-MySqlStatus -Sql $mysqlCmdPre -OutputFile $preFile

$redisPre = Join-Path $outDir "redis_info_pre.txt"
if ($enableRedisMetrics) {
    Get-RedisInfo | Out-File -FilePath $redisPre -Encoding utf8
}

$locustPrefix = Join-Path $outDir "locust"
$locustCmd = "locust -f `"$LocustFile`" --host `"$BaseUrl`" --headless -u $Users -r $SpawnRate -t $Duration --csv `"$locustPrefix`""
Write-Host "===> Start pressure test: $locustCmd"
cmd /c $locustCmd

$postFile = Join-Path $outDir "mysql_status_post.txt"
$mysqlCmdPost = @"
SHOW GLOBAL STATUS WHERE Variable_name IN (
'Questions','Com_select','Com_insert','Com_update',
'Threads_connected','Threads_running','Innodb_rows_read',
'Innodb_row_lock_time','Slow_queries'
);
"@
Invoke-MySqlStatus -Sql $mysqlCmdPost -OutputFile $postFile

$redisPost = Join-Path $outDir "redis_info_post.txt"
if ($enableRedisMetrics) {
    Get-RedisInfo | Out-File -FilePath $redisPost -Encoding utf8
}

Stop-Job $counterJob | Out-Null
Remove-Job $counterJob | Out-Null

if ($redisJob -ne $null) {
    Stop-Job $redisJob | Out-Null
    Remove-Job $redisJob | Out-Null
}

$summaryFile = Join-Path $outDir "SUMMARY_TEMPLATE.md"
@"
# Perf Summary with Redis Metrics ($ts)

## Locust
- Files: locust_stats.csv, locust_failures.csv
- Focus: Requests/s, Avg RT, P95, P99, Error Rate

## System
- File: system_counters.csv
- Focus: CPU peak, Java CPU peak, available memory min, Java working set peak

## Redis
- Files: redis_stats.csv, redis_info_pre.txt, redis_info_post.txt
- Focus: keyspace_hits, keyspace_misses, hit_rate_percent, evicted_keys, expired_keys

## MySQL
- Files: mysql_status_pre.txt, mysql_status_post.txt
- Focus delta: Questions, Com_select, Slow_queries, Innodb_row_lock_time, Threads_running
"@ | Out-File -FilePath $summaryFile -Encoding utf8

Write-Host "===> Done. Output: $outDir"
