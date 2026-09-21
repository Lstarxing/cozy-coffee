import http from 'k6/http';
import { check } from 'k6';
import { Rate } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://cozy-perf-gateway:8080';
const vus = Number(__ENV.VUS || 200);
const businessErrors = new Rate('business_errors');

export const options = {
  scenarios: {
    cold_burst: {
      executor: 'per-vu-iterations',
      vus,
      iterations: 1,
      maxDuration: '30s',
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max', 'count'],
  thresholds: {
    http_req_failed: ['rate<0.001'],
    business_errors: ['rate<0.001'],
  },
};

export default function () {
  const response = http.get(`${baseUrl}/api/order/products`, {
    tags: { name: 'GET /api/order/products (cold burst)' },
    timeout: '10s',
  });
  let body;
  try {
    body = response.json();
  } catch (_) {
    body = null;
  }
  const ok = check(response, {
    'HTTP 200': (r) => r.status === 200,
    'business success': () => body && body.success === true && body.code === 200,
    'menu is non-empty': () => body && Array.isArray(body.data) && body.data.length > 0,
  });
  businessErrors.add(!ok);
}

export function handleSummary(data) {
  return {
    '/results/summary.json': JSON.stringify(data, null, 2),
  };
}
