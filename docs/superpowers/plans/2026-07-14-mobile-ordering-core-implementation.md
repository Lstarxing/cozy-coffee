# Mobile Ordering Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the fixed-store pickup ordering vertical slice from session recovery through menu, cart, checkout, mock payment, and order detail.

**Architecture:** Pure domain modules own normalization, preview calculation, and state transitions. Pinia stores keep durable user/cart state and short-lived checkout intent; `CheckoutWorkflow` coordinates focused session, preview, order, payment, network, and logging services. Pages compose reusable components and do not contain pricing or transport rules.

**Tech Stack:** Vue 3, uni-app, Pinia, Vite, Sass, Vitest, Vue Test Utils, uni-automator

---

## File map

- Modify `package.json` and `vite.config.js`: test commands and environment loading.
- Create `.env.development`, `.env.test`, `.env.production` examples without secrets.
- Create `src/domain/cart/`, `src/domain/checkout/`, `src/services/`, `src/components/product/`, `src/components/cart/`, `src/components/checkout/`, and `src/components/states/`.
- Replace `src/stores/user.js` with focused `src/stores/session.js`; migrate imports.
- Refactor `src/stores/cart.js` to versioned cart-line keys and migrations.
- Create `src/stores/checkout.js` for intent and machine state only.
- Refactor `src/api/request.js`, `src/api/order.js`, and `src/api/auth.js`.
- Refactor `src/pages/menu/menu.vue`, `src/pages/order/confirm.vue`, `src/pages/order/result.vue`, and `src/pages/order/detail.vue`.
- Modify `src/App.vue` for network/session lifecycle restoration.

### Task 1: Add unit-test and environment foundations

**Files:**
- Modify: `cozy-coffee-mobile/package.json`
- Modify: `cozy-coffee-mobile/vite.config.js`
- Create: `cozy-coffee-mobile/vitest.config.js`
- Create: `cozy-coffee-mobile/.env.development`
- Create: `cozy-coffee-mobile/.env.test`
- Create: `cozy-coffee-mobile/.env.production`

- [ ] **Step 1: Install test dependencies**

Run: `npm install -D vitest @vue/test-utils jsdom`

- [ ] **Step 2: Add scripts**

```json
"test": "vitest run",
"test:watch": "vitest",
"test:unit": "vitest run src/**/*.test.js"
```

- [ ] **Step 3: Add Vitest aliases and globals**

Configure `@` to `src`, environment `node` by default, and `jsdom` per component test file.

- [ ] **Step 4: Replace hard-coded API configuration**

Use `import.meta.env.VITE_API_BASE_URL`; H5 development keeps `/api`, while test/production values are explicit. Add an early error if a non-H5 build has no URL.

- [ ] **Step 5: Verify**

Run: `npm test -- --passWithNoTests`

Expected: PASS.

Run: `npm run build:h5`

Expected: build completes.

- [ ] **Step 6: Commit**

```bash
git add cozy-coffee-mobile/package.json cozy-coffee-mobile/package-lock.json cozy-coffee-mobile/vite.config.js cozy-coffee-mobile/vitest.config.js cozy-coffee-mobile/.env.*
git commit -m "test(mobile): add test and environment foundations"
```

### Task 2: Create typed request errors and logging

**Files:**
- Create: `cozy-coffee-mobile/src/services/errors/AppError.js`
- Create: `cozy-coffee-mobile/src/services/logging/Logger.js`
- Create: `cozy-coffee-mobile/src/services/errors/AppError.test.js`
- Modify: `cozy-coffee-mobile/src/api/request.js`

- [ ] **Step 1: Write failing error-mapping tests**

Assert HTTP/network failures become `NetworkError`, code 401 becomes `AuthError`, validation codes become `ValidationError`, and other non-200 business codes become `BusinessError` with `{code,message,retryable}`.

- [ ] **Step 2: Run the test**

Run: `npm test -- src/services/errors/AppError.test.js`

Expected: FAIL.

- [ ] **Step 3: Implement error classes and pure response mapper**

Export `NetworkError`, `BusinessError`, `AuthError`, `ValidationError`, and `mapResponseToResult(res)`. Remove Toast and navigation side effects from `request.js`; callers decide presentation.

- [ ] **Step 4: Implement Logger**

Expose `info(event, context)`, `warn`, and `error`. Redact `token`, `phone`, `address`, and payment fields before output.

- [ ] **Step 5: Run tests and build**

Run: `npm test -- src/services/errors/AppError.test.js && npm run build:h5`

Expected: PASS and successful build.

- [ ] **Step 6: Commit**

```bash
git add cozy-coffee-mobile/src/api/request.js cozy-coffee-mobile/src/services
git commit -m "refactor(mobile): standardize request errors and logs"
```

### Task 3: Version and migrate cart lines

**Files:**
- Create: `cozy-coffee-mobile/src/domain/cart/cartLineKey.js`
- Create: `cozy-coffee-mobile/src/domain/cart/cartMigrations.js`
- Create: `cozy-coffee-mobile/src/domain/cart/cartLineKey.test.js`
- Modify: `cozy-coffee-mobile/src/stores/cart.js`

- [ ] **Step 1: Write failing key tests**

Test `v1:` prefix, enum normalization, stable empty `skuId`, milk type participation, different-spec separation, and identical-spec equality.

- [ ] **Step 2: Run the test**

Run: `npm test -- src/domain/cart/cartLineKey.test.js`

Expected: FAIL.

- [ ] **Step 3: Implement canonical keys**

```js
export const CART_KEY_VERSION = 'v1'
export function createCartLineKey(item) {
  const normalized = normalizeCartOptions(item)
  return `${CART_KEY_VERSION}:${[
    normalized.productId, normalized.skuId, normalized.cupSize,
    normalized.temperature, normalized.sugarLevel,
    normalized.milkType, normalized.coffeeStrength
  ].join('|')}`
}
```

- [ ] **Step 4: Add storage migration**

Read the existing `cart` key, convert safe legacy entries to `v1`, and return `discardedItems` for ambiguous entries. Persist `{version:'v1',items:[...]}`.

- [ ] **Step 5: Refactor cart store APIs**

Use `lineKey` for add/increase/decrease/remove/updateOptions. Keep computed `totalCount` and `subtotal`; do not put checkout discounts here.

- [ ] **Step 6: Verify**

Run: `npm test -- src/domain/cart/cartLineKey.test.js && npm run build:h5`

- [ ] **Step 7: Commit**

```bash
git add cozy-coffee-mobile/src/domain/cart cozy-coffee-mobile/src/stores/cart.js
git commit -m "feat(mobile): version cart line identity"
```

### Task 4: Add checkout preview and state machine

**Files:**
- Create: `cozy-coffee-mobile/src/domain/checkout/computeCheckoutPreview.js`
- Create: `cozy-coffee-mobile/src/domain/checkout/checkoutMachine.js`
- Create: `cozy-coffee-mobile/src/domain/checkout/checkoutMachine.test.js`
- Create: `cozy-coffee-mobile/src/domain/checkout/computeCheckoutPreview.test.js`
- Create: `cozy-coffee-mobile/src/stores/checkout.js`

- [ ] **Step 1: Write preview tests**

Cover subtotal, LARGE surcharge already present in line price, coupon discount, non-negative payable, stable `previewVersion`, and version invalidation when quantity/coupon changes.

- [ ] **Step 2: Write state transition tests**

Cover the Frozen Spec Mermaid transitions and reject `idle → paying` or `success → submitting`.

- [ ] **Step 3: Run tests and verify failure**

Run: `npm test -- src/domain/checkout`

- [ ] **Step 4: Implement pure modules**

`transitionCheckout(state,event)` returns the next state or throws `ValidationError`. `computeCheckoutPreview(input)` returns `{subtotal,discount,payable,previewVersion}` and never reads a Store.

- [ ] **Step 5: Implement checkout store**

Store only `storeId`, `pickupTime`, `selectedCouponId`, `remark`, `status`, `idempotencyKey`, and the latest immutable preview reference. Add `start()`, `applyPreview()`, `transition()`, and `reset()`.

- [ ] **Step 6: Verify and commit**

Run: `npm test -- src/domain/checkout && npm run build:h5`

```bash
git add cozy-coffee-mobile/src/domain/checkout cozy-coffee-mobile/src/stores/checkout.js
git commit -m "feat(mobile): add checkout preview state machine"
```

### Task 5: Add focused session, network, order, and payment services

**Files:**
- Create: `cozy-coffee-mobile/src/stores/session.js`
- Create: `cozy-coffee-mobile/src/services/session/SessionService.js`
- Create: `cozy-coffee-mobile/src/services/network/NetworkService.js`
- Create: `cozy-coffee-mobile/src/services/order/OrderService.js`
- Create: `cozy-coffee-mobile/src/services/payment/PaymentService.js`
- Create: `cozy-coffee-mobile/src/services/payment/adapters/MockPaymentAdapter.js`
- Create: `cozy-coffee-mobile/src/services/payment/PaymentService.test.js`
- Modify: all imports of `@/stores/user`
- Modify: `cozy-coffee-mobile/src/App.vue`

- [ ] **Step 1: Write payment adapter tests**

Test success, cancel, failure, and that `PaymentService` does not expose adapter-specific details.

- [ ] **Step 2: Run and verify failure**

Run: `npm test -- src/services/payment/PaymentService.test.js`

- [ ] **Step 3: Implement services**

`SessionService.ensureCheckoutIdentity()` restores or requests login. `NetworkService` wraps `uni.getNetworkType` and `uni.onNetworkStatusChange`. `OrderService.checkCart()` calls `/order/cart/check`; `create()` sends `Idempotency-Key`. `MockPaymentAdapter.pay()` displays an explicit mock confirmation and returns `{status:'success'|'cancelled'}`.

- [ ] **Step 4: Wire app lifecycle**

On launch restore session/cart and network state. On show after more than five minutes, mark checkout Preview stale; do not clear intent.

- [ ] **Step 5: Verify**

Run: `npm test -- src/services/payment/PaymentService.test.js && npm run build:h5 && npm run build:mp-weixin`

- [ ] **Step 6: Commit**

```bash
git add cozy-coffee-mobile/src/stores cozy-coffee-mobile/src/services cozy-coffee-mobile/src/App.vue cozy-coffee-mobile/src/pages
git commit -m "feat(mobile): add session order and payment services"
```

### Task 6: Implement CheckoutWorkflow

**Files:**
- Create: `cozy-coffee-mobile/src/services/checkout/CheckoutPreviewService.js`
- Create: `cozy-coffee-mobile/src/services/checkout/CheckoutWorkflow.js`
- Create: `cozy-coffee-mobile/src/services/checkout/CheckoutWorkflow.test.js`

- [ ] **Step 1: Write workflow tests with fakes**

Cover success, offline, auth recovery, stale Preview, payment cancel, order failure, and repeated submit using the same idempotency key.

- [ ] **Step 2: Run and verify failure**

Run: `npm test -- src/services/checkout/CheckoutWorkflow.test.js`

- [ ] **Step 3: Implement the workflow**

Expose `preview()`, `submit()`, and `recover()`. Coordinate services through constructor injection so tests use fakes. Log each Frozen Spec event with `traceId`, `idempotencyKey`, `previewVersion`, stage, duration, platform, and error code.

- [ ] **Step 4: Run workflow tests**

Run: `npm test -- src/services/checkout/CheckoutWorkflow.test.js`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add cozy-coffee-mobile/src/services/checkout
git commit -m "feat(mobile): orchestrate checkout workflow"
```

### Task 7: Refactor menu, specification sheet, and cart UI

**Files:**
- Create: `cozy-coffee-mobile/src/components/product/ProductListItem.vue`
- Create: `cozy-coffee-mobile/src/components/product/ProductSpecSheet.vue`
- Create: `cozy-coffee-mobile/src/components/cart/CartBar.vue`
- Create: `cozy-coffee-mobile/src/components/cart/CartSheet.vue`
- Create: `cozy-coffee-mobile/src/components/cart/CartLineItem.vue`
- Create: `cozy-coffee-mobile/src/components/product/ProductSpecSheet.test.js`
- Modify: `cozy-coffee-mobile/src/pages/menu/menu.vue`

- [ ] **Step 1: Write a failing component test**

Mount `ProductSpecSheet` with temperature, cup size, sugar, milk, and strength options. Select values and assert the emitted cart line contains every normalized field.

- [ ] **Step 2: Run the test**

Run: `npm test -- src/components/product/ProductSpecSheet.test.js`

- [ ] **Step 3: Build focused components**

Keep the menu page responsible only for loading products, active category, opening the sheet, and opening checkout. Cart editing reuses the same spec sheet.

- [ ] **Step 4: Apply the approved efficient-ordering UI**

Use left categories, right product list, fixed bottom cart bar, 44px minimum touch targets, safe-area padding, skeleton/empty/retry states, and restrained brown only for primary actions.

- [ ] **Step 5: Verify**

Run: `npm test -- src/components/product/ProductSpecSheet.test.js && npm run build:h5 && npm run build:mp-weixin`

- [ ] **Step 6: Commit**

```bash
git add cozy-coffee-mobile/src/components/product cozy-coffee-mobile/src/components/cart cozy-coffee-mobile/src/pages/menu/menu.vue
git commit -m "feat(mobile): rebuild menu and cart experience"
```

### Task 8: Refactor checkout, mock payment, and order result pages

**Files:**
- Create: `cozy-coffee-mobile/src/components/checkout/StoreSummary.vue`
- Create: `cozy-coffee-mobile/src/components/checkout/CheckoutPriceSummary.vue`
- Create: `cozy-coffee-mobile/src/components/checkout/CheckoutSubmitBar.vue`
- Create: `cozy-coffee-mobile/src/components/states/LoadingSkeleton.vue`
- Create: `cozy-coffee-mobile/src/components/states/EmptyState.vue`
- Create: `cozy-coffee-mobile/src/components/states/RetryState.vue`
- Create: `cozy-coffee-mobile/src/components/states/OfflineNotice.vue`
- Modify: `cozy-coffee-mobile/src/pages/order/confirm.vue`
- Modify: `cozy-coffee-mobile/src/pages/order/result.vue`
- Modify: `cozy-coffee-mobile/src/pages/order/detail.vue`

- [ ] **Step 1: Add checkout-page integration tests**

Assert fixed store only, no delivery/address controls, status-specific submit labels, cancel preserving cart, success clearing cart, and order ID/取餐码 rendering.

- [ ] **Step 2: Run tests and verify failure**

Run: `npm test -- src/pages/order`

- [ ] **Step 3: Replace confirm-page business logic**

The page loads `CheckoutWorkflow.preview()`, renders store/items/remark/preview, and calls `submit()`. Remove local coupon allocation, delivery fee, address, and direct `createOrder` logic.

- [ ] **Step 4: Update result and detail pages**

Read order data from API, not only query-string estimates. Render pending/preparing/completed/cancelled states and pickup code where available.

- [ ] **Step 5: Verify builds and tests**

Run: `npm test && npm run build:h5 && npm run build:mp-weixin`

- [ ] **Step 6: Commit**

```bash
git add cozy-coffee-mobile/src/components/checkout cozy-coffee-mobile/src/components/states cozy-coffee-mobile/src/pages/order
git commit -m "feat(mobile): complete pickup checkout flow"
```

### Task 9: End-to-end verification and beginner handoff

**Files:**
- Create: `cozy-coffee-mobile/tests/ordering-flow.spec.js`
- Create: `cozy-coffee-mobile/README.md`

- [ ] **Step 1: Add automated ordering flow**

Cover browse → choose specs → add cart → checkout identity → preview → mock pay → result. Add a loop that triggers submit 10 times and asserts one order ID.

- [ ] **Step 2: Add background recovery case**

Simulate hide/show with a five-minute clock advance; assert intent remains and Preview refreshes before submit.

- [ ] **Step 3: Document local development**

Document environment files, H5 proxy, WeChat LAN/test-domain setup, test commands, build commands, directory responsibilities, and the rule that pages never calculate checkout price.

- [ ] **Step 4: Run full verification**

Run: `npm test`

Run: `npm run build:h5`

Run: `npm run build:mp-weixin`

Expected: all PASS/build complete.

- [ ] **Step 5: Perform manual WeChat checks**

Verify safe areas, category scrolling, bottom sheets, offline recovery, background recovery, login recovery, price-change prompt, payment cancel, and one-order idempotency on a real device.

- [ ] **Step 6: Commit**

```bash
git add cozy-coffee-mobile/tests cozy-coffee-mobile/README.md
git commit -m "test(mobile): verify pickup ordering journey"
```
