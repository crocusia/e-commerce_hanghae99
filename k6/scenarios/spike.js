import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');

// Test configuration - Spike Test
export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '10s', target: 10 },   // Start with 10 users
        { duration: '30s', target: 500 },  // Spike to 500 users
        { duration: '30s', target: 500 },  // Hold at 500 users
        { duration: '30s', target: 10 },   // Drop back to 10 users
        { duration: '20s', target: 10 },   // Recovery period
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1000'],  // 95% of requests should be below 1000ms
    http_req_failed: ['rate<0.05'],     // Error rate should be less than 5%
    errors: ['rate<0.05'],
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
    tags: { name: 'CouponIssue-Spike' },
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
    'response time < 1000ms': (r) => r.timings.duration < 1000,
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

  // Shorter think time during spike
  sleep(0.5);
}
