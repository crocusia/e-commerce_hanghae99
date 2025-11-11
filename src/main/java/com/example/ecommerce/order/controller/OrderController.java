package com.example.ecommerce.order.controller;

import com.example.ecommerce.common.dto.PageResponse;
import com.example.ecommerce.order.dto.OrderRequest;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.payment.dto.PaymentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/orders")
public class OrderController implements OrderApi {

    @Override
    public ResponseEntity<PageResponse<OrderResponse>> getOrders(
        @RequestParam @Positive Long userId,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // TODO: 서비스 레이어 구현 후 연결
        PageResponse<OrderResponse> response = PageResponse.empty(
            pageable.getPageNumber(),
            pageable.getPageSize()
        );
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<OrderResponse> getOrder(
        @PathVariable @Positive Long orderId
    ) {
        //Mock 데이터
        OrderResponse mockResponse = new OrderResponse(
            null,
            null,
            List.of(),
            null,
            null,
            null,
            null,
            null
        );
        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.ok(mockResponse);
    }

    @Override
    public ResponseEntity<OrderResponse> createOrder(
        @RequestBody @Valid OrderRequest request
    ) {
        //Mock 데이터
        OrderResponse mockResponse = new OrderResponse(
            null,
            null,
            List.of(),
            null,
            null,
            null,
            null,
            null
        );
        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.status(201).body(mockResponse);
    }

    @Override
    public ResponseEntity<OrderResponse> processPayment(
        @RequestBody @Valid PaymentRequest request
    ) {
        //Mock 데이터
        OrderResponse mockResponse = new OrderResponse(
            null,
            null,
            List.of(),
            null,
            null,
            null,
            null,
            null
        );
        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.ok(mockResponse);
    }

    @Override
    public ResponseEntity<OrderResponse> cancelOrder(
        @PathVariable @Positive Long orderId
    ) {
        //Mock 데이터
        OrderResponse mockResponse = new OrderResponse(
            null,
            null,
            List.of(),
            null,
            null,
            null,
            null,
            null
        );
        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.ok(mockResponse);
    }
}
