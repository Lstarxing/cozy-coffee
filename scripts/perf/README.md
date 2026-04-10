# Perf Scripts (Round 2)

## Files

- `locust_hot_read_v2.py`: Hot-read API pressure model.
- `run_perf_with_redis_metrics.ps1`: One-command test runner with system, MySQL, and Redis metrics.
- `benchmark_order_products.ps1`: Quick benchmark for `/api/order/products` with Avg/P50/P95/P99 and Redis key status.

## Run

```powershell
cd CozyCoffee
pip install locust

.\scripts\perf\run_perf_with_redis_metrics.ps1 `
  -BaseUrl "http://localhost:8080" `
  -Users 100 `
  -SpawnRate 20 `
  -Duration "5m" `
  -MySqlPassword "666" `
  -RedisHost "127.0.0.1" `
  -RedisPort 6379
```

If `redis-cli` is not in PATH, pass `-RedisCliPath`.

## Quick Benchmark (Order Products)

```powershell
cd CozyCoffee

.\scripts\perf\benchmark_order_products.ps1 `
  -BaseUrl "http://localhost:8080" `
  -Endpoint "/api/order/products" `
  -WarmupRequests 10 `
  -Requests 200 `
  -ClearRedisBefore `
  -RedisKey "cozy:menu:coffee:active"
```

Outputs:

- `records.csv`: per-request latency/status.
- `summary.json`: aggregated Avg/P50/P95/P99, success rate, Redis EXISTS/TTL.

## Output

The script creates `perf_with_redis_yyyyMMdd_HHmmss` and writes:

- `locust_stats.csv`
- `system_counters.csv`
- `redis_stats.csv`
- `mysql_status_pre.txt`
- `mysql_status_post.txt`
- `SUMMARY_TEMPLATE.md`
