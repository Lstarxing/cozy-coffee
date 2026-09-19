# ADR 0001：用户生命周期与会员权益的事件边界

- **状态**：Accepted（边界决策已定；代码实现分期，见「实施状态」）
- **日期**：2026-09-19
- **相关**：本仓 CHANGELOG `2026-09-19`；待写的 ADR 0002《商品目录所有权》

## 背景

`cozy-user-provider` 目前承担了不属于它的职责：在注册 / 资料 / 生日 / 邀请流程里直接写会员积分与会员券。
核实后共 **9 处调用 + 2 个 `@DubboReference`**，全部集中在 `UserServiceImpl.java`：

| 位置 | 调用 | 类型 |
|---|---|---|
| `:166` / `:225` / `:256` | `memberService.createMember` | 写（注册 / 微信开发登录 / 微信登录） |
| `:171` | `pointsMallService.issueNewUserCoupon` | 写（新人券） |
| `:559` | `memberService.addPoints` | 写（完善资料奖励） |
| `:573` | `memberService.grantBirthdayReward` | 写（生日权益） |
| `:846` | `pointsMallService.issueCouponToUser` | 写（邀请奖励券） |
| `:719` / `:777` | `memberService.getMemberByUserId` | 读（`listAllUsers` 的 N+1 / `getUserDetail`） |

写与读的问题性质不同：

1. **写** —— 副作用靠 `CompletableFuture` 在事务提交后异步执行，**没有任何持久化**。进程重启、容器被 OOM kill、
   发布滚动重启都会静默丢任务；`register` 里 `createMember` 一旦抛错，后面的 `issueNewUserCoupon` 会被整段跳过
   （catch 吞掉）。`updateProfile` 的两次异步（`:557`、`:571`）**连 `AfterCommit` 都没有**，与 `register` 不一致。
2. **读** —— `listAllUsers()` 在循环里逐用户调 member，管理端用户列表是 **N+1 RPC**；member 不可用时静默降级为
   `basic/0/0` 且**不留任何日志**，管理端看到的是"所有人都是 basic、0 积分"的假数据。

此外 member 侧反向依赖 user（`MemberServiceImpl:181` / `:255` / `:954`，以及 `FirstOrderConsumer:71` 调
`grantInviteRewardOnFirstOrder`），构成 `user ↔ member` 双向依赖。

## 决策

### 1. 领域所有权

- **user**：账号、认证身份、联系方式、生日**事实**、邀请关系与奖励**资格**。
- **member**：会员档案、等级、积分、生日**权益执行**、月度任务。
- **mall**：优惠券模板、券实例、发券幂等。
- **gateway**：只做读模型组合（BFF）。**不承载奖励规则，不持久状态。**

判定归属的规则是 *"这个事实本身属于谁"*，而不是 *"谁现在写得方便"*。
生日是用户的属性 → user 存事实；生日**权益**是会员规则 → member 执行。

### 2. 一致性语义

- 生产侧：业务变更与 outbox INSERT 处于**同一本地事务**。
- 投递：**at-least-once**，不宣称 exactly-once。
- 消费侧：幂等必须落在**数据库唯一键或条件更新**上，**不得只做"先查后写"**。
- 事件只表达**已发生的事实**，不携带 `MemberDTO` / `UserDTO` 等查询模型，也不携带明文手机号、邮箱、openid。

### 3. 事件契约与版本策略

事件带 `eventId`、`occurredAt`、`schemaVersion` 与业务幂等键（稳定、可人工理解的字符串）。
字段只追加、不改义；破坏性变更通过新增 tag 或版本号承载。

| Tag | 生产时机 | 消费者 | 幂等键 |
|---|---|---|---|
| `USER_CREATED` | 任一渠道首次创建 user | member | `userId`（`member_info.user_id` 已有 UNIQUE 索引） |
| `WELCOME_GIFT_ELIGIBLE` | 普通账号注册成功且符合新人礼资格 | mall | `NEW_USER_COUPON_{userId}`（复用现有 key，见 C2） |
| `PROFILE_COMPLETED` | 手机号 + 邮箱首次同时完整 | member | `profile:{userId}` |
| `BIRTHDAY_SET` | 生日设置 / 按规则修改成功 | member | `birthday:{userId}:{benefitYear}` |
| `INVITE_REWARD_EARNED` | 被邀请人首单完成且 user 已认领奖励资格 | mall | `invite_firstorder_{inviteeUserId}_{inviterId}`（复用现有前缀） |

### 4. 实施约束

以下五条是评审阶段核实代码后得出的**硬约束**，实现时必须遵守；违反任何一条都会让本 ADR 的核心保证失效。

**C1 —— `updateProfile` 不是事务方法，动它之前必须先补 `@Transactional`。**

核实：`UserServiceImpl` 的 `@Transactional` 只出现在 `65` / `206` / `237` / `285` / `646` / `736` / `816` 行，
**`updateProfile`（`:430`）没有**。若直接在它内部写 outbox，`userMapper.updateById()`（`:537`）与 outbox INSERT
会各自 autocommit，事务保证整个不成立 —— 恰好退化成要消灭的那个问题。
**"在同一事务内写 outbox" 的任何方案，前提是先给该方法加 `@Transactional`。**

**C2 —— 新人券的幂等键必须复用现有 `NEW_USER_COUPON_{userId}`，不得新造 key。**

核实：`PointsMallServiceImpl.issueNewUserCoupon`（`:2154`）用的是 `NEW_USER_COUPON_{userId}`，且是
**check-then-insert**（`:2160-2166`）。`user_coupon.coupon_code` 虽有 `uk_coupon_code` 唯一索引
（mall `V1__init.sql:161`），但唯一索引只保证"同 key 不重复"，**不保证"同一业务动作不重复"**。
若事件消费者改用新 key（如 `welcome:{userId}`），切换窗口内旧同步路径与新事件路径并存，同一用户会拿到**两张**新人券。

**C3 —— 幂等实现必须下沉到数据库。**

`issueNewUserCoupon` 的"先查后写"违反本 ADR §2；实施时需改为依赖 `uk_coupon_code` 的条件插入
或捕获 `DuplicateKeyException`。

**C4 —— 邀请奖励的"资格认领"必须用条件更新，不能用 `updateById`。**

MyBatis-Plus 的 `updateById` 做不到条件更新。需
`UPDATE user SET invite_reward_granted=1 WHERE id=? AND invite_reward_granted=0`，并校验 affected rows = 1。
否则会重蹈现在 `grantInviteRewardOnFirstOrder`（`:834` 先查、`:856` 后写）的 TOCTOU。

**C5 —— `invite_reward_granted` 的语义是"资格已可靠入队"，不是"mall 已确认发券"。**

不为此增加跨域回执。若产品需要展示真实发放状态，再单独设计回执事件。

### 5. 可见性与降级

- 会员与权益允许**秒级最终一致**；注册 / 登录接口**不等待**消费结果。
- 读路径：Gateway 拿不到会员数据时按现有默认值降级（`basic` / `0` / `0`），但**必须留下聚合级日志**，
  不允许静默（现在的无日志降级正是管理端假数据的来源）。
- member 侧"查不到会员就补建"的自愈逻辑**保留为兜底**，不作为主流程的可靠性手段。

### 6. 失败责任

- producer 负责 outbox 的投递可见性（PENDING / DEAD / 最老滞留时长三类指标）。
- consumer 失败必须**抛出**以触发 RocketMQ 重投。
- 业务幂等冲突视为已成功，**不进入无限重试**。

### 7. 部署顺序

**消费者先上线 → 生产者后切换 → 依赖最后删除。** 每个阶段都必须能独立回滚，
且回滚语义是"镜像级"的 —— 已执行的事件不会被撤回，由消费者幂等吸收。

## 被否决的方案

| 方案 | 否决理由 |
|---|---|
| 继续用 `CompletableFuture` | 副作用不持久化，进程重启即丢；不可观测、不可重放，正是要修的问题 |
| 把写编排上移到 Gateway | BFF 会变成分布式事务协调者，持久状态与奖励规则泄漏到网关，且网关重启同样丢任务 |
| 保持同步 Dubbo 调用、不引入 outbox | 跨服务写被绑进调用方事务，一个域故障会拖垮另一个域的写接口 |
| 引入 Seata 等分布式事务框架 | 为 5 个低频事件引入全局事务协调器，运维与认知成本远超收益；最终一致对本场景足够 |
| 一次性大爆炸切换 | 没有回滚窗口；事件契约与消费者未经真实流量验证就切断旧路径 |
| 先建 outbox 骨架（表 + MQ 配置 + 指标）但不接生产 / 消费者 | 纯负债：表、配置、指标都要维护，却没有任何事件流过，也无法验证正确性 |

## 后果

**正面** —— 写依赖改由事件承载后，user 域不再持有会员规则；副作用可重试、可观测、可重放；
管理端列表从 N+1 降为一次批量查询；member 侧不再是"唯一但不可靠"的会员初始化路径。

**负面** —— 引入秒级最终一致窗口；多了一张 outbox 表、一个 topic、若干消费者与一份运维说明；
排障时需要看 outbox 状态而不只是业务表。

## 实施状态

- **已实施**：读路径上移 Gateway（`AdminUserProfileCoordinator`）；`listAllUsers()` / `getUserDetail()`
  不再反查 member，N+1 与假数据降级一并消除。
- **推迟**：outbox 表、事件 DTO、消费者与生产者切换（原计划的 Phase 1–4 与 Phase 6）。
  理由：在没有生产者和消费者的情况下先建骨架收益为零，只会提前引入表、MQ 配置与指标维护成本；
  这些内容应与消费者实现同批进入，避免"基础设施建了但没人用"。
- **本 ADR 之外**：`member → order`（月度统计投影）、`mall → order`（商品目录所有权，见待写的 ADR 0002）、
  `cozy-common` 拆分。
