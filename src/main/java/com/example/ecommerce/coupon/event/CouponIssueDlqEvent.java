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
public class CouponIssueDlqEvent implements Serializable {

    private CouponIssueRequestEvent originalEvent;
    private String failureReason;
    private Integer retryCount;
    private LocalDateTime lastAttemptAt;
    private String stackTrace;
}
