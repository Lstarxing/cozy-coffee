# 压测复现材料（归档快照）

这里保存菜单缓存 / DTO N+1 优化测试与 2026-09-21 容量测试的**报告、编排脚本与汇总结果**快照。原始归档在仓库外的
`surx-note/CozyCoffee/压测/`，本目录是**精选入库 + 脱敏**后的版本 —— 目的是让口径**可复核**。

口径与结论见根 README 的 [压测说明](../../../README.md#压测说明)，细节见 `reports/` 下的报告。

## 目录

- `reports/`：完整报告（含 `容量测试-20260921-current.md`、`压测报告.md`、`DTO转换N+1修复复测报告.md`、`缓存击穿修复复测报告.md`）、
  容量摘要 `capacity-20260921-summary.json`、环境清单、资源与幂等汇总，以及**复现所需的源码补丁**（`*.patch`）
- `orchestration/`：隔离 Compose、k6 场景、PowerShell 编排与汇总脚本、RocketMQ broker 配置
- `summaries/<批次>/`：每个批次的 k6 汇总件（`aggregate.csv` / `aggregate.json` / `all-runs.csv` / `summary.json`）

## 未入库的内容

**逐轮原始日志**（`order-provider.log`、`container-stats.csv`、`mysql-*.txt`、`redis-*.txt`、
Surefire `TEST-*.xml`）**未随仓库发布** —— 它们含本机绝对路径（`C:\Users\...`、Maven 本地仓库路径）
且体积大。因此本目录支持的是**可复核**（口径、负载、轮数、汇总原始件齐全，可照着重跑），
**不是**"克隆即可一键复现同一组数"。

## ⚠️ 脱敏说明：跑之前先填两个凭据

归档里的凭据已**全部替换为占位符**，原口令未进仓库：

| 文件 | 占位符 | 说明 |
|---|---|---|
| `orchestration/docker-compose.perf.yml` | `${PERF_DB_PASSWORD:?…}`、`${PERF_JWT_SECRET:?…}` | Compose 变量替换；未设置会**直接报错**，不会静默用空口令 |
| `orchestration/*.ps1`（含 `scripts/` 下的同名副本） | `<PERF_DB_PASSWORD>` | 手工替换为隔离压测库口令 |

这两个口令**只用于隔离压测环境**，与生产 `.env.prod` 无关；请勿复用任何生产凭据。

## 复现前提

1. 菜单缓存 / DTO N+1 历史测试从提交 `94361cf5316bdb9d0490f7a91645ea6dcea75389` 构建；当前容量测试对应提交 `050f357e8778721af62ff24c11cc09fcf4ba2f06`。
2. 复现历史三模式 A/B 时，生产代码修复见 `reports/menu-cache-coldfix-and-n1.patch`，并直接使用
   `reports/benchmark-menu-cache-with-coldfix.patch`（已同时包含两项修复与测试开关）。
3. 用 `orchestration/docker-compose.perf.yml` 起隔离环境（专用 Nacos / RocketMQ / MySQL schema / Redis DB 15）。
4. 订单脚本需要现场登录拿测试 token —— **归档刻意不保存 token**。
5. 测试开关默认是生产行为 `L1_L2`。
