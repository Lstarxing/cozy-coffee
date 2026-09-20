# ADR 0001：用户生命周期与会员权益的事件边界

- **状态**：Accepted（边界决策已定；代码实现分期，见「实施状态」）
- **日期**：2026-09-19
- **修订**：2026-09-20 —— 按 7 个写点的逐条核实结果修订：幂等键表（`PROFILE_COMPLETED` 明确为
  事件键 `profile_completed_{userId}` + 落库 `source_type=profile`/`source_id={userId}`，
  `BIRTHDAY_SET` 改回既有 `birthday_{userId}_{benefitYear}` 且年度由事件携带）、新增 `USER_EVENTS` topic、
  C2/C3 扩围、新增 C6（消费者必须复用原发券方法）、C7（先修既有幂等缺陷，含 `addPointsWithLot` 的幂等机制）、
  C8（时间参数由事件携带，消费者不得用 `now()` 重算）、新增 §8 迁移顺序（第 3、4 步为同一批次）；
  全文去掉易漂移的行号，改引类名与方法名
- **相关**：本仓 CHANGELOG `2026-09-19` / `2026-09-20`；**ADR 0002**《商品目录所有权》（结论：移除 `mall → order`）；
  写点盘点证据见 `surx-note/CozyCoffee/方案/用户生命周期事件迁移-写点对照与证据.md`

## 背景

`cozy-user-provider` 目前承担了不属于它的职责：在注册 / 资料 / 生日 / 邀请流程里直接写会员积分与会员券。
核实后共 **9 处调用 + 2 个 `@DubboReference`**，全部集中在 `UserServiceImpl.java`（7 写 + 2 读）：

| 调用 | 方法（均在 `UserServiceImpl`） | 类型 |
|---|---|---|
| `memberService.createMember` | `register` / `loginWechatDev` / `loginWechat` | 写（会员建档） |
| `pointsMallService.issueNewUserCoupon` | `register` | 写（新人券） |
| `memberService.addPoints` | `updateProfile` | 写（完善资料奖励） |
| `memberService.grantBirthdayReward` | `updateProfile` | 写（生日权益） |
| `pointsMallService.issueCouponToUser` | `grantInviteRewardOnFirstOrder` | 写（邀请奖励券） |
| `memberService.getMemberByUserId` | `listAllUsers` / `getUserDetail` | 读（N+1 / 详情） |

> 本 ADR 只引用类名与方法名，**不写行号** —— 行号会随无关改动漂移，
> 上一版背景表的行号已被同批的读路径改动推偏（其中 `issueCouponToUser` 一处偏了 28 行）。
> 逐个写点的**当前行号、调用链与核实证据**见 `surx-note/CozyCoffee/方案/用户生命周期事件迁移-写点对照与证据.md`。

写与读的问题性质不同：

1. **写** —— 副作用靠 `CompletableFuture` 在事务提交后异步执行，**没有任何持久化**。进程重启、容器被 OOM kill、
   发布滚动重启都会静默丢任务；`register` 里 `createMember` 一旦抛错，后面的 `issueNewUserCoupon` 会被整段跳过
   （catch 吞掉）。`updateProfile` 的两次异步（addPoints / grantBirthdayReward）**连 `AfterCommit` 都没有**，
   与 `register` 不一致。
2. **读** —— `listAllUsers()` 在循环里逐用户调 member，管理端用户列表是 **N+1 RPC**；member 不可用时静默降级为
   `basic/0/0` 且**不留任何日志**，管理端看到的是"所有人都是 basic、0 积分"的假数据。

此外 member 侧反向依赖 user（`MemberServiceImpl` 三处、以及 `FirstOrderConsumer` 调
`grantInviteRewardOnFirstOrder`），构成 `user ↔ member` 双向依赖。其中 `FirstOrderConsumer` 这处是
**`INVITE_REWARD_EARNED` 的触发方**，迁移终态见 §8 第 7 步。

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

**Topic 归属**：新增 `MqTopics.USER_EVENTS` 承载本 ADR 的全部事件。
user-provider **不得**把消息发进 `MqTopics.MEMBER_EVENTS`（该 topic 语义属会员域，
现有唯一消费者 `CouponGrantConsumer` 也只绑定它的 tag）。为了复用某个 listener 而借用别的域的 topic，
会把刚解开的领域所有权重新糊回去。

**事件只表达已发生的事实，不携带产品规则。** 券的面额、门槛、有效期、模板类型属于 mall
（见 §4 C6），事件里带一份就等于把 mall 的产品配置复制到 user。

| Tag | 生产时机 | 消费者（归属） | 幂等键 |
|---|---|---|---|
| `USER_CREATED` | user 任一渠道首次创建 | member（专用消费者） | `userId`（`member_info` 的 `UNIQUE INDEX user_id`） |
| `WELCOME_GIFT_ELIGIBLE` | 普通账号注册成功且符合新人礼资格 | mall（**专用消费者**，直调 `issueNewUserCoupon`，见 C6） | `NEW_USER_COUPON_{userId}`（复用现有 key，见 C2） |
| `PROFILE_COMPLETED` | 手机号 + 邮箱首次同时完整 | member（专用消费者） | 事件键 `profile_completed_{userId}`；落库为 `source_type=profile` + `source_id={userId}`（**新增**，不用 `hashCode()` —— 见 C7） |
| `BIRTHDAY_SET` | 生日设置 / 按规则修改成功 | member（专用消费者） | `birthday_{userId}_{benefitYear}`（**沿用既有格式与原 `sourceId` 派生算法**；`benefitYear` **由事件携带**，见 C2 / C8） |
| `INVITE_REWARD_EARNED` | user 认领奖励资格成功（触发方见 §8 第 7 步） | mall（**专用消费者**，复用六参 `issueCouponToUser` 业务方法，见 C6） | `invite_firstorder_{inviteeUserId}_{inviterId}`（复用现有前缀） |

### 4. 实施约束

以下八条是评审阶段核实代码后得出的**硬约束**，实现时必须遵守；违反任何一条都会让本 ADR 的核心保证失效。

**C1 —— `updateProfile` 不是事务方法，动它之前必须先补 `@Transactional`。（已完成）**

核实：`UserServiceImpl` 中带 `@Transactional` 的方法不含 `updateProfile`。若直接在它内部写 outbox，
`userMapper.updateById()` 与 outbox INSERT 会各自 autocommit，事务保证整个不成立 —— 恰好退化成要消灭的那个问题。
**"在同一事务内写 outbox" 的任何方案，前提是先给该方法加 `@Transactional`。**

已落地：`updateProfile` 补上 `@Transactional`，两处副作用改为 `AfterCommit.run(...)` 派发；
并由反射守卫测试 `UserServiceImplTest#updateProfileIsTransactional` 固定该注解，防止以后被误删。

**C2 —— 幂等键必须复用既有业务键，不得因事件化重新生成。**

同一条数据在"旧同步路径"与"新事件路径"并存期间使用两个不同的 key，会让同一个业务动作执行两次：

- 新人券：现有 `NEW_USER_COUPON_{userId}`，**不得**新造 `welcome:{userId}`（切换窗口内同一用户会拿到两张券）。
- 生日权益：现有 `birthday_{userId}_{benefitYear}` 连同 `sourceId = abs(baseKey.hashCode())` 的派生方式一并沿用，
  **不得**改成 `birthday:{userId}:{benefitYear}` —— 键换了等于换了一把锁，同一年的权益会重复发。
  且 `benefitYear` 必须**由事件携带**，不得由消费者用 `LocalDate.now()` 重算（见 C8）。
- 邀请券：现有 `invite_firstorder_{inviteeUserId}_{inviterId}`。

**C3 —— 幂等实现必须下沉到数据库，覆盖所有事件消费者的写入。**

原实现普遍是"先查后写"，违反 §2。存在三个同类点，实施时都要改：

1. `issueNewUserCoupon` —— 已有 `NEW_USER_COUPON_{userId}`，但靠 `selectCount` 判重（check-then-insert）。
   需改为依赖 `user_coupons` 的 `uk_coupon_code` 唯一索引做条件插入，或捕获 `DuplicateKeyException`。
2. `createMember` —— 同样是 `selectCount` → `insert`。`member_info` 已有 `UNIQUE INDEX user_id`，
   但代码不依赖它，重投会抛异常而非静默重入，需显式捕获。
3. `PROFILE_COMPLETED` 的积分写入 —— 见 C7，目前**完全没有**数据库级幂等。

判据：把同一个事件重复投递两次，第二次必须**不产生新的业务写入**，而不是"日志里显示跳过了"。

**C4 —— 邀请奖励的"资格认领"必须用条件更新，不能用 `updateById`。**

MyBatis-Plus 的 `updateById` 做不到条件更新。需
`UPDATE user SET invite_reward_granted=1 WHERE id=? AND invite_reward_granted=0`，并校验 affected rows = 1。
否则会重蹈 `grantInviteRewardOnFirstOrder` 现有"先查 `inviteRewardGranted`、后 `updateById`"的 TOCTOU。

**C5 —— `invite_reward_granted` 的语义是"资格已可靠入队"，不是"mall 已确认发券"。**

不为此增加跨域回执。若产品需要展示真实发放状态，再单独设计回执事件。

**C6 —— 券的消费者必须复用原业务的「发券方法」，而不是"看起来等价"的通用入口。**

现有两个发券入口**不是同一个生产者**，不可互换：

- 新人券走 `issueNewUserCoupon` → `issueCouponByConfig(buildNewUserCouponConfig())`：配置是
  `DISCOUNT / 50 / DRINK_ONLY / SINGLE_ITEM / maxDiscountAmount=20 / tag=NEW_USER_GIFT / mutex=L1_EXCLUSIVE`。
- 通用入口 `issueCouponToUser` 是**模板驱动**的，而 `CouponTemplateConfig` 里**没有新人券模板**，
  未命中就落到 `FULL_REDUCE` 兜底分支。

因此把 `NEW_USER_COUPON_{userId}` 喂给通用入口，key 一样但**券不一样**（产品可见的静默回归）。
`WELCOME_GIFT_ELIGIBLE` 的消费者必须直接调用 `issueNewUserCoupon`；同理不新增通用模板来"凑等价"。
邀请券相反：`InviteRewardConfig.couponType` 默认 `BOGO`、模板存在，且现有代码调的就是通用入口的六参方法，
**业务方法可复用**。

同时注意：现有 `CouponGrantConsumer` 的 `@RocketMQMessageListener` 被硬绑定到
`MEMBER_EVENTS:COUPON_GRANT_REQUESTED`，所以"可复用"只成立于**发券业务方法**这一层，
`USER_EVENTS` 的两个新事件需要各自的 listener（见 §3 的 topic 归属）。

**C7 —— 事件化的前提是先修既有幂等缺陷，否则只是把窗口扩大。**

`addPoints(userId, points, sourceType, description)` 的签名里**没有 sourceId**，
内部 `recordTransaction` 硬编码 `source_id = null`；而 `points_transactions` 的唯一索引是
`uk_reward(user_id, source_type, source_id)` —— MySQL 对含 NULL 的元组不做约束，
所以这条路径**至今没有任何数据库级防重**（并发提交两次 `updateProfile` 会加两次分）。

`PROFILE_COMPLETED` 的键映射（**必须照此落库，不要用 `String.hashCode()` 派生** ——
`source_id` 就是 `userId`：主键本身无碰撞，且正好构成 `uk_reward` 的第三列）：

| 项 | 值 |
|---|---|
| 事件键（人类可读） | `profile_completed_{userId}` |
| `source_type` | `profile`（= 现有 `ProfileRewardConfig.getSourceType()`） |
| `source_id` | `{userId}` |

改为调用 `addPointsWithLot(userId, points, sourceType, sourceId, description)`。该方法的幂等机制已核实：

- 方法先通过 `selectByUserIdForUpdate` 对 `member_info` 的用户行执行 `SELECT ... FOR UPDATE`，
  **取得行锁后**才按 `(user_id, source_type, source_id)` 查询 `points_transactions`。
  因此同一用户的并发调用会被串行化；后进入的事务在前一个事务提交后查到既有流水并正常返回。
- `uk_reward` 唯一索引仍是最终数据库兜底，防止未来代码绕过或破坏上述锁顺序。
  若兜底触发，`DuplicateKeyException` 会让余额、批次和流水所在事务整体回滚；消息重试后会走正常的
  “锁行 → 查到既有流水 → 返回”路径，**调用方不应跨 Dubbo 边界解析并吞掉异常**。
- `points_lots` **没有唯一索引**（只有非唯一的 `idx_source`）；它依靠同一事务、会员行锁以及
  `uk_reward` 的最终兜底获得传递性保护。

注：本步只改 `updateProfile` 的调用点（`addPoints` → `addPointsWithLot`）；
`addPoints` 本身及其另一个调用方（网关 `MemberController` 的手动加分接口）都不动。

**C8 —— 决定幂等键的时间参数必须由事件携带，消费者不得用 `now()` 重算。**

`grantBirthdayReward(userId)` 内部取 `LocalDate.now().getYear()` 去拼 `birthday_{userId}_{year}`。
原样搬进消费者后，**跨年重投**（事件在 12-31 产生、次年才被消费或重投）会算出**下一年的键**，
于是同一笔权益跨年各发一次 —— 而幂等键本意正是防这个。

要求：`BIRTHDAY_SET` 事件携带生产者盖章的 `benefitYear`（= 产生该事实的那一年，
与现有"执行时 `now()`"的语义一致，只在跨年那一瞬与其不同，而那正是要消除的不确定性），
消费者用事件里的 `benefitYear` 生成键。
对应实现改动：`grantBirthdayRewardInternal(userId, year)` 已接受 `year`，需给对外的
`grantBirthdayReward` 增加一个接受年度的入口。

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

### 8. 迁移顺序（7 步；第 3、4 步为同一批次，其余每步独立可回滚）

1. **先独立修 C7**（完善资料重复加分）。这是**现存正确性缺陷**，与事件化无关，值得单独一个 commit 先修。
2. **修数据库级幂等（C3）**：`issueNewUserCoupon`、`createMember`。
3. **建基础设施与消费者 —— 同一开发 / 发布批次**：`MqTopics.USER_EVENTS` + 事件 DTO
   + user 侧 outbox 表 / relay / 三类指标（范本是 member 的 `CouponGrantOutboxService`，
   但 user 侧需要自己的实体与表）+ 各事件的 listener。
4. **该批次整体上线**：此时还没有事件流过，可安全发版。

   > 第 3、4 步是**一个批次**，不是两次发布。"先基础设施、后消费者"只是**实现顺序**；
   > 只有表 / relay / 指标而没有消费者的骨架**不得单独上线** —— 那恰好就是「被否决的方案」里
   > "先建 outbox 骨架（表 + MQ 配置 + 指标）但不接生产 / 消费者"那一行。
5. **第 7 处（邀请券，`INVITE_REWARD_EARNED`）作为首个切换试点** —— 消费者复用的是已在线验证过的
   发券业务方法（C6），改动最小、回滚最干净。
6. **依次切换第 4、6、5、1–3 处**：新人券（C6 专用消费者）→ 生日（C2 沿用原键与原派生算法）→
   完善资料（依赖第 1 步）→ `USER_CREATED`（收益最低，member 读侧有自愈兜底）。
7. **最后改邀请链的拓扑**：让 user 以独立消费者**直接消费 `ORDER_COMPLETED`** 并认领奖励资格，
   删除 `member → user`（`FirstOrderConsumer` 调 `grantInviteRewardOnFirstOrder`）这条中间跳。

> 第 7 步与第 5 步是两件事：第 5 步只把**发券传输**从同步 RPC 换成事件，触发方仍是 member；
> 第 7 步才消除反向依赖。**不要合并** —— 否则一次改动同时动了传输与依赖方向，回滚无法拆开。

## 被否决的方案

| 方案 | 否决理由 |
|---|---|
| 继续用 `CompletableFuture` | 副作用不持久化，进程重启即丢；不可观测、不可重放，正是要修的问题 |
| 把写编排上移到 Gateway | BFF 会变成分布式事务协调者，持久状态与奖励规则泄漏到网关，且网关重启同样丢任务 |
| 保持同步 Dubbo 调用、不引入 outbox | 跨服务写被绑进调用方事务，一个域故障会拖垮另一个域的写接口 |
| 引入 Seata 等分布式事务框架 | 为 5 个低频事件引入全局事务协调器，运维与认知成本远超收益；最终一致对本场景足够 |
| 一次性大爆炸切换 | 没有回滚窗口；事件契约与消费者未经真实流量验证就切断旧路径 |
| 先建 outbox 骨架（表 + MQ 配置 + 指标）但不接生产 / 消费者 | 纯负债：表、配置、指标都要维护，却没有任何事件流过，也无法验证正确性 |
| user 事件复用 mall 现有的 `CouponGrantConsumer`（即发进 `MEMBER_EVENTS`） | 该 listener 的 topic 语义属会员域；为了省一个 listener 而借用别的域的 topic，会把刚解开的领域所有权重新糊回去。可复用的只有**发券业务方法** |
| 给新人券补一个 `CouponTemplateConfig` 模板，改走通用入口 `issueCouponToUser` | 新人券现有口径由 `CouponConfig`（`buildNewUserCouponConfig`）定义，与模板是两套配置源。凑出的"等价"需要逐字段人工维护，且不等价时**静默**落成 `FULL_REDUCE` 券 —— 产品可见回归，不值得为省一个消费者承担 |
| `PROFILE_COMPLETED` 沿用 `addPoints` 承载 | 该方法签名无 `sourceId`，内部硬编码 `source_id = null`，落不到 `uk_reward` 上；事件重投会重复加分，等于把现存缺陷放大 |

## 后果

**正面** —— 写依赖改由事件承载后，user 域不再持有会员规则；副作用可重试、可观测、可重放；
管理端列表从 N+1 降为一次批量查询；member 侧不再是"唯一但不可靠"的会员初始化路径。

**负面** —— 引入秒级最终一致窗口；多了一张 outbox 表、一个 topic、若干消费者与一份运维说明；
排障时需要看 outbox 状态而不只是业务表。

## 实施状态

- **已实施（读侧）**：读路径上移 Gateway（`AdminUserProfileCoordinator`）；`listAllUsers()` / `getUserDetail()`
  不再反查 member，N+1 与假数据降级一并消除。
- **已实施（C1）**：`updateProfile` 补 `@Transactional`，两处副作用改 `AfterCommit` 派发，含反射守卫测试。
- **已完成（盘点）**：7 个写点 → 5 个事件的对照表，含各自的既有幂等键、DB 兜底索引与消费者归属。
  逐条核实证据（含当前行号与调用链）见
  `surx-note/CozyCoffee/方案/用户生命周期事件迁移-写点对照与证据.md`。
- **推迟**：outbox 表、事件 DTO、消费者与生产者切换（§8 第 2–7 步）。
  理由：在没有生产者和消费者的情况下先建骨架收益为零，只会提前引入表、MQ 配置与指标维护成本；
  这些内容应与消费者实现同批进入，避免"基础设施建了但没人用"。
- **本 ADR 之外**：`member → order`（月度统计投影，尚无 ADR 覆盖）、
  `mall → order`（见 **ADR 0002**，结论是**移除**该依赖：经核实两处读取都是冗余的）、`cozy-common` 拆分。
