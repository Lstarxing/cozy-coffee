param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Endpoint = "/api/order/products",
    [int]$WarmupRequests = 10,
    [int]$Requests = 200,
    [int]$TimeoutSec = 10,
    [string]$BearerToken = "",
    [string]$RedisCliPath = "redis-cli",
    [string]$RedisHost = "127.0.0.1",
    [int]$RedisPort = 6379,
    [string]$RedisKey = "cozy:menu:coffee:active",
    [switch]$ClearRedisBefore,
    [string]$OutputDir = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-Percentile {
    param(
        [double[]]$Values,
        [double]$Percent
    )

    if (-not $Values -or $Values.Count -eq 0) {
        return 0
    }

    $sorted = $Values | Sort-Object
    if ($sorted.Count -eq 1) {
        return [math]::Round($sorted[0], 2)
    }

    $rank = ($Percent / 100.0) * ($sorted.Count - 1)
    $low = [math]::Floor($rank)
    $high = [math]::Ceiling($rank)

    if ($low -eq $high) {
        return [math]::Round($sorted[$low], 2)
    }

    $weight = $rank - $low
    $value = $sorted[$low] + ($sorted[$high] - $sorted[$low]) * $weight
    return [math]::Round($value, 2)
}

function Invoke-Redis {
    param(
        [string[]]$Args
    )

    try {
        $output = & $RedisCliPath -h $RedisHost -p $RedisPort @Args 2>$null
        if ($LASTEXITCODE -ne 0) {
            return $null
        }
        return ($output | Out-String).Trim()
    } catch {
        return $null
    }
}

$targetUrl = ($BaseUrl.TrimEnd('/') + $Endpoint)
$headers = @{}
if ($BearerToken -ne "") {
    $headers["Authorization"] = "Bearer $BearerToken"
}

if ($OutputDir -eq "") {
    $ts = Get-Date -Format "yyyyMMdd_HHmmss"
    $OutputDir = Join-Path -Path (Get-Location) -ChildPath ("order_products_benchmark_" + $ts)
}
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

if ($ClearRedisBefore) {
    $null = Invoke-Redis -Args @("DEL", $RedisKey)
}

Write-Host "Benchmark URL: $targetUrl"
Write-Host "Warmup requests: $WarmupRequests"
Write-Host "Measured requests: $Requests"

# Warmup
for ($i = 1; $i -le $WarmupRequests; $i++) {
    try {
        Invoke-WebRequest -Uri $targetUrl -Method Get -Headers $headers -TimeoutSec $TimeoutSec -UseBasicParsing | Out-Null
    } catch {
        # Warmup failures are ignored on purpose.
    }
}

$records = New-Object System.Collections.Generic.List[object]
$okCount = 0
$failCount = 0

for ($i = 1; $i -le $Requests; $i++) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $status = 0
    $success = $false
    $err = ""

    try {
        $resp = Invoke-WebRequest -Uri $targetUrl -Method Get -Headers $headers -TimeoutSec $TimeoutSec -UseBasicParsing
        $status = [int]$resp.StatusCode
        $success = ($status -ge 200 -and $status -lt 400)
    } catch {
        $success = $false
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $status = [int]$_.Exception.Response.StatusCode
        }
        $err = $_.Exception.Message
    }

    $sw.Stop()
    $elapsedMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)

    if ($success) {
        $okCount++
    } else {
        $failCount++
    }

    $records.Add([PSCustomObject]@{
        index = $i
        elapsedMs = $elapsedMs
        status = $status
        success = $success
        error = $err
    })

    if (($i % 20) -eq 0 -or $i -eq $Requests) {
        Write-Host ("Progress: {0}/{1}" -f $i, $Requests)
    }
}

$latencies = @($records | Where-Object { $_.success } | ForEach-Object { [double]$_.elapsedMs })
$total = $records.Count
$successRate = if ($total -gt 0) { [math]::Round(($okCount * 100.0 / $total), 2) } else { 0 }
$avg = if ($latencies.Count -gt 0) { [math]::Round((($latencies | Measure-Object -Average).Average), 2) } else { 0 }
$p50 = Get-Percentile -Values $latencies -Percent 50
$p95 = Get-Percentile -Values $latencies -Percent 95
$p99 = Get-Percentile -Values $latencies -Percent 99
$min = if ($latencies.Count -gt 0) { [math]::Round((($latencies | Measure-Object -Minimum).Minimum), 2) } else { 0 }
$max = if ($latencies.Count -gt 0) { [math]::Round((($latencies | Measure-Object -Maximum).Maximum), 2) } else { 0 }

$redisExists = Invoke-Redis -Args @("EXISTS", $RedisKey)
$redisTtl = Invoke-Redis -Args @("TTL", $RedisKey)

$summary = [PSCustomObject]@{
    timestamp = (Get-Date).ToString("s")
    targetUrl = $targetUrl
    requests = $Requests
    warmupRequests = $WarmupRequests
    timeoutSec = $TimeoutSec
    successCount = $okCount
    failCount = $failCount
    successRatePct = $successRate
    avgMs = $avg
    p50Ms = $p50
    p95Ms = $p95
    p99Ms = $p99
    minMs = $min
    maxMs = $max
    redisKey = $RedisKey
    redisExists = $redisExists
    redisTtl = $redisTtl
}

$recordsCsv = Join-Path $OutputDir "records.csv"
$summaryJson = Join-Path $OutputDir "summary.json"

$records | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $recordsCsv
$summary | ConvertTo-Json -Depth 5 | Out-File -Encoding UTF8 -FilePath $summaryJson

Write-Host ""
Write-Host "===== Benchmark Summary ====="
$summary | Format-List
Write-Host ""
Write-Host "Records: $recordsCsv"
Write-Host "Summary: $summaryJson"
