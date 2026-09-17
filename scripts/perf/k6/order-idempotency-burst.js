import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://cozy-perf-gateway:8080';
const token = __ENV.TOKEN;
const previewToken = __ENV.PREVIEW_TOKEN;
const idempotencyKey = __ENV.IDEMPOTENCY_KEY;
const vus = Number(__ENV.VUS || 50);
const businessErrors = new Rate('business_errors');
const nonReplayOrders = new Counter('non_replay_orders');

export const options = {
  scenarios: {
    idempotency_burst: { executor: 'per-vu-iterations', vus, iterations: 1, maxDuration: '30s' },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max', 'count'],
  thresholds: {
    http_req_failed: ['rate<0.001'],
    business_errors: ['rate<0.001'],
    non_replay_orders: ['count==1'],
  },
};

const item = {
  productId: 36,
  quantity: 1,
  cupSize: 'MEDIUM',
  sugarLevel: 'STANDARD',
  temperature: 'HOT',
  addonsJson: '[]',
};
const body = JSON.stringify({
  items: [item], storeId: 1, diningMethod: 'TAKEOUT', previewToken,
});

export default function () {
  const response = http.post(`${baseUrl}/api/order/create`, body, {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey,
    },
    tags: { name: 'POST /api/order/create (same key)' },
    timeout: '10s',
  });
  let result;
  try { result = response.json(); } catch (_) { result = null; }
  const ok = check(response, {
    'HTTP 200': (r) => r.status === 200,
    'business success': () => result && result.success === true,
    'order id present': () => Number(result?.data?.id) > 0,
  });
  if (ok && result.data.idempotentReplay !== true) nonReplayOrders.add(1);
  businessErrors.add(!ok);
}

export function handleSummary(data) {
  return { '/results/summary.json': JSON.stringify(data, null, 2) };
}
