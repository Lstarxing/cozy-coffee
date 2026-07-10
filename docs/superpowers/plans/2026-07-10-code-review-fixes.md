# Code Review Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all P0-P2 items from the code review report that can be resolved autonomously without breaking existing functionality.

**Architecture:** Surgical fixes to existing code. No architectural changes, no dependency upgrades, no large refactoring. Each task is independent and ends with a commit.

**Tech Stack:** Java 17 + Spring Boot 3.0.2 + Dubbo 3.2 + MyBatis Plus + Vue 3 + Vite + uni-app

## Global Constraints

- **Do NOT upgrade dependencies** (Spring Boot/jjwt/RocketMQ CVE upgrades are out of scope - too risky for autonomous work)
- **Do NOT split large files** (OrderServiceImpl 1627 lines, MemberCenter.vue 800+ lines - too large for this session)
- **Do NOT add @ControllerAdvice or business exception hierarchy** (design decision, needs user input)
- **Do NOT change localStorage token to httpOnly cookie** (architectural change, needs backend coordination)
- **Each task ends with a commit** - use conventional commit messages
- **Verify after each change** - compile (maven/npm) or build check
- **Preserve existing behavior** - only fix the specific issue, don't refactor surrounding code
- **Java 17 syntax** - switch arrow `->` is valid and does NOT fall through

## Scope

### In Scope (will fix)
- All P0 items with clear fixes (6 items)
- All P1 items with clear fixes (~15 items)
- P2 quick wins (~3 items)

### Out of Scope (skipped with rationale)
| Item | Reason |
|------|--------|
| 生日权益 switch break | **FALSE ALARM** - Java 14+ arrow syntax `case "X" -> {}` does NOT fall through. Code is correct. |
| CVE dependency upgrades | Too risky for autonomous work - needs careful testing |
| OrderServiceImpl splitting (1627 lines) | Large refactoring - needs dedicated session |
| MemberCenter.vue splitting (800+ lines) | Large refactoring - needs dedicated session |
| @ControllerAdvice global exception | Design decision - needs user input on exception hierarchy |
| Business exception hierarchy | Design decision - needs user input |
| JSR-303 DTO validation | Would touch many DTOs - needs dedicated pass |
| localStorage -> httpOnly cookie | Architectural change - needs backend coordination |
| Mobile 地址 picker | UI redesign - needs design input |
| 门店数据 API | Needs new backend endpoint |
| RocketMQ 5.x upgrade | Needs migration testing |

---

## Phase 1: P0 Frontend Fixes

### Task 1: Fix RecentActivity.vue slot scope

**Files:**
- Modify: `cozy-coffee-admin/src/components/dashboard/RecentActivity.vue`

**Problem:** All `<template #default>` slots are missing `="{ row }"` scope binding, so `row` is undefined.

- [ ] **Step 1: Read current file to identify all slot patterns**

Run: `grep -n "#default" cozy-coffee-admin/src/components/dashboard/RecentActivity.vue`
Expected: Multiple lines with `<template #default>` (no scope binding)

- [ ] **Step 2: Fix all slot bindings**

Replace all `<template #default>` with `<template #default="{ row }">` in the file.

- [ ] **Step 3: Verify build**

Run: `cd cozy-coffee-admin && npm run build`
Expected: Build succeeds without errors

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-admin/src/components/dashboard/RecentActivity.vue
git commit -m "fix(admin): bind row scope in RecentActivity table slots

All #default slots were missing =\"{ row }\" binding, causing row to be
undefined and the entire 'Recent Activity' panel to show no data."
```

---

### Task 2: Fix ProductFormDialog.vue $attrs double application

**Files:**
- Modify: `cozy-coffee-admin/src/views/products/components/ProductFormDialog.vue`

**Problem:** `v-bind="$attrs"` is applied to both `el-dialog` and inner `el-form`, causing parent attributes to leak into the form.

- [ ] **Step 1: Read current file**

- [ ] **Step 2: Remove `v-bind="$attrs"` from inner `el-form`**

Keep `v-bind="$attrs"` only on `<el-dialog>`. The inner `<el-form>` should not inherit parent attrs.

- [ ] **Step 3: Verify build**

Run: `cd cozy-coffee-admin && npm run build`
Expected: Build succeeds

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-admin/src/views/products/components/ProductFormDialog.vue
git commit -m "fix(admin): remove $attrs from inner form in ProductFormDialog

v-bind=\"\$attrs\" was applied to both el-dialog and el-form, causing dialog
attributes (title, width) to leak into the form element."
```

---

### Task 3: Fix ShoppingCart.vue isSubmitting reset

**Files:**
- Modify: `cozy-coffee-web/src/components/cart/ShoppingCart.vue`

**Problem:** `isSubmitting` is set to `true` on checkout but only reset in `watch isOpen`. If submission fails, the button stays permanently disabled.

- [ ] **Step 1: Read the handleCheckout function**

- [ ] **Step 2: Add finally block to reset isSubmitting**

```javascript
const handleCheckout = async () => {
  // ... existing validation ...
  isSubmitting.value = true
  try {
    // ... existing submission logic ...
  } catch (error) {
    // ... existing error handling ...
  } finally {
    isSubmitting.value = false
  }
}
```

- [ ] **Step 3: Verify build**

Run: `cd cozy-coffee-web && npm run build`
Expected: Build succeeds

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-web/src/components/cart/ShoppingCart.vue
git commit -m "fix(web): reset isSubmitting in finally block

Checkout button stayed permanently disabled if submission failed,
because isSubmitting was only reset when cart reopened."
```

---

## Phase 2: P0 Backend Fixes

### Task 4: Register AdminAuthInterceptor in WebMvcConfig

**Files:**
- Modify: `cozy-coffee-backend/cozy-common/src/main/java/com/cozy/common/config/WebMvcConfig.java`

**Problem:** `AdminAuthInterceptor` class exists but is never registered. `/api/admin/**` paths have no admin role check.

- [ ] **Step 1: Read current WebMvcConfig.java**

- [ ] **Step 2: Add AdminAuthInterceptor registration**

```java
@Autowired
private JwtAuthInterceptor jwtAuthInterceptor;

@Autowired
private AdminAuthInterceptor adminAuthInterceptor;

@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(jwtAuthInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/test",
                    "/api/member/test"
            );
    registry.addInterceptor(adminAuthInterceptor)
            .addPathPatterns("/api/admin/**");
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd cozy-coffee-backend && mvn compile -pl cozy-common -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-backend/cozy-common/src/main/java/com/cozy/common/config/WebMvcConfig.java
git commit -m "fix(backend): register AdminAuthInterceptor for /api/admin/**

AdminAuthInterceptor was defined but never registered in WebMvcConfig,
meaning admin role checks were not enforced on admin API endpoints."
```

---

### Task 5: Externalize JWT secret to environment variable

**Files:**
- Modify: `cozy-coffee-backend/cozy-common/src/main/java/com/cozy/common/util/JwtUtil.java`

**Problem:** JWT signing key is hardcoded as a string constant.

- [ ] **Step 1: Read current JwtUtil.java**

- [ ] **Step 2: Replace hardcoded secret with env var fallback**

```java
public class JwtUtil {
    private static final String SECRET_KEY = System.getenv()
            .getOrDefault("JWT_SECRET", "cozy-coffee-dev-secret-key-change-in-production-32bytes");
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    // ... rest unchanged ...
}
```

Add import: `import java.nio.charset.StandardCharsets;`

- [ ] **Step 3: Verify compilation**

Run: `cd cozy-coffee-backend && mvn compile -pl cozy-common -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-backend/cozy-common/src/main/java/com/cozy/common/util/JwtUtil.java
git commit -m "fix(backend): externalize JWT secret to env variable

JWT signing key was hardcoded. Now reads from JWT_SECRET env var with
a dev-only fallback. Production must set JWT_SECRET."
```

---

### Task 6: Improve JwtAuthInterceptor to reject invalid tokens on protected paths

**Files:**
- Modify: `cozy-coffee-backend/cozy-common/src/main/java/com/cozy/common/interceptor/JwtAuthInterceptor.java`

**Problem:** `return true` after token validation failure silently lets invalid tokens through. While Service-layer `getUserId()` provides defense-in-depth, the interceptor should be stricter.

**Approach:** Keep `return true` for anonymous access (no token at all), but return 401 when a malformed/invalid token IS present. This is safer than the current behavior without breaking anonymous endpoints.

- [ ] **Step 1: Read current JwtAuthInterceptor.java**

- [ ] **Step 2: Modify to reject invalid tokens (but allow no-token)**

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {
    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        // No token - allow anonymous access, Service layer will enforce auth via getUserId()
        return true;
    }

    String token = authHeader.substring(7);
    try {
        if (!JwtUtil.validateToken(token)) {
            log.warn("JwtAuthInterceptor - Invalid token: uri={}", request.getRequestURI());
            return sendUnauthorized(response, "无效的登录凭证");
        }

        if (stringRedisTemplate != null) {
            String sessionKey = RedisKeyConstants.userLoginSession(token);
            String cachedUserId = stringRedisTemplate.opsForValue().get(sessionKey);
            if (cachedUserId == null || cachedUserId.isBlank()) {
                log.warn("JwtAuthInterceptor - Session missing in Redis: uri={}", request.getRequestURI());
                return sendUnauthorized(response, "登录已失效，请重新登录");
            }
        }

        Long userId = JwtUtil.getUserIdFromToken(token);
        String role = JwtUtil.getRoleFromToken(token);
        UserContext.setUserId(userId);
        UserContext.setRole(role);
        request.setAttribute("userId", userId);
        log.debug("JwtAuthInterceptor - Token valid, userId: {}, role: {}", userId, role);
        return true;
    } catch (Exception e) {
        log.warn("JwtAuthInterceptor - Token parse error: {}", e.getMessage());
        return sendUnauthorized(response, "登录凭证解析失败");
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd cozy-coffee-backend && mvn compile -pl cozy-common -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-backend/cozy-common/src/main/java/com/cozy/common/interceptor/JwtAuthInterceptor.java
git commit -m "fix(backend): reject invalid tokens in JwtAuthInterceptor

Previously, invalid Bearer tokens were silently allowed through. Now
returns 401 for malformed/expired tokens. Anonymous (no-token) access
still allowed for public endpoints."
```

---

### Task 7: Add @Profile restriction to test API endpoints

**Files:**
- Modify: `cozy-coffee-backend/cozy-common/src/main/java/com/cozy/common/config/WebMvcConfig.java`

**Problem:** Test endpoints `/api/auth/test` and `/api/member/test` are excluded from auth in all environments.

- [ ] **Step 1: Read WebMvcConfig.java (already modified in Task 4)**

- [ ] **Step 2: Make test endpoint exclusions dev-only**

Instead of hardcoding test exclusions, use a configurable property:

```java
@Value("${cozy.security.exclude-test-paths:false}")
private boolean excludeTestPaths;

@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(jwtAuthInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                    "/api/auth/login",
                    "/api/auth/register",
                    excludeTestPaths ? "/api/auth/test" : "",
                    excludeTestPaths ? "/api/member/test" : ""
            );
    registry.addInterceptor(adminAuthInterceptor)
            .addPathPatterns("/api/admin/**");
}
```

Add to `application.yml` of gateway:
```yaml
cozy:
  security:
    exclude-test-paths: ${EXCLUDE_TEST_PATHS:false}
```

- [ ] **Step 3: Verify compilation**

Run: `cd cozy-coffee-backend && mvn compile -pl cozy-common -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-backend/cozy-common/src/main/java/com/cozy/common/config/WebMvcConfig.java
git commit -m "fix(backend): gate test endpoint exclusions behind profile flag

Test endpoints /api/auth/test and /api/member/test are now only
excluded from auth when cozy.security.exclude-test-paths=true.
Production defaults to false."
```

---

## Phase 3: P1 Backend Business Logic

### Task 8: Use OrderStateMachine in OrderServiceImpl

**Files:**
- Modify: `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/service/impl/OrderServiceImpl.java`

**Problem:** `OrderStateMachine` enum is defined with `assertCanTransition` but never used. Status changes use raw string comparison.

- [ ] **Step 1: Read current acceptOrder/completeOrder/cancelOrder methods**

- [ ] **Step 2: Replace string checks with OrderStateMachine**

In `acceptOrder`:
```java
OrderStateMachine current = OrderStateMachine.from(order.getStatus());
current.assertCanTransition(OrderStateMachine.PREPARING);
order.setStatus(OrderStateMachine.PREPARING.value());
```

In `completeOrder`:
```java
OrderStateMachine current = OrderStateMachine.from(order.getStatus());
current.assertCanTransition(OrderStateMachine.COMPLETED);
order.setStatus(OrderStateMachine.COMPLETED.value());
```

In `cancelOrder` and `cancelUserOrder`:
```java
OrderStateMachine current = OrderStateMachine.from(order.getStatus());
current.assertCanTransition(OrderStateMachine.CANCELLED);
order.setStatus(OrderStateMachine.CANCELLED.value());
```

- [ ] **Step 3: Verify compilation**

Run: `cd cozy-coffee-backend && mvn compile -pl cozy-provider/cozy-order-provider -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/service/impl/OrderServiceImpl.java
git commit -m "refactor(order): use OrderStateMachine for status transitions

Replace raw string comparisons with OrderStateMachine.assertCanTransition
to enforce valid state transitions."
```

---

### Task 9: Fix order number generation (add milliseconds)

**Files:**
- Modify: `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/service/impl/OrderServiceImpl.java`

**Problem:** `generateOrderNo()` uses second precision + 4-digit random, collision risk under concurrency.

- [ ] **Step 1: Read current generateOrderNo method (line ~1393)**

- [ ] **Step 2: Add millisecond precision and wider random range**

```java
private String generateOrderNo() {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    int random = ThreadLocalRandom.current().nextInt(1000, 10000);
    return "CF" + timestamp + random;
}
```

Note: `ThreadLocalRandom` is already imported. Remove `new Random()` import if no longer used.

- [ ] **Step 3: Verify compilation**

Run: `cd cozy-coffee-backend && mvn compile -pl cozy-provider/cozy-order-provider -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/service/impl/OrderServiceImpl.java
git commit -m "fix(order): add millisecond precision to order number

Order number used second precision + 4-digit random, risking collisions
under high concurrency. Now uses millisecond precision + ThreadLocalRandom."
```

---

### Task 10: Fix generateMemberCode uniqueness

**Files:**
- Modify: `cozy-coffee-backend/cozy-provider/cozy-user-provider/src/main/java/com/cozy/user/service/impl/UserServiceImpl.java`

**Problem:** `generateMemberCode()` generates 8-digit random number without uniqueness check.

- [ ] **Step 1: Read current generateMemberCode method**

- [ ] **Step 2: Add retry loop for uniqueness**

```java
private String generateMemberCode() {
    for (int attempt = 0; attempt < 5; attempt++) {
        String code = String.format("%08d", ThreadLocalRandom.current().nextInt(100000000));
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getMemberCode, code)) == 0) {
            return code;
        }
        log.warn("Member code collision, retrying: attempt={}, code={}", attempt + 1, code);
    }
    throw new RuntimeException("生成会员码失败：多次碰撞，请重试");
}
```

Add imports: `import java.util.concurrent.ThreadLocalRandom;`

- [ ] **Step 3: Verify compilation**

Run: `cd cozy-coffee-backend && mvn compile -pl cozy-provider/cozy-user-provider -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-backend/cozy-provider/cozy-user-provider/src/main/java/com/cozy/user/service/impl/UserServiceImpl.java
git commit -m "fix(user): retry on member code collision

generateMemberCode had no uniqueness check. Now retries up to 5 times
on collision before failing."
```

---

### Task 11: Fix grantOneOffPoints hashCode collision risk

**Files:**
- Modify: `cozy-coffee-backend/cozy-provider/cozy-member-provider/src/main/java/com/cozy/member/service/impl/MemberServiceImpl.java`

**Problem:** `grantOneOffPoints` uses `String.hashCode()` for idempotency, which can collide.

- [ ] **Step 1: Read current grantOneOffPoints method (line ~787)**

- [ ] **Step 2: Use stable hash (SHA-256 truncated) instead of String.hashCode**

```java
private void grantOneOffPoints(Long userId, int points, String uniqueSourceIdStr, String desc) {
    // Use SHA-256 to generate a stable positive long, avoiding hashCode collisions
    try {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(uniqueSourceIdStr.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long sourceId = 0;
        for (int i = 0; i < 8; i++) {
            sourceId = (sourceId << 8) | (hash[i] & 0xFF);
        }
        sourceId = Math.abs(sourceId);

        LambdaQueryWrapper<PointsTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsTransaction::getUserId, userId)
                .eq(PointsTransaction::getSourceType, "upgrade_reward")
                .eq(PointsTransaction::getSourceId, sourceId);

        if (transactionMapper.selectCount(wrapper) == 0) {
            addPointsWithLot(userId, points, "upgrade_reward", sourceId, desc);
        }
    } catch (java.security.NoSuchAlgorithmException e) {
        throw new RuntimeException("SHA-256 not available", e);
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd cozy-coffee-backend && mvn compile -pl cozy-provider/cozy-member-provider -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-backend/cozy-provider/cozy-member-provider/src/main/java/com/cozy/member/service/impl/MemberServiceImpl.java
git commit -m "fix(member): use SHA-256 for idempotency key in grantOneOffPoints

String.hashCode() can collide, risking duplicate reward grants.
Use SHA-256 truncated to 8 bytes for a more stable unique key."
```

---

### Task 12: Fix PickupCodeService NULL counter race

**Files:**
- Modify: `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/service/PickupCodeService.java`

**Problem:** If counter is NULL, concurrent inserts may both try to create, causing DuplicateKeyException.

- [ ] **Step 1: Read current PickupCodeService.java**

- [ ] **Step 2: Catch DuplicateKeyException and retry**

```java
public String generatePickupCode(Long storeId, LocalDateTime businessDate) {
    // ... existing logic that may throw DuplicateKeyException on counter insert ...
    try {
        return doGeneratePickupCode(storeId, businessDate);
    } catch (org.springframework.dao.DuplicateKeyException e) {
        log.warn("Pickup code counter race detected, retrying: storeId={}", storeId);
        return doGeneratePickupCode(storeId, businessDate);
    }
}

private String doGeneratePickupCode(Long storeId, LocalDateTime businessDate) {
    // ... existing implementation ...
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd cozy-coffee-backend && mvn compile -pl cozy-provider/cozy-order-provider -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/service/PickupCodeService.java
git commit -m "fix(order): retry on pickup code counter race

Concurrent counter creation could throw DuplicateKeyException.
Now catches and retries once."
```

---

## Phase 4: P1 Backend Data Access

### Task 13: Fix wrapper.last LIMIT SQL injection

**Files:**
- Modify: `cozy-coffee-backend/cozy-provider/cozy-member-provider/src/main/java/com/cozy/member/service/impl/MemberServiceImpl.java`

**Problem:** `wrapper.last("LIMIT " + limit)` uses string concatenation, bypassing parameterization.

- [ ] **Step 1: Read current getPointsTransactions method (line ~680)**

- [ ] **Step 2: Use MyBatis Plus Page for pagination**

```java
@Override
public List<PointsTransactionDTO> getPointsTransactions(Long userId, int limit) {
    if (userId == null) {
        throw new RuntimeException("用户ID不能为空");
    }
    int safeLimit = Math.min(Math.max(limit, 1), 100);

    LambdaQueryWrapper<PointsTransaction> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(PointsTransaction::getUserId, userId)
            .orderByDesc(PointsTransaction::getCreatedAt);

    com.baomidou.mybatisplus.extension.plugins.pagination.Page<PointsTransaction> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, safeLimit);
    return transactionMapper.selectPage(page, wrapper).getRecords().stream()
            .map(this::toTransactionDTO)
            .collect(Collectors.toList());
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd cozy-coffee-backend && mvn compile -pl cozy-provider/cozy-member-provider -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-backend/cozy-provider/cozy-member-provider/src/main/java/com/cozy/member/service/impl/MemberServiceImpl.java
git commit -m "fix(member): use parameterized pagination for points transactions

wrapper.last(\"LIMIT \" + limit) bypassed parameterization. Switch to
MyBatis Plus Page for safe pagination."
```

---

### Task 14: Fix getMembersByUserIds N+1 remote call

**Files:**
- Modify: `cozy-coffee-backend/cozy-api/cozy-user-api/src/main/java/com/cozy/user/api/UserService.java`
- Modify: `cozy-coffee-backend/cozy-provider/cozy-user-provider/src/main/java/com/cozy/user/service/impl/UserServiceImpl.java`
- Modify: `cozy-coffee-backend/cozy-provider/cozy-member-provider/src/main/java/com/cozy/member/service/impl/MemberServiceImpl.java`

**Problem:** `getMembersByUserIds` loops over userIds calling `userService.getUserById()` one by one (N+1 Dubbo RPC).

- [ ] **Step 1: Add batch method to UserService interface**

```java
List<UserDTO> getUsersByIds(Set<Long> userIds);
```

- [ ] **Step 2: Implement batch method in UserServiceImpl**

```java
@Override
public List<UserDTO> getUsersByIds(Set<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
        return Collections.emptyList();
    }
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    wrapper.in(User::getId, userIds);
    return userMapper.selectList(wrapper).stream()
            .map(this::toUserDTO)
            .collect(Collectors.toList());
}
```

- [ ] **Step 3: Update MemberServiceImpl.getMembersByUserIds to use batch**

```java
Map<Long, com.cozy.user.dto.response.UserDTO> userMap = new HashMap<>();
try {
    if (userService != null) {
        List<com.cozy.user.dto.response.UserDTO> users = userService.getUsersByIds(userIds);
        if (users != null) {
            for (com.cozy.user.dto.response.UserDTO user : users) {
                userMap.put(user.getId(), user);
            }
        }
    }
} catch (Exception e) {
    log.warn("批量获取用户信息失败: {}", e.getMessage());
}
```

- [ ] **Step 4: Verify compilation**

Run: `cd cozy-coffee-backend && mvn compile -pl cozy-provider/cozy-member-provider -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add cozy-coffee-backend/cozy-api/cozy-user-api/src/main/java/com/cozy/user/api/UserService.java
git add cozy-coffee-backend/cozy-provider/cozy-user-provider/src/main/java/com/cozy/user/service/impl/UserServiceImpl.java
git add cozy-coffee-backend/cozy-provider/cozy-member-provider/src/main/java/com/cozy/member/service/impl/MemberServiceImpl.java
git commit -m "perf(user): add batch getUsersByIds to eliminate N+1 RPC

getMembersByUserIds called userService.getUserById in a loop, causing
N Dubbo RPCs per batch. Now uses single batch query."
```

---

### Task 15: Fix PointsMallServiceImpl stock restore with distributed lock

**Files:**
- Modify: `cozy-coffee-backend/cozy-provider/cozy-mall-provider/src/main/java/com/cozy/mall/service/impl/PointsMallServiceImpl.java`

**Problem:** `cancelOrder` does read-modify-write on stock without lock, unsafe under concurrent cancels.

- [ ] **Step 1: Read current cancelOrder method (line ~539)**

- [ ] **Step 2: Wrap stock restore in Redis distributed lock**

```java
private void restoreProductStock(Long productId, int quantity) {
    String lockKey = "cozy:lock:product_stock:" + productId;
    String lockToken = UUID.randomUUID().toString();
    try {
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockToken, 10, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new RuntimeException("获取库存锁失败，请稍后重试");
        }
        PointsProduct product = productMapper.selectById(productId);
        if (product != null) {
            product.setStock(product.getStock() + quantity);
            productMapper.updateById(product);
        }
    } finally {
        releaseLockSafely(lockKey, lockToken);
    }
}
```

Call `restoreProductStock` in cancelOrder instead of inline stock update.

- [ ] **Step 3: Verify compilation**

Run: `cd cozy-coffee-backend && mvn compile -pl cozy-provider/cozy-mall-provider -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-backend/cozy-provider/cozy-mall-provider/src/main/java/com/cozy/mall/service/impl/PointsMallServiceImpl.java
git commit -m "fix(mall): lock stock restore in cancelOrder

cancelOrder did read-modify-write on stock without lock, unsafe under
concurrent cancels. Now uses Redis distributed lock."
```

---

### Task 16: Fix MenuCacheService lock release atomicity

**Files:**
- Modify: `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/service/impl/MenuCacheService.java`

**Problem:** `releaseRebuildLock` does `get` then `delete`, not atomic - can delete another thread's lock.

- [ ] **Step 1: Read current releaseRebuildLock method**

- [ ] **Step 2: Use Lua script for atomic compare-and-delete**

```java
private void releaseLockSafely(String lockKey, String lockToken) {
    try {
        String releaseScript = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "return redis.call('del', KEYS[1]) else return 0 end";
        org.springframework.data.redis.core.script.DefaultRedisScript<Long> redisScript =
                new org.springframework.data.redis.core.script.DefaultRedisScript<>();
        redisScript.setScriptText(releaseScript);
        redisScript.setResultType(Long.class);
        stringRedisTemplate.execute(redisScript, Collections.singletonList(lockKey), lockToken);
    } catch (Exception e) {
        log.warn("释放Redis重建锁失败: key={}", lockKey, e);
    }
}
```

Rename existing `releaseRebuildLock` to call this, or replace its body.

- [ ] **Step 3: Verify compilation**

Run: `cd cozy-coffee-backend && mvn compile -pl cozy-provider/cozy-order-provider -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/java/com/cozy/order/service/impl/MenuCacheService.java
git commit -m "fix(order): use Lua script for atomic lock release in MenuCacheService

get + delete was non-atomic, could delete another thread's lock.
Uses Lua compare-and-delete script (same pattern as OrderServiceImpl)."
```

---

## Phase 5: P1 Frontend Fixes

### Task 17: Fix MemberLayout avatar save to call backend API

**Files:**
- Modify: `cozy-coffee-web/src/views/member/MemberLayout.vue`

**Problem:** `saveAvatar` only updates local state and localStorage, doesn't call backend API.

- [ ] **Step 1: Read current saveAvatar method**

- [ ] **Step 2: Call updateProfile API on save**

```javascript
const saveAvatar = async () => {
  if (!avatarPreview.value) return
  try {
    // Call backend to persist avatar URL
    await updateProfile({ avatar: avatarPreview.value })
    userStore.userInfo.avatar = avatarPreview.value
    localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
    showAvatarModal.value = false
    ElMessage.success('头像保存成功')
  } catch (error) {
    ElMessage.error('头像保存失败: ' + error.message)
  }
}
```

Ensure `updateProfile` is imported from `@/api/auth`.

- [ ] **Step 3: Verify build**

Run: `cd cozy-coffee-web && npm run build`
Expected: Build succeeds

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-web/src/views/member/MemberLayout.vue
git commit -m "fix(web): call backend API on avatar save

Avatar changes were only saved locally, lost on refresh. Now calls
updateProfile to persist."
```

---

### Task 18: Fix Mobile login error handling

**Files:**
- Modify: `cozy-coffee-mobile/src/pages/login/index.vue`

**Problem:** Business errors (wrong password, code !== 200) have no user-facing error message.

- [ ] **Step 1: Read current handleLogin method**

- [ ] **Step 2: Add error handling for business errors**

```javascript
const handleLogin = async () => {
  // ... validation ...
  try {
    const res = await loginApi(form.username, form.password)
    if (res.code === 200 && res.data?.token) {
      // ... success logic ...
    } else {
      // Business error - show message to user
      uni.showToast({
        title: res.message || res.msg || '登录失败',
        icon: 'none',
        duration: 3000
      })
    }
  } catch (error) {
    uni.showToast({
      title: error.message || '登录失败，请重试',
      icon: 'none',
      duration: 3000
    })
  }
}
```

- [ ] **Step 3: Verify build**

Run: `cd cozy-coffee-mobile && npm run build:h5` (if available) or check syntax
Expected: Build succeeds

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-mobile/src/pages/login/index.vue
git commit -m "fix(mobile): show error toast on login business errors

Business errors (wrong password, disabled account) had no user
feedback. Now shows toast with error message."
```

---

### Task 19: Fix DeliveryAddress field name mismatch

**Files:**
- Modify: `cozy-coffee-web/src/components/cart/DeliveryAddress.vue`

**Problem:** Template uses `addr.contactName` and `addr.phone`, but API returns `receiverName` and `receiverPhone`.

- [ ] **Step 1: Read current template bindings**

- [ ] **Step 2: Align field names with backend DTO**

Change `addr.contactName` -> `addr.receiverName` and `addr.phone` -> `addr.receiverPhone` in all bindings.

- [ ] **Step 3: Verify build**

Run: `cd cozy-coffee-web && npm run build`
Expected: Build succeeds

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-web/src/components/cart/DeliveryAddress.vue
git commit -m "fix(web): align DeliveryAddress field names with backend

Template used contactName/phone but API returns receiverName/receiverPhone,
causing saved addresses to display incorrectly."
```

---

### Task 20: Fix getBanners to return real data or proper empty

**Files:**
- Modify: `cozy-coffee-mobile/src/api/product.js`

**Problem:** `getBanners()` returns `Promise.resolve({ code: 200, data: [] })`, banners always empty.

- [ ] **Step 1: Read current getBanners**

- [ ] **Step 2: Either call real endpoint or clearly mark as TODO**

If backend has `/order/banners` endpoint:
```javascript
export function getBanners() {
    return request.get('/order/banners')
}
```

If not available, add clear TODO:
```javascript
// TODO: backend endpoint /order/banners not yet implemented
// Currently returns empty - frontend falls back to defaultBanners
export function getBanners() {
    return Promise.resolve({ code: 200, data: [] })
}
```

(Check if backend has a banners endpoint first - grep for "banner" in backend code)

- [ ] **Step 3: Verify build**

Run: `cd cozy-coffee-mobile && npm run build:h5` (if available)
Expected: Build succeeds

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-mobile/src/api/product.js
git commit -m "fix(mobile): clarify getBanners status with TODO comment

getBanners returns empty array. Added TODO comment so it's clear this
needs a backend endpoint, not a bug."
```

---

## Phase 6: P1 Config Fixes

### Task 21: Enable flyway validate-on-migrate

**Files:**
- Modify: `cozy-coffee-backend/cozy-provider/cozy-member-provider/src/main/resources/application.yml`
- Modify: `cozy-coffee-backend/cozy-provider/cozy-order-provider/src/main/resources/application.yml`
- Modify: `cozy-coffee-backend/cozy-provider/cozy-mall-provider/src/main/resources/application.yml`
- Modify: `cozy-coffee-backend/cozy-provider/cozy-user-provider/src/main/resources/application.yml`

**Problem:** `flyway.validate-on-migrate: false` in all providers, schema drift risk.

- [ ] **Step 1: Read one provider's application.yml to find flyway config**

- [ ] **Step 2: Change `validate-on-migrate: false` to `true` in all 4 providers**

```yaml
flyway:
  validate-on-migrate: true
```

- [ ] **Step 3: Verify config loads**

Run: `cd cozy-coffee-backend && mvn compile -pl cozy-provider/cozy-user-provider -q`
Expected: BUILD SUCCESS (config not loaded at compile time, but verifies no syntax errors)

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-backend/cozy-provider/*/src/main/resources/application.yml
git commit -m "fix(config): enable flyway validate-on-migrate in all providers

Was disabled globally, risking schema drift between environments.
Now enabled - Flyway will validate migrations before applying."
```

---

## Phase 7: P2 Quick Wins

### Task 22: Add .dockerignore files

**Files:**
- Create: `cozy-coffee-web/.dockerignore`
- Create: `cozy-coffee-admin/.dockerignore`
- Create: `cozy-coffee-mobile/.dockerignore`
- Create: `cozy-coffee-backend/.dockerignore`

**Problem:** No .dockerignore, build context includes node_modules/target/.git.

- [ ] **Step 1: Create .dockerignore for frontend projects**

Content for each frontend `.dockerignore`:
```
node_modules
dist
.git
.gitignore
*.log
.env*
.idea
.vscode
Dockerfile
.dockerignore
```

- [ ] **Step 2: Create .dockerignore for backend**

Content for `cozy-coffee-backend/.dockerignore`:
```
**/target/
.git
.gitignore
*.log
.idea
.vscode
**/.idea/
Dockerfile*
.dockerignore
```

- [ ] **Step 3: Commit**

```bash
git add cozy-coffee-web/.dockerignore cozy-coffee-admin/.dockerignore cozy-coffee-mobile/.dockerignore cozy-coffee-backend/.dockerignore
git commit -m "chore: add .dockerignore files

Build context was including node_modules, target, .git - slowing
builds and bloating images."
```

---

### Task 23: Use npm ci in Dockerfiles

**Files:**
- Modify: `cozy-coffee-web/Dockerfile`
- Modify: `cozy-coffee-admin/Dockerfile`

**Problem:** Uses `npm install` instead of `npm ci` - slower and not reproducible.

- [ ] **Step 1: Read current Dockerfiles**

- [ ] **Step 2: Change `npm install` to `npm ci`**

In both Dockerfiles, replace:
```dockerfile
RUN npm install
```
with:
```dockerfile
RUN npm ci
```

- [ ] **Step 3: Commit**

```bash
git add cozy-coffee-web/Dockerfile cozy-coffee-admin/Dockerfile
git commit -m "chore: use npm ci for reproducible Docker builds

npm install can modify lockfile and is slower. npm ci strictly
follows lockfile for reproducible builds."
```

---

### Task 24: Remove non-functional "记住密码" checkbox

**Files:**
- Modify: `cozy-coffee-admin/src/views/Login.vue`
- Modify: `cozy-coffee-web/src/views/Login.vue`

**Problem:** "记住密码" checkbox has no `v-model` binding and no logic. It's a dead control that misleads users.

- [ ] **Step 1: Read both Login.vue files**

- [ ] **Step 2: Remove the remember checkbox block**

Remove the `<div class="remember-forgot">` block containing the non-functional checkbox, or keep only the "忘记密码" link.

- [ ] **Step 3: Verify build**

Run: `cd cozy-coffee-admin && npm run build && cd ../cozy-coffee-web && npm run build`
Expected: Build succeeds

- [ ] **Step 4: Commit**

```bash
git add cozy-coffee-admin/src/views/Login.vue cozy-coffee-web/src/views/Login.vue
git commit -m "fix(login): remove non-functional 记住密码 checkbox

Checkbox had no v-model and no logic - was a dead control misleading
users. Removed to avoid confusion."
```

---

## Phase 8: Baseline Testing

### Task 25: Run backend maven build and tests

**Goal:** Verify all backend changes compile and existing tests pass.

- [ ] **Step 1: Run full backend build**

Run: `cd cozy-coffee-backend && mvn clean compile -DskipTests -q`
Expected: BUILD SUCCESS for all modules

- [ ] **Step 2: Run existing tests**

Run: `cd cozy-coffee-backend && mvn test -pl cozy-provider/cozy-member-provider -q`
Expected: PointsFIFOConsistencyTest passes

- [ ] **Step 3: Fix any compilation errors found**

If build fails, read error messages and fix the specific issue. Common issues:
- Missing imports (add them)
- Method signature mismatches (align with interface)
- Type errors (fix casting)

- [ ] **Step 4: Commit any fixups**

```bash
git add -A
git commit -m "fix: compilation fixes from baseline test"
```

---

### Task 26: Run frontend builds

**Goal:** Verify all frontend changes compile.

- [ ] **Step 1: Build admin frontend**

Run: `cd cozy-coffee-admin && npm run build`
Expected: Build succeeds, no errors

- [ ] **Step 2: Build web frontend**

Run: `cd cozy-coffee-web && npm run build`
Expected: Build succeeds, no errors

- [ ] **Step 3: Build mobile frontend (if H5 target available)**

Run: `cd cozy-coffee-mobile && npm run build:h5 2>/dev/null || npm run build`
Expected: Build succeeds

- [ ] **Step 4: Fix any build errors found**

Read error output, fix specific issues (usually template syntax errors or missing imports).

- [ ] **Step 5: Commit any fixups**

```bash
git add -A
git commit -m "fix: frontend build fixes from baseline test"
```

---

### Task 27: Final verification and push

- [ ] **Step 1: Review all commits**

Run: `git log --oneline main..HEAD`
Expected: See all task commits

- [ ] **Step 2: Run full backend test suite one more time**

Run: `cd cozy-coffee-backend && mvn test -q`
Expected: All tests pass

- [ ] **Step 3: Verify git status is clean**

Run: `git status`
Expected: "nothing to commit, working tree clean"

- [ ] **Step 4: Push branch**

```bash
git push -u origin HEAD:code-review-fixes
```

(Do NOT push to main - push to a feature branch for review)

---

## Self-Review Checklist

After all tasks complete, verify:

- [ ] All P0 已确认 items fixed (Tasks 1-7)
- [ ] All P1 clear-fix items fixed (Tasks 8-21)
- [ ] P2 quick wins done (Tasks 22-24)
- [ ] Baseline tests pass (Tasks 25-27)
- [ ] No new compiler warnings introduced
- [ ] No existing tests broken
- [ ] Git history is clean (one commit per logical change)
- [ ] Branch pushed for review

## Items Deferred (need user input or separate session)

| Item | Why Deferred |
|------|-------------|
| CVE dependency upgrades | Need careful testing - separate session |
| OrderServiceImpl splitting (1627 lines) | Large refactoring - separate session |
| MemberCenter.vue splitting (800+ lines) | Large refactoring - separate session |
| @ControllerAdvice + business exception hierarchy | Design decision - needs user input |
| JSR-303 DTO validation | Touches many DTOs - dedicated pass |
| localStorage -> httpOnly cookie | Architectural - needs backend coordination |
| Mobile 地址 picker | UI redesign - needs design input |
| 门店数据 backend API | Needs new endpoint design |
| RocketMQ 5.x upgrade | Migration testing needed |
