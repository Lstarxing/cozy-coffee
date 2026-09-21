param(
    [Parameter(Mandatory = $true)]
    [string]$Token,
    [int]$Rate = 5,
    [string]$Duration = '60s',
    [string]$RunLabel = 'run1',
    [string]$ResultsRoot
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$perfRoot = Split-Path -Parent $PSScriptRoot
$repoRoot = Split-Path -Parent $perfRoot
$composeFile = Join-Path $perfRoot 'docker-compose.perf.yml'
$k6Root = Join-Path $perfRoot 'k6'
if ([string]::IsNullOrWhiteSpace($ResultsRoot)) { $ResultsRoot = Join-Path $perfRoot 'results-order' }

$safeDuration = $Duration -replace '[^a-zA-Z0-9_-]', '_'
$resultDir = Join-Path $ResultsRoot ("workflow_{0}rps_{1}_{2}" -f $Rate, $safeDuration, $RunLabel)
New-Item -ItemType Directory -Force -Path $resultDir | Out-Null

$env:MENU_CACHE_MODE = 'L1_L2'
docker compose -f $composeFile up -d nacos-perf rocketmq-perf-namesrv rocketmq-perf-broker user-provider member-provider order-provider gateway `
    | Tee-Object -FilePath (Join-Path $resultDir 'compose-up.log')
if ($LASTEXITCODE -ne 0) { throw "docker compose up failed: $LASTEXITCODE" }

$ready = $false
for ($i = 0; $i -lt 90; $i++) {
    try {
        $health = Invoke-RestMethod -Uri 'http://127.0.0.1:18080/actuator/health/readiness' -TimeoutSec 3
        if ($health.status -eq 'UP') { $ready = $true; break }
    } catch { }
    Start-Sleep -Seconds 2
}
if (-not $ready) { throw 'Gateway readiness timed out' }

$schemaSql = @"
SELECT SCHEMA_NAME, COALESCE(SUM(COUNT_STAR),0), COALESCE(SUM(SUM_ROWS_EXAMINED),0), COALESCE(SUM(SUM_TIMER_WAIT),0)
FROM performance_schema.events_statements_summary_by_digest
WHERE SCHEMA_NAME IN ('cozy_perf_order','cozy_perf_user','cozy_perf_member')
GROUP BY SCHEMA_NAME ORDER BY SCHEMA_NAME;
"@
$schemaArgs = @('exec', 'cozy-mysql', 'mysql', '-ucozy_perf', '-p<PERF_DB_PASSWORD>', '-N', '-e', $schemaSql)
& docker @schemaArgs | Out-File -Encoding utf8 (Join-Path $resultDir 'mysql-schema-pre.txt')
docker exec cozy-mysql mysql -ucozy_perf -p<PERF_DB_PASSWORD> -Dcozy_perf_order -N -e 'SELECT COUNT(*) FROM shop_orders;' `
    | Out-File -Encoding utf8 (Join-Path $resultDir 'orders-pre.txt')

$statsFile = Join-Path $resultDir 'container-stats.csv'
'timestamp,name,cpu,mem_usage,mem_percent,net_io,block_io,pids' | Out-File -Encoding ascii $statsFile
$statsJob = Start-Job -ScriptBlock {
    param($OutputFile)
    while ($true) {
        $timestamp = (Get-Date).ToString('o')
        $lines = docker stats --no-stream --format '{{.Name}},{{.CPUPerc}},{{.MemUsage}},{{.MemPerc}},{{.NetIO}},{{.BlockIO}},{{.PIDs}}' `
            cozy-perf-gateway cozy-perf-order cozy-perf-member cozy-perf-user cozy-perf-rocketmq-broker cozy-mysql cozy-redis
        foreach ($line in $lines) { "$timestamp,$line" | Out-File -Encoding ascii -Append $OutputFile }
        Start-Sleep -Seconds 2
    }
} -ArgumentList $statsFile

$dockerArgs = @(
    'run', '--rm', '--network', 'cozycoffee_default',
    '-e', 'BASE_URL=http://cozy-perf-gateway:8080',
    '-e', "TOKEN=$Token",
    '-e', "RATE=$Rate",
    '-e', "DURATION=$Duration",
    '-v', "${k6Root}:/scripts:ro",
    '-v', "${resultDir}:/results",
    'grafana/k6:1.5.0', 'run', '/scripts/order-workflow.js'
)

try {
    & docker @dockerArgs 2>&1 | Tee-Object -FilePath (Join-Path $resultDir 'k6-console.txt')
    $k6ExitCode = $LASTEXITCODE
} finally {
    Stop-Job $statsJob -ErrorAction SilentlyContinue | Out-Null
    Remove-Job $statsJob -Force -ErrorAction SilentlyContinue | Out-Null
}

& docker @schemaArgs | Out-File -Encoding utf8 (Join-Path $resultDir 'mysql-schema-post.txt')
docker exec cozy-mysql mysql -ucozy_perf -p<PERF_DB_PASSWORD> -Dcozy_perf_order -N -e 'SELECT COUNT(*) FROM shop_orders;' `
    | Out-File -Encoding utf8 (Join-Path $resultDir 'orders-post.txt')
docker logs cozy-perf-gateway --since 15m | Out-File -Encoding utf8 (Join-Path $resultDir 'gateway.log')
docker logs cozy-perf-order --since 15m | Out-File -Encoding utf8 (Join-Path $resultDir 'order-provider.log')

[ordered]@{
    timestamp = (Get-Date).ToString('o')
    gitSha = (git -C $repoRoot rev-parse HEAD).Trim()
    scenario = 'order-workflow'
    rate = $Rate
    duration = $Duration
    k6Image = 'grafana/k6:1.5.0'
    k6ExitCode = $k6ExitCode
    resourcesPerJavaService = '1 CPU / 512 MiB'
    registry = 'dedicated cozy-perf-nacos'
    messageQueue = 'dedicated RocketMQ 5.3.0'
} | ConvertTo-Json -Depth 5 | Out-File -Encoding utf8 (Join-Path $resultDir 'metadata.json')

Write-Host "Result: $resultDir"
if ($k6ExitCode -ne 0) { exit $k6ExitCode }
