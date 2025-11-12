package com.example.ecommerce.user.dto;

import com.example.ecommerce.user.domain.User;
import com.example.ecommerce.user.domain.status.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "사용자 응답")
public record UserResponse(
    @Schema(description = "사용자 ID") Long id,
    @Schema(description = "사용자 이름") String name,
    @Schema(description = "이메일") String email,
    @Schema(description = "잔액") Long balance,
    @Schema(description = "상태") UserStatus status,
    @Schema(description = "생성 일시") LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getBalance(),
            user.getStatus(),
            user.getCreatedAt()
        );
    }
}