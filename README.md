<div align="center">

# CozyCoffee

### 咖啡新零售全链路 · 点单履约 × 会员运营 × 精品咖啡小程序

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-6DB33F?logo=springboot&logoColor=white)
![Dubbo](https://img.shields.io/badge/Dubbo-3.2.7-718D19?logo=apache&logoColor=white)
![RocketMQ](https://img.shields.io/badge/RocketMQ-5.3.0-D77310)
![Nacos](https://img.shields.io/badge/Nacos-2.2.3-0096E0?logo=alibaba&logoColor=white)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.11-1B6AC6)

![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Vue 3](https://img.shields.io/badge/Vue-3-4FC08D?logo=vue.js&logoColor=white)
![uni-app](https://img.shields.io/badge/uni--app-%E5%BE%AE%E4%BF%A1%E5%B0%8F%E7%A8%8B%E5%BA%8F-2FB344?logo=wechat&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF?logo=githubactions&logoColor=white)

![GitHub Repo stars](https://img.shields.io/github/stars/Lstarxing/cozy-coffee?style=social)

**咖啡点单与会员运营全链路｜Web + 微信小程序 + 管理端三端｜RocketMQ 事件驱动一致性｜可复现的性能与并发验证**

</div>

---
## 项目简介
CozyCoffee 是面向咖啡零售场景的微服务业务系统，覆盖用户认证、商品点单、订单履约、会员成长、积分权益、优惠券规则与运营后台，前端同时交付 **Web 用户端、微信小程序、运营管理端**。

项目定位：
- 面向真实业务链路的前后端工程：后端微服务按业务域独立数据库，Gateway 统一鉴权与编排
- 强调高频查询性能、交易链路稳定性与营销规则系统的可配置化落地
- 展示从需求建模、原型设计、开发交付到压测优化、CI 测试的完整工程闭环

## 核心亮点
- **微服务架构**：用户 / 会员 / 订单 / 积分商城四 Provider 各自独立数据库（cozy_user / member / order / mall），基于 Dubbo 3.2 + Nacos 2.2 完成 RPC 与服务发现；Gateway 统一鉴权、聚合与 SSE 推送
- **会员积分闭环**：EXP / 积分双账户、五级成长与等级特权矩阵、FIFO 先到期先消耗；签到 / 月度挑战 / 生日 / 首单 / 邀请 / 晋升礼 / 月度权益等奖励规则全部 `@ConfigurationProperties` 配置化——**改 yml 即可调整业务策略，不改码**
- **优惠券系统**：发券模板配置化 + 抵扣策略化（`CouponCalculator` 接口 + 9 类券按类型分发）+ L1 主券 / L2 辅券组合引擎，合法性校验收敛到后端单一事实源，前端不再复刻营销规则
- **V2 商品体系**：统一规格校验与定价核心（杯型 / 出品方式定价）、加料组权威解析、咖啡内容层（8 产区 / 8 单品豆 / 2 拼配）数据驱动，三端点单 / 菜单 / 详情同源
- **Redis 深度应用**
  - String：菜单等热点数据缓存（本地 + Redis 多级缓存、空值缓存、TTL 抖动、本地 SingleFlight、Redis 重建锁）
  - Bitmap：签到日历与连续签到统计
  - ZSet：订单超时到期队列
- **菜单查询优化**：批量预加载加料组、咖啡豆与拼配档案，消除 DTO 转换 N+1；一次完整菜单重建的 SQL 从 **77 条降至 6 条**
- **事件驱动一致性**：RocketMQ 解耦订单创建、完成与取消后的业务处理；`ORDER_COMPLETED` 和 `ORDER_CANCELLED` 在订单事务内写入 **Outbox**，由定时 Relay 可靠投递，券回滚消费端通过 **Inbox** 按事件 ID 去重；`ORDER_CREATED` 由 Gateway 使用 `syncSend` 投递并重试，SSE 推送及缓存失效由消费者异步处理
- **稳定性治理**：分布式锁、CAS 乐观锁（防超卖 / 防重复发放 / 防重复领取）、幂等防重、订单状态机、降级兜底
- **性能与并发验证**：使用 k6 在隔离环境进行多轮 A/B 测试；菜单接口在 200 RPS 下连续 3 轮无失败或丢弃，冷缓存 200 并发连续 3 轮均返回有效菜单且每轮只重建 1 次

## 技术栈
| 层 | 技术 |
|---|---|
| 后端 | Java 17 · Spring Boot 3.4 · Dubbo 3.2 · Nacos 2.2 · RocketMQ 5.3 · MyBatis-Plus 3.5 · Flyway 数据库迁移 |
| 数据与缓存 | MySQL 8.0 · Redis 7 · MinIO 对象存储（S3 兼容，部署使用；本地开发走本地文件） |
| 前端 | Vue 3（Web / Admin）· uni-app 微信小程序 · Element Plus · Pinia |
| 工程与质量 | Maven · JUnit 5 / Vitest · GitHub Actions CI · k6 压测 |

## 系统架构图
```mermaid
flowchart LR
    U[用户端 Web / 小程序] --> G[Gateway 统一鉴权与路由]
    A[管理端 Admin] --> G

    G --> US[User Service]
    G --> MS[Member Service]
    G --> OS[Order Service]
    G --> PS[Points Mall Service]

    US --> MYSQL1[(MySQL user)]
    MS --> MYSQL2[(MySQL member)]
    OS --> MYSQL3[(MySQL order)]
    PS --> MYSQL4[(MySQL mall)]

    OS --> OUTBOX[(Message Outbox)]
    OUTBOX --> MQ[RocketMQ]
    G -->|ORDER_CREATED syncSend| MQ
    MQ --> G
    MQ --> MS
    MQ --> PS

    G --> MINIO[(MinIO)]

    US --> R[(Redis)]
    MS --> R
    OS --> R
    PS --> R

    US --> N[Nacos]
    MS --> N
    OS --> N
    PS --> N
    G --> N
```

> Gateway 在订单创建成功后使用 `syncSend` 投递 `ORDER_CREATED`，等待 Broker ACK 后返回；SSE 推送和管理端缓存失效由消费者异步处理。订单完成与取消事件在订单事务内写入 Outbox，再由 Relay 投递至 RocketMQ。

## 事件驱动与最终一致性
订单完成与取消事件采用事务内 Outbox：业务状态与待投递事件在同一事务中提交，Relay 定时扫描并发送。优惠券回滚消费端通过 Inbox 去重；订单创建事件由 Gateway 使用 `syncSend` 投递，消费者异步处理 SSE 广播和管理端缓存失效。

```
cozy-order-events
 ├── order_created   → Gateway syncSend → SSE 广播 + 管理端缓存失效 (BROADCASTING)
 ├── order_completed → Order Outbox → 积分 / EXP / 首单奖励 / 月度任务 (CLUSTERING)
 │                    → gateway SSE 完成通知                  (BROADCASTING)
 └── order_cancelled → Order Outbox → mall 券回滚（Inbox 去重） (CLUSTERING)

积分退款 / 兑换回滚补偿：points_refund_outbox 本地 Outbox → 定时 relay 投递
```

## 下单时序图
```mermaid
sequenceDiagram
    participant C as Client
    participant GW as Gateway
    participant OS as OrderService
    participant RS as Redis
    participant DB as MySQL
    participant MQ as RocketMQ
    participant SSE as SSE Publisher

    C->>GW: POST /api/order/create
    GW->>OS: createOrder(userId, request)
    OS->>RS: 校验缓存/规则数据
    OS->>DB: 写订单与明细
    OS-->>GW: 返回订单结果
    GW->>MQ: syncSend OrderCreatedEvent（等待 Broker ACK）
    GW-->>C: 下单成功响应
    MQ-->>GW: 广播消费(BROADCASTING)
    GW->>RS: 失效管理端订单与看板缓存
    GW->>SSE: 推送新订单事件
```

> 说明：下单响应会等待 `ORDER_CREATED` 获得 Broker ACK，但不会等待 SSE 广播与缓存失效等消费端逻辑完成；`ORDER_COMPLETED` 和 `ORDER_CANCELLED` 由 Order Provider 在事务内写入 Outbox，分别触发积分、成长值、首单、月度任务和优惠券回滚。

## 缓存策略图
```mermaid
flowchart TD
    Q[请求菜单] --> L1{本地缓存命中?}
    L1 -- 是 --> RET1[直接返回]
    L1 -- 否 --> L2{Redis命中?}
    L2 -- 是 --> RET2[回填本地缓存并返回]
    L2 -- 否 --> LOCK{获取重建锁?}
    LOCK -- 否 --> WAIT[有界随机退避并等待Redis结果]
    WAIT --> READY{Redis结果已就绪?}
    READY -- 是 --> RET2
    READY -- 否 --> FALLBACK[单实例受控数据库兜底]
    FALLBACK --> DBQ
    LOCK -- 是 --> DBQ[查询MySQL]
    DBQ --> BATCH[批量组装DTO 规避N+1]
    BATCH --> W1[写入Redis 含TTL抖动]
    W1 --> W2[写入本地缓存]
    W2 --> RET3[返回结果]
```

## 订单超时取消图
```mermaid
flowchart TD
    T[定时任务触发] --> L{获取分布式锁}
    L -- 否 --> END1[结束]
    L -- 是 --> Z[ZSet按score拉取到期订单]
    Z --> CHK{订单状态仍是pending?}
    CHK -- 否 --> CLEAN[清理脏索引]
    CHK -- 是 --> CANCEL[执行取消订单]
    CANCEL --> RECOVER[回收资源/失效缓存]
    CLEAN --> NEXT{还有批次?}
    RECOVER --> NEXT
    NEXT -- 是 --> Z
    NEXT -- 否 --> END2[释放锁并结束]
```

## 项目结构
- cozy-coffee-backend：后端微服务（cozy-common / cozy-api / cozy-provider / cozy-gateway）
- cozy-coffee-web：用户端 Web（Vue 3）
- cozy-coffee-mobile：微信小程序（uni-app）+ 可点击 HTML 原型
- cozy-coffee-admin：运营管理后台（Vue 3 + Element Plus）
- scripts：测试与运维脚本

## Docker 部署说明
`docker-compose.yml` 默认只拉起基础设施（MySQL、Redis、Nacos、RocketMQ 和 MinIO），4 个 Provider 与 Gateway 在 IDE 本地运行，Web 与管理端可选择 Docker 或本地 `npm run dev`。需要全链路 Docker 验证时，取消 `cozy-*-provider` / `cozy-gateway` 服务注释后执行 `docker compose up -d --build`。

基础设施启动：
```bash
docker compose up -d
```

本地访问：
- Gateway API: http://localhost:8080
- 用户端 Web: http://localhost:5173
- 管理端: http://localhost:5174

## 测试与 CI
- 后端：100+ 单元测试（订单状态机 / 定价与加料 / 奖励发放 / 券计算与组合 / 积分一致性），JUnit 5 + Spring Boot Test
- 菜单缓存并发回归：200 个冷缓存请求全部返回有效菜单，并验证同一实例只执行一次顶层重建
- DTO 批量转换回归：验证加料组、咖啡豆和拼配档案使用批量查询，不再逐商品执行 `selectById`
- 后端测试覆盖订单状态流转、定价与加料、优惠券计算与组合、用券下单、取消回滚、奖励发放和积分一致性；另提供积分及优惠券业务链路验证脚本
- 前端：Web / 小程序 / 管理端 Vitest 单测
- GitHub Actions：JDK17 + `mvn test` + 三端前端测试矩阵，自动判败

## 压测说明
- **工具与模型**：k6 1.5.0；稳态测试使用固定到达率开放模型，突发测试使用并发 VU
- **环境隔离**：专用 Nacos、RocketMQ、MySQL Schema 和 Redis DB；Gateway、Order Provider 各限制为 1 CPU / 512 MiB
- **统计口径**：正式 A/B 使用 5 轮中位数，容量与突发测试使用 3 轮中位数；不取单轮最好成绩

### 菜单缓存 A/B

该组测试完成于 DTO N+1 修复前；对照双方使用相同代码、数据和容器资源，仅切换 DB 直读与本地缓存 + Redis 模式。测试负载为 20 RPS、60 秒/轮，各运行 5 轮。

| 模式 | 平均响应时间中位数 | P95 中位数 | P99 中位数 | HTTP 失败 | 丢弃迭代 |
|---|---:|---:|---:|---:|---:|
| DB 直读 | 40.879 ms | 64.610 ms | 90.488 ms | 0 | 0 |
| 本地缓存 + Redis | 3.956 ms | 5.909 ms | 8.005 ms | 0 | 0 |

缓存模式下平均响应时间降低 **90.32%**，P99 降低 **91.15%**。在 200 RPS 下连续运行 3 轮，共完成 36,003 次请求，P95 / P99 中位数为 3.991 / 7.499 ms，无失败或丢弃。

### DTO N+1 优化

使用 DB 直读模式按相同的 20 RPS、60 秒、5 轮口径复测，避免缓存命中掩盖查询成本。

| 指标 | 优化前 | 优化后 | 变化 |
|---|---:|---:|---:|
| 完整菜单重建 SQL | 77 | 6 | -92.21% |
| 平均响应时间中位数 | 40.879 ms | 10.389 ms | -74.59% |
| P95 中位数 | 64.610 ms | 15.938 ms | -75.33% |
| P99 中位数 | 90.488 ms | 22.614 ms | -75.01% |
| 5 轮 MySQL 语句数中位数 | 92,473 | 7,208 | -92.21% |

### 冷缓存与下单正确性

- 冷缓存 200 并发连续 3 轮均为 200/200 非空、0 HTTP 错误、0 业务错误，每轮只记录 1 次缓存更新和 6 条 SQL。
- 完整“购物车预览 → 创建订单 → RocketMQ 发布”链路以 5 个工作流/秒运行 5 轮，共创建并落库 1,503 笔订单，0 业务失败、0 丢弃；创建订单 P95 / P99 中位数为 54.806 / 61.857 ms。
- 50 个并发请求使用同一个幂等键创建订单时，1 次首次创建、49 次幂等重放，数据库只生成 1 笔订单。

> 上述数据用于说明当前本地隔离环境下的优化效果与并发正确性，不代表线上容量上限。

## 前端界面预览

### 用户端首页（Web）
目标：展示品牌叙事与产区/菜单/会员内容在 Web 首页的编辑式呈现。

![Web 首页-品牌首屏](docs/images/frontend-web/01-home.png)
![Web 首页-产区探索](docs/images/frontend-web/02-home-origins.png)
![Web 首页-菜单精选](docs/images/frontend-web/03-home-menu.png)
![Web 首页-会员](docs/images/frontend-web/04-home-member.png)

### 用户端核心流程（Web）
目标：展示从浏览商品到完成下单的完整体验链路。

![用户端-菜单点单](docs/images/frontend-web/05-menu.png)
![用户端-咖啡详情](docs/images/frontend-web/06-coffee-details.png)
![用户端-购物车](docs/images/frontend-web/07-cart.png)
![用户端-订单列表](docs/images/frontend-web/08-order-list.png)

### 积分与优惠（Web）
目标：展示积分商城、积分获取与优惠券在前端的实际呈现。

![积分商城](docs/images/frontend-web/09-points-mall.png)
![月度挑战与积分获取](docs/images/frontend-web/10-points-earn.png)
![我的券包](docs/images/frontend-web/11-coupon-wallet.png)

### 会员中心（Web）
目标：展示会员中心、个人信息与会员权益页面。

![会员中心](docs/images/frontend-web/12-member-center.png)
![个人信息](docs/images/frontend-web/13-profile.png)
![会员权益](docs/images/frontend-web/14-member-benefits.png)

### 管理后台运营视图
目标：展示后台实时订单处理与经营分析能力。

![后台-运营看板](docs/images/frontend-admin/01-dashboard.png)
![后台-实时订单列表](docs/images/frontend-admin/02-order-list.png)
![后台-订单详情](docs/images/frontend-admin/03-order-detail.png)
![后台-商品管理](docs/images/frontend-admin/04-product-management.png)
![后台-内容档案](docs/images/frontend-admin/05-content-archives.png)
![后台-积分商城](docs/images/frontend-admin/06-mall-management.png)
![后台-会员管理](docs/images/frontend-admin/07-member-management.png)

### 小程序端（uni-app）
微信小程序点单、会员与订单链路：品牌叙事首页 → 产区探索 → 菜单点单 → 选规格 → 结算 → 订单 → 积分商城 → 会员运营。

<p align="center">
  <img width="24%" src="docs/images/frontend-mobile/01-home.png" alt="首页-品牌叙事">
  <img width="24%" src="docs/images/frontend-mobile/02-home-origins.png" alt="首页-产区探索入口">
  <img width="24%" src="docs/images/frontend-mobile/03-origins.png" alt="产区探索">
  <img width="24%" src="docs/images/frontend-mobile/04-about.png" alt="关于我们">
</p>
<p align="center">
  <img width="24%" src="docs/images/frontend-mobile/05-menu.png" alt="菜单点单">
  <img width="24%" src="docs/images/frontend-mobile/06-spec.png" alt="确认订单">
  <img width="24%" src="docs/images/frontend-mobile/07-confirm.png" alt="商品选规格">
  <img width="24%" src="docs/images/frontend-mobile/09-order-detail.png" alt="订单详情">
</p>
<p align="center">
  <img width="24%" src="docs/images/frontend-mobile/08-order-list.png" alt="订单列表">
  <img width="24%" src="docs/images/frontend-mobile/11-points-mall.png" alt="积分商城">
  <img width="24%" src="docs/images/frontend-mobile/14-points-detail.png" alt="积分明细">
  <img width="24%" src="docs/images/frontend-mobile/10-redemption.png" alt="兑换订单">
</p>
<p align="center">
  <img width="24%" src="docs/images/frontend-mobile/13-points-rules.png" alt="积分规则">
  <img width="24%" src="docs/images/frontend-mobile/12-coupons.png" alt="优惠券">
  <img width="24%" src="docs/images/frontend-mobile/19-profile.png" alt="我的-会员卡">
  <img width="24%" src="docs/images/frontend-mobile/16-benefits.png" alt="会员权益">
</p>
<p align="center">
  <img width="24%" src="docs/images/frontend-mobile/18-levels.png" alt="会员等级">
  <img width="24%" src="docs/images/frontend-mobile/15-signin.png" alt="每日签到">
  <img width="24%" src="docs/images/frontend-mobile/17-challenge.png" alt="月度挑战">
</p>

## 作者
- Name: 苏瑞鑫
- Email: 3187979459@qq.com
- GitHub: https://github.com/Lstarxing
