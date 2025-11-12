package com.example.ecommerce.user.dto;

import com.example.ecommerce.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest(
    @NotBlank(message = "이름은 필수입니다")
    @Schema(description = "사용자 이름")
    String name,

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이어야 합니다")
    @Schema(description = "이메일")
    String email,

    @NotNull(message = "초기 잔액은 필수입니다")
    @Min(value = 0, message = "잔액은 0 이상이어야 합니다")
    @Schema(description = "초기 잔액")
    Long balance
) {
    public User toEntity() {
        return User.builder()
            .name(name)
            .email(email)
            .balance(balance)
            .build();
    }
}

