# OrderServiceImpl 拆分方案

## 现状

- **行数**：1456 行（`OrderServiceImpl.java`）
- **public 方法**：19 个（全部是 `OrderService` 接口方法，Dubbo 对外契约）
- **private 方法**：17 个
- **已有辅助类**：`OrderDtoConverter`（@Component，DTO 转换）、`OrderRewardService`（@Component，积分倍率/黑卡加速包）、`MenuCacheService`（@Component，菜单三级缓存）、`OrderStateMachine`（enum，状态机）、`PickupCodeService`（@Service，取餐码生成）、`ProductSkuValidationService`（@Service，SKU 校验）、`OutboxService`（@Service，Outbox 投递）
- **死引用**：`monthlyTaskService`、`userService` 两个 `@DubboReference` 字段声明但全文件无任何调用（v6.2 已解耦到 MQ 消费者，遗留未清理）。**本次一并删除**（顺手 cleanup）

## public 方法清单（19 个，按职责分组）

| 分组 | 方法 | 行数 |
|---|---|---|
| 商品查询 | `listCoffeeProducts` / `getProduct` / `listAllProducts` | 109-123, 1033-1039 |
| 订单查询 | `listUserOrders` / `getOrder` / `getOrderDetail` / `listAllOrders` / `getOrderStatusCounts` / `getMonthlyStats` | 645-754, 775-789, 1391-1455 |
| 订单创建 | `createOrder` | 130-631 |
| 状态变更 | `updateOrderStatus` / `acceptOrder` / `completeOrder` / `cancelOrder` / `cancelUserOrder` | 758-982 |
| 商品管理 | `addProduct` / `updateProduct` / `deleteProduct` / `toggleProductStatus` | 1041-1161 |

## private 方法清单（17 个，按共享度分类）

**仅 Creation 用**：`doCreateOrderInTx`（@Transactional）、`buildModifiersJson`、`generateOrderNo`
**仅 Command 用**：`doCompleteInTx`（@Transactional）、`resolveMemberLevel`、`resolvePointsEarned`、`checkFirstOrder`、`checkNewProduct`
**仅 ProductAdmin 用**：`invalidateMenuCache`（可直接复用 `menuCacheService.invalidate()`）
**Creation + Command 共享**：`publishCouponRollbackEvent`、`parseAddonCouponIds`
**全服务共享（Query/Creation/Command 都用）**：`toOrderDTO`（2 个重载）、`toOrderDTOWithMember`、`getOrderItemsByOrderId`、`populateExpiryInfo`、`syncPendingTimeoutIndex`、`removePendingTimeoutIndex`

## 拆分方案

### 新增 Service 1: OrderQueryService（约 170 行）

- **职责**：所有读侧查询（商品 + 订单 + 月度统计）
- **public 方法**：`listCoffeeProducts` / `getProduct` / `listAllProducts` / `listUserOrders` / `getOrder` / `getOrderDetail` / `listAllOrders` / `getOrderStatusCounts` / `getMonthlyStats`
- **依赖**：`productMapper`、`orderMapper`、`orderItemMapper`、`menuCacheService`、`dtoConverter`、`memberService`（@DubboReference）、`OrderDtoEnricher`
- **事务**：纯查询，无需 `@Transactional`
- **位置**：`com.cozy.order.service.impl.OrderQueryService`

### 新增 Service 2: OrderCreationService（约 520 行）

- **职责**：订单创建主流程（验证商品 -> 金额计算 -> 券核销 -> 取餐码 -> 奖励预估 -> 落库）
- **public 方法**：`createOrder`（保留非 `@Transactional`，券核销在事务外）
- **private 方法**：`doCreateOrderInTx`（@Transactional，**保留 self-invocation 模式**）、`buildModifiersJson`、`generateOrderNo`
- **依赖**：`productMapper`、`orderMapper`、`orderItemMapper`、`pickupCodeService`、`skuValidationService`、`objectMapper`、`dtoConverter`、`rewardService`、`memberService`（@DubboReference）、`pointsMallService`（@DubboReference）、`OrderDtoEnricher`、`OrderTimeoutIndexService`、`CouponRollbackPublisher`
- **位置**：`com.cozy.order.service.impl.OrderCreationService`

### 新增 Service 3: OrderCommandService（约 210 行）

- **职责**：订单状态变更（接单/完成/取消/管理端状态更新）
- **public 方法**：`updateOrderStatus`（@Transactional）、`acceptOrder`（@Transactional）、`completeOrder`（非 @Transactional，与现状一致）、`cancelOrder`（@Transactional）、`cancelUserOrder`（@Transactional）
- **private 方法**：`doCompleteInTx`（@Transactional，**保留 self-invocation**）、`resolveMemberLevel`、`resolvePointsEarned`、`checkFirstOrder`、`checkNewProduct`
- **依赖**：`orderMapper`、`orderItemMapper`、`productMapper`、`pickupCodeService`、`rewardService`、`memberService`（@DubboReference）、`OrderDtoEnricher`、`OrderTimeoutIndexService`、`CouponRollbackPublisher`
- **位置**：`com.cozy.order.service.impl.OrderCommandService`

### 新增 Service 4: ProductAdminService（约 130 行）

- **职责**：商品 CRUD（管理端）
- **public 方法**：`addProduct` / `updateProduct` / `deleteProduct` / `toggleProductStatus`（均 @Transactional）
- **依赖**：`productMapper`、`dtoConverter`、`menuCacheService`（直接调用 `invalidate()`，**移除私有 `invalidateMenuCache`**，已有 `MenuCacheService.invalidate()` 公开方法）
- **位置**：`com.cozy.order.service.impl.ProductAdminService`

### 新增 Service 5: OrderDtoEnricher（约 110 行）

- **职责**：`ShopOrder` entity -> `ShopOrderDTO` 的完整装配（含会员信息查询、商品明细加载、过期信息填充）
- **public 方法**：`toOrderDTO(ShopOrder, items)`、`toOrderDTO(ShopOrder, items, MemberDTO)`、`toOrderDTOWithMember(ShopOrder, items, MemberDTO)`、`getOrderItemsByOrderId(orderId)`、`populateExpiryInfo(entity, dto)`
- **依赖**：`dtoConverter`、`memberService`（@DubboReference）、`orderItemMapper`、`@Value("${cozy.order.timeout-cancel.timeout-minutes:1}") orderTimeoutMinutes`
- **位置**：`com.cozy.order.service.impl.OrderDtoEnricher`
- **理由**：`toOrderDTO` 被 Query/Creation/Command 三类服务调用，是最核心的共享逻辑，必须独立

### 新增 Service 6: OrderInfraService（约 80 行）

- **职责**：订单相关基础设施操作（Redis ZSet 超时索引 + Outbox 券回滚事件）
- **public 方法**：
  - `syncPendingTimeoutIndex(ShopOrder)`、`removePendingTimeoutIndex(Long orderId)` -- Redis ZSet 维护
  - `publishCouponRollbackEvent(ShopOrder)` -- 构造 `OrderCancelledEvent` 并写入 outbox
- **private 方法**：`parseAddonCouponIds(ShopOrder)` -- 仅 publish 用，保留 private
- **依赖**：`stringRedisTemplate`、`outboxService`、`objectMapper`、`@Value orderTimeoutMinutes`
- **位置**：`com.cozy.order.service.impl.OrderInfraService`
- **理由**：TimeoutIndex 和 CouponRollback 都属于订单基础设施（Redis/MQ/Outbox），职责接近。合并避免 Service 碎片化。以后新增 `sendDelayMessage` / `publishOrderEvent` 等也可继续放这里

### OrderServiceImpl 保留（约 150 行）

- **角色**：`@DubboService` 入口，实现 `OrderService` 接口，19 个方法全部委托
- **依赖**：6 个新 Service（构造器注入）
- **移除**：所有 `@DubboReference` 字段（迁移到各子 Service；死引用 `monthlyTaskService` / `userService` 直接删除）、所有 mapper/redis/objectMapper 字段（不再直接使用）、`@Value orderTimeoutMinutes`（迁移到 `OrderDtoEnricher` / `OrderInfraService`）
- **保留**：`@DubboService` + `@Slf4j` + `@RequiredArgsConstructor`
- **委托示例**：`public ShopOrderDTO createOrder(Long u, String l, CreateOrderRequest r) { return creationService.createOrder(u, l, r); }`

## 实施步骤

1. **创建 `OrderDtoEnricher`**：迁移 `toOrderDTO` 两个重载 + `toOrderDTOWithMember` + `getOrderItemsByOrderId` + `populateExpiryInfo`。注入 `memberService`、`orderItemMapper`、`dtoConverter`、`@Value orderTimeoutMinutes`。编译验证
2. **创建 `OrderInfraService`**：合并 `syncPendingTimeoutIndex` + `removePendingTimeoutIndex` + `publishCouponRollbackEvent` + `parseAddonCouponIds`（private）。注入 `stringRedisTemplate` + `outboxService` + `objectMapper` + `@Value orderTimeoutMinutes`
3. **创建 `ProductAdminService`**：迁移 4 个商品管理方法。`invalidateMenuCache` 删除，直接调用 `menuCacheService.invalidate()`
4. **创建 `OrderQueryService`**：迁移 9 个查询方法。`toOrderDTO` 调用改为 `dtoEnricher.toOrderDTO`
5. **创建 `OrderCreationService`**：迁移 `createOrder` + `doCreateOrderInTx` + `buildModifiersJson` + `generateOrderNo`。`toOrderDTO` 改委托；`syncPendingTimeoutIndex` / `publishCouponRollbackEvent` 改委托
6. **创建 `OrderCommandService`**：迁移 5 个状态变更方法 + `doCompleteInTx` + `resolveMemberLevel` + `resolvePointsEarned` + `checkFirstOrder` + `checkNewProduct`。同样委托共享服务
7. **重构 `OrderServiceImpl`**：清空方法体，全部委托；移除字段，仅保留 6 个子 Service 依赖；**删除死引用 `monthlyTaskService` / `userService`**
8. **编译 + 运行 `OrderFlowBaselineTest`**：3 个 baseline test 必须全绿

## 风险与对策

### 风险 1：@Transactional self-invocation 语义（**关键**）

- **现状**：`doCreateOrderInTx` 和 `doCompleteInTx` 是 private @Transactional 方法，被同类 public 方法 `this.` 调用。Spring AOP 代理对 private 方法和 self-invocation 都不生效，所以**当前实际无事务边界**，每条 SQL autocommit
- **对策**：在新 Service 中**保留相同 self-invocation 模式**--`doCreateOrderInTx` / `doCompleteInTx` 仍为 private @Transactional，由同类 public 方法 `this.` 调用。行为完全一致
- **严禁**：不要改成 public @Transactional 后跨 bean 调用--这会**真正启用事务**，属于行为变更，违反约束

### 风险 2：`OrderFlowBaselineTest` 跨 JVM Dubbo 调用

- 该测试通过 `OrderService` 接口注入（实际是 Dubbo 代理），走完整 RPC 链路。委托后调用链多一层本地方法调用，无 RPC 开销，行为不变
- `@BeforeTransaction` 重置 BOGO 券状态，不受重构影响

### 风险 3：`@DubboReference` 分布到多个 Service 类

- 当前 4 个 `@DubboReference` 集中在 `OrderServiceImpl`。拆分后 `memberService`（Creation/Command/Query/DtoEnricher）、`pointsMallService`（Creation）需要分别声明
- Dubbo 对 `@DubboReference` 在任意 Spring bean 中都生效，无功能问题
- **死引用 `monthlyTaskService` / `userService` 本次直接删除**（顺手 cleanup，已确认 0 调用）

### 风险 4：循环依赖

- `OrderCreationService` 和 `OrderCommandService` 都依赖 `CouponRollbackPublisher`（独立）和 `OrderDtoEnricher`（独立）。无 Creation ↔ Command 互相依赖，无循环
- `OrderServiceImpl` 依赖 7 个子 Service，子 Service 不依赖 `OrderServiceImpl`，无循环

### 风险 5：`orderTimeoutMinutes` 多处注入

- `OrderDtoEnricher`（用于 `populateExpiryInfo`）和 `OrderTimeoutIndexService`（用于 `syncPendingTimeoutIndex`）各需一份 `@Value`。Spring 允许同一 property 注入多 bean，无问题

### 风险 6：包路径

- 所有新 Service 放在 `com.cozy.order.service.impl` 包，与现有 `OrderDtoConverter` / `OrderRewardService` / `MenuCacheService` 一致。`PickupCodeService` / `ProductSkuValidationService` 在 `com.cozy.order.service` 包--本次不调整

## 关键决策摘要

1. **拆 6 个新 Service + 1 个 thin delegator**：Query/Creation/Command/ProductAdmin 4 个业务 Service + DtoEnricher/OrderInfraService 2 个共享 Service。OrderServiceImpl 仅作 Dubbo 入口
2. **@Transactional self-invocation 模式原样保留**：`doCreateOrderInTx` / `doCompleteInTx` 不变 private + self-call，避免改变事务语义（当前实际无 tx 边界，保留此行为）
3. **共享逻辑全部独立成 Service**：DTO 装配（DtoEnricher）+ Redis/MQ/Outbox 基础设施（OrderInfraService，合并 TimeoutIndex 和 CouponRollback）各自成 bean，避免循环依赖
4. **`invalidateMenuCache` 删除复用**：`MenuCacheService.invalidate()` 已是公开方法，直接调用
5. **死引用 `monthlyTaskService` / `userService` 顺手删除**：已确认 0 调用，cleanup

## Technical Debt（本次不修，记录后续处理）

### @Transactional self-invocation 失效

**现象**：`OrderCreationService.doCreateOrderInTx()` 和 `OrderCommandService.doCompleteInTx()`（拆分后位置）是 `private @Transactional` 方法，被同类 public 方法 `this.` 调用。Spring AOP 代理对 private 方法和 self-invocation 都不生效，**实际无事务边界**，每条 SQL autocommit。

**当前状态**：是 Bug，不是设计。但本次 refactor 不修，保留原行为避免引入新风险。

**后续修复方向**（独立 session）：
- 将 `doCreateOrderInTx` / `doCompleteInTx` 改为 public @Transactional
- 通过 self-injection（`@Lazy @Autowired private OrderCreationService self;`）或 `ApplicationContext.getBean()` 获取代理对象调用
- 或者拆成独立的 `OrderCreationTxService` bean，跨 bean 调用 public @Transactional 方法
- 修复后需确认所有 SQL 在事务边界内（DB 写顺序、异常回滚行为）
- 加测试验证事务回滚（故意 throw，确认 DB 状态回滚）

**风险**：修复后会真正启用事务，可能暴露之前 autocommit 时隐藏的问题（如重复 INSERT、部分写失败等）。需要充分回归测试。
