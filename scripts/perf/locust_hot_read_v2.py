import os
from locust import HttpUser, task, between

TOKEN = os.getenv("COZY_TOKEN", "")


class CozyCoffeeHotReadUser(HttpUser):
    wait_time = between(0.2, 1.0)

    def on_start(self):
        self.common_headers = {}
        if TOKEN:
            self.common_headers["Authorization"] = f"Bearer {TOKEN}"

    @task(7)
    def list_coffee_products(self):
        self.client.get(
            "/api/order/products",
            headers=self.common_headers,
            name="GET /api/order/products",
        )

    @task(3)
    def list_points_mall_products(self):
        self.client.get(
            "/api/member/mall/products",
            headers=self.common_headers,
            name="GET /api/member/mall/products",
        )
