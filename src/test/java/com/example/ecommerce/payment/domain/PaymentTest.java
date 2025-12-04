package com.example.ecommerce.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.example.ecommerce.payment.domain.status.PaymentStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Payment 도메인 테스트")
class PaymentTest {

    // 헬퍼 메서드
    private Payment createTestPayment(Long id, Long orderId, Long userId, Long amount) {
        LocalDateTime now = LocalDateTime.now();
        return Payment.builder()
            .id(id)
            .orderId(orderId)
            .userId(userId)
            .amount(amount)
            .status(PaymentStatus.PENDING)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    private void assertPaymentBasicFields(Payment payment, Long expectedOrderId,
        Long expectedUserId, Long expectedAmount) {
        assertAll(
            () -> assertThat(payment.getOrderId()).isEqualTo(expectedOrderId),
            () -> assertThat(payment.getUserId()).isEqualTo(expectedUserId),
            () -> assertThat(payment.getAmount()).isEqualTo(expectedAmount)
        );
    }

    @Nested
    @DisplayName("결제 생성 테스트")
    class CreateTest {

        @ParameterizedTest
        @CsvSource({
            "1, 100, 1000, 50000",
            "2, 200, 2000, 100000",
            "3, 300, 3000, 150000"
        })
        @DisplayName("결제를 정상적으로 생성한다")
        void createPayment_Success(Long id, Long orderId, Long userId, Long amount) {
            // when
            Payment payment = createTestPayment(id, orderId, userId, amount);

            // then
            assertAll(
                () -> assertThat(payment.getId()).isEqualTo(id),
                () -> assertPaymentBasicFields(payment, orderId, userId, amount),
                () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING),
                () -> assertThat(payment.getFailureReason()).isNull(),
                () -> assertThat(payment.getCreatedAt()).isNotNull(),
                () -> assertThat(payment.getCompletedAt()).isNull(),
                () -> assertThat(payment.getUpdatedAt()).isNotNull()
            );
        }

        @Test
        @DisplayName("결제 생성 시 초기 상태는 PENDING이다")
        void createPayment_InitialStatusIsPending() {
            // when
            Payment payment = createTestPayment(1L, 100L, 1000L, 50000L);

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("결제 생성 시 createdAt과 updatedAt이 자동으로 설정된다")
        void createPayment_TimestampsAreSet() {
            // given
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            // when
            Payment payment = createTestPayment(1L, 100L, 1000L, 50000L);

            // then
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);
            assertAll(
                () -> assertThat(payment.getCreatedAt()).isAfter(before),
                () -> assertThat(payment.getCreatedAt()).isBefore(after),
                () -> assertThat(payment.getUpdatedAt()).isAfter(before),
                () -> assertThat(payment.getUpdatedAt()).isBefore(after)
            );
        }
    }

    @Nested
    @DisplayName("결제 완료 테스트")
    class CompleteTest {

        @Test
        @DisplayName("결제를 정상적으로 완료한다")
        void complete_Success() {
            // given
            Payment payment = createTestPayment(1L, 100L, 1000L, 50000L);
            LocalDateTime beforeComplete = LocalDateTime.now().minusSeconds(1);

            // when
            payment.complete();

            // then
            LocalDateTime afterComplete = LocalDateTime.now().plusSeconds(1);
            assertAll(
                () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED),
                () -> assertThat(payment.getCompletedAt()).isNotNull(),
                () -> assertThat(payment.getCompletedAt()).isAfter(beforeComplete),
                () -> assertThat(payment.getCompletedAt()).isBefore(afterComplete),
                () -> assertThat(payment.getUpdatedAt()).isAfter(beforeComplete),
                () -> assertThat(payment.getUpdatedAt()).isBefore(afterComplete)
            );
        }

        @Test
        @DisplayName("이미 완료된 결제를 다시 완료하려고 하면 예외가 발생한다")
        void complete_AlreadyCompleted() {
            // given
            Payment payment = createTestPayment(1L, 100L, 1000L, 50000L);
            payment.complete();

            // when & then
            assertThatThrownBy(payment::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 완료된 결제입니다.");
        }

        @Test
        @DisplayName("결제 완료 시 상태만 변경되고 다른 필드는 유지된다")
        void complete_OnlyStatusChanges() {
            // given
            Payment payment = createTestPayment(1L, 100L, 1000L, 50000L);
            Long originalOrderId = payment.getOrderId();
            Long originalUserId = payment.getUserId();
            Long originalAmount = payment.getAmount();

            // when
            payment.complete();

            // then
            assertPaymentBasicFields(payment, originalOrderId, originalUserId, originalAmount);
        }
    }

    @Nested
    @DisplayName("결제 실패 테스트")
    class FailTest {

        @ParameterizedTest
        @CsvSource({
            "'잔액 부족'",
            "'결제 시스템 오류'",
            "'카드 한도 초과'",
            "'유효하지 않은 결제 정보'"
        })
        @DisplayName("결제를 실패 처리한다")
        void fail_Success(String reason) {
            // given
            Payment payment = createTestPayment(1L, 100L, 1000L, 50000L);
            LocalDateTime beforeFail = LocalDateTime.now().minusSeconds(1);

            // when
            payment.fail(reason);

            // then
            LocalDateTime afterFail = LocalDateTime.now().plusSeconds(1);
            assertAll(
                () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED),
                () -> assertThat(payment.getFailureReason()).isEqualTo(reason),
                () -> assertThat(payment.getUpdatedAt()).isAfter(beforeFail),
                () -> assertThat(payment.getUpdatedAt()).isBefore(afterFail),
                () -> assertThat(payment.getCompletedAt()).isNull()
            );
        }

        @Test
        @DisplayName("실패한 결제를 다시 실패 처리할 수 있다")
        void fail_CanFailAgain() {
            // given
            Payment payment = createTestPayment(1L, 100L, 1000L, 50000L);
            payment.fail("첫 번째 실패");

            // when
            payment.fail("두 번째 실패");

            // then
            assertAll(
                () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED),
                () -> assertThat(payment.getFailureReason()).isEqualTo("두 번째 실패")
            );
        }

        @Test
        @DisplayName("결제 실패 시 상태와 실패 사유만 변경되고 다른 필드는 유지된다")
        void fail_OnlyStatusAndReasonChange() {
            // given
            Payment payment = createTestPayment(1L, 100L, 1000L, 50000L);
            Long originalOrderId = payment.getOrderId();
            Long originalUserId = payment.getUserId();
            Long originalAmount = payment.getAmount();

            // when
            payment.fail("잔액 부족");

            // then
            assertPaymentBasicFields(payment, originalOrderId, originalUserId, originalAmount);
        }
    }

    @Nested
    @DisplayName("결제 상태 전이 테스트")
    class StateTransitionTest {

        @Test
        @DisplayName("PENDING -> COMPLETED 전이가 정상적으로 이루어진다")
        void transition_PendingToCompleted() {
            // given
            Payment payment = createTestPayment(1L, 100L, 1000L, 50000L);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);

            // when
            payment.complete();

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("PENDING -> FAILED 전이가 정상적으로 이루어진다")
        void transition_PendingToFailed() {
            // given
            Payment payment = createTestPayment(1L, 100L, 1000L, 50000L);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);

            // when
            payment.fail("결제 실패");

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("FAILED 상태에서 COMPLETED로 전이할 수 있다")
        void transition_FailedToCompleted() {
            // given
            Payment payment = createTestPayment(1L, 100L, 1000L, 50000L);
            payment.fail("결제 실패");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);

            // when
            payment.complete();

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("결제 필드 조회 테스트")
    class GetterTest {

        @Test
        @DisplayName("모든 필드를 정상적으로 조회할 수 있다")
        void getFields_Success() {
            // given
            Long id = 1L;
            Long orderId = 100L;
            Long userId = 1000L;
            Long amount = 50000L;

            // when
            Payment payment = createTestPayment(id, orderId, userId, amount);

            // then
            assertAll(
                () -> assertThat(payment.getId()).isEqualTo(id),
                () -> assertThat(payment.getOrderId()).isEqualTo(orderId),
                () -> assertThat(payment.getUserId()).isEqualTo(userId),
                () -> assertThat(payment.getAmount()).isEqualTo(amount),
                () -> assertThat(payment.getStatus()).isNotNull(),
                () -> assertThat(payment.getCreatedAt()).isNotNull(),
                () -> assertThat(payment.getUpdatedAt()).isNotNull()
            );
        }
    }

    @Nested
    @DisplayName("결제 시나리오 통합 테스트")
    class IntegrationScenarioTest {

        @Test
        @DisplayName("결제 성공 시나리오: 생성 -> 완료")
        void scenario_SuccessfulPayment() {
            // given - 결제 생성
            Payment payment = createTestPayment(1L, 100L, 1000L, 50000L);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);

            // when - 결제 완료
            payment.complete();

            // then
            assertAll(
                () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED),
                () -> assertThat(payment.getCompletedAt()).isNotNull(),
                () -> assertThat(payment.getFailureReason()).isNull()
            );
        }

        @Test
        @DisplayName("결제 실패 시나리오: 생성 -> 실패")
        void scenario_FailedPayment() {
            // given - 결제 생성
            Payment payment = createTestPayment(1L, 100L, 1000L, 50000L);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);

            // when - 결제 실패
            String failureReason = "잔액 부족";
            payment.fail(failureReason);

            // then
            assertAll(
                () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED),
                () -> assertThat(payment.getFailureReason()).isEqualTo(failureReason),
                () -> assertThat(payment.getCompletedAt()).isNull()
            );
        }
    }
}
