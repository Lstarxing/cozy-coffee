# ADR 0002：商品目录的所有权与 mall 侧冗余读取的移除

- **状态**：Accepted（**修正版**）。第一版把 `CoffeeProductDTO.price` 误当成"标准杯价格"、结论写成
  "接受 `mall → order` 只读依赖"；第二版又误称 `user_coupons` 有 `linked_product_id` 列。
  **两版的相关描述均已作废，不要按旧文执行。**
- **日期**：2026-09-20（同日两轮修正）
- **相关**：ADR 0001《用户生命周期与会员权益事件边界》

## 背景

### 1. 商品目录的归属

8 张表全部位于 `cozy-order-provider`：`coffee_products`、`product_sku`、`product_addons`、
`coffee_product_addon_group`、`coffee_product_addon`、`coffee_origin`、`coffee_bean`、`coffee_blend`。

契约在 `cozy-order-api` 的 `OrderService`：

- **读**：`listCoffeeProducts()`（顾客菜单）、`getProduct(Long)`、`listOrigins` / `listBeans` / `listBlends`
- **管理端写**（**包括但不限于**，完整清单以 `OrderService` 为准）：
  `addProduct` / `updateProduct` / `deleteProduct` / `toggleProductStatus` / `listAllProducts`、
  `saveAddonGroups` / `listAddonCatalog`、
  `saveOrigin` / `deleteOrigin`、`saveBean` / `deleteBean`、`saveBlend` / `deleteBlend`

**写入方唯一**：网关 `AdminOrderController` → `OrderService` → order-provider 的 `ProductAdminService` /
`CoffeeContentAdminService`。没有其他域写这些表。

### 2. 第一版的核心事实错误

第一版断言「`CoffeeProductDTO.price` 表示标准杯价格，因此 mall 的券估值必须反查 order」。**不成立**：

**（a）`price` 不是统一的标准杯价格，而且经常是 NULL。**
`ProductAdminService.validateIntegrity` 强制价格互斥：

- `size_type = MEDIUM_LARGE` ⇒ **`price` 必须为 NULL**，价格在 `priceMedium` / `priceLarge`
- `size_type = DEFAULT` ⇒ `price` 必填，`priceMedium` / `priceLarge` 必须为 NULL

**（b）order 早已把权威的「当前规格基础价」交给 mall 了。**
`ProductPricingService.price(product, cupSize, temperature, sugarLevel, brewMethod, addonsJson)`
返回 `basePrice` 与 `addonFee` **分开**；`OrderPreviewer:110`（试算）与 `OrderCreator:202`（下单）填入：

```java
itemChecks.add(new ItemCheckDTO(product.getId(), unitPrice /* = priceResult.basePrice() */, ...));
```

即 **`ItemCheckDTO.price` = order 权威计算的、当前规格（杯型/温度/糖度/冲煮方式）的基础价，不含加料费**；
加料费另走 `addonsTotal` / `addonPrices` 单独统计。

### 3. 于是两处读取都是冗余的

| 位置 | 原意 | 核实结论 |
|---|---|---|
| `ExchangeCouponCalculator.java:80`（取 `getPrice()`） | 拿"标准杯价格"做折扣基准 | **两种情况都拿不到增量信息**：`MEDIUM_LARGE` 商品的 `price` 恒为 NULL，代码**本来就**落到 `item.getPrice()`；`DEFAULT` 商品的 `price` 与 `item.getPrice()`（同规格、不含加料）**相等** |
| `PointsMallServiceImpl.java:1998`（取 `getName()`） | 拼「XX兑换券」标题 | **纯展示**。而且**不需要跨域查**：券的来源是 mall 自己的 `points_products` 行（`coupon_type='EXCHANGE'` + `linked_product_id` + 自带 `name`），发券时本来就持有 `PointsProduct` |

**关于券实例的存储（第二版在这里说错了）**：`user_coupons` **没有** `linked_product_id` 列 ——
关联 ID 保存在 `rule_json.linkedProductId` 里；该列属于 `points_products`。
`user_coupons` 已有的是 **`display_title`**（varchar(100)），正好可以承载标题快照。

**而且 mall 对 `cozy-order-api` 的全部用法，就是这两个文件里的 4 行 import** ——
两处删掉，整条依赖随之消失，可以加禁令锁死。

## 决策

**移除 `mall → order` 依赖，而不是接受它。**

1. **`ItemCheckDTO.price` 升格为契约字段**：在 DTO 上写明「order 权威计算的当前规格基础价，不含加料费」，
   并补**守卫测试**。移除依赖后，它是 order 与 mall 之间**唯一**的券估值契约。
2. **`ExchangeCouponCalculator` 直接使用 `item.getPrice()`**，删除核销阶段的 `getProduct()` 调用。
3. **券标题：用 mall 本地数据 + 发券时快照**（不是"二选一"，两件事同时做）：
   - 积分兑换路径本来就持有 `PointsProduct` → 以 **`points_products.name` 作为完整券标题**
   - 发券时写入**现有的** `user_coupons.display_title` → **天然形成快照**，
     **不新增列、不新增 Flyway 迁移**
   - `linkedProductId` 继续保存在 `rule_json`，不变
   - 公共入口 `issueCouponToUser(EXCHANGE_<id>)` 若拿不到来源名称，**继续降级为「商品兑换券」**（现有兜底）
   - **注意不要再拼后缀**：现逻辑是 `productName + "兑换券"`，而 `points_products.name` 可能已含"券/兑换券"字样，
     直接当完整标题即可，避免出现「拿铁兑换券兑换券」
4. 两处读清零后，从 `cozy-mall-provider` 删除 `cozy-order-api`，并加 **Maven `bannedDependencies`** 禁令
   （含传递依赖）。

### 商品目录的所有权（不变）

- `cozy_order` **独占** 8 张表；其他域**不得**直连、**不得**整体复制目录。
- 写操作只经 `OrderService` 的管理端方法，由网关发起。
- 移除本依赖后，order 侧改 `coffee_products` 的**任何**字段（含 `price` / `priceMedium`）都不再波及 mall ——
  影响面收缩到 `ItemCheckDTO.price` **一个**契约字段。**这才是应该被保护的东西**；
  第一版把保护范围错画到了整个商品目录上。

### 回归范围（按真实覆盖修正，别以为现有测试兜得住）

- `CouponCalculatorTest` 的 linked-product 用例**只覆盖 `orderService` 为空时的 fallback**
  （用例注释原文"orderService 为空时回退到商品实际价"），**没有**覆盖 `getProduct().price` 成功路径。
  即这条路径**当前没有测试保护** —— 删它不需要改测试，但**必须补**新口径的断言。
- 券标题的兜底覆盖在 **`CouponTemplateTest.exchangeFromCode`**（断言 `"商品兑换券"`），
  **不在** `PointsMallServiceImplTest`。改标题逻辑必须同步改这条。
- order 侧：`ProductPricingServiceTest`。
- 端到端：指定商品兑换券的**预览**与**核销**，`MEDIUM_LARGE` 与 `DEFAULT` 商品各验一个。

**必须新增的守卫**：一条钉住 `ItemCheckDTO.price` 语义的测试 ——
order 侧构造 `ItemCheckDTO` 时填入的是 `basePrice`（基础规格价），**不是**含加料的 `lineTotal`。

## 被否决的方案

| 方案 | 否决理由 |
|---|---|
| 接受 `mall → order` 只读依赖（**第一版结论**） | 这条依赖是**冗余**而非必要的；为一个不带来任何信息的 RPC 长期背跨域契约与回归责任，不划算 |
| 抽取 `catalog` 微服务 | 8 张表 + 新部署面，只为消除一处冗余读取，收益明显不足 |
| 把券估值上移 BFF | 违反 ADR 0001 §1：BFF 不承载业务规则 |
| mall 直连 order 的库 | 跨域 DB 耦合，比 RPC 更糟 |
| 事件驱动同步商品（`PRODUCT_CHANGED`） | 为 2 个**可以删掉**的读点引入事件版本、顺序、回填与对账 |
| 保留 `getProduct()` 但加缓存 | 缓存只能缓解开销，消不掉冗余本身与契约负担 |
| 新增 `user_coupons.linked_product_name` 列 + 迁移（**第二版提议**） | 不必要：`display_title` 已经存在且语义就是显示标题，直接写它即可 |

## 后果

**正面** —— mall 不再有任何商品目录依赖，可以用 Maven 禁令锁死；券估值只用 order 已经算好的权威价，
**试算与下单口径天然一致**（同一个 `ItemCheckDTO`）；order 改商品表的爆炸半径收缩到 1 个契约字段；
标题快照**零迁移成本**（复用 `display_title`）。

**负面** —— 需要一次跨模块改动（mall 两个文件 + `ItemCheckDTO` 契约注释 + 一条守卫测试 + 禁令）；
券标题采用"发券时快照"，即**改价改名后旧券标题不变** —— 对已发出的券而言这通常**正是**想要的
（券面信息不应随后台改价而变），但需产品侧确认这一口径。

## 实施状态

- 本 ADR 只定义决策，**尚未改代码**。
- 待办（单独立项）：
  1. mall-api `ItemCheckDTO.price` 补契约注释；order 侧补守卫测试
  2. 删 `ExchangeCouponCalculator` 的 `getProduct()`，改用 `item.getPrice()`
  3. 积分兑换发券时把 `PointsProduct.name` 写入 `user_coupons.display_title`（完整标题，不再拼后缀）；
     公共 `issueCouponToUser(EXCHANGE_<id>)` 无来源名时保持「商品兑换券」兜底
  4. 删 `cozy-mall-provider` 的 `cozy-order-api` 依赖 + 加 `bannedDependencies`（含传递依赖）
- 第一版（"接受只读依赖"）与第二版（"新增名称列"）的相关描述**均已作废**。
