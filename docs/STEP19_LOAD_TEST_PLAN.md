# STEP 19: 부하 테스트 계획서

## 1. 테스트 개요

### 1.1 테스트 목적
선착순 쿠폰 발급 시스템의 성능 한계를 파악하고, 대량 트래픽 발생 시 시스템의 안정성을 검증합니다.

### 1.2 테스트 대상
- **API 엔드포인트**: `POST /api/user-coupons/issue-async`
- **핵심 기능**: Redis 기반 선착순 쿠폰 발급 (수량 제어, 중복 방지)
- **관련 인프라**: Redis, Kafka, MySQL

### 1.3 현재 시스템 아키텍처
```
Client Request
    ↓
[Controller] UserCouponController
    ↓
[Facade] CouponIssueFacade
    ↓
├─ Redis: 수량 예약 (INCR)
├─ Redis: 중복 체크 (SADD)
├─ Kafka: 비동기 이벤트 발행
└─ Redis: 상태 저장 (SET)
    ↓
[Consumer] CouponIssueConsumer
    ↓
MySQL: 실제 발급 처리
```

## 2. 테스트 시나리오

### 2.1 시나리오 1: 정상 부하 테스트 (Baseline)
**목적**: 일반적인 트래픽에서 시스템 성능 기준선 확인

- **VU (Virtual Users)**: 100명
- **Duration**: 1분
- **Ramp-up**: 10초
- **Request Rate**: 약 100 RPS

**기대 결과**:
- 응답 시간: p95 < 500ms
- 성공률: > 99%
- CPU/Memory: 안정적

### 2.2 시나리오 2: 피크 부하 테스트 (Spike Test)
**목적**: 순간적인 트래픽 폭증 시 시스템 동작 검증

- **VU (Virtual Users)**: 10 → 500 → 10
- **Duration**: 2분
- **Spike Duration**: 30초
- **Request Rate**: 최대 500 RPS

**기대 결과**:
- 응답 시간: p95 < 1000ms
- 성공률: > 95%
- 시스템 복구: 30초 이내

## 3. 성능 지표 (KPI)

### 3.1 응답 시간 (Latency)
- **p50 (Median)**: < 200ms
- **p95**: < 500ms
- **p99**: < 1000ms

### 3.2 처리율 (Throughput)
- **목표 RPS**: 200 RPS
- **최대 RPS**: 500 RPS

### 3.3 에러율 (Error Rate)
- **목표**: < 1%
- **허용**: < 5%

### 3.4 시스템 리소스
- **CPU**: < 70%
- **Memory**: < 80%
- **Redis Connection**: < 80%

## 4. 테스트 환경

### 4.1 인프라
- **Application**: Spring Boot (로컬 실행)
- **Redis**: Docker (redis:7-alpine)
- **Kafka**: Docker (confluentinc/cp-kafka:7.5.0)
- **MySQL**: Docker (mysql:8.0)
- **K6**: Docker (grafana/k6:latest)

### 4.2 테스트 데이터
- **쿠폰 ID**: 고정 값 (예: 1)
- **사용자 ID**: 랜덤 생성 (1 ~ 10000)
- **쿠폰 수량**: 시나리오별 설정

## 5. 테스트 도구

### 5.1 K6 (Load Testing)
- 오픈소스 부하 테스트 도구
- JavaScript로 시나리오 작성
- 실시간 메트릭 수집
- Docker 환경에서 실행

### 5.2 모니터링
- **K6 내장 메트릭**: http_req_duration, http_reqs, http_req_failed
- **애플리케이션 로그**: 발급 성공/실패, 에러 로그
- **Redis 모니터링**: redis-cli INFO stats
- **Kafka UI**: 메시지 처리 현황


## 6. 테스트 실행 계획

### 6.1 사전 준비
1. Docker 인프라 실행 (`docker-compose up -d`)
2. 애플리케이션 실행
3. 테스트용 쿠폰 데이터 생성
4. Redis, Kafka 정상 동작 확인

### 6.2 테스트 순서
1. Baseline 테스트 (시나리오 1)
2. Spike 테스트 (시나리오 2)

### 6.3 각 테스트 간 대기 시간
- 시스템 안정화를 위해 테스트 간 1분 대기
- Redis 데이터 초기화 (테스트 독립성 보장)

## 7. 결과 분석 기준

### 7.1 성공 기준
- Baseline: 에러율 < 1%, p95 < 500ms
- Spike: 에러율 < 5%, p95 < 1000ms
- 시스템 리소스 < 80%

### 7.2 개선 필요 기준
- 에러율 > 5%
- p95 응답 시간 > 1000ms
- 시스템 리소스 > 80%

### 7.3 분석 항목
1. 응답 시간 분포 (p50, p95, p99)
2. 에러 발생 패턴 및 원인
3. 처리율 추이 (RPS)
4. 시스템 리소스 사용률 (CPU, Memory)
5. Redis Connection Pool 사용률
6. Kafka Producer 전송 지연
