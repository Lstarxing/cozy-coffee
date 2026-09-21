param(
    [ValidateSet('DB_ONLY', 'REDIS_ONLY', 'L1_L2')]
    [string]$Mode,
    [ValidateSet('steady', 'cold-burst', 'warm-burst')]
    [string]$Scenario = 'steady',
    [int]$Rate = 50,
    [string]$Duration = '2m',
    [int]$BurstVus = 200,
    [string]$RunLabel = 'run1',
    [string]$ResultsRoot
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$perfRoot = Split-Path -Parent $PSScriptRoot
$repoRoot = Split-Path -Parent $perfRoot
$composeFile = Join-Path $perfRoot 'docker-compose.perf.yml'
$k6Root = Join-Path $perfRoot 'k6'

if ([string]::IsNullOrWhiteSpace($ResultsRoot)) {
    $ResultsRoot = Join-Path $perfRoot 'results'
}

$safeDuration = $Duration -replace '[^a-zA-Z0-9_-]', '_'
$resultDir = Join-Path $ResultsRoot ("{0}_{1}_{2}rps_{3}_{4}" -f $Mode, $Scenario, $Rate, $safeDuration, $RunLabel)
New-Item -ItemType Directory -Force -Path $resultDir | Out-Null

$env:MENU_CACHE_MODE = $Mode
docker compose -f $composeFile up -d --force-recreate order-provider gateway | Tee-Object -FilePath (Join-Path $resultDir 'compose-up.log')
if ($LASTEXITCODE -ne 0) { throw "docker compose up failed: $LASTEXITCODE" }

$ready = $false
for ($i = 0; $i -lt 90; $i++) {
    try {
        $health = Invoke-RestMethod -Uri 'http://127.0.0.1:18080/actuator/health/readiness' -TimeoutSec 3
        if ($health.status -eq 'UP') { $ready = $true; break }
    } catch { }
    Start-Sleep -Seconds 2
}
if (-not $ready) {
    docker logs cozy-perf-gateway --tail 200 | Out-File -Encoding utf8 (Join-Path $resultDir 'gateway-startup.log')
    docker logs cozy-perf-order --tail 200 | Out-File -Encoding utf8 (Join-Path $resultDir 'order-startup.log')
    throw 'Gateway readiness timed out'
}

# DB 15 is dedicated to the performance environment.
docker exec cozy-redis redis-cli -n 15 DEL cozy:menu:coffee:active | Out-File -Encoding utf8 (Join-Path $resultDir 'redis-reset.txt')

if ($Scenario -eq 'steady') {
    # Warm the JVM and the selected cache mode before the measured interval.
    for ($i = 0; $i -lt 100; $i++) {
        try { Invoke-WebRequest -Uri 'http://127.0.0.1:18080/api/order/products' -TimeoutSec 5 -UseBasicParsing | Out-Null } catch { }
    }
}

if ($Scenario -eq 'warm-burst') {
    # Do not start the measured burst until the cache is demonstrably populated.
    # A freshly recreated Dubbo provider can briefly be absent from the gateway's
    # routing table even though both readiness probes are already UP.
    $menuWarmed = $false
    for ($i = 0; $i -lt 30; $i++) {
        try {
            $warmResponse = Invoke-RestMethod -Uri 'http://127.0.0.1:18080/api/order/products' -TimeoutSec 10
            if ($warmResponse.success -eq $true -and $null -ne $warmResponse.data -and $warmResponse.data.Count -gt 0) {
                $menuWarmed = $true
                break
            }
        } catch { }
        Start-Sleep -Milliseconds 500
    }
    if (-not $menuWarmed) { throw 'Warm-burst precondition failed: menu cache never returned non-empty data' }
}

$mysqlSql = @'
SHOW GLOBAL STATUS WHERE Variable_name IN ('Questions','Com_select','Threads_connected','Threads_running','Innodb_rows_read','Innodb_row_lock_time','Slow_queries');
'@
$mysqlArgs = @('exec', 'cozy-mysql', 'mysql', '-ucozy_perf', '-p<PERF_DB_PASSWORD>', '-N', '-e', $mysqlSql)
& docker @mysqlArgs | Out-File -Encoding utf8 (Join-Path $resultDir 'mysql-pre.txt')
$schemaSql = "SELECT COALESCE(SUM(COUNT_STAR),0) AS statement_count, COALESCE(SUM(SUM_ROWS_EXAMINED),0) AS rows_examined, COALESCE(SUM(SUM_TIMER_WAIT),0) AS timer_wait_ps FROM performance_schema.events_statements_summary_by_digest WHERE SCHEMA_NAME='cozy_perf_order';"
$schemaArgs = @('exec', 'cozy-mysql', 'mysql', '-ucozy_perf', '-p<PERF_DB_PASSWORD>', '-N', '-e', $schemaSql)
& docker @schemaArgs | Out-File -Encoding utf8 (Join-Path $resultDir 'mysql-schema-pre.txt')
docker exec cozy-redis redis-cli -n 15 INFO stats | Out-File -Encoding utf8 (Join-Path $resultDir 'redis-pre.txt')

$statsFile = Join-Path $resultDir 'container-stats.csv'
'timestamp,name,cpu,mem_usage,mem_percent,net_io,block_io,pids' | Out-File -Encoding ascii $statsFile
$statsJob = Start-Job -ScriptBlock {
    param($OutputFile)
    while ($true) {
        $timestamp = (Get-Date).ToString('o')
        $lines = docker stats --no-stream --format '{{.Name}},{{.CPUPerc}},{{.MemUsage}},{{.MemPerc}},{{.NetIO}},{{.BlockIO}},{{.PIDs}}' cozy-perf-gateway cozy-perf-order cozy-mysql cozy-redis
        foreach ($line in $lines) { "$timestamp,$line" | Out-File -Encoding ascii -Append $OutputFile }
        Start-Sleep -Seconds 2
    }
} -ArgumentList $statsFile

$scriptName = if ($Scenario -in @('cold-burst', 'warm-burst')) { 'menu-cold-burst.js' } else { 'menu-steady.js' }
$containerScript = "/scripts/$scriptName"
$dockerArgs = @(
    'run', '--rm', '--network', 'cozycoffee_default',
    '-e', 'BASE_URL=http://cozy-perf-gateway:8080',
    '-e', "RATE=$Rate",
    '-e', "DURATION=$Duration",
    '-e', "VUS=$BurstVus",
    '-e', 'PRE_ALLOCATED_VUS=50',
    '-e', 'MAX_VUS=300',
    '-v', "${k6Root}:/scripts:ro",
    '-v', "${resultDir}:/results",
    'grafana/k6:1.5.0', 'run', '--quiet', $containerScript
)

try {
    & docker @dockerArgs 2>&1 | Tee-Object -FilePath (Join-Path $resultDir 'k6-console.txt')
    $k6ExitCode = $LASTEXITCODE
} finally {
    Stop-Job $statsJob -ErrorAction SilentlyContinue | Out-Null
    Remove-Job $statsJob -Force -ErrorAction SilentlyContinue | Out-Null
}

& docker @mysqlArgs | Out-File -Encoding utf8 (Join-Path $resultDir 'mysql-post.txt')
& docker @schemaArgs | Out-File -Encoding utf8 (Join-Path $resultDir 'mysql-schema-post.txt')
docker exec cozy-redis redis-cli -n 15 INFO stats | Out-File -Encoding utf8 (Join-Path $resultDir 'redis-post.txt')
docker exec cozy-redis redis-cli -n 15 TTL cozy:menu:coffee:active | Out-File -Encoding utf8 (Join-Path $resultDir 'menu-key-ttl.txt')
docker logs cozy-perf-order --since 30m | Out-File -Encoding utf8 (Join-Path $resultDir 'order-provider.log')

$metadata = [ordered]@{
    timestamp = (Get-Date).ToString('o')
    gitSha = (git -C $repoRoot rev-parse HEAD).Trim()
    mode = $Mode
    scenario = $Scenario
    rate = $Rate
    duration = $Duration
    burstVus = $BurstVus
    k6Image = 'grafana/k6:1.5.0'
    k6ExitCode = $k6ExitCode
    javaOptions = '-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:TieredStopAtLevel=1'
    gatewayResources = '1 CPU / 512 MiB'
    orderProviderResources = '1 CPU / 512 MiB'
    redisDatabase = 15
    resultDir = $resultDir
}
$metadata | ConvertTo-Json -Depth 5 | Out-File -Encoding utf8 (Join-Path $resultDir 'metadata.json')

Write-Host "Result: $resultDir"
if ($k6ExitCode -ne 0) { exit $k6ExitCode }
