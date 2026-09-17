# Perf Scripts

压测脚本目录。**正式 A/B 数据由 k6 产出**（口径与根 README 的压测章节一致）；Locust 是早期版本，已被取代，仅作历史保留。

## 目录

```text
scripts/perf/
├─ k6/                              正式口径（推荐）
│  ├─ menu-steady.js                菜单稳态（constant-arrival-rate 开放模型）
│  ├─ menu-cold-burst.js            冷缓存突发（per-vu-iterations）
│  ├─ order-workflow.js             完整下单链路（cart/check → create）
│  └─ order-idempotency-burst.js    同幂等键并发（断言只有 1 次真实创建）
├─ locust_hot_read_v2.py            历史：热点读模型
├─ locust_resume_benchmark_v1.py    历史：多场景基准
├─ run_perf_with_redis_metrics.ps1  历史：一键跑 Locust + 系统/MySQL/Redis 指标
└─ benchmark_order_products.ps1     历史：单接口快速基准（Avg/P50/P95/P99 + Redis 状态）
```

## k6 场景

| 脚本 | 执行器 | 负载（默认） | 打什么 | 额外 env |
|---|---|---|---|---|
| `menu-steady.js` | `constant-arrival-rate` | 50 req/s × 2m | `GET /api/order/products` | `RATE`、`DURATION`、`PRE_ALLOCATED_VUS`、`MAX_VUS` |
| `menu-cold-burst.js` | `per-vu-iterations` | 200 VU × 1 次 | 同上（冷缓存同时打） | `VUS` |
| `order-workflow.js` | `constant-arrival-rate` | 5 工作流/s × 60s | `POST /api/order/cart/check` → `POST /api/order/create`（每轮新幂等键） | `TOKEN`、`RATE`、`DURATION` |
| `order-idempotency-burst.js` | `per-vu-iterations` | 50 VU × 1 次 | 同一个 `Idempotency-Key` 并发创建 | `TOKEN`、`PREVIEW_TOKEN`、`IDEMPOTENCY_KEY`、`VUS` |

公共 env：`BASE_URL`，默认 `http://cozy-perf-gateway:8080`（**隔离压测环境**里 Gateway 的服务名，非本机地址）。

阈值（脚本内建，不达标即退出码非 0）：`http_req_failed < 0.001`、`business_errors < 0.001`、`dropped_iterations == 0`；
幂等场景额外断言 `non_replay_orders == 1`。

## 运行

脚本把汇总写到 `/results/summary.json`（为容器内挂载设计），所以用 k6 镜像跑最省事：

```bash
docker run --rm -i \
  --network cozy-perf_default \
  -v "$PWD/scripts/perf/k6:/scripts:ro" \
  -v "$PWD/perf-results:/results" \
  -e BASE_URL=http://cozy-perf-gateway:8080 \
  -e RATE=20 -e DURATION=60s \
  grafana/k6 run /scripts/menu-steady.js
```

本机直跑（k6 已安装）：

```bash
mkdir -p /results   # 或改用 --summary-export 覆盖输出路径，否则写 /results 会失败
k6 run -e BASE_URL=http://localhost:8080 -e RATE=20 -e DURATION=60s scripts/perf/k6/menu-steady.js
```

需要登录态的两个订单脚本：先用测试账号登录拿 **JWT**（`TOKEN`）；幂等脚本还要先跑一次 `cart/check` 取 `PREVIEW_TOKEN`，
并自定一个 `IDEMPOTENCY_KEY`。归档里**刻意不保存 token**。

## 统计口径

- 正式 A/B **取 5 轮中位数**（容量与突发场景取 3 轮），**不取单轮最好成绩**。
- 稳态用固定到达率开放模型（`constant-arrival-rate`）看容量，突发用并发 VU 看击穿行为，两者不可混比。
- 对照双方的代码、数据、容器资源必须一致，只切换被测变量。

## 复现归档

脚本本体之外的**隔离环境、编排脚本、原始结果与报告**都在：

```text
surx-note/CozyCoffee/压测/
├─ 脚本/docker-compose.perf.yml      隔离环境（专用 Nacos/RocketMQ/MySQL schema/Redis DB）
├─ 脚本/run-menu-benchmark.ps1       菜单 A/B 编排（-Mode DB_ONLY|L1_L2 -Scenario steady|cold-burst|warm-burst）
├─ 脚本/run-order-workflow.ps1       下单链路编排（-Token -Rate -Duration）
├─ 报告/*.patch                      复现所需的生产代码修复补丁
└─ 原始结果/…                        每轮 k6 / MySQL / Redis / 资源 / 应用日志
```

复现前提（见该目录 `README.md`）：从提交 `94361cf5316bdb9d0490f7a91645ea6dcea75389` 构建镜像，并按需应用
`报告/` 下的补丁；压测开关默认为生产行为 `L1_L2`。

## Locust（历史，已被 k6 取代）

Round 2 阶段的自建压测，特点是**同一个脚本内混合多场景 + 同时采集系统/MySQL/Redis 指标**，
适合本地做快速对照；正式 A/B 数据已全部改用 k6 产出，故不再作为口径来源。保留脚本供回溯。

```powershell
cd CozyCoffee
pip install locust

.\scripts\perf\run_perf_with_redis_metrics.ps1 `
  -BaseUrl "http://localhost:8080" `
  -LocustFile ".\scripts\perf\locust_resume_benchmark_v1.py" `
  -Scenario "hot_read" `
  -UserToken "<user_token>" -AdminToken "<admin_token>" `
  -Users 100 -SpawnRate 20 -Duration "5m" `
  -MySqlPassword "666" -RedisHost "127.0.0.1" -RedisPort 6379 `
  -OutputTag "redis_branch"
```

场景名：`hot_read`（菜单 + 积分商城）、`signin_stats`（签到统计/日历）、`admin_cache`（管理端订单缓存 + 失效）、
`timeout_cancel`（待支付订单 + 管理端轮询）。输出目录含 `locust_stats.csv`、`system_counters.csv`、
`redis_stats.csv`（含 `pending_timeout_zset_size`）、`mysql_status_pre/post.txt`。`redis-cli` 不在 PATH 时传 `-RedisCliPath`。
