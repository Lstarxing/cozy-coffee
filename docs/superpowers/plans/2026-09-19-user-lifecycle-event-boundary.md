# 用户生命周期与会员权益事件边界实施计划

> **执行范围（2026-09-19 确认）**：本轮**只批准 Phase 0（ADR）+ Phase 5（读路径上移网关）**。
> **Phase 1–4 与 Phase 6 暂缓，后续另行确认** —— 本文下面的完整路线图是**待评估的方案**，不是当前执行范围。
> 推迟 Phase 1 的具体理由：在没有生产者 / 消费者的情况下先建 outbox 骨架，只有表、MQ 配置与指标要维护，
> 却没有事件流过，无法验证正确性；应与 Phase 2 的消费者同批进入。
>
> **评审结论已并入实施**：本计划评审时发现的 5 条硬约束（其中最要紧的是 C1「`updateProfile` 根本不是事务方法」、
> C2「新人券换幂等键会导致双发」）已写入
> [`docs/adr/0001-user-lifecycle-and-membership-events.md`](../../adr/0001-user-lifecycle-and-membership-events.md)
> 的「实施约束」，恢复 Phase 1 之前必须先读那五条。
>
> 状态：Draft（Phase 0 / Phase 5 已实施，其余待批）
>
> 日期：2026-09-19

## 1. 目标

在不改变三端接口和用户可见行为的前提下，消除 `cozy-user-provider` 对 `cozy-member-api`、`cozy-mall-api` 的直接依赖，并把当前不可恢复的 `CompletableFuture` 副作用改为可重试、可观测的本地 outbox + RocketMQ 事件链路。

完成后的依赖与调用原则：

```text
写：user 本地事务 -> user_event_outbox -> RocketMQ -> member / mall 幂等消费
读：gateway/BFF -> user + member 批量/单体查询 -> 组合响应
锁：user-provider Maven bannedDependencies 禁止 member-api / mall-api（含传递依赖）
```

## 2. 本轮范围

### 2.1 纳入

- 先形成并评审《用户生命周期与会员权益事件边界》ADR。
- 迁移 `UserServiceImpl` 中 7 处跨域写调用：
  - `createMember` ×3；
  - `issueNewUserCoupon` ×1；
  - `addPoints` ×1；
  - `grantBirthdayReward` ×1；
  - `issueCouponToUser` ×1。
- 迁移 2 处 `getMemberByUserId` 读取，由 Gateway 组合。
- 处理邀请奖励的隐藏反向链路：`member FirstOrderConsumer -> user -> mall`。
- 为 user outbox 提供 PENDING / DEAD / oldest pending age 指标、重试和人工重放说明。
- 删除 user-provider 的 member/mall API 依赖，并增加 Maven 依赖方向守卫。

### 2.2 不纳入

- `member -> order` 的月度统计投影。
- `mall -> order` 的商品目录读取和 catalog 拆分。
- `member -> user` 的会员资料反查；它不阻塞本轮删除 `user -> member/mall`，后续单独评估。
- `cozy-common` 模块拆分。
- 新建监控栈；本轮只暴露指标并补运维说明。
- 重写 RocketMQ/outbox 为通用框架；只复用当前已验证的实现约定。

## 3. 当前问题与基线

| 场景 | 当前行为 | 风险/问题 | 目标行为 |
|---|---|---|---|
| 普通注册 | 提交后 `CompletableFuture` 调 member，再调 mall | 进程重启会丢；第一步失败会跳过第二步 | 同事务写两条 outbox；member/mall 独立消费 |
| 微信首次登录 | 事务内同步 `createMember`，异常被吞 | 用户已创建但会员初始化可能永久缺失 | 同事务写 `USER_CREATED` |
| 首次完善资料 | `CompletableFuture` 调 member 加积分 | 无持久化重试；重复执行边界不清 | `PROFILE_COMPLETED`，member 按 userId 幂等 |
| 设置生日 | `CompletableFuture` 调 member 发权益 | 无持久化重试 | `BIRTHDAY_SET`，member 幂等处理 |
| 首单邀请奖励 | member 消费订单事件后 RPC user，user 再 RPC mall | 跨三域同步链；失败被记录后吞掉 | user 独立消费 `ORDER_COMPLETED`，同事务认领奖励并写 outbox；mall 消费 |
| 管理端用户列表 | user 对每个用户调用 member | N+1 RPC | Gateway 调 `getMembersByUserIds` 一次批量组合 |
| 管理端用户详情 | user 内部调用 member | user API 混入会员字段 | Gateway 单次组合 user + member |

保留现有外部语义：

- 普通账号注册仍发新人券。
- 微信新建账号当前只创建会员、不发新人券；除非 ADR 明确改变产品规则，否则迁移时保持不变。
- 会员初始化与权益发放允许最终一致，注册/登录接口不等待消费结果。
- 现有前端响应结构不变，Gateway 继续返回带会员摘要字段的 `UserDTO`。

## 4. ADR 决策门

创建：

```text
docs/adr/0001-user-lifecycle-and-membership-events.md
```

ADR 必须明确并得到确认：

1. **领域所有权**
   - user：账号、认证身份、联系方式、生日事实、邀请关系和奖励资格。
   - member：会员档案、等级、积分、生日权益执行。
   - mall：优惠券模板、券实例和发券幂等。
   - gateway：只负责读模型组合，不承载奖励规则和持久状态。
2. **一致性语义**
   - 生产侧：业务变更与 outbox INSERT 同一本地事务。
   - 投递：at-least-once；不宣称 exactly-once。
   - 消费侧：必须用数据库唯一键或条件更新实现幂等，不能只做“先查后写”。
3. **事件契约和版本策略**
   - 事件带 `eventId`、`occurredAt`、`schemaVersion` 和业务幂等键。
   - 字段只追加、不改义；破坏性变更新增事件版本或 tag。
4. **可见性**
   - 会员和权益允许秒级最终一致。
   - Gateway 读取会员为空时按现有默认值降级；member 当前的缺失自愈保留为兜底，不作为主流程可靠性手段。
5. **失败责任**
   - producer 负责 outbox 投递可见性。
   - consumer 失败必须抛出以触发 RocketMQ 重投。
   - 业务幂等冲突视为已成功，不进入无限重试。
6. **部署策略**
   - 消费者先上线，生产者后切换，依赖最后删除。

ADR 未通过前，不创建表、不加事件、不改调用链。

## 5. 事件契约

沿用现有 `cozy-common.mq` 放共享消息契约；本轮不借机拆 `cozy-common`。新增 `MqTopics.USER_EVENTS` 及对应 tags。

| Tag | 生产时机 | 载荷最小字段 | 消费者 | 幂等键 |
|---|---|---|---|---|
| `USER_CREATED` | 任一渠道首次创建 user | `eventId, userId, registrationChannel, occurredAt, schemaVersion` | member | `userId`（`member_info.user_id` 唯一） |
| `WELCOME_GIFT_ELIGIBLE` | 普通账号注册成功且符合新人礼资格 | `eventId, userId, eligibilityKey, occurredAt, schemaVersion` | mall | `welcome:{userId}` |
| `PROFILE_COMPLETED` | 手机号和邮箱首次同时完整 | `eventId, userId, occurredAt, schemaVersion` | member | `profile:{userId}` |
| `BIRTHDAY_SET` | 生日设置/按规则修改成功 | `eventId, userId, birthday, benefitYear, occurredAt, schemaVersion` | member | `birthday:{userId}:{benefitYear}` |
| `INVITE_REWARD_EARNED` | 被邀请人首单完成且 user 成功认领奖励资格 | `eventId, inviteeUserId, inviterUserId, sourceOrderId, rewardKey, occurredAt, schemaVersion` | mall | `invite:{inviteeUserId}` |

约束：

- 事件表达已发生事实，不携带 `MemberDTO`、`UserDTO` 等查询模型。
- `PROFILE_COMPLETED` 不携带积分数量；奖励额度由 member 域配置。
- `WELCOME_GIFT_ELIGIBLE`、`INVITE_REWARD_EARNED` 不携带券表实体；mall 将资格映射为券模板。
- 不把手机号、邮箱、openid 等敏感信息放入 MQ payload。
- `eventId` 用 UUID；业务幂等键用稳定、可人工理解的字符串。

## 6. 数据与基础设施设计

### 6.1 user outbox

新增 Flyway `V3__add_user_event_outbox.sql`，表建议为 `user_event_outbox`：

```text
id                  bigint PK
event_id            varchar(64) UNIQUE NOT NULL
event_key           varchar(128) UNIQUE NOT NULL
aggregate_id        bigint NOT NULL
event_type          varchar(64) NOT NULL
topic               varchar(64) NOT NULL
tag                 varchar(64) NOT NULL
payload             json/text NOT NULL
status              varchar(16) NOT NULL   -- PENDING/SENT/DEAD
retry_count         int NOT NULL
next_retry_at       datetime NOT NULL
created_at          datetime NOT NULL
updated_at          datetime NOT NULL
INDEX(status, next_retry_at)
```

实现文件：

- `cozy-user-provider/.../entity/UserEventOutbox.java`
- `cozy-user-provider/.../mapper/UserEventOutboxMapper.java`
- `cozy-user-provider/.../mq/UserEventOutboxService.java`
- `cozy-user-provider/.../mq/UserLifecycleEventPublisher.java`
- 对应单测。

行为沿用已上线的 member outbox：

- 调用方事务内 INSERT。
- `AfterCommit` 只用于降低延迟。
- 每 30 秒扫描 PENDING，批量 100。
- 10/30/60/120/300 秒退避，5 次后 DEAD。
- `event_key` 唯一冲突按“已入队”处理。
- 投递消息 header 至少带 `KEYS=event_key`、`OUTBOX_ID=id`、`EVENT_ID=event_id`。

### 6.2 指标

新增：

```text
cozy.user.event_outbox.pending
cozy.user.event_outbox.dead
cozy.user.event_outbox.oldest_pending_age_seconds
```

user-provider 增加：

- `rocketmq-spring-boot-starter`；
- Prometheus registry（若尚未由父 POM传入）；
- `/actuator/prometheus` 暴露；
- `rocketmq.name-server` 配置；
- `docker-compose.prod.yml` 中 user-provider 的 `ROCKETMQ_NAME_SERVER` 和 namesrv 启动依赖；
- 本地注释 compose 同步配置，避免全链路 Docker 验证遗漏。

不要把 scrape 配置或告警规则伪装成已部署监控；仅在运维手册记录建议阈值和重放 SQL。

## 7. 分阶段执行

### Phase 0：ADR（纯文档）

改动：

- 新建 ADR 0001。
- 把本计划的领域所有权、事件契约、最终一致窗口和回滚策略写入 ADR。
- 记录被否决方案：继续 `CompletableFuture`、Gateway 执行写编排、同步分布式事务、一次性大爆炸切换。

验收：

- 9 个调用点都有明确归宿。
- 普通注册与微信注册的新人券差异有明确结论。
- 邀请奖励由哪个域判定资格、哪个域发券没有歧义。
- ADR 状态从 `Proposed` 改为 `Accepted` 后才进入 Phase 1。

建议提交：

```text
docs(adr): 明确用户生命周期与会员权益事件边界
```

### Phase 1：事件契约 + user outbox 基础设施（不切业务）

改动：

- 在 `cozy-common.mq` 增加 topic、tags 和五种事件 DTO。
- user-provider 增加 RocketMQ 依赖、配置、outbox 表/实体/mapper/service/指标。
- prod/local compose 补 user-provider 的 RocketMQ 配置。
- 不修改 `UserServiceImpl` 现有调用；此阶段行为零变化。

测试：

- 写入 outbox 与调用方事务原子提交。
- 回滚时没有 outbox 行。
- event_key 重复不重复入队。
- 投递成功转 SENT；失败累计 retry；超过阈值转 DEAD。
- PENDING/DEAD/age 指标与造数一致。
- Flyway 在空库和已有 V2 的库上都能升级。

建议提交：

```text
feat(user): 增加生命周期事件 outbox 与投递可见性
```

### Phase 2：消费者先上线（仍不切生产者）

#### member-provider

- 新增 `UserCreatedConsumer`：调用幂等的会员初始化；并发唯一键冲突视为成功。
- 新增 `ProfileCompletedConsumer`：使用 `addPointsWithLot(userId, points, sourceType, userId, description)`；依赖 `points_transaction(user_id, source_type, source_id)` 唯一键兜底。
- 新增 `BirthdaySetConsumer`：调用现有生日权益逻辑，保留年度幂等和 member -> mall 发券 outbox。
- 将完善资料奖励配置移动/归属到 member，user 不再决定积分额度。

#### mall-provider

- 新增 `WelcomeGiftEligibleConsumer`：映射新人券模板并用 `welcome:{userId}` 发券。
- 新增 `InviteRewardEarnedConsumer`：映射邀请奖励券模板并用 `invite:{inviteeUserId}` 发给 inviter。
- 两者复用 `issueCouponToUser` 的 coupon code 唯一幂等；异常必须抛出让 RocketMQ 重投。

测试：

- 每个消费者正常、重复投递、非法载荷、下游异常四类测试。
- 两个并发相同事件只能产生一笔积分/一张券/一个会员。
- 消费者组必须独立，不能让不同领域竞争消费同一条事件。

建议拆成两个提交：

```text
feat(member): 消费用户生命周期事件并幂等初始化权益
feat(mall): 消费用户权益资格事件并幂等发券
```

部署门：先上线 Phase 1 + Phase 2，确认所有消费者 healthy。此时没有新生产者，允许安全回滚。

### Phase 3：切换注册、资料与生日写链路

在 `UserServiceImpl` 的原事务中写 outbox：

- `register`：写 `USER_CREATED`；普通账号另写 `WELCOME_GIFT_ELIGIBLE`。
- `loginWechatDev` / `loginWechat`：仅首次插入用户时写 `USER_CREATED`，保持当前“不发新人券”语义。
- `updateProfile`：在同一事务内，首次完整时写 `PROFILE_COMPLETED`；生日变更成功时写 `BIRTHDAY_SET`。

随后删除对应 `CompletableFuture` 和同步 Dubbo 调用，但暂时保留 POM 依赖和未使用 API，给回滚留窗口。

测试：

- 注册事务失败时没有 user、没有 outbox。
- 普通注册恰好产生 user-created + welcome 两个事件。
- 微信新用户只产生 user-created；已有微信用户不重复产生。
- 更新资料重试不会重复产生 profile/birthday 事件。
- broker 不可用时注册/资料更新仍能成功，outbox 保持 PENDING。

建议提交：

```text
refactor(user): 用户生命周期副作用改由本地 outbox 派发
```

### Phase 4：切换首单邀请奖励

现状是 `member FirstOrderConsumer -> user.grantInviteRewardOnFirstOrder -> mall`。改为：

1. user-provider 新增独立 consumer group，消费现有 `ORDER_COMPLETED`。
2. 仅处理 `isFirstOrder=true`。
3. user 本地事务中按 `invitee userId + invite_reward_granted=false` 条件更新认领一次资格。
4. 同一事务写 `INVITE_REWARD_EARNED` outbox；outbox 失败则资格更新回滚。
5. member 的 `FirstOrderConsumer` 只保留首单积分，删除回调 user 的部分。
6. mall 消费事件为 inviter 发券；按 `invite:{inviteeUserId}` 幂等。

注意：当前列名 `invite_reward_granted` 实际表达“奖励资格已可靠入队”，不是“mall 已确认发券”。ADR 中明确语义；本轮不为追求命名完美增加跨域回执。若产品需要展示真实发放状态，再单独设计回执事件。

测试：

- 非首单、无邀请人、已处理事件均无副作用。
- 相同/不同 orderId 的重复首单事件都只能认领一次。
- outbox 写入失败时 `invite_reward_granted` 不得变为 true。
- member 首单积分仍正常且不再调用 user。
- mall 重复消费不会重复发券。

建议提交：

```text
refactor(user): 首单邀请奖励改为事件驱动
```

### Phase 5：读取上移 Gateway/BFF

#### 用户列表

- `userService.listAllUsers()` 只返回 user 域字段。
- `AdminListService.listUsers()` 收集 userIds，一次调用现有 `memberService.getMembersByUserIds(Set<Long>)`。
- Gateway 按 userId 合并 `memberLevel/currentPoints/totalPoints` 后再过滤会员等级。
- member 调用失败时保持现有降级值：`basic/0/0`，并记录一次聚合级日志；禁止逐用户告警风暴。

#### 用户详情

- 新增或扩展 Gateway `AdminUserProfileCoordinator`。
- 并行/顺序调用 user 基础详情与 member 单体查询，合并为原 `UserDTO` 响应。
- `AdminUserController` 改走 coordinator；三端响应结构不变。

#### user-provider

- 删除 `listAllUsers()` 和 `getUserDetail()` 中两处 `getMemberByUserId`。
- 增加契约测试，保证 user API 不再偷偷填充会员字段。

测试：

- 用户列表只发生一次 member 批量 RPC，禁止 N+1。
- 空列表不调用 member。
- member 部分缺失、整体超时/异常时正确降级。
- 详情组合成功、member 降级和 user 不存在三类测试。
- 现有管理端筛选、排序、字段结构保持不变。

建议提交：

```text
refactor(gateway): 用户会员信息改由 BFF 批量组合
```

### Phase 6：删除依赖并锁边

前提：Phase 3~5 已部署并观察至少一个业务周期，outbox 无持续堆积、消费者无重复副作用。

改动：

- 删除 `UserServiceImpl` 的两个 `@DubboReference`、imports 和构造/字段依赖。
- 从 user-provider POM 删除 `cozy-member-api`、`cozy-mall-api`。
- 删除已无调用方的 `UserService.grantInviteRewardOnFirstOrder` 及实现。
- 对确实无调用方的专用旧 API 做全仓检索后再删；公共能力仍被其他模块使用则保留。
- user-provider 加 `maven-enforcer-plugin`：禁止 member-api、mall-api，包含传递依赖。
- 增加依赖方向测试/脚本，验证临时加回任一依赖会 BUILD FAILURE。

验收命令：

```text
mvn -pl cozy-provider/cozy-user-provider -am validate
mvn -pl cozy-provider/cozy-user-provider -am test
mvn -pl cozy-provider/cozy-member-provider,cozy-provider/cozy-mall-provider,cozy-gateway -am test
mvn test
```

建议提交：

```text
refactor(user): 移除 member/mall 依赖并锁定领域边界
```

## 8. 测试矩阵

| 层级 | 必测内容 |
|---|---|
| 单元测试 | event_key 生成、事件载荷、consumer 幂等、重试/DEAD、Gateway 合并与降级 |
| 数据库测试 | outbox 唯一键、条件更新认领、事务回滚、并发重复消费 |
| 契约测试 | 新事件可反序列化；新增字段向后兼容；Dubbo API 不再返回跨域拼接数据 |
| 架构测试 | user-provider 不得依赖 member-api/mall-api；禁令含传递依赖 |
| 冒烟测试 | 普通注册、微信首登、资料完善、生日设置、被邀请人首单、管理端用户列表/详情 |
| 故障测试 | broker 停止时写业务成功且 outbox PENDING；恢复后自动投递；重复消息不重复奖励 |

不要只 mock `UserEventOutboxService.publish()`：至少保留一组真实事务测试证明业务行与 outbox 行同提交、同回滚。

## 9. 发布顺序与回滚

### Release 1：基础设施和消费者

- 部署 common 契约、user outbox 表和 member/mall 消费者。
- 业务生产者尚未切换，行为不变。
- 因 `cozy-common` 变更，5 个后端镜像全部重建。
- 验证 user-provider 可连接 RocketMQ、消费者组启动、指标可抓取。

回滚：直接回滚镜像；V3 表保留不影响旧版本。

### Release 2：写链路切换

- 部署 Phase 3、Phase 4。
- 重点监控 user outbox pending/dead/age、member/mall 消费异常、奖励唯一键冲突。
- 冒烟覆盖五种事件。

回滚：回滚 user/member/mall 到 Release 1；旧同步 API 和 POM 依赖此时仍保留。已投递事件由消费者幂等吸收。

### Release 3：读组合与依赖删除

- 部署 Gateway BFF 组合。
- 验证管理端列表无 N+1，详情响应兼容。
- 删除 user 依赖并启用 bannedDependencies。

回滚：只回滚 Gateway/user 到 Release 2；数据库和事件契约向后兼容。

## 10. 运维验收

上线后记录并检查：

- `user_event_outbox` 的 PENDING/DEAD 数量和最老滞留时间。
- 每个 consumer group 的消费延迟和异常日志。
- 注册用户中缺失 `member_info` 的数量。
- profile 奖励按 `(userId, sourceType, sourceId)` 是否唯一。
- 新人券与邀请券按业务 uniqueKey 是否唯一。

建议阈值沿用 member outbox：

- `dead > 0` 立即告警/人工处理。
- `oldest_pending_age_seconds > 300` 告警。
- PENDING 短暂大于 0 可接受，但不得持续增长。

运维手册补充：查询 SQL、重放前置检查、只将 DEAD 改回 PENDING 的安全 SQL，以及重复投递为何不会重复奖励。

## 11. 完成定义

本轮只有同时满足以下条件才算完成：

- ADR 0001 已接受并与实际实现一致。
- `UserServiceImpl` 中 9 处 member/mall 调用全部清零，两个 `@DubboReference` 删除。
- user-provider POM 不再依赖 member-api/mall-api。
- Maven 禁令经过“临时加回依赖必失败”的反向验证。
- 五种事件均具备生产、消费、幂等和失败重投测试。
- broker 故障恢复测试证明事件不会因进程重启永久丢失。
- 管理端用户列表无 N+1 RPC，三端响应结构不变。
- Release 2 冒烟通过且 outbox 无 DEAD/长期 PENDING。
- CHANGELOG、ADR、运维手册与最终实现同步。

## 12. 后续工作

**ADR 0002《商品目录所有权》已定稿（Accepted）**：`docs/adr/0002-product-catalog-ownership.md`。
它不依赖本计划的完成 —— 写侧迁移开工前先把这条边界的责任划清更划算。

该 ADR 的结论是**移除** `mall → order` 依赖（而不是接受）：核实后 mall 的两处读取都是**冗余**的 ——
order 已经把权威的当前规格基础价通过 `ItemCheckDTO.price` 交给 mall；
券标题所需的商品名用 mall 本地数据即可 —— 取来源 `points_products.name` 作为完整标题并
**快照到现有的 `user_coupons.display_title`**（`linkedProductId` 继续留在 `rule_json`，无需新增列与迁移）。
两处删除后即可给 mall-provider 加 `bannedDependencies` 禁令。具体待办见该文件；本计划不实现它的任何一项。

> 该 ADR 的第一版结论为"接受 `mall → order` 只读依赖"，因核心事实有误（把 `CoffeeProductDTO.price`
> 误当成标准杯价格）**已作废**。
