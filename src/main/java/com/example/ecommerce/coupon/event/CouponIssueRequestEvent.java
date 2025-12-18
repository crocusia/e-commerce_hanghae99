package com.example.ecommerce.coupon.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueRequestEvent implements Serializable {

    private String eventId;
    private String eventType;
    private LocalDateTime occurredAt;

    private Long couponId;
    private Long userId;
    private Long timestamp;
}
