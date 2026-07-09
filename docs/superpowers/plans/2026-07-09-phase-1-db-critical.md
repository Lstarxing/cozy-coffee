# Phase 1: DB Critical Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 7 database design defects (DC3/DC5/DC6/DC7/DH2/DH3/DH5) that block production safety. Each fix is a single DDL migration with up/down scripts, verified by Phase 0 baseline tests.

**Architecture:** All migrations run against the running Docker MySQL (port 3306, root/123456). Migration files live in `cozy-coffee-backend/mysql/migrations/` with Flyway-style naming `V<YYYYMMDD>_<NNN>__<description>.sql`. Each migration has a paired `<same-name>.down.sql` for rollback. After each migration, re-run Phase 0 baseline (6 tests) to confirm no regression.

**Tech Stack:** MySQL 8.0, plain SQL migrations (no Flyway/Liquibase tooling -- applied manually via `docker exec mysql`).

## Global Constraints

- **DB connection:** `docker exec cozy-mysql mysql -uroot -p123456 <db_name>` for all migrations. Password `123456` matches docker-compose default.
- **Migration file location:** `cozy-coffee-backend/mysql/migrations/V20260709_NNN__desc.sql` (new directory). Each migration paired with `V20260709_NNN__desc.down.sql`.
- **Pre-migration backup:** Before Task 1, take a full DB dump: `docker exec cozy-mysql mysqldump -uroot -p123456 --all-databases > /tmp/cozy-backup-$(date +%Y%m%d).sql`. Store on host at `C:\Users\dell\Desktop\CozyCoffee\backups\`.
- **Baseline regression check:** After each migration, run: `cd cozy-coffee-backend && mvn -pl cozy-provider/cozy-member-provider -am test -Dtest=PointsConsistencyTest,PointsFIFOConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false` (5/5 must pass). Task 6 (shop_orders ENUM) and Task 7 (member_info) also require order-provider test: `-Dtest=OrderFlowBaselineTest` (1/1 must pass).
- **No placeholder SQL:** Every SQL block is complete and runnable verbatim.
- **Commit style:** `<type>(db): <subject>` e.g. `fix(db): drop shop_order_items FK on order_id (DC3)`.
- **Order provider may need restart** after schema change if MyBatis caches metadata. If baseline tests fail with "unknown column" or similar, ask user to restart order-provider in IDE.

---

## Task 1: Drop 2 Foreign Keys (DC3)

**Files:**
- Create: `cozy-coffee-backend/mysql/migrations/V20260709_001__drop_foreign_keys.sql`
- Create: `cozy-coffee-backend/mysql/migrations/V20260709_001__drop_foreign_keys.down.sql`

**Verified FKs to drop (queried via information_schema.KEY_COLUMN_USAGE):**
- `cozy_order.shop_order_items.fk_shop_order_items_order_id` -> `shop_orders(id) ON DELETE CASCADE`
- `cozy_member.points_lot_consumptions.fk_points_lot_consumptions_lot_id` -> `points_lots(id) ON DELETE RESTRICT`

**Why:** Internet-facing production projects forbid FKs -- they cause cascading locks, make sharding impossible, and hide cross-table integrity in DB rather than app code. CASCADE deletes are especially dangerous (one bad delete wipes child rows).

- [ ] **Step 1: Take pre-migration backup**

```bash
mkdir -p "C:/Users/dell/Desktop/CozyCoffee/backups"
docker exec cozy-mysql mysqldump -uroot -p123456 --all-databases --routines --triggers > "C:/Users/dell/Desktop/CozyCoffee/backups/cozy-backup-$(date +%Y%m%d-%H%M%S).sql"
ls -lh "C:/Users/dell/Desktop/CozyCoffee/backups/"
```
Expected: a `.sql` file > 1MB. If empty or missing, STOP -- backup is critical.

- [ ] **Step 2: Write up migration**

Create `cozy-coffee-backend/mysql/migrations/V20260709_001__drop_foreign_keys.sql`:
```sql
-- DC3: Drop foreign keys (internet project anti-pattern)
-- Verified FKs via: SELECT * FROM information_schema.KEY_COLUMN_USAGE WHERE REFERENCED_TABLE_NAME IS NOT NULL

-- 1. cozy_order.shop_order_items -> shop_orders (ON DELETE CASCADE, dangerous)
USE cozy_order;
ALTER TABLE shop_order_items
  DROP FOREIGN KEY fk_shop_order_items_order_id;

-- 2. cozy_member.points_lot_consumptions -> points_lots (ON DELETE RESTRICT)
USE cozy_member;
ALTER TABLE points_lot_consumptions
  DROP FOREIGN KEY fk_points_lot_consumptions_lot_id;
```

- [ ] **Step 3: Write down migration**

Create `cozy-coffee-backend/mysql/migrations/V20260709_001__drop_foreign_keys.down.sql`:
```sql
-- Rollback: re-add the 2 foreign keys

USE cozy_order;
ALTER TABLE shop_order_items
  ADD CONSTRAINT fk_shop_order_items_order_id
    FOREIGN KEY (order_id) REFERENCES shop_orders(id)
    ON DELETE CASCADE;

USE cozy_member;
ALTER TABLE points_lot_consumptions
  ADD CONSTRAINT fk_points_lot_consumptions_lot_id
    FOREIGN KEY (lot_id) REFERENCES points_lots(id)
    ON DELETE RESTRICT;
```

- [ ] **Step 4: Execute up migration**

```bash
docker exec -i cozy-mysql mysql -uroot -p123456 < "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee/cozy-coffee-backend/mysql/migrations/V20260709_001__drop_foreign_keys.sql"
```
Expected: no output (success). Verify:
```bash
docker exec cozy-mysql mysql -uroot -p123456 -e "SELECT TABLE_NAME, CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA IN ('cozy_order','cozy_member') AND REFERENCED_TABLE_NAME IS NOT NULL;"
```
Expected: empty result set (0 rows).

- [ ] **Step 5: Verify baseline tests still pass**

```bash
cd "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee/cozy-coffee-backend"
mvn -pl cozy-provider/cozy-member-provider -am test -Dtest=PointsConsistencyTest,PointsFIFOConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run:|BUILD"
```
Expected: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0` + `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
cd "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee"
git add cozy-coffee-backend/mysql/migrations/V20260709_001__drop_foreign_keys.sql
git add cozy-coffee-backend/mysql/migrations/V20260709_001__drop_foreign_keys.down.sql
git commit -m "fix(db): drop 2 foreign keys (DC3)

shop_order_items.fk_shop_order_items_order_id (CASCADE -- dangerous)
points_lot_consumptions.fk_points_lot_consumptions_lot_id (RESTRICT)

FKs cause cascading locks and block future sharding. App layer enforces
integrity (OrderService deletes items explicitly; points_lot_consumptions
is written by PointsLotMapper only).

Verified: 5/5 member-provider baseline tests still pass.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 2: Add uk_aggregate_type to message_outbox (DC5)

**Files:**
- Create: `cozy-coffee-backend/mysql/migrations/V20260709_002__add_outbox_unique_key.sql`
- Create: `cozy-coffee-backend/mysql/migrations/V20260709_002__add_outbox_unique_key.down.sql`

**Verified table state:** `cozy_order.message_outbox` has `idx_aggregate (aggregate_id)` non-unique index, no unique constraint. Existing data may contain duplicates -- need to check first.

- [ ] **Step 1: Check for existing duplicates**

```bash
docker exec cozy-mysql mysql -uroot -p123456 cozy_order -e "SELECT aggregate_id, message_type, COUNT(*) as cnt FROM message_outbox GROUP BY aggregate_id, message_type HAVING cnt > 1;"
```
Expected: empty (no duplicates) OR list of duplicate groups. If duplicates exist, STOP and report DONE_WITH_CONCERNS -- need to deduplicate first (keep latest `id` per group, delete rest). For Phase 1, assume no duplicates (table has AUTO_INCREMENT=2, only 1 row ever written).

- [ ] **Step 2: Write up migration**

Create `cozy-coffee-backend/mysql/migrations/V20260709_002__add_outbox_unique_key.sql`:
```sql
-- DC5: Add unique key to message_outbox to prevent duplicate event publishes
USE cozy_order;

-- Drop the non-unique index (will be replaced by unique key)
ALTER TABLE message_outbox
  DROP INDEX idx_aggregate;

-- Add unique key
ALTER TABLE message_outbox
  ADD UNIQUE KEY uk_aggregate_type (aggregate_id, message_type);
```

- [ ] **Step 3: Write down migration**

Create `cozy-coffee-backend/mysql/migrations/V20260709_002__add_outbox_unique_key.down.sql`:
```sql
USE cozy_order;
ALTER TABLE message_outbox
  DROP INDEX uk_aggregate_type;
ALTER TABLE message_outbox
  ADD INDEX idx_aggregate (aggregate_id);
```

- [ ] **Step 4: Execute up migration**

```bash
docker exec -i cozy-mysql mysql -uroot -p123456 < "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee/cozy-coffee-backend/mysql/migrations/V20260709_002__add_outbox_unique_key.sql"
docker exec cozy-mysql mysql -uroot -p123456 cozy_order -e "SHOW INDEX FROM message_outbox WHERE Key_name='uk_aggregate_type';"
```
Expected: 2 rows (one per column in composite key), `Non_unique=0`.

- [ ] **Step 5: Verify baseline tests**

Same as Task 1 Step 5. Expected: 5/5 passing.

- [ ] **Step 6: Commit**

```bash
cd "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee"
git add cozy-coffee-backend/mysql/migrations/V20260709_002__add_outbox_unique_key.sql
git add cozy-coffee-backend/mysql/migrations/V20260709_002__add_outbox_unique_key.down.sql
git commit -m "fix(db): add uk_aggregate_type unique key to message_outbox (DC5)

Prevents duplicate event publishes when OutboxService.publish is called
twice for the same aggregate (e.g., createOrder retry). Existing
idx_aggregate (non-unique) replaced.

App-side: OutboxService.publish must catch DuplicateKeyException and
skip silently -- this is a Phase 3 code change, not Phase 1.

Verified: 5/5 member-provider baseline tests pass.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 3: Add uk_consume_lot to points_lot_consumptions (DC6)

**Files:**
- Create: `cozy-coffee-backend/mysql/migrations/V20260709_003__add_consumptions_unique_key.sql`
- Create: `cozy-coffee-backend/mysql/migrations/V20260709_003__add_consumptions_unique_key.down.sql`

**Verified table state:** `cozy_member.points_lot_consumptions` has `idx_consume (consume_type, consume_id)` non-unique, no unique constraint. Need 3-column unique key: `(consume_type, consume_id, lot_id)`.

- [ ] **Step 1: Check for existing duplicates**

```bash
docker exec cozy-mysql mysql -uroot -p123456 cozy_member -e "SELECT consume_type, consume_id, lot_id, COUNT(*) as cnt FROM points_lot_consumptions GROUP BY consume_type, consume_id, lot_id HAVING cnt > 1;"
```
Expected: empty. If duplicates, STOP and dedupe first (keep latest `id`).

- [ ] **Step 2: Write up migration**

Create `cozy-coffee-backend/mysql/migrations/V20260709_003__add_consumptions_unique_key.sql`:
```sql
-- DC6: Add unique key to points_lot_consumptions to prevent duplicate FIFO deductions
USE cozy_member;

-- Keep idx_consume (consume_type, consume_id) for non-unique lookups
-- Add new unique key with 3 columns
ALTER TABLE points_lot_consumptions
  ADD UNIQUE KEY uk_consume_lot (consume_type, consume_id, lot_id);
```

- [ ] **Step 3: Write down migration**

Create `cozy-coffee-backend/mysql/migrations/V20260709_003__add_consumptions_unique_key.down.sql`:
```sql
USE cozy_member;
ALTER TABLE points_lot_consumptions
  DROP INDEX uk_consume_lot;
```

- [ ] **Step 4: Execute and verify**

```bash
docker exec -i cozy-mysql mysql -uroot -p123456 < "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee/cozy-coffee-backend/mysql/migrations/V20260709_003__add_consumptions_unique_key.sql"
docker exec cozy-mysql mysql -uroot -p123456 cozy_member -e "SHOW INDEX FROM points_lot_consumptions WHERE Key_name='uk_consume_lot';"
```
Expected: 3 rows, `Non_unique=0`.

- [ ] **Step 5: Verify baseline tests (CRITICAL -- this table is exercised by PointsFIFOConsistencyTest)**

Same as Task 1 Step 5. Expected: 5/5 passing. If `testFIFODeductionAcrossBatches` fails with "Duplicate entry" on insert, the test's `consumePointsFIFO` is trying to insert a row that violates the new UK -- this means the UK is too strict and the unique key definition is wrong. STOP and report DONE_WITH_CONCERNS.

- [ ] **Step 6: Commit**

```bash
cd "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee"
git add cozy-coffee-backend/mysql/migrations/V20260709_003__add_consumptions_unique_key.sql
git add cozy-coffee-backend/mysql/migrations/V20260709_003__add_consumptions_unique_key.down.sql
git commit -m "fix(db): add uk_consume_lot unique key to points_lot_consumptions (DC6)

Prevents duplicate FIFO deduction when PointsLotService.consumePointsFIFO
is called twice for the same consume_id (e.g., MQ retry). The 3-column
key (consume_type, consume_id, lot_id) allows one consumption per lot
per consume operation.

App-side: consumePointsFIFO must catch DuplicateKeyException -- Phase 3
code change.

Verified: 5/5 member-provider baseline tests pass (incl. FIFO tests).

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 4: Drop redundant indexes on shop_orders (DH2)

**Files:**
- Create: `cozy-coffee-backend/mysql/migrations/V20260709_004__drop_redundant_indexes.sql`
- Create: `cozy-coffee-backend/mysql/migrations/V20260709_004__drop_redundant_indexes.down.sql`

**Verified indexes to drop:**
- `idx_user_id (user_id)` -- redundant: `idx_user_created (user_id, created_at)` covers prefix lookups
- `idx_order_no (order_no)` -- redundant: `order_no` already has `UNIQUE KEY order_no` (which IS an index)

**Indexes to KEEP:**
- `PRIMARY KEY (id)`
- `UNIQUE KEY order_no (order_no)` -- serves uniqueness + lookup
- `idx_created_at (created_at)` -- for admin time-range queries (no user prefix)
- `idx_user_created (user_id, created_at)` -- covers user-scoped time queries
- `idx_status_created (status, created_at)` -- for admin status+time queries
- `idx_orders_dining_method` -- for dining method filter
- `idx_delivery_fee_waived` -- for delivery fee report

- [ ] **Step 1: Write up migration**

Create `cozy-coffee-backend/mysql/migrations/V20260709_004__drop_redundant_indexes.sql`:
```sql
-- DH2: Drop redundant indexes on shop_orders (saves write overhead)
USE cozy_order;

-- idx_user_id is prefix of idx_user_created -- duplicate
ALTER TABLE shop_orders DROP INDEX idx_user_id;

-- idx_order_no duplicates UNIQUE KEY order_no -- the unique constraint already indexes it
ALTER TABLE shop_orders DROP INDEX idx_order_no;
```

- [ ] **Step 2: Write down migration**

Create `cozy-coffee-backend/mysql/migrations/V20260709_004__drop_redundant_indexes.down.sql`:
```sql
USE cozy_order;
ALTER TABLE shop_orders ADD INDEX idx_user_id (user_id) USING BTREE;
ALTER TABLE shop_orders ADD INDEX idx_order_no (order_no) USING BTREE;
```

- [ ] **Step 3: Execute and verify**

```bash
docker exec -i cozy-mysql mysql -uroot -p123456 < "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee/cozy-coffee-backend/mysql/migrations/V20260709_004__drop_redundant_indexes.sql"
docker exec cozy-mysql mysql -uroot -p123456 cozy_order -e "SHOW INDEX FROM shop_orders;"
```
Expected: 6 indexes remaining (PRIMARY, order_no UNIQUE, idx_created_at, idx_user_created, idx_status_created, idx_orders_dining_method, idx_delivery_fee_waived). idx_user_id and idx_order_no gone.

- [ ] **Step 4: Verify baseline tests**

Run both member-provider AND order-provider tests:
```bash
cd "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee/cozy-coffee-backend"
mvn -pl cozy-provider/cozy-member-provider -am test -Dtest=PointsConsistencyTest,PointsFIFOConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run:|BUILD"
mvn -pl cozy-provider/cozy-order-provider -am test -Dtest=OrderFlowBaselineTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run:|BUILD"
```
Expected: 5/5 + 1/1, both BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
cd "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee"
git add cozy-coffee-backend/mysql/migrations/V20260709_004__drop_redundant_indexes.sql
git add cozy-coffee-backend/mysql/migrations/V20260709_004__drop_redundant_indexes.down.sql
git commit -m "perf(db): drop redundant indexes on shop_orders (DH2)

idx_user_id is prefix of idx_user_created (covered).
idx_order_no duplicates the UNIQUE KEY order_no (already indexed).

Saves 2 index writes per INSERT on shop_orders.

Verified: 5/5 member + 1/1 order baseline tests pass.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 5: Add uk_user_used_order to user_coupons (DH5)

**Files:**
- Create: `cozy-coffee-backend/mysql/migrations/V20260709_005__add_user_coupons_unique_key.sql`
- Create: `cozy-coffee-backend/mysql/migrations/V20260709_005__add_user_coupons_unique_key.down.sql`

**Verified table state:** `cozy_mall.user_coupons` has `uk_coupon_code (coupon_code)` unique + `idx_user_status` + `idx_expires`. No UK on `(user_id, used_shop_order_id)`. DH5 wants to prevent "one coupon used by two orders" race.

**Subtlety:** `used_shop_order_id` is NULL for ISSUED coupons (not yet used). MySQL unique keys allow multiple NULLs (standard SQL behavior), so a UK on `(user_id, used_shop_order_id)` will allow many unused coupons per user -- which is correct -- while preventing duplicate `used_shop_order_id` per user.

- [ ] **Step 1: Check for existing duplicates**

```bash
docker exec cozy-mysql mysql -uroot -p123456 cozy_mall -e "SELECT user_id, used_shop_order_id, COUNT(*) as cnt FROM user_coupons WHERE used_shop_order_id IS NOT NULL GROUP BY user_id, used_shop_order_id HAVING cnt > 1;"
```
Expected: empty. If duplicates, dedupe first (keep latest, NULL out others).

- [ ] **Step 2: Write up migration**

Create `cozy-coffee-backend/mysql/migrations/V20260709_005__add_user_coupons_unique_key.sql`:
```sql
-- DH5: Add unique key to prevent coupon double-spending
-- (user_id, used_shop_order_id) ensures one coupon per user per order
USE cozy_mall;

ALTER TABLE user_coupons
  ADD UNIQUE KEY uk_user_used_order (user_id, used_shop_order_id);
```

- [ ] **Step 3: Write down migration**

Create `cozy-coffee-backend/mysql/migrations/V20260709_005__add_user_coupons_unique_key.down.sql`:
```sql
USE cozy_mall;
ALTER TABLE user_coupons
  DROP INDEX uk_user_used_order;
```

- [ ] **Step 4: Execute and verify**

```bash
docker exec -i cozy-mysql mysql -uroot -p123456 < "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee/cozy-coffee-backend/mysql/migrations/V20260709_005__add_user_coupons_unique_key.sql"
docker exec cozy-mysql mysql -uroot -p123456 cozy_mall -e "SHOW INDEX FROM user_coupons WHERE Key_name='uk_user_used_order';"
```
Expected: 2 rows, `Non_unique=0`.

- [ ] **Step 5: Verify baseline tests (CRITICAL -- OrderFlowBaselineTest uses coupon 6M9E9JJE)**

Run both:
```bash
cd "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee/cozy-coffee-backend"
mvn -pl cozy-provider/cozy-member-provider -am test -Dtest=PointsConsistencyTest,PointsFIFOConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run:|BUILD"
mvn -pl cozy-provider/cozy-order-provider -am test -Dtest=OrderFlowBaselineTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run:|BUILD"
```
Expected: 5/5 + 1/1.

**If OrderFlowBaselineTest fails with "Duplicate entry" on coupon use:** the `@BeforeTransaction` reset isn't NULLing `used_shop_order_id`. Check the reset SQL in `OrderFlowBaselineTest.java:64` -- it sets `used_shop_order_id=NULL` already, so should be fine. If still failing, report DONE_WITH_CONCERNS.

- [ ] **Step 6: Commit**

```bash
cd "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee"
git add cozy-coffee-backend/mysql/migrations/V20260709_005__add_user_coupons_unique_key.sql
git add cozy-coffee-backend/mysql/migrations/V20260709_005__add_user_coupons_unique_key.down.sql
git commit -m "fix(db): add uk_user_used_order unique key to user_coupons (DH5)

Prevents coupon double-spending: (user_id, used_shop_order_id) ensures
one coupon can be linked to at most one order per user. NULL
used_shop_order_id (unused coupons) allows multiple per user (standard
SQL NULL semantics).

Verified: 5/5 member + 1/1 order baseline tests pass.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 6: shop_orders.status -> ENUM + fix DEFAULT bug (DC7)

**Files:**
- Create: `cozy-coffee-backend/mysql/migrations/V20260709_006__shop_orders_status_enum.sql`
- Create: `cozy-coffee-backend/mysql/migrations/V20260709_006__shop_orders_status_enum.down.sql`

**Verified state:**
- Current: `status VARCHAR(20) DEFAULT 'completed'` -- DEFAULT is the bug (new order should be pending)
- Comment says "pending/completed/cancelled" but real states include `preparing` (per OrderServiceImpl.acceptOrder)
- Existing data: mix of 'pending'/'preparing'/'completed'/'cancelled'

- [ ] **Step 1: Verify existing data has only valid status values**

```bash
docker exec cozy-mysql mysql -uroot -p123456 cozy_order -e "SELECT status, COUNT(*) FROM shop_orders GROUP BY status;"
```
Expected: only values in ('pending','preparing','completed','cancelled'). If any other value (e.g., 'Completed', 'PENDING' case mismatch), STOP -- need data cleanup first.

- [ ] **Step 2: Write up migration**

Create `cozy-coffee-backend/mysql/migrations/V20260709_006__shop_orders_status_enum.sql`:
```sql
-- DC7: shop_orders.status -> ENUM + fix DEFAULT bug
-- Current DEFAULT 'completed' is wrong (new orders should be pending)
USE cozy_order;

ALTER TABLE shop_orders
  MODIFY COLUMN status ENUM('pending','preparing','completed','cancelled')
    NOT NULL DEFAULT 'pending'
    COMMENT '订单状态: pending=待处理 preparing=制作中 completed=已完成 cancelled=已取消';
```

- [ ] **Step 3: Write down migration**

Create `cozy-coffee-backend/mysql/migrations/V20260709_006__shop_orders_status_enum.down.sql`:
```sql
USE cozy_order;
ALTER TABLE shop_orders
  MODIFY COLUMN status VARCHAR(20)
    DEFAULT 'completed'
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
    COMMENT '订单状态:pending/completed/cancelled';
```

- [ ] **Step 4: Execute and verify**

```bash
docker exec -i cozy-mysql mysql -uroot -p123456 < "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee/cozy-coffee-backend/mysql/migrations/V20260709_006__shop_orders_status_enum.sql"
docker exec cozy-mysql mysql -uroot -p123456 cozy_order -e "SHOW COLUMNS FROM shop_orders WHERE Field='status';"
```
Expected: `Type=enum('pending','preparing','completed','cancelled')`, `Default=pending`, `Null=NO`.

- [ ] **Step 5: Verify baseline tests**

Run both member + order tests (shop_orders is the central table):
```bash
cd "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee/cozy-coffee-backend"
mvn -pl cozy-provider/cozy-member-provider -am test -Dtest=PointsConsistencyTest,PointsFIFOConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run:|BUILD"
mvn -pl cozy-provider/cozy-order-provider -am test -Dtest=OrderFlowBaselineTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run:|BUILD"
```
Expected: 5/5 + 1/1.

- [ ] **Step 6: Commit**

```bash
cd "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee"
git add cozy-coffee-backend/mysql/migrations/V20260709_006__shop_orders_status_enum.sql
git add cozy-coffee-backend/mysql/migrations/V20260709_006__shop_orders_status_enum.down.sql
git commit -m "fix(db): shop_orders.status -> ENUM + fix DEFAULT bug (DC7)

VARCHAR(20) DEFAULT 'completed' was wrong on both counts:
1. New orders should default to 'pending' (DEFAULT 'completed' was a bug)
2. No enum constraint allowed arbitrary strings (state machine uncontrolled)

Now ENUM('pending','preparing','completed','cancelled') NOT NULL DEFAULT 'pending'.
Existing data verified clean (only valid values present).

Verified: 5/5 member + 1/1 order baseline tests pass.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 7: Fix member_info charset + monthly_spent_month -> DATE (DH3)

**Files:**
- Create: `cozy-coffee-backend/mysql/migrations/V20260709_007__member_info_charset_and_date.sql`
- Create: `cozy-coffee-backend/mysql/migrations/V20260709_007__member_info_charset_and_date.down.sql`

**Verified state:**
- `member_info.monthly_spent_month VARCHAR(7)` storing '2025-12' format strings
- Column comments show `????` (mojibake) -- charset issue on the table itself
- Need: convert VARCHAR(7) 'YYYY-MM' to DATE (first day of month)

- [ ] **Step 1: Verify current monthly_spent_month data format**

```bash
docker exec cozy-mysql mysql -uroot -p123456 cozy_member -e "SELECT monthly_spent_month, COUNT(*) FROM member_info GROUP BY monthly_spent_month;"
```
Expected: values like '2026-07' (7-char YYYY-MM). If any malformed (NULL, '2026-7', etc.), STOP -- data cleanup needed.

- [ ] **Step 2: Write up migration**

Create `cozy-coffee-backend/mysql/migrations/V20260709_007__member_info_charset_and_date.sql`:
```sql
-- DH3: Fix member_info charset + monthly_spent_month -> DATE
USE cozy_member;

-- Step A: Convert VARCHAR(7) 'YYYY-MM' to DATE (first of month)
-- Using STR_TO_DATE then DATE format
ALTER TABLE member_info
  ADD COLUMN monthly_spent_month_new DATE NULL AFTER monthly_spent_month;

UPDATE member_info
  SET monthly_spent_month_new = CASE
    WHEN monthly_spent_month IS NOT NULL AND monthly_spent_month REGEXP '^[0-9]{4}-[0-9]{2}$'
      THEN STR_TO_DATE(CONCAT(monthly_spent_month, '-01'), '%Y-%m-%d')
    ELSE NULL
  END;

ALTER TABLE member_info DROP COLUMN monthly_spent_month;
ALTER TABLE member_info
  CHANGE COLUMN monthly_spent_month_new monthly_spent_month DATE NULL
    COMMENT '当前月度消费统计月份 (DATE, 月初)';

-- Step B: Fix mojibake comments by re-CONVERTING to proper charset
-- (this resets column charsets/collations to table default utf8mb4)
ALTER TABLE member_info CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Step C: Re-add comments (the CONVERT above strips comments -- MySQL quirk)
-- Re-run the original column comments from the schema file
ALTER TABLE member_info
  MODIFY COLUMN user_id bigint NOT NULL COMMENT '用户ID',
  MODIFY COLUMN member_level varchar(20) NOT NULL DEFAULT 'basic' COMMENT '会员等级: basic/silver/gold/diamond/black',
  MODIFY COLUMN total_points int NOT NULL DEFAULT 0 COMMENT '累计积分',
  MODIFY COLUMN current_points int NOT NULL DEFAULT 0 COMMENT '当前可用积分',
  MODIFY COLUMN exp_total int NOT NULL DEFAULT 0 COMMENT '累计EXP',
  MODIFY COLUMN consecutive_sign_days int NOT NULL DEFAULT 0 COMMENT '连续签到天数',
  MODIFY COLUMN last_signin_date date NULL COMMENT '最后签到日期',
  MODIFY COLUMN monthly_spent decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '本月累计消费金额',
  MODIFY COLUMN monthly_spent_month DATE NULL COMMENT '当前月度消费统计月份 (月初)',
  MODIFY COLUMN monthly_accelerate_remaining decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '本月剩余加速额度';
```

- [ ] **Step 3: Write down migration**

Create `cozy-coffee-backend/mysql/migrations/V20260709_007__member_info_charset_and_date.down.sql`:
```sql
USE cozy_member;

-- Revert monthly_spent_month to VARCHAR(7)
ALTER TABLE member_info
  ADD COLUMN monthly_spent_month_str VARCHAR(7) NULL AFTER monthly_spent_month;

UPDATE member_info
  SET monthly_spent_month_str = DATE_FORMAT(monthly_spent_month, '%Y-%m')
  WHERE monthly_spent_month IS NOT NULL;

ALTER TABLE member_info DROP COLUMN monthly_spent_month;
ALTER TABLE member_info
  CHANGE COLUMN monthly_spent_month_str monthly_spent_month VARCHAR(7) NULL
    COMMENT '当前月度消费统计月份 (YYYY-MM)';
```

- [ ] **Step 4: Execute and verify**

```bash
docker exec -i cozy-mysql mysql -uroot -p123456 < "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee/cozy-coffee-backend/mysql/migrations/V20260709_007__member_info_charset_and_date.sql"
docker exec cozy-mysql mysql -uroot -p123456 cozy_member -e "SHOW COLUMNS FROM member_info WHERE Field='monthly_spent_month';"
```
Expected: `Type=date`, `Null=YES`.

If the migration fails partway (e.g., CONVERT strips comments and re-adding comments fails because column types don't match exactly), use the down migration to revert and report DONE_WITH_CONCERNS.

- [ ] **Step 5: Verify baseline tests (CRITICAL -- member_info is central)**

Run both:
```bash
cd "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee/cozy-coffee-backend"
mvn -pl cozy-provider/cozy-member-provider -am test -Dtest=PointsConsistencyTest,PointsFIFOConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run:|BUILD"
mvn -pl cozy-provider/cozy-order-provider -am test -Dtest=OrderFlowBaselineTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run:|BUILD"
```
Expected: 5/5 + 1/1.

**If member tests fail with "Invalid value for monthly_spent_month" or similar:** the MyBatis entity is still mapping to String. This is expected -- Phase 1 is DB-only, app code adapts in Phase 2+. The failure is acceptable IF the test was passing before Task 7 (so we know the regression is from this migration). Report DONE_WITH_CONCERNS listing the specific failure -- app-side fix is needed (change `MemberInfo.monthlySpentMonth` from `String` to `LocalDate`).

- [ ] **Step 6: Commit**

```bash
cd "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee"
git add cozy-coffee-backend/mysql/migrations/V20260709_007__member_info_charset_and_date.sql
git add cozy-coffee-backend/mysql/migrations/V20260709_007__member_info_charset_and_date.down.sql
git commit -m "fix(db): member_info charset fix + monthly_spent_month -> DATE (DH3)

VARCHAR(7) 'YYYY-MM' -> DATE (month-first). DATE enables native
date arithmetic (DATE_ADD, LAST_DAY) instead of string concat.

CONVERT TO CHARACTER SET utf8mb4 fixes mojibake column comments.
Re-adds comments explicitly (CONVERT strips them -- MySQL quirk).

If MyBatis entity MemberInfo.monthlySpentMonth is still String,
app-side LocalDate migration is Phase 2 code change.

Verified: 5/5 member + 1/1 order baseline tests pass (or known regression flagged).

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Final: Update CHANGELOG and Ledger

**Files:**
- Modify: `C:\Users\dell\Desktop\surx-note\CozyCoffee\CHANGELOG.md` (external path)
- Modify: `.superpowers/sdd/progress.md`

- [ ] **Step 1: Update CHANGELOG with Phase 1 summary**

Add a new `### Phase 1：DB Critical Fixes（7 个 DDL migration）` section under `## 2026-07-09` in CHANGELOG, listing all 7 migrations with commit SHAs.

- [ ] **Step 2: Update progress ledger**

Append to `.superpowers/sdd/progress.md`:
```
- Phase 1: complete
  - Task 1: drop FKs (commit <sha>)
  - Task 2: uk_aggregate_type (commit <sha>)
  - Task 3: uk_consume_lot (commit <sha>)
  - Task 4: drop redundant indexes (commit <sha>)
  - Task 5: uk_user_used_order (commit <sha>)
  - Task 6: status ENUM (commit <sha>)
  - Task 7: member_info charset + DATE (commit <sha>)
  - All 7 migrations verified: 6/6 baseline tests still passing
```

- [ ] **Step 3: Commit ledger + CHANGELOG pointer**

```bash
cd "C:/Users/dell/Desktop/CozyCoffee/cozy-coffee"
git add .superpowers/sdd/progress.md docs/superpowers/plans/PHASE-1-PROGRESS.md
git commit -m "docs(phase-1): record DB Critical Fixes progress

7 DDL migrations applied, 6/6 baseline tests still passing.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```
