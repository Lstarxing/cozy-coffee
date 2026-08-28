# Java Architecture Rules（cozy-coffee）

> AI 代码审计统一规则。审前先读本文件，按「分层 / 命名 / 职责边界」静态扫描，**只审不修改**，每项发现引用真实文件路径与类名。
> 没有证据的问题不要猜；个人风格偏好标 P3，不当作架构错误。

## 分层 Package

- `controller` / `service` / `service.impl` / `mapper` / `entity` / `dto.request` / `dto.response` / `enums`
- 跨模块共享放 `cozy-common`
- 新增业务能力按职责落 package，不塞 controller / service

## 命名

- `XxxController` / `XxxService` / `XxxServiceImpl` / `XxxMapper` / `XxxDTO` / `XxxRequest` / `XxxResponse`
- Service 名与职责匹配：`XxxResolver`（解析配置）、`XxxValidator`（规则校验）、`XxxCalculator`（纯计算）、`XxxAssembler`（DTO 转换）
- 避免 `XxxManager` / `XxxHelper` / `XxxUtil` 承担业务职责
- boolean 方法用 is/has/can/should 语义；`getXxx()` 不执行计算/副作用

## 职责边界（V2 商品 / 订单）

- Controller = transport（协议 / 参数）
- Service = use case / 应用编排
- Validator = 规则校验（规格合法性：size/temp/sugar/addon 组 min-max/默认项）
- Resolver = 解析商品配置（加料组 → `price_delta`）
- Pricing = 价格计算（统一定价核心，preview / create 共享；只读后端配置）
- Assembler = DTO/VO 转换
- Mapper = 持久化访问（无业务判断）
- Entity 不做 API 请求/响应对象（用 DTO）
- 订单快照：`addons_json` 存成交时实际生效价格（`price_delta`，含默认项），`unit_price` 存规格价快照

## 概念词典（V2）

- 商品 Product / 加料 Addon / 加料组 AddonGroup / 规格 Specification
- 杯型 Size / 温度 Temperature / 甜度 SugarLevel（DB `sugar_type` / `default_sugar_level`）
- 价格增量 PriceDelta / 成交价 FinalPrice / 订单快照 Snapshot
- 拼配 Blend / 单品豆档案 Bean / 来源主题 Origin
- 禁止混用：addon / option / modifier / extra / ingredient；price / amount / fee；validate / check / verify
- **V2 无 SKU 概念**：不新建 SKU 命名；历史 `Sku` 命名视为待清理残留

## Forbidden

- Controller → Mapper；Service → Controller；Mapper → Service；Entity → Controller / VO
- ServiceImpl → HTTP 协议对象
- Utils/Helper 承担业务逻辑
- 前端传价参与定价（V2 定价只读后端 `coffee_product_addon.price_delta`，未绑定 addon 一律拒绝）
- 硬编码规格加价（大杯 +3 等；基础价按 `size_type` 读价格列）
