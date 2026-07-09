# Phase 0 Progress

- Date: 2026-07-09
- Status: COMPLETE
- Branch: feature/audit-spec-refactor
- Plan: docs/superpowers/plans/2026-07-09-phase-0-test-baseline.md

## Tests verified (6/6 passing)

- PointsConsistencyTest: 3/3 PASS
  - testAdminRepairConsistency
  - testAdminAdjustPositive
  - testAdminAdjustNegative
- PointsFIFOConsistencyTest: 2/2 PASS
  - testFIFODeductionAcrossBatches
  - testFIFODeductionSingleBatch
- OrderFlowBaselineTest: 1/1 PASS
  - testCreateAndCompleteOrder_bogoCoupon_baseline

## Phase 0.5 (added unexpectedly)

Test infra was broken. Three fixes (commit dc8a0e5):
1. Parent pom: lock maven-surefire-plugin 3.2.5 (was 2.12.4, no JUnit 5)
2. .gitignore: exempt src/test/resources/ and src/main/resources/ from `resources/` rule
3. PointsConsistencyTest: fix stale assertion (v5.3 changed createMember to 0 bonus)

## Commits on branch (oldest -> newest)

- e2e4bc7 docs(plan): add Phase 0 test baseline plan + SDD ledger
- dc8a0e5 test(infra): fix broken test infrastructure (Phase 0.5)
- c7dc95b test(order): add OrderFlowBaselineTest happy-path baseline

## CHANGELOG entry

See C:\Users\dell\Desktop\surx-note\CozyCoffee\CHANGELOG.md (2026-07-09 section)

## Next: Phase 1 DB Critical Fixes

7 DDL migrations per Audit-Spec section 5.3:
1.1 drop FOREIGN KEYs (DC3)
1.2 message_outbox uk_aggregate_type (DC5)
1.3 points_lot_consumptions uk_consume_lot (DC6)
1.4 drop redundant indexes on shop_orders (DH2)
1.5 user_coupons uk_user_used_order (DH5)
1.6 shop_orders.status -> ENUM + fix DEFAULT (DC7)
1.7 member_info charset fix + monthly_spent_month -> DATE (DH3)
