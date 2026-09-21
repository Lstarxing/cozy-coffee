import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://cozy-perf-gateway:8080';
const token = __ENV.TOKEN;
const rate = Number(__ENV.RATE || 5);
const duration = __ENV.DURATION || '60s';
const businessErrors = new Rate('business_errors');
const cartCheckDuration = new Trend('cart_check_duration', true);
const orderCreateDuration = new Trend('order_create_duration', true);

export const options = {
  scenarios: {
    order_workflow: {
      executor: 'constant-arrival-rate',
      rate,
      timeUnit: '1s',
      duration,
      preAllocatedVUs: 20,
      maxVUs: 100,
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max', 'count'],
  thresholds: {
    http_req_failed: ['rate<0.001'],
    business_errors: ['rate<0.001'],
    dropped_iterations: ['count==0'],
  },
};

const headers = {
  Authorization: `Bearer ${token}`,
  'Content-Type': 'application/json',
};
const item = {
  productId: 36,
  quantity: 1,
  cupSize: 'MEDIUM',
  sugarLevel: 'STANDARD',
  temperature: 'HOT',
  addonsJson: '[]',
};
const cartBody = JSON.stringify({ items: [item], storeId: 1, diningMethod: 'TAKEOUT' });

export default function () {
  const checkResponse = http.post(`${baseUrl}/api/order/cart/check`, cartBody, {
    headers,
    tags: { name: 'POST /api/order/cart/check' },
    timeout: '10s',
  });
  cartCheckDuration.add(checkResponse.timings.duration);

  let checkBody;
  try { checkBody = checkResponse.json(); } catch (_) { checkBody = null; }
  const previewOk = check(checkResponse, {
    'cart check HTTP 200': (r) => r.status === 200,
    'cart check success': () => checkBody && checkBody.success === true,
    'preview token present': () => Boolean(checkBody?.data?.preview?.previewToken),
    'no invalid items': () => Array.isArray(checkBody?.data?.invalidItems) && checkBody.data.invalidItems.length === 0,
  });
  if (!previewOk) {
    businessErrors.add(true);
    return;
  }

  const idempotencyKey = `perf-${__VU}-${__ITER}-${Date.now()}`;
  const createBody = JSON.stringify({
    items: [item],
    storeId: 1,
    diningMethod: 'TAKEOUT',
    previewToken: checkBody.data.preview.previewToken,
  });
  const createResponse = http.post(`${baseUrl}/api/order/create`, createBody, {
    headers: { ...headers, 'Idempotency-Key': idempotencyKey },
    tags: { name: 'POST /api/order/create' },
    timeout: '10s',
  });
  orderCreateDuration.add(createResponse.timings.duration);

  let createResult;
  try { createResult = createResponse.json(); } catch (_) { createResult = null; }
  const createOk = check(createResponse, {
    'create HTTP 200': (r) => r.status === 200,
    'create success': () => createResult && createResult.success === true,
    'order id present': () => Number(createResult?.data?.id) > 0,
  });
  businessErrors.add(!createOk);
}

export function handleSummary(data) {
  return { '/results/summary.json': JSON.stringify(data, null, 2) };
}
