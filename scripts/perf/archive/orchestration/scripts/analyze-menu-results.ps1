param(
    [Parameter(Mandatory = $true)]
    [string]$ResultsRoot,
    [string]$OutputDir = $ResultsRoot
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-Median([double[]]$Values) {
    if (-not $Values -or $Values.Count -eq 0) { return $null }
    $sorted = @($Values | Sort-Object)
    $middle = [int][math]::Floor($sorted.Count / 2)
    if (($sorted.Count % 2) -eq 1) { return $sorted[$middle] }
    return ($sorted[$middle - 1] + $sorted[$middle]) / 2.0
}

function Get-TripletDelta([string]$BeforePath, [string]$AfterPath) {
    if (-not (Test-Path -LiteralPath $BeforePath) -or -not (Test-Path -LiteralPath $AfterPath)) {
        return @(0, 0, 0)
    }
    $before = @((Get-Content -LiteralPath $BeforePath -Raw).Trim() -split "\s+")
    $after = @((Get-Content -LiteralPath $AfterPath -Raw).Trim() -split "\s+")
    if ($before.Count -lt 3 -or $after.Count -lt 3) { return @(0, 0, 0) }
    return @(
        ([double]$after[0] - [double]$before[0])
        ([double]$after[1] - [double]$before[1])
        ([double]$after[2] - [double]$before[2])
    )
}

function Get-RedisDelta([string]$BeforePath, [string]$AfterPath, [string]$Metric) {
    if (-not (Test-Path -LiteralPath $BeforePath) -or -not (Test-Path -LiteralPath $AfterPath)) { return 0 }
    $beforeMatch = Select-String -LiteralPath $BeforePath -Pattern "^${Metric}:(\d+)" | Select-Object -First 1
    $afterMatch = Select-String -LiteralPath $AfterPath -Pattern "^${Metric}:(\d+)" | Select-Object -First 1
    if ($null -eq $beforeMatch -or $null -eq $afterMatch) { return 0 }
    return [double]$afterMatch.Matches[0].Groups[1].Value - [double]$beforeMatch.Matches[0].Groups[1].Value
}

$rows = @()
Get-ChildItem -LiteralPath $ResultsRoot -Directory | ForEach-Object {
    $summaryPath = Join-Path $_.FullName 'summary.json'
    $metadataPath = Join-Path $_.FullName 'metadata.json'
    if (-not (Test-Path -LiteralPath $summaryPath) -or -not (Test-Path -LiteralPath $metadataPath)) { return }

    $summary = Get-Content -LiteralPath $summaryPath -Raw -Encoding utf8 | ConvertFrom-Json
    $metadata = Get-Content -LiteralPath $metadataPath -Raw -Encoding utf8 | ConvertFrom-Json
    $durationMetric = $summary.metrics.http_req_duration.values
    $failedMetric = $summary.metrics.http_req_failed.values
    $requestMetric = $summary.metrics.http_reqs.values
    $dropProperty = $summary.metrics.PSObject.Properties['dropped_iterations']
    $dropMetric = if ($null -eq $dropProperty) { $null } else { $dropProperty.Value }
    $schemaDelta = Get-TripletDelta (Join-Path $_.FullName 'mysql-schema-pre.txt') (Join-Path $_.FullName 'mysql-schema-post.txt')
    $redisPre = Join-Path $_.FullName 'redis-pre.txt'
    $redisPost = Join-Path $_.FullName 'redis-post.txt'

    $rows += [pscustomobject]@{
        directory = $_.Name
        mode = $metadata.mode
        scenario = $metadata.scenario
        rateTarget = [int]$metadata.rate
        duration = $metadata.duration
        requests = [int]$requestMetric.count
        actualRps = [math]::Round([double]$requestMetric.rate, 3)
        avgMs = [math]::Round([double]$durationMetric.avg, 3)
        p50Ms = [math]::Round([double]$durationMetric.med, 3)
        p95Ms = [math]::Round([double]$durationMetric.'p(95)', 3)
        p99Ms = [math]::Round([double]$durationMetric.'p(99)', 3)
        maxMs = [math]::Round([double]$durationMetric.max, 3)
        errorRatePct = [math]::Round([double]$failedMetric.rate * 100, 4)
        droppedIterations = if ($null -eq $dropMetric) { 0 } else { [int]$dropMetric.values.count }
        dbStatements = [int64]$schemaDelta[0]
        dbRowsExamined = [int64]$schemaDelta[1]
        dbTimeMs = [math]::Round([double]$schemaDelta[2] / 1000000000.0, 3)
        redisCommands = [int64](Get-RedisDelta $redisPre $redisPost 'total_commands_processed')
        redisHits = [int64](Get-RedisDelta $redisPre $redisPost 'keyspace_hits')
        redisMisses = [int64](Get-RedisDelta $redisPre $redisPost 'keyspace_misses')
    }
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$rows | Sort-Object scenario,rateTarget,mode,directory | Export-Csv -NoTypeInformation -Encoding utf8 (Join-Path $OutputDir 'all-runs.csv')

$groups = $rows | Where-Object { $_.scenario -eq 'steady' } | Group-Object mode,rateTarget
$aggregate = foreach ($group in $groups) {
    $items = @($group.Group)
    [pscustomobject]@{
        mode = $items[0].mode
        rateTarget = $items[0].rateTarget
        runs = $items.Count
        actualRpsMedian = [math]::Round((Get-Median @($items.actualRps)), 3)
        avgMsMedian = [math]::Round((Get-Median @($items.avgMs)), 3)
        p50MsMedian = [math]::Round((Get-Median @($items.p50Ms)), 3)
        p95MsMedian = [math]::Round((Get-Median @($items.p95Ms)), 3)
        p99MsMedian = [math]::Round((Get-Median @($items.p99Ms)), 3)
        errorRatePctMax = [math]::Round((($items.errorRatePct | Measure-Object -Maximum).Maximum), 4)
        droppedIterationsTotal = [int](($items.droppedIterations | Measure-Object -Sum).Sum)
        dbStatementsMedian = [math]::Round((Get-Median @($items.dbStatements)), 3)
        dbRowsExaminedMedian = [math]::Round((Get-Median @($items.dbRowsExamined)), 3)
        dbTimeMsMedian = [math]::Round((Get-Median @($items.dbTimeMs)), 3)
        redisCommandsMedian = [math]::Round((Get-Median @($items.redisCommands)), 3)
    }
}
$aggregate | Sort-Object rateTarget,mode | Export-Csv -NoTypeInformation -Encoding utf8 (Join-Path $OutputDir 'aggregate.csv')

$aggregate | ConvertTo-Json -Depth 5 | Out-File -Encoding utf8 (Join-Path $OutputDir 'aggregate.json')
$rows | Format-Table -AutoSize
