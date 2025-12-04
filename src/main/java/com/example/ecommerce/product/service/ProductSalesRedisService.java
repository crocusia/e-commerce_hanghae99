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
    private static final String RANK_KEY = "product:rank";
    private static final String RANKING_KEY_PREFIX = "product:ranking:";
    private static final String SNAPSHOT_KEY = "product:ranking:3days:snapshot";
    private static final int RANKING_DAYS = 3;
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

    public Map<Long, Long> getTopNProducts(int limit) {
        try {
            Set<ZSetOperations.TypedTuple<String>> topProducts = redisTemplate.opsForZSet()
                .reverseRangeWithScores(SALES_KEY, 0, limit - 1);

            if (topProducts == null || topProducts.isEmpty()) {
                log.debug("Redis에서 인기 상품 조회 결과 없음");
                return new LinkedHashMap<>();
            }

            Map<Long, Long> result = new LinkedHashMap<>();
            for (ZSetOperations.TypedTuple<String> tuple : topProducts) {
                Long productId = Long.parseLong(tuple.getValue());
                Long salesCount = tuple.getScore().longValue();
                result.put(productId, salesCount);
            }

            log.debug("Top {} 인기 상품 조회 완료 - count: {}", limit, result.size());
            return result;

        } catch (Exception e) {
            log.error("Redis에서 인기 상품 조회 실패 - limit: {}", limit, e);
            return new LinkedHashMap<>();
        }
    }

    public long getSalesCount(Long productId) {
        try {
            Double score = redisTemplate.opsForZSet().score(SALES_KEY, productId.toString());
            return score != null ? score.longValue() : 0L;

        } catch (Exception e) {
            log.error("Redis 판매 수 조회 실패 - productId: {}", productId, e);
            return 0L;
        }
    }

    public int getRank(Long productId) {
        try {
            Long rank = redisTemplate.opsForZSet()
                .reverseRank(SALES_KEY, productId.toString());

            // rank는 0-based이므로 1을 더함
            return rank != null ? rank.intValue() + 1 : -1;

        } catch (Exception e) {
            log.error("Redis 순위 조회 실패 - productId: {}", productId, e);
            return -1;
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

    public void resetSales(Long productId) {
        try {
            redisTemplate.opsForZSet().remove(SALES_KEY, productId.toString());
            log.debug("상품 판매 수 초기화 - productId: {}", productId);

        } catch (Exception e) {
            log.error("Redis 판매 수 초기화 실패 - productId: {}", productId, e);
        }
    }

    public long resetAllSales() {
        try {
            Long removed = redisTemplate.delete(SALES_KEY) ?
                redisTemplate.opsForZSet().size(SALES_KEY) : 0L;

            log.info("전체 판매 수 데이터 초기화 완료 - 삭제된 항목: {}", removed);
            return removed != null ? removed : 0L;

        } catch (Exception e) {
            log.error("Redis 전체 판매 수 초기화 실패", e);
            return 0L;
        }
    }

    /**
     * Redis에서 DB로 판매 수 동기화
     *
     * @param salesMap DB에서 집계한 판매 수
     */
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

    public Map<Long, Long> getTop3DaysProducts(int limit) {
        String tempKey = null;
        try {
            List<String> keys = getRecentThreeDaysKeys();
            tempKey = "product:ranking:3days:" + System.currentTimeMillis();

            // 3일 간 판매량 합산 (ZUNIONSTORE)
            Long unionSize = redisTemplate.opsForZSet()
                .unionAndStore(keys.get(0), keys, tempKey);

            if (unionSize == null || unionSize == 0) {
                log.debug("최근 3일 판매 데이터 없음");
                return new LinkedHashMap<>();
            }

            // Top N 조회 (내림차순)
            Set<ZSetOperations.TypedTuple<String>> topProducts = redisTemplate.opsForZSet()
                .reverseRangeWithScores(tempKey, 0, limit - 1);

            if (topProducts == null || topProducts.isEmpty()) {
                return new LinkedHashMap<>();
            }

            Map<Long, Long> result = new LinkedHashMap<>();
            for (ZSetOperations.TypedTuple<String> tuple : topProducts) {
                Long productId = Long.parseLong(tuple.getValue());
                Long salesCount = tuple.getScore().longValue();
                result.put(productId, salesCount);
            }

            log.debug("최근 3일 Top {} 상품 조회 완료 - count: {}", limit, result.size());
            return result;

        } catch (Exception e) {
            log.error("최근 3일 Top N 상품 조회 실패 - limit: {}", limit, e);
            return new LinkedHashMap<>();
        } finally {
            // 임시 키 삭제
            if (tempKey != null) {
                redisTemplate.delete(tempKey);
            }
        }
    }

    public Integer getProduct3DaysRank(Long productId) {
        String tempKey = null;
        try {
            List<String> keys = getRecentThreeDaysKeys();
            tempKey = "product:ranking:3days:" + System.currentTimeMillis();

            // 3일 간 판매량 합산
            redisTemplate.opsForZSet().unionAndStore(keys.get(0), keys, tempKey);

            Long rank = redisTemplate.opsForZSet()
                .reverseRank(tempKey, productId.toString());

            return rank != null ? rank.intValue() + 1 : null;

        } catch (Exception e) {
            log.error("최근 3일 순위 조회 실패 - productId: {}", productId, e);
            return null;
        } finally {
            if (tempKey != null) {
                redisTemplate.delete(tempKey);
            }
        }
    }

    public long getProduct3DaysSalesCount(Long productId) {
        try {
            List<String> keys = getRecentThreeDaysKeys();
            long totalSales = 0;

            for (String key : keys) {
                Double score = redisTemplate.opsForZSet().score(key, productId.toString());
                if (score != null) {
                    totalSales += score.longValue();
                }
            }

            return totalSales;

        } catch (Exception e) {
            log.error("최근 3일 판매량 조회 실패 - productId: {}", productId, e);
            return 0L;
        }
    }

    public long getSalesCountByDate(Long productId, LocalDate date) {
        try {
            String key = getRankingKey(date);
            Double score = redisTemplate.opsForZSet().score(key, productId.toString());
            return score != null ? score.longValue() : 0L;

        } catch (Exception e) {
            log.error("날짜별 판매량 조회 실패 - date: {}, productId: {}", date, productId, e);
            return 0L;
        }
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

    /**
     * 3일 통합 랭킹 스냅샷 생성
     * 매일 새벽 3시에 호출되어 하루 1회 갱신
     *
     * @return 생성 성공 여부
     */
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

    /**
     * 스냅샷에서 Top N 조회
     * 스냅샷 없으면 빈 리스트 반환 (실시간 계산 하지 않음)
     *
     * @param limit 조회 개수
     * @return 상품 ID와 판매량 Map (순위 순), 스냅샷 없으면 빈 Map
     */
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

    /**
     * 스냅샷에서 특정 상품 순위 조회
     * 스냅샷 없으면 null 반환 (실시간 계산 하지 않음)
     *
     * @param productId 상품 ID
     * @return 순위 (1-based), 스냅샷 없거나 랭킹에 없으면 null
     */
    public Integer getProductRankFromSnapshot(Long productId) {
        try {
            Boolean hasSnapshot = redisTemplate.hasKey(SNAPSHOT_KEY);

            if (Boolean.TRUE.equals(hasSnapshot)) {
                Long rank = redisTemplate.opsForZSet()
                    .reverseRank(SNAPSHOT_KEY, productId.toString());

                if (rank != null) {
                    return rank.intValue() + 1;
                }
            }

            // 스냅샷 없으면 null 반환
            log.debug("스냅샷 없음 - null 반환");
            return null;

        } catch (Exception e) {
            log.error("스냅샷에서 순위 조회 실패", e);
            return null;
        }
    }

    public boolean hasSnapshot() {
        try {
            Boolean exists = redisTemplate.hasKey(SNAPSHOT_KEY);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("스냅샷 존재 확인 실패", e);
            return false;
        }
    }

    public void deleteSnapshot() {
        try {
            redisTemplate.delete(SNAPSHOT_KEY);
            log.info("스냅샷 수동 삭제 완료");
        } catch (Exception e) {
            log.error("스냅샷 삭제 실패", e);
        }
    }
}
