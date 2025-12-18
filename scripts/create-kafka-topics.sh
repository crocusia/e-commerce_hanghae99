#!/bin/bash

# Kafka 토픽 생성 스크립트

echo "=== Kafka 토픽 생성 시작 ==="

# Kafka 컨테이너가 준비될 때까지 대기
echo "Kafka 컨테이너 준비 대기 중..."
sleep 10

# 1. 쿠폰 발급 요청 토픽 생성
echo "1. coupon-issue-requests 토픽 생성 중..."
docker exec kafka-container kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic coupon-issue-requests \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

# 2. DLQ 토픽 생성
echo "2. coupon-issue-dlq 토픽 생성 중..."
docker exec kafka-container kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic coupon-issue-dlq \
  --partitions 1 \
  --replication-factor 1 \
  --if-not-exists

# 3. 주문 이벤트 토픽 (이미 있을 수 있음)
echo "3. order-created-events 토픽 생성 중..."
docker exec kafka-container kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic order-created-events \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

# 토픽 목록 확인
echo ""
echo "=== 생성된 토픽 목록 ==="
docker exec kafka-container kafka-topics --list \
  --bootstrap-server localhost:9092

echo ""
echo "=== 토픽 상세 정보 ==="
docker exec kafka-container kafka-topics --describe \
  --bootstrap-server localhost:9092 \
  --topic coupon-issue-requests

echo ""
echo "=== Kafka 토픽 생성 완료 ==="
