import os
from collections import deque
from datetime import datetime

from locust import HttpUser, between, task


SCENARIO = os.getenv("COZY_SCENARIO", "hot_read").strip().lower()
USER_TOKEN = os.getenv("COZY_USER_TOKEN") or os.getenv("COZY_TOKEN", "")
ADMIN_TOKEN = os.getenv("COZY_ADMIN_TOKEN", "")
ORDER_PRODUCT_ID = int(os.getenv("COZY_ORDER_PRODUCT_ID", "1"))
MALL_PRODUCT_ID = int(os.getenv("COZY_MALL_PRODUCT_ID", "1"))
MAX_TRACKED_ORDER_IDS = int(os.getenv("COZY_MAX_TRACKED_ORDER_IDS", "200"))


class BaseCozyUser(HttpUser):
    abstract = True
    wait_time = between(0.2, 1.0)

    def on_start(self):
        self.user_headers = {}
        self.admin_headers = {}
        if USER_TOKEN:
            self.user_headers["Authorization"] = f"Bearer {USER_TOKEN}"
        if ADMIN_TOKEN:
            self.admin_headers["Authorization"] = f"Bearer {ADMIN_TOKEN}"

        self.order_ids = deque(maxlen=max(1, MAX_TRACKED_ORDER_IDS))

    def _create_order_payload(self):
        return {
            "items": [
                {
                    "productId": ORDER_PRODUCT_ID,
                    "quantity": 1,
                    "cupSize": "medium",
                    "temperature": "iced",
                }
            ],
            "diningMethod": "TAKEOUT",
            "remark": "locust perf benchmark",
        }


class HotReadUser(BaseCozyUser):
    weight = 1 if SCENARIO == "hot_read" else 0

    @task(7)
    def list_coffee_products(self):
        self.client.get(
            "/api/order/products",
            headers=self.user_headers,
            name="GET /api/order/products",
        )

    @task(3)
    def list_points_mall_products(self):
        self.client.get(
            "/api/member/mall/products",
            headers=self.user_headers,
            name="GET /api/member/mall/products",
        )


class SigninStatsUser(BaseCozyUser):
    weight = 1 if SCENARIO == "signin_stats" else 0

    @task(6)
    def signin_month_stats(self):
        month = datetime.now().strftime("%Y%m")
        self.client.get(
            f"/api/member/signin/stats?month={month}",
            headers=self.user_headers,
            name="GET /api/member/signin/stats",
        )

    @task(3)
    def signin_calendar(self):
        month = datetime.now().strftime("%Y%m")
        self.client.get(
            f"/api/member/signin/calendar?month={month}",
            headers=self.user_headers,
            name="GET /api/member/signin/calendar",
        )

    @task(1)
    def signin_try(self):
        self.client.post(
            "/api/member/signin",
            headers=self.user_headers,
            name="POST /api/member/signin",
        )


class AdminCacheUser(BaseCozyUser):
    weight = 1 if SCENARIO == "admin_cache" else 0

    @task(6)
    def admin_recent_orders(self):
        self.client.get(
            "/api/admin/orders/recent?limit=20",
            headers=self.admin_headers,
            name="GET /api/admin/orders/recent",
        )

    @task(3)
    def admin_orders_list(self):
        self.client.get(
            "/api/admin/orders",
            headers=self.admin_headers,
            name="GET /api/admin/orders",
        )

    @task(1)
    def create_order_to_invalidate_cache(self):
        with self.client.post(
            "/api/order/create",
            json=self._create_order_payload(),
            headers=self.user_headers,
            name="POST /api/order/create (invalidate admin cache)",
            catch_response=True,
        ) as response:
            if response.status_code >= 400:
                response.failure(f"status={response.status_code}")
                return
            try:
                data = response.json().get("data") or {}
                order_id = data.get("id")
                if order_id is not None:
                    self.order_ids.append(order_id)
            except Exception:
                pass
            response.success()


class TimeoutCancelUser(BaseCozyUser):
    weight = 1 if SCENARIO == "timeout_cancel" else 0

    @task(4)
    def create_pending_order(self):
        with self.client.post(
            "/api/order/create",
            json=self._create_order_payload(),
            headers=self.user_headers,
            name="POST /api/order/create",
            catch_response=True,
        ) as response:
            if response.status_code >= 400:
                response.failure(f"status={response.status_code}")
                return
            try:
                data = response.json().get("data") or {}
                order_id = data.get("id")
                if order_id is not None:
                    self.order_ids.append(order_id)
            except Exception:
                pass
            response.success()

    @task(3)
    def admin_recent_orders(self):
        self.client.get(
            "/api/admin/orders/recent?limit=20",
            headers=self.admin_headers,
            name="GET /api/admin/orders/recent",
        )

    @task(2)
    def poll_order_detail(self):
        if not self.order_ids:
            return
        order_id = self.order_ids[-1]
        self.client.get(
            f"/api/admin/orders/{order_id}",
            headers=self.admin_headers,
            name="GET /api/admin/orders/{id}",
        )

    @task(1)
    def admin_order_counts(self):
        self.client.get(
            "/api/admin/orders/counts",
            headers=self.admin_headers,
            name="GET /api/admin/orders/counts",
        )


if SCENARIO not in {"hot_read", "signin_stats", "admin_cache", "timeout_cancel"}:
    raise RuntimeError(
        "Unsupported COZY_SCENARIO. Use one of: hot_read, signin_stats, admin_cache, timeout_cancel"
    )
