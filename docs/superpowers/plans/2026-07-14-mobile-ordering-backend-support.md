# Mobile Ordering Backend Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the backend contracts required for safe mobile checkout: cart validation/preview, stable business errors, and idempotent order creation.

**Architecture:** Keep HTTP concerns in `cozy-gateway`, domain validation and pricing in `cozy-order-provider`, and shared request/response contracts in `cozy-order-api`. Store the idempotency key with the order and return the existing order for repeated requests from the same user.

**Tech Stack:** Java 17, Spring Boot, Apache Dubbo, MyBatis-Plus, MySQL/Flyway, JUnit 5

---

## File map

- Create `cozy-coffee-backend/cozy-api/cozy-order-api/src/main/java/com/cozy/order/dto/request/CartCheckRequest.java`: cart validation input.
- Create `cozy-coffee-backend/cozy-api/cozy-order-api/src/main/java/com/cozy/order/dto/response/CheckoutPreviewDTO.java`: authoritative preview result.
- Create `cozy-coffee-backend/cozy-api/cozy-order-api/src/main/java/com/cozy/order/dto/response/CartCheckResultDTO.java`: changed and invalid item result.
- Modify `cozy-coffee-backend/cozy-api/cozy-order-api/src/main/java/com/cozy/order/api/OrderService.java`: expose check and idempotent create operations.
- Modify `cozy-coffee-backend/cozy-gateway/src/main/java/com/cozy/gateway/controller/OrderController.java`: read `Idempotency-Key` and add `/order/cart/check`.
- Modify `cozy-coffee-backend/cozy-gateway/src/main/java/com/cozy/gateway/service/OrderCoordinatorService.java`: pass user and idempotency context.
- Create `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/service/OrderPreviewService.java`: validate items and calculate preview without persistence.
- Modify `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/service/impl/OrderCreationService.java`: reuse preview validation.
- Modify `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/entity/ShopOrder.java`: persist idempotency key.
- Modify `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/mapper/ShopOrderMapper.java`: find an order by user and key.
- Create `cozy-coffee-backend/mysql/migrations/V20260714_014__add_order_idempotency_key.sql` and matching `.down.sql`.
- Create tests under `cozy-order-provider/src/test/java/com/cozy/order/service/` and `cozy-gateway/src/test/java/com/cozy/gateway/controller/`.

### Task 1: Define cart-check contracts

**Files:**
- Create: `cozy-coffee-backend/cozy-api/cozy-order-api/src/main/java/com/cozy/order/dto/request/CartCheckRequest.java`
- Create: `cozy-coffee-backend/cozy-api/cozy-order-api/src/main/java/com/cozy/order/dto/response/CheckoutPreviewDTO.java`
- Create: `cozy-coffee-backend/cozy-api/cozy-order-api/src/main/java/com/cozy/order/dto/response/CartCheckResultDTO.java`
- Modify: `cozy-coffee-backend/cozy-api/cozy-order-api/src/main/java/com/cozy/order/api/OrderService.java`

- [ ] **Step 1: Add a compile-time contract test**

Add `OrderApiContractTest.java` that constructs `CartCheckRequest`, sets `items`, `couponCode`, `storeId`, and asserts getters plus `CheckoutPreviewDTO.previewToken` are available.

- [ ] **Step 2: Run the test and verify compilation fails**

Run: `mvn -pl cozy-api/cozy-order-api -am test -Dtest=OrderApiContractTest`

Expected: FAIL because the DTOs do not exist.

- [ ] **Step 3: Add DTOs and service signatures**

Use these public fields:

```java
// CartCheckRequest
private List<OrderItemRequest> items;
private String couponCode;
private List<String> addonCouponCodes;
private Long storeId;
private String pickupTime;

// CheckoutPreviewDTO
private BigDecimal subtotal;
private BigDecimal discount;
private BigDecimal payable;
private String previewToken;
private LocalDateTime expiresAt;

// CartCheckResultDTO
private List<Long> changedItems;
private List<Long> invalidItems;
private CheckoutPreviewDTO preview;
```

Add to `OrderService`:

```java
CartCheckResultDTO checkCart(Long userId, String memberLevel, CartCheckRequest request);
ShopOrderDTO createOrder(Long userId, String memberLevel, String idempotencyKey, CreateOrderRequest request);
```

- [ ] **Step 4: Run API tests**

Run: `mvn -pl cozy-api/cozy-order-api -am test -Dtest=OrderApiContractTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add cozy-coffee-backend/cozy-api/cozy-order-api
git commit -m "feat(order-api): define checkout preview contracts"
```

### Task 2: Extract authoritative preview validation

**Files:**
- Create: `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/service/OrderPreviewService.java`
- Create: `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/test/java/com/cozy/order/service/OrderPreviewServiceTest.java`
- Modify: `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/service/impl/OrderServiceImpl.java`

- [ ] **Step 1: Write failing tests**

Cover active items, offline items, invalid SKU options, a LARGE surcharge, and a stable token for identical normalized input. Assert that changing quantity changes the token.

- [ ] **Step 2: Run the focused test**

Run: `mvn -pl cozy-provider/cozy-order-provider -am test -Dtest=OrderPreviewServiceTest`

Expected: FAIL because `OrderPreviewService` does not exist.

- [ ] **Step 3: Implement the service**

Expose:

```java
public CartCheckResultDTO preview(Long userId, String memberLevel, CartCheckRequest request)
```

Reuse `CoffeeProductMapper` and `ProductSkuValidationService`. Build `previewToken` as a SHA-256 digest of a canonical JSON payload containing normalized items, coupon IDs/codes, member level, store ID, and pricing-rule version `v1`. Do not persist or consume coupons.

- [ ] **Step 4: Run preview and baseline order tests**

Run: `mvn -pl cozy-provider/cozy-order-provider -am test -Dtest=OrderPreviewServiceTest,OrderFlowBaselineTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add cozy-coffee-backend/cozy-provider/cozy-order-provider
git commit -m "feat(order): add authoritative cart preview"
```

### Task 3: Expose POST /api/order/cart/check

**Files:**
- Modify: `cozy-coffee-backend/cozy-gateway/src/main/java/com/cozy/gateway/controller/OrderController.java`
- Modify: `cozy-coffee-backend/cozy-gateway/src/main/java/com/cozy/gateway/service/OrderCoordinatorService.java`
- Create: `cozy-coffee-backend/cozy-gateway/src/test/java/com/cozy/gateway/controller/OrderControllerTest.java`

- [ ] **Step 1: Write a failing MockMvc test**

POST `/api/order/cart/check` with an authenticated user and one item. Expect HTTP 200, business code 200, and `data.preview.previewToken`.

- [ ] **Step 2: Run the test**

Run: `mvn -pl cozy-gateway -am test -Dtest=OrderControllerTest`

Expected: FAIL with 404.

- [ ] **Step 3: Add the endpoint**

```java
@PostMapping("/cart/check")
public Result<CartCheckResultDTO> checkCart(@Valid @RequestBody CartCheckRequest request) {
    return Result.success(orderCoordinatorService.checkCart(AuthUtil.requireUserId(), request));
}
```

The coordinator obtains member level exactly as existing order creation does and delegates to `OrderService.checkCart`.

- [ ] **Step 4: Run gateway tests**

Run: `mvn -pl cozy-gateway -am test -Dtest=OrderControllerTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add cozy-coffee-backend/cozy-gateway
git commit -m "feat(gateway): expose cart validation endpoint"
```

### Task 4: Persist and enforce Idempotency-Key

**Files:**
- Create: `cozy-coffee-backend/mysql/migrations/V20260714_014__add_order_idempotency_key.sql`
- Create: `cozy-coffee-backend/mysql/migrations/V20260714_014__add_order_idempotency_key.down.sql`
- Modify: `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/entity/ShopOrder.java`
- Modify: `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/mapper/ShopOrderMapper.java`
- Modify: `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/service/impl/OrderCreationService.java`
- Create: `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/test/java/com/cozy/order/service/OrderIdempotencyTest.java`

- [ ] **Step 1: Write failing duplicate-submit tests**

Call create twice with the same user and key; assert the same order ID is returned and only one row exists. Call with a different user and the same key; assert a different order can be created.

- [ ] **Step 2: Run the test**

Run: `mvn -pl cozy-provider/cozy-order-provider -am test -Dtest=OrderIdempotencyTest`

Expected: FAIL because the key is ignored.

- [ ] **Step 3: Add the migration**

```sql
ALTER TABLE shop_orders ADD COLUMN idempotency_key VARCHAR(64) NULL;
CREATE UNIQUE INDEX uk_shop_orders_user_idempotency
  ON shop_orders(user_id, idempotency_key);
```

The down migration drops the index and column.

- [ ] **Step 4: Implement lookup-before-create and duplicate-key recovery**

Reject blank or keys longer than 64 characters. Before coupon consumption or persistence, query by `(userId, idempotencyKey)`. On a unique-key race, re-query and return the existing order. Never consume a coupon twice.

- [ ] **Step 5: Run focused and baseline tests**

Run: `mvn -pl cozy-provider/cozy-order-provider -am test -Dtest=OrderIdempotencyTest,OrderFlowBaselineTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add cozy-coffee-backend/mysql/migrations cozy-coffee-backend/cozy-provider/cozy-order-provider
git commit -m "feat(order): make order creation idempotent"
```

### Task 5: Wire Idempotency-Key through HTTP and Dubbo

**Files:**
- Modify: `cozy-coffee-backend/cozy-gateway/src/main/java/com/cozy/gateway/controller/OrderController.java`
- Modify: `cozy-coffee-backend/cozy-gateway/src/main/java/com/cozy/gateway/service/OrderCoordinatorService.java`
- Modify: `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/service/impl/OrderServiceImpl.java`
- Modify: `cozy-coffee-backend/cozy-api/cozy-order-api/src/main/java/com/cozy/order/api/OrderService.java`

- [ ] **Step 1: Extend the controller test**

POST twice with the same `Idempotency-Key`; assert both responses contain the same order ID. Missing header returns business validation failure.

- [ ] **Step 2: Run the test and verify failure**

Run: `mvn -pl cozy-gateway -am test -Dtest=OrderControllerTest`

- [ ] **Step 3: Pass the header through all layers**

```java
public Result<ShopOrderDTO> createOrder(
    @RequestHeader("Idempotency-Key") String idempotencyKey,
    @Valid @RequestBody CreateOrderRequest request) {
  return Result.success(orderCoordinatorService.createOrder(
      AuthUtil.requireUserId(), idempotencyKey, request));
}
```

- [ ] **Step 4: Run backend verification**

Run: `mvn test`

Workdir: `cozy-coffee-backend`

Expected: all tests PASS.

- [ ] **Step 5: Commit**

```bash
git add cozy-coffee-backend
git commit -m "feat(order): expose idempotent create contract"
```

### Task 6: Add WeChat mini-program session exchange

**Files:**
- Create: `cozy-coffee-backend/cozy-gateway/src/main/java/com/cozy/gateway/dto/WechatSessionRequest.java`
- Create: `cozy-coffee-backend/cozy-gateway/src/main/java/com/cozy/gateway/service/WechatSessionService.java`
- Modify: `cozy-coffee-backend/cozy-gateway/src/main/java/com/cozy/gateway/controller/AuthController.java`
- Modify: `cozy-coffee-backend/cozy-gateway/src/main/java/com/cozy/gateway/service/AuthService.java`
- Create: `cozy-coffee-backend/cozy-gateway/src/test/java/com/cozy/gateway/service/WechatSessionServiceTest.java`

- [ ] **Step 1: Write failing session tests**

Given a valid one-time WeChat code, assert the service exchanges it for an `openid`, finds or creates the linked user, and returns the normal CozyCoffee token. Invalid or already-used codes return a stable auth error and never log `session_key`.

- [ ] **Step 2: Run the test**

Run: `mvn -pl cozy-gateway -am test -Dtest=WechatSessionServiceTest`

Expected: FAIL because the service does not exist.

- [ ] **Step 3: Implement the exchange boundary**

Add `POST /api/auth/wechat/session` with body `{ "code": "..." }`. Keep the WeChat HTTP client behind `WechatSessionService` so tests inject a fake response. Read App ID and secret from environment-backed Spring configuration; never commit credentials.

- [ ] **Step 4: Verify**

Run: `mvn -pl cozy-gateway -am test -Dtest=WechatSessionServiceTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add cozy-coffee-backend/cozy-gateway
git commit -m "feat(auth): add wechat session exchange"
```

### Task 7: Return stable checkout business codes

**Files:**
- Create: `cozy-coffee-backend/cozy-common/src/main/java/com/cozy/common/exception/BusinessErrorCode.java`
- Modify: `cozy-coffee-backend/cozy-common/src/main/java/com/cozy/common/exception/BusinessException.java`
- Modify: `cozy-coffee-backend/cozy-gateway/src/main/java/com/cozy/gateway/exception/GlobalExceptionHandler.java`
- Modify: `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/service/OrderPreviewService.java`
- Create: `cozy-coffee-backend/cozy-gateway/src/test/java/com/cozy/gateway/exception/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: Write failing error-shape tests**

Assert `STORE_CLOSED`, `ITEM_OFFLINE`, `ITEM_CHANGED`, `COUPON_EXPIRED`, and `PREVIEW_EXPIRED` responses include stable `errorCode`, readable `message`, and `retryable`.

- [ ] **Step 2: Run the test**

Run: `mvn -pl cozy-gateway -am test -Dtest=GlobalExceptionHandlerTest`

- [ ] **Step 3: Implement coded business errors**

Extend `BusinessException` with `BusinessErrorCode code` and `boolean retryable`. Preserve the existing message constructor for unrelated legacy call sites. The handler returns these fields without parsing message text.

- [ ] **Step 4: Run the full backend suite**

Run: `mvn test`

Workdir: `cozy-coffee-backend`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add cozy-coffee-backend/cozy-common cozy-coffee-backend/cozy-gateway cozy-coffee-backend/cozy-provider/cozy-order-provider
git commit -m "feat(api): add stable checkout error codes"
```
