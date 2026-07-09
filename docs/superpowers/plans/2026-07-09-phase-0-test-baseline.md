# Phase 0: Test Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish a minimal "behavior is currently X" baseline before any refactor, so Phase 1+ changes can be verified as behavior-preserving.

**Architecture:** Leverage existing `@SpringBootTest` pattern from `PointsConsistencyTest.java`. Add ONE new integration test in `cozy-order-provider` covering the happy-path order->complete->points flow with a single BOGO coupon. Full 5-coupon × complete/cancel matrix is deferred to Phase 1+ (added incrementally alongside each fix).

**Tech Stack:** JUnit 5, Spring Boot Test, MyBatis-Plus, Dubbo references (real, not mocked -- tests assume backend services running per [[local-dev-run-mode]] memory: IDE runs 5 microservices, Docker runs infra).

## Global Constraints

- **Backend running mode:** 5 microservices run in IDE (cozy-user-provider, cozy-member-provider, cozy-order-provider, cozy-mall-provider, cozy-gateway). MySQL/Redis/Nacos/RocketMQ run in Docker. Tests must NOT mock Dubbo references -- they hit real services.
- **Database:** Tests use `@Transactional` rollback by default (matches `PointsConsistencyTest` pattern). No test data persisted after test run.
- **Test user IDs:** Existing tests use `999L` and `998L`. New tests use `997L` to avoid collision.
- **Migration filename format:** `V<YYYYMMDD>_<NNN>__<description>.sql` (Flyway convention, matching existing `docker/mysql/init/` pattern).
- **CHANGELOG location:** `C:\Users\dell\Desktop\surx-note\CozyCoffee\CHANGELOG.md` (NOT in repo -- see [[changelog-workflow]] memory).
- **Commit message style:** `<type>(<scope>): <subject>` lowercase (matches `git log --oneline` recent commits like `fix(web):`, `feat(storage):`).
- **No placeholder values:** Every code block in this plan is complete and runnable verbatim.

---

## Task 1: Verify Existing Tests Pass

**Files:**
- Read: `cozy-coffee-backend/cozy-provider/cozy-member-provider/src/test/java/com/cozy/member/service/PointsConsistencyTest.java`
- Read: `cozy-coffee-backend/cozy-provider/cozy-member-provider/src/test/java/com/cozy/member/service/PointsFIFOConsistencyTest.java`
- No modifications

**Interfaces:**
- Consumes: Running `cozy-member-provider` service (port 8082 per docker-compose.yml comments), MySQL on 3306, Redis on 6379, Nacos on 8848
- Produces: Confirmation that the 2 existing tests are green -- this is the baseline assumption for all later tasks

**Prerequisite check:** Before running tests, confirm backend services are up. If `PointsConsistencyTest` fails because member-provider isn't running, the test failure is environmental, not a code regression.

- [ ] **Step 1: Confirm backend services are running**

Dubbo providers don't expose Spring Boot Actuator HTTP endpoints -- they register with Nacos instead. Use Nacos query as the liveness check.

Run from project root:
```bash
curl -s "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=cozy-member-provider" | grep -oE '"hosts":\[[^]]*\]' | head -1
```
Expected: a non-empty `hosts` array containing `"healthy":true` for `cozy-member-provider`. If `"hosts":[]` (empty array) or curl fails: stop and report BLOCKED -- user needs to start cozy-member-provider in IDE per [[local-dev-run-mode]].

For Task 2 which also needs mall-provider, optionally verify:
```bash
curl -s "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=cozy-mall-provider" | grep -oE '"hosts":\[[^]]*\]' | head -1
```

- [ ] **Step 2: Run PointsConsistencyTest**

Run:
```bash
cd C:\Users\dell\Desktop\CozyCoffee\cozy-coffee\cozy-coffee-backend
mvn -pl cozy-provider/cozy-member-provider -am test -Dtest=PointsConsistencyTest -q
```
Expected: `BUILD SUCCESS`, both `testAdminRepairConsistency` and `testAdminAdjustPositive` pass.

- [ ] **Step 3: Run PointsFIFOConsistencyTest**

Run:
```bash
mvn -pl cozy-provider/cozy-member-provider -am test -Dtest=PointsFIFOConsistencyTest -q
```
Expected: `BUILD SUCCESS`, all test methods pass.

- [ ] **Step 4: Record baseline result**

No code change. Capture the actual output (test count, time) -- it will be referenced in Task 3 when writing the CHANGELOG entry. Note any deprecation warnings or flakiness observed.

- [ ] **Step 5: Commit nothing (verification-only task)**

This task makes no code changes. No commit. Report status DONE with the test output summary.

---

## Task 2: Write OrderFlowBaselineTest (Minimal Happy Path)

**Files:**
- Create: `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/test/java/com/cozy/order/service/OrderFlowBaselineTest.java`
- Read-only: `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/service/impl/OrderServiceImpl.java` (for createOrder signature)
- Read-only: `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/service/impl/OrderServiceImpl.java` (for completeOrder signature)

**Interfaces:**
- Consumes: `OrderService.createOrder(Long userId, String memberLevel, CreateOrderRequest request)` returning `ShopOrderDTO` (confirmed in `cozy-api/cozy-order-api/src/main/java/com/cozy/order/api/OrderService.java:32`) and `OrderService.completeOrder(Long orderId)` returning `ShopOrderDTO` (`OrderService.java:80`). `CreateOrderRequest` fields (confirmed in `cozy-api/cozy-order-api/src/main/java/com/cozy/order/dto/request/CreateOrderRequest.java`): `items: List<OrderItemRequest>`, `couponCode: String` (NOT couponId), `diningMethod: String` ("DINE_IN"/"TAKEOUT"/"DELIVERY"), `addonCouponCodes: List<String>`, `deliveryAddressId: Long`, `remark: String`.
- Produces: A green test `testCreateAndCompleteOrder_bogoCoupon_baseline` that future Phase 1+ tasks must NOT break. If a refactor breaks this test, the refactor changed behavior.

**Architectural decision (locked in this plan):**
- Test runs in `cozy-order-provider` module only (not cross-module end-to-end)
- Uses `@SpringBootTest` to load full Spring context
- Uses real Dubbo references (not mocked) -- hits running `cozy-member-provider` and `cozy-mall-provider`
- Uses `userId=997L` (existing tests use 999/998)
- Tests ONE coupon type only: `BOGO` (most common per `PointsMallServiceImpl.java:2250`)
- Tests complete flow only; cancel flow deferred to a later baseline expansion

**Why minimal:** Writing 5 coupon types × 2 flows = 10 test cases upfront requires architectural decisions about test data setup that belong in Phase 1+ tasks (where each fix adds its own covering test). This task establishes the scaffold + one baseline case.

- [ ] **Step 1: Read OrderItemRequest to confirm item fields**

Read `cozy-coffee-backend/cozy-api/cozy-order-api/src/main/java/com/cozy/order/dto/request/OrderItemRequest.java` and record its fields in the implementer report. (CreateOrderRequest already confirmed: `items: List<OrderItemRequest>`, `couponCode: String`, `diningMethod: String`.)

- [ ] **Step 2: Write the failing test (compiles, fails at first run due to no test data)**

Create file `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/test/java/com/cozy/order/service/OrderFlowBaselineTest.java` with this content:

```java
package com.cozy.order.service;

import com.cozy.order.api.OrderService;
import com.cozy.order.dto.request.CreateOrderRequest;
import com.cozy.order.dto.request.OrderItemRequest;
import com.cozy.order.dto.response.ShopOrderDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Baseline test capturing current order flow behavior BEFORE any refactor.
 * Phase 1+ refactors MUST NOT break this test -- if they do, they changed behavior.
 *
 * Scope (Phase 0): single happy-path with BOGO coupon, complete flow only.
 * Cancel flow + remaining 4 coupon types added in Phase 1+ alongside each fix.
 */
@SpringBootTest
public class OrderFlowBaselineTest {

    @Autowired
    private OrderService orderService;

    @Test
    public void testCreateAndCompleteOrder_bogoCoupon_baseline() {
        Long userId = 997L;
        String memberLevel = "basic";

        CreateOrderRequest request = buildBogoOrderRequest(userId);

        // Act: create order
        ShopOrderDTO created = orderService.createOrder(userId, memberLevel, request);
        assertNotNull(created.getId(), "Order should have an ID after creation");
        assertEquals("pending", created.getStatus(), "New order should be pending");

        // Act: complete order
        ShopOrderDTO completed = orderService.completeOrder(created.getId());
        assertEquals("completed", completed.getStatus(), "Order should be completed");
    }

    private CreateOrderRequest buildBogoOrderRequest(Long userId) {
        // See Step 3: implementer must discover real product ID and BOGO coupon code
        // via DB query (Step 3 explains how). The values below are SENTINELS that will
        // cause the first run to fail -- this is intentional, Step 4 replaces them.
        Long productId = -1L;   // SENTINEL: replace in Step 4
        String bogoCouponCode = "SENTINEL-BOGO-CODE";  // SENTINEL: replace in Step 4

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(Collections.singletonList(item));
        request.setCouponCode(bogoCouponCode);
        request.setDiningMethod("TAKEOUT");
        return request;
    }
}
```

- [ ] **Step 3: Discover real product ID and BOGO coupon code by querying DB**

Run against the running MySQL (per [[local-dev-run-mode]]: MySQL on port 3306, password from docker-compose `MYSQL_ROOT_PASSWORD` default `123456`):

```bash
docker exec -i cozy-mysql mysql -uroot -p123456 cozy_order -e "SELECT id, name, status FROM coffee_products WHERE status='active' LIMIT 1;"
docker exec -i cozy-mysql mysql -uroot -p123456 cozy_mall -e "SELECT id, coupon_code, coupon_type FROM coupons WHERE coupon_type LIKE '%BOGO%' LIMIT 1;"
```

Record the actual `productId` and `bogoCouponCode` values in the implementer report.

**If no BOGO coupon exists in DB:** This is a fixture gap. Two options:
1. Insert a test BOGO coupon for user 997 via `docker exec -i cozy-mysql mysql -uroot -p123456 cozy_mall -e "INSERT INTO user_coupons..."` (implementer must check the `user_coupons` table schema first)
2. Report DONE_WITH_CONCERNS requesting fixture guidance

Option 1 is preferred if the schema is simple. Do NOT spend more than 30 minutes on fixture setup -- if it gets complex, escalate.

- [ ] **Step 4: Replace sentinel values with real ones**

Edit `OrderFlowBaselineTest.java`:
- Replace `productId = -1L` with the real product ID from Step 3
- Replace `bogoCouponCode = "SENTINEL-BOGO-CODE"` with the real coupon code from Step 3

Show the full `buildBogoOrderRequest` method after replacement in the implementer report.

- [ ] **Step 5: Run test to verify it passes**

Run:
```bash
mvn -pl cozy-provider/cozy-order-provider -am test -Dtest=OrderFlowBaselineTest -q
```
Expected: `BUILD SUCCESS`, `testCreateAndCompleteOrder_bogoCoupon_baseline` passes.

If test fails due to: (a) user 997 doesn't exist -> insert via `cozy_user` and `cozy_member` tables; (b) coupon not owned by user 997 -> insert into `user_coupons`; (c) any other environmental issue -> report DONE_WITH_CONCERNS with the specific failure.

- [ ] **Step 6: Commit the new test**

```bash
cd C:\Users\dell\Desktop\CozyCoffee\cozy-coffee
git add cozy-coffee-backend/cozy-provider/cozy-order-provider/src/test/java/com/cozy/order/service/OrderFlowBaselineTest.java
git commit -m "test(order): add OrderFlowBaselineTest happy-path baseline

Establishes pre-refactor baseline: create order with BOGO coupon -> complete.
Phase 1+ refactors must not break this test.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 3: Document Baseline in CHANGELOG

**Files:**
- Modify: `C:\Users\dell\Desktop\surx-note\CozyCoffee\CHANGELOG.md` (NOT in repo -- see [[changelog-workflow]] memory)

**Interfaces:**
- Consumes: Test output summary from Task 1 (existing tests pass count + time) and Task 2 (new baseline test result)
- Produces: A CHANGELOG entry under `## 2026-07-09` documenting the baseline state

- [ ] **Step 1: Read current CHANGELOG top section**

Read first 60 lines of `C:\Users\dell\Desktop\surx-note\CozyCoffee\CHANGELOG.md` to confirm the latest date heading and entry style.

- [ ] **Step 2: Compose baseline entry**

Entry should match this structure (adjust actual test count/time per Task 1 results):

```markdown
---

### Phase 0：测试基线建立（Audit-Spec 重构前置）

**目标**：建立"行为不变"判定标准，Phase 1+ 重构必须不破坏以下基线。

**基线测试结果**（2026-07-09）：
- `PointsConsistencyTest`：2/2 通过（testAdminRepairConsistency, testAdminAdjustPositive）
- `PointsFIFOConsistencyTest`：N/N 通过（具体方法数按实际跑出的结果填）
- `OrderFlowBaselineTest.testCreateAndCompleteOrder_bogoCoupon_baseline`：通过

**覆盖范围**：
- ✅ 积分一致性（admin 修复 + 正向调整）
- ✅ 积分 FIFO 扣减
- ✅ 订单创建 + 完成链路（BOGO 券，单一 happy path）

**未覆盖**（Phase 1+ 增量补充）：
- ❌ 取消订单 -> 券回滚链路
- ❌ 5 种券类型完整矩阵（SHOT/DELIVERY_FEE/NEW_PRODUCT_HALF/NEW_PRODUCT_FREE）
- ❌ 月度任务奖励链路
- ❌ SSE 通知

**结论**：基线已建立，可启动 Phase 1 DB Critical Fixes。每个 Phase 1+ 任务必须在 PR 描述中说明"未破坏 Phase 0 基线"。
```

- [ ] **Step 3: Insert entry at top of CHANGELOG (under latest date heading)**

If `## 2026-07-09` already exists (e.g., from earlier work), append this entry as a new `###` subsection under that date. If not, create new `## 2026-07-09` heading first.

Use the Edit tool to insert. Preserve existing entries above and below.

- [ ] **Step 4: Verify CHANGELOG well-formed**

Read the modified section back. Confirm:
- Markdown headers are consistent (## for date, ### for entry)
- `---` separators are present between entries
- No accidental deletion of existing content

- [ ] **Step 5: Commit the plan reference (CHANGELOG itself is not in repo)**

The CHANGELOG file is in `surx-note/` outside the repo. No git commit for the CHANGELOG edit itself. But add a reference in the repo:

Create or update `docs/superpowers/plans/PHASE-0-PROGRESS.md` with:
```markdown
# Phase 0 Progress

- Date: 2026-07-09
- Status: COMPLETE
- Tests verified: PointsConsistencyTest, PointsFIFOConsistencyTest, OrderFlowBaselineTest
- CHANGELOG entry: see C:\Users\dell\Desktop\surx-note\CozyCoffee\CHANGELOG.md (2026-07-09 section)
```

```bash
cd C:\Users\dell\Desktop\CozyCoffee\cozy-coffee
git add docs/superpowers/plans/PHASE-0-PROGRESS.md docs/superpowers/plans/2026-07-09-phase-0-test-baseline.md
git commit -m "docs(plan): add Phase 0 test baseline plan and progress tracker

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```
