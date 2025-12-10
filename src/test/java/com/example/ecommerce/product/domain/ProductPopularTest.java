package com.example.ecommerce.product.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("ProductPopular 도메인 테스트")
class ProductPopularTest {

    // 헬퍼 메서드
    private ProductPopular create(Long productId, long salesCount, int rank) {
        return ProductPopular.builder()
            .productId(productId)
            .salesCount(salesCount)
            .rank(rank)
            .lastAggregatedAt(LocalDateTime.now())
            .build();
    }

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("정상적으로 인기 상품 정보를 생성한다")
        void createProductPopular() {
            // given
            Long productId = 100L;
            long salesCount = 1000L;
            int rank = 1;

            // when
            ProductPopular productPopular = create(productId, salesCount, rank);

            // then
            assertAll(
                () -> assertThat(productPopular.getProductId()).isEqualTo(productId),
                () -> assertThat(productPopular.getSalesCount()).isEqualTo(salesCount),
                () -> assertThat(productPopular.getRank()).isEqualTo(rank),
                () -> assertThat(productPopular.getLastAggregatedAt()).isNotNull()
            );
        }

        @Test
        @DisplayName("생성 시 마지막 집계 시간이 설정된다")
        void lastAggregatedAtIsSetOnCreation() {
            // given
            LocalDateTime beforeCreation = LocalDateTime.now();

            // when
            ProductPopular productPopular = create(100L, 1000L, 1);

            // then
            assertThat(productPopular.getLastAggregatedAt())
                .isAfterOrEqualTo(beforeCreation)
                .isBeforeOrEqualTo(LocalDateTime.now());
        }
    }

    @Nested
    @DisplayName("상위 N개 판단 테스트")
    class IsTopNTest {

        @ParameterizedTest
        @CsvSource({
            "1, 5, true",
            "3, 5, true",
            "5, 5, true",
            "6, 5, false",
            "10, 5, false",
            "1, 10, true",
            "10, 10, true",
            "11, 10, false"
        })
        @DisplayName("순위가 상위 N개 이내인지 판단한다")
        void isTopN(int rank, int topN, boolean expected) {
            // given
            ProductPopular productPopular = create(100L, 1000L, rank);

            // when
            boolean result = productPopular.isTopN(topN);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 3, 5})
        @DisplayName("상위 5개 내의 순위는 true를 반환한다")
        void isTop5(int rank) {
            // given
            ProductPopular productPopular = create(100L, 1000L, rank);

            // when
            boolean result = productPopular.isTopN(5);

            // then
            assertThat(result).isTrue();
        }

        @ParameterizedTest
        @ValueSource(ints = {6, 10, 100})
        @DisplayName("상위 5개 밖의 순위는 false를 반환한다")
        void isNotTop5(int rank) {
            // given
            ProductPopular productPopular = create(100L, 1000L, rank);

            // when
            boolean result = productPopular.isTopN(5);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("순위가 0이면 상위 N개가 아니다")
        void rankZeroIsNotTopN() {
            // given
            ProductPopular productPopular = create(100L, 1000L, 0);

            // when
            boolean result = productPopular.isTopN(5);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("순위가 음수면 상위 N개가 아니다")
        void negativeRankIsNotTopN() {
            // given
            ProductPopular productPopular = create(100L, 1000L, -1);

            // when
            boolean result = productPopular.isTopN(5);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("집계 시간 만료 여부 테스트")
    class IsStaleTest {
        @Test
        @DisplayName("임계값보다 이전 시간이면 만료된 것으로 판단한다")
        void isStaleWhenOlderThanThreshold() {
            // given
            ProductPopular productPopular = create(100L, 1000L, 1);
            Duration threshold = Duration.ofMillis(1);

            // when
            try {
                Thread.sleep(10); // 충분한 시간 경과
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            boolean result = productPopular.isStale(threshold);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("임계값보다 최근 시간이면 만료되지 않은 것으로 판단한다")
        void isNotStaleWhenNewerThanThreshold() {
            // given
            ProductPopular productPopular = create(100L, 1000L, 1);
            Duration threshold = Duration.ofHours(24);

            // when
            boolean result = productPopular.isStale(threshold);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("판매 수량 증가 테스트")
    class IncreaseSalesCountTest {

        @ParameterizedTest
        @CsvSource({
            "1000, 100, 1100",
            "0, 500, 500",
            "5000, 2500, 7500"
        })
        @DisplayName("판매 수량을 정상적으로 증가시킨다")
        void increaseSalesCount(long initialCount, long increaseAmount, long expectedCount) {
            // given
            ProductPopular productPopular = create(100L, initialCount, 1);

            // when
            productPopular.increaseSalesCount(increaseAmount);

            // then
            assertThat(productPopular.getSalesCount()).isEqualTo(expectedCount);
        }

        @Test
        @DisplayName("판매 수량을 0으로 증가시킬 수 있다")
        void increaseSalesCountByZero() {
            // given
            long initialCount = 1000L;
            ProductPopular productPopular = create(100L, initialCount, 1);

            // when
            productPopular.increaseSalesCount(0);

            // then
            assertThat(productPopular.getSalesCount()).isEqualTo(initialCount);
        }
    }
}
