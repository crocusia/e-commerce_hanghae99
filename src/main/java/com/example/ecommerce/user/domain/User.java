package com.example.ecommerce.user.domain;

import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.user.domain.status.UserStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
public class User {
    private final Long id;
    private String name;
    private String email;
    private Long balance;
    private UserStatus status;
    private LocalDateTime deletedAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private User(Long id, String name, String email, Long balance) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.balance = balance;
        this.status = UserStatus.ACTIVE;
        this.deletedAt = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // static factory method
    public static User create(String name, String email, Long balance) {
        validateBalance(balance);

        return User.builder()
            .name(name)
            .email(email)
            .balance(balance)
            .build();
    }

    public static User create(String name, String email) {
        return create(name, email, 0L);
    }

    public void chargeBalance(Long amount) {
        if (amount == null || amount <= 0) {
            throw new CustomException(ErrorCode.USER_INVALID_CHARGE_AMOUNT);
        }
        this.balance += amount;
        this.updatedAt = LocalDateTime.now();
    }

    public void deductBalance(Long amount) {
        if (this.balance < amount) {
            throw new CustomException(ErrorCode.USER_INSUFFICIENT_BALANCE);
        }
        this.balance -= amount;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasEnoughBalance(Long requiredAmount) {
        return this.balance >= requiredAmount;
    }

    private static void validateBalance(Long balance) {
        if (balance == null || balance < 0) {
            throw new CustomException(ErrorCode.USER_INVALID_BALANCE);
        }
    }
}
