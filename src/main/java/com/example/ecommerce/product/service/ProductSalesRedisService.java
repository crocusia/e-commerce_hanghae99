package com.example.ecommerce.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSalesRedisService {

    private static final String SALES_KEY = "product:sales";
    private static final String RANKING_KEY_PREFIX = "product:ranking:";
    private static final String SNAPSHOT_KEY = "product:ranking:3days:snapshot";
    private static final int RANKING_TTL_DAYS = 4;
    private static final int SNAPSHOT_TTL_HOURS = 25; // 스냅샷 TTL (24시간 + 1시간 버퍼)

    private final RedisTemplate<String, String> redisTemplate;

    public void incrementSales(Long productId, int quantity) {
        try {
            Double newScore = redisTemplate.opsForZSet()
                .incrementScore(SALES_KEY, productId.toString(), quantity);

            log.debug("상품 판매 수 증가 - productId: {}, quantity: {}, totalSales: {}",
                productId, quantity, newScore);

        } catch (Exception e) {
            log.error("Redis 판매 수 증가 실패 - productId: {}, quantity: {}",
                productId, quantity, e);
        }
    }

    public Map<Long, Long> getAllSales() {
        try {
            Set<ZSetOperations.TypedTuple<String>> allProducts = redisTemplate.opsForZSet()
                .reverseRangeWithScores(SALES_KEY, 0, -1); // -1 = 전체 조회

            if (allProducts == null || allProducts.isEmpty()) {
                return new LinkedHashMap<>();
            }

            Map<Long, Long> result = new LinkedHashMap<>();
            for (ZSetOperations.TypedTuple<String> tuple : allProducts) {
                Long productId = Long.parseLong(tuple.getValue());
                Long salesCount = tuple.getScore().longValue();
                result.put(productId, salesCount);
            }

            log.debug("전체 판매 수 조회 완료 - 상품 수: {}", result.size());
            return result;

        } catch (Exception e) {
            log.error("Redis 전체 판매 수 조회 실패", e);
            return new LinkedHashMap<>();
        }
    }

    public void syncFromDB(Map<Long, Long> salesMap) {
        if (salesMap.isEmpty()) {
            log.info("동기화할 판매 데이터 없음");
            return;
        }

        try {
            // 기존 데이터 삭제
            redisTemplate.delete(SALES_KEY);

            // 새로운 데이터 추가
            for (Map.Entry<Long, Long> entry : salesMap.entrySet()) {
                redisTemplate.opsForZSet()
                    .add(SALES_KEY, entry.getKey().toString(), entry.getValue());
            }

            log.info("DB → Redis 판매 수 동기화 완료 - 상품 수: {}", salesMap.size());

        } catch (Exception e) {
            log.error("DB → Redis 동기화 실패", e);
        }
    }

    public void incrementSalesByDate(Long productId, int quantity, LocalDate date) {
        try {
            String key = getRankingKey(date);
            Double newScore = redisTemplate.opsForZSet()
                .incrementScore(key, productId.toString(), quantity);

            // TTL 설정 (4일)
            redisTemplate.expire(key, Duration.ofDays(RANKING_TTL_DAYS));

            log.debug("날짜별 상품 판매량 증가 - date: {}, productId: {}, quantity: {}, totalSales: {}",
                date, productId, quantity, newScore);

        } catch (Exception e) {
            log.error("날짜별 판매량 증가 실패 - date: {}, productId: {}, quantity: {}",
                date, productId, quantity, e);
        }
    }

    public void incrementTodaySales(Long productId, int quantity) {
        incrementSalesByDate(productId, quantity, LocalDate.now());
    }

    private List<String> getRecentThreeDaysKeys() {
        LocalDate today = LocalDate.now();
        return List.of(
            getRankingKey(today),
            getRankingKey(today.minusDays(1)),
            getRankingKey(today.minusDays(2))
        );
    }

    private String getRankingKey(LocalDate date) {
        return RANKING_KEY_PREFIX + date.format(DateTimeFormatter.ISO_DATE);
    }

    public boolean createRankingSnapshot() {
        try {
            List<String> keys = getRecentThreeDaysKeys();

            // 3일 간 판매량 합산 후 스냅샷 키에 저장
            Long unionSize = redisTemplate.opsForZSet()
                .unionAndStore(keys.get(0), keys, SNAPSHOT_KEY);

            if (unionSize == null || unionSize == 0) {
                log.warn("스냅샷 생성 실패 - 3일 간 판매 데이터 없음");
                return false;
            }

            // TTL 설정 (25시간 = 24시간 + 1시간 버퍼)
            redisTemplate.expire(SNAPSHOT_KEY, Duration.ofHours(SNAPSHOT_TTL_HOURS));

            log.info("3일 랭킹 스냅샷 생성 완료 - 상품 수: {}, TTL: {}시간", unionSize, SNAPSHOT_TTL_HOURS);
            return true;

        } catch (Exception e) {
            log.error("스냅샷 생성 실패", e);
            return false;
        }
    }

    public Map<Long, Long> getTopProductsFromSnapshot(int limit) {
        try {
            // 스냅샷 존재 확인
            Boolean hasSnapshot = redisTemplate.hasKey(SNAPSHOT_KEY);

            if (Boolean.TRUE.equals(hasSnapshot)) {
                // 스냅샷에서 조회
                Set<ZSetOperations.TypedTuple<String>> topProducts = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(SNAPSHOT_KEY, 0, limit - 1);

                if (topProducts != null && !topProducts.isEmpty()) {
                    Map<Long, Long> result = new LinkedHashMap<>();
                    for (ZSetOperations.TypedTuple<String> tuple : topProducts) {
                        Long productId = Long.parseLong(tuple.getValue());
                        Long salesCount = tuple.getScore().longValue();
                        result.put(productId, salesCount);
                    }

                    log.debug("스냅샷에서 Top {} 조회 완료 - count: {}", limit, result.size());
                    return result;
                }
            }

            // 스냅샷 없으면 빈 리스트 반환
            log.warn("스냅샷 없음 - 빈 리스트 반환 (매일 새벽 3시 갱신 대기)");
            return new LinkedHashMap<>();

        } catch (Exception e) {
            log.error("스냅샷 조회 실패 - 빈 리스트 반환", e);
            return new LinkedHashMap<>();
        }
    }
}
