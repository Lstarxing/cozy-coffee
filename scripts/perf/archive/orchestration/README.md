# CozyCoffee reproducible performance benchmark

This benchmark isolates application data in `cozy_perf_*` MySQL schemas and Redis DB 15.
It starts dedicated Nacos, RocketMQ, Gateway, Order, User, and Member containers. Only the
existing MySQL and Redis engines are shared; schemas/databases are isolated from development data.

## Compared modes

- `DB_ONLY`: always query MySQL.
- `REDIS_ONLY`: skip the process-local cache, use Redis with DB fallback.
- `L1_L2`: production L1 + Redis + DB behavior.

## Main scenarios

- Constant-arrival-rate steady test for `/api/order/products`.
- Cold and verified-warm 200-VU bursts for `/api/order/products`.
- Constant-arrival-rate cart preview -> order creation workflow, including RocketMQ publication.
- Same-key 50-VU order creation burst to verify idempotency and database uniqueness.

All runs save k6 summary JSON, MySQL/Redis snapshots, container samples, application logs, and metadata.

`MenuCacheService` has a test-only configuration switch whose default remains `L1_L2`. It makes
the DB-only, Redis-only, and production-cache paths comparable without changing response semantics.
