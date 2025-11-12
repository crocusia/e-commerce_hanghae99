package com.example.ecommerce.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.user.domain.User;
import com.example.ecommerce.user.domain.status.UserStatus;
import com.example.ecommerce.user.dto.BalanceDeductRequest;
import com.example.ecommerce.user.dto.UserCreateRequest;
import com.example.ecommerce.user.dto.UserResponse;
import com.example.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // 공통 테스트 데이터
    private static final Long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_USER_NAME = "홍길동";
    private static final String DEFAULT_USER_EMAIL = "hong@example.com";
    private static final Long DEFAULT_BALANCE = 10000L;

    // 공통 User 객체
    private User defaultUser;
    private User userWithBalance5000;

    @BeforeEach
    void setUp() {
        defaultUser = createUser(DEFAULT_USER_ID, DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_BALANCE);
        userWithBalance5000 = createUser(DEFAULT_USER_ID, DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, 5000L);
    }

    // 헬퍼 메서드
    private User createUser(Long id, String name, String email, Long balance) {
        return User.builder()
            .id(id)
            .name(name)
            .email(email)
            .balance(balance)
            .build();
    }

    private UserCreateRequest createUserRequest(String name, String email, Long balance) {
        return new UserCreateRequest(name, email, balance);
    }

    private BalanceDeductRequest createDeductRequest(Long userId, Long amount) {
        return new BalanceDeductRequest(userId, amount);
    }

    private void assertUserResponse(UserResponse response, Long expectedId, String expectedName,
        String expectedEmail, Long expectedBalance, UserStatus expectedStatus) {
        assertAll(
            () -> assertThat(response.id()).isEqualTo(expectedId),
            () -> assertThat(response.name()).isEqualTo(expectedName),
            () -> assertThat(response.email()).isEqualTo(expectedEmail),
            () -> assertThat(response.balance()).isEqualTo(expectedBalance),
            () -> assertThat(response.status()).isEqualTo(expectedStatus),
            () -> assertThat(response.createdAt()).isNotNull()
        );
    }

    @Nested
    @DisplayName("유저 생성 테스트")
    class CreateUserTest {

        @Test
        @DisplayName("유저를 정상적으로 생성한다")
        void createUser_Success() {
            // given
            UserCreateRequest request = createUserRequest(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_BALANCE);

            given(userRepository.save(any(User.class))).willReturn(defaultUser);

            // when
            UserResponse response = userService.createUser(request);

            // then
            assertUserResponse(response, DEFAULT_USER_ID, DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_BALANCE, UserStatus.ACTIVE);
            then(userRepository).should(times(1)).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("유저 조회 테스트")
    class GetUserByIdTest {

        @Test
        @DisplayName("유저 ID로 유저를 조회한다")
        void getUserById_Success() {
            // given
            given(userRepository.findByIdOrElseThrow(DEFAULT_USER_ID)).willReturn(defaultUser);

            // when
            UserResponse response = userService.getUserById(DEFAULT_USER_ID);

            // then
            assertUserResponse(response, DEFAULT_USER_ID, DEFAULT_USER_NAME, DEFAULT_USER_EMAIL, DEFAULT_BALANCE,
                UserStatus.ACTIVE);
            then(userRepository).should(times(1)).findByIdOrElseThrow(DEFAULT_USER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 유저 ID로 조회하면 예외가 발생한다")
        void getUserById_NotFound() {
            // given
            Long userId = 999L;
            given(userRepository.findByIdOrElseThrow(userId))
                .willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
            then(userRepository).should().findByIdOrElseThrow(userId);
        }
    }

    @Nested
    @DisplayName("잔액 차감 테스트")
    class DeductBalanceTest {

        @Test
        @DisplayName("유저의 잔액을 정상적으로 차감한다")
        void deductBalance_Success() {
            // given
            BalanceDeductRequest request = createDeductRequest(DEFAULT_USER_ID, 5000L);

            given(userRepository.findByIdOrElseThrow(DEFAULT_USER_ID)).willReturn(defaultUser);
            given(userRepository.save(any(User.class))).willReturn(userWithBalance5000);

            // when
            UserResponse response = userService.deductBalance(request);

            // then
            assertThat(response.balance()).isEqualTo(5000L);
            then(userRepository).should().findByIdOrElseThrow(DEFAULT_USER_ID);
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("잔액이 부족하면 예외가 발생한다")
        void deductBalance_InsufficientBalance() {
            // given
            BalanceDeductRequest request = createDeductRequest(DEFAULT_USER_ID, 15000L);

            given(userRepository.findByIdOrElseThrow(DEFAULT_USER_ID)).willReturn(defaultUser);

            // when & then
            assertThatThrownBy(() -> userService.deductBalance(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_INSUFFICIENT_BALANCE);
            then(userRepository).should().findByIdOrElseThrow(DEFAULT_USER_ID);
            then(userRepository).should(times(0)).save(any(User.class));
        }
    }
}
