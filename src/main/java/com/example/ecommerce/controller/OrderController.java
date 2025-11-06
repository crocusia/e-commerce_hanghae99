package com.example.ecommerce.controller;

import com.example.ecommerce.dto.common.PageResponse;
import com.example.ecommerce.dto.order.OrderRequest;
import com.example.ecommerce.dto.order.OrderResponse;
import com.example.ecommerce.dto.order.PaymentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    public ResponseEntity<PageResponse<OrderResponse>> getOrders(
        @RequestParam @Positive Long userId
        //@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // TODO: 서비스 레이어 구현 후 연결
        PageResponse<OrderResponse> response = PageResponse.empty(
            0,  // pageable.getPageNumber()
            20  // pageable.getPageSize()
        );
        return ResponseEntity.ok(response);
    }

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
