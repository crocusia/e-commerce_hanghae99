package com.example.ecommerce.user.domain;

import com.example.ecommerce.common.domain.SoftDeleteEntity;
import com.example.ecommerce.common.exception.CustomException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.user.domain.status.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class User extends SoftDeleteEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "balance", nullable = false)
    private Long balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    // static factory method
    public static User create(String name, String email, Long balance) {
        validateBalance(balance);

        return User.builder()
            .name(name)
            .email(email)
            .balance(balance)
            .status(UserStatus.ACTIVE)
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
    }

    public void deductBalance(Long amount) {
        if (this.balance < amount) {
            throw new CustomException(ErrorCode.USER_INSUFFICIENT_BALANCE);
        }
        this.balance -= amount;
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
