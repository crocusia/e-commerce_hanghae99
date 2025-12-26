import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');

// Test configuration
export const options = {
  scenarios: {
    baseline: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 100 },  // Ramp-up to 100 users
        { duration: '1m', target: 100 },   // Stay at 100 users
        { duration: '10s', target: 0 },    // Ramp-down to 0
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95% of requests should be below 500ms
    http_req_failed: ['rate<0.01'],    // Error rate should be less than 1%
    errors: ['rate<0.01'],
  },
};

// Base URL (change if needed)
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

export default function () {
  // Generate random couponId (1-5) and userId (1-10000)
  const couponId = Math.floor(Math.random() * 5) + 1;
  const userId = Math.floor(Math.random() * 10000) + 1;

  const payload = JSON.stringify({
    couponId: couponId,
    userId: userId,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
    tags: { name: 'CouponIssue' },
  };

  // Issue coupon request
  const response = http.post(
    `${BASE_URL}/api/user-coupons/issue-async`,
    payload,
    params
  );

  // Check response
  const result = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
    'has success field': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.hasOwnProperty('success');
      } catch (e) {
        return false;
      }
    },
  });

  // Record errors
  errorRate.add(!result);

  // Think time
  sleep(1);
}
