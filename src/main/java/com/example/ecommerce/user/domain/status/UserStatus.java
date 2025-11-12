package com.example.ecommerce.user.domain.status;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "유저 상태: 활성, 비활성, 삭제됨")
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    DELETED
}
