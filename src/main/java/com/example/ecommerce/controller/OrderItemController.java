package com.example.ecommerce.controller;

import com.example.ecommerce.api.OrderItemApi;
import com.example.ecommerce.dto.order.OrderItemResponse;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/order-items")
public class OrderItemController implements OrderItemApi {

    @Override
    public ResponseEntity<List<OrderItemResponse>> getOrderItems(
        @PathVariable @Positive Long orderId
    ) {
        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.ok(List.of());
    }

    @Override
    public ResponseEntity<OrderItemResponse> getOrderItem(
        @PathVariable @Positive Long orderItemId
    ) {
        // Mock 데이터
        OrderItemResponse mockResponse = new OrderItemResponse(
            orderItemId,
            null,
            null,
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
    public ResponseEntity<OrderItemResponse> cancelOrderItem(
        @PathVariable @Positive Long orderItemId
    ) {
        // Mock 데이터
        OrderItemResponse mockResponse = new OrderItemResponse(
            orderItemId,
            null,
            null,
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
    public ResponseEntity<OrderItemResponse> returnOrderItem(
        @PathVariable @Positive Long orderItemId
    ) {
        // Mock 데이터
        OrderItemResponse mockResponse = new OrderItemResponse(
            orderItemId,
            null,
            null,
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
    public ResponseEntity<OrderItemResponse> exchangeOrderItem(
        @PathVariable @Positive Long orderItemId
    ) {
        // Mock 데이터
        OrderItemResponse mockResponse = new OrderItemResponse(
            orderItemId,
            null,
            null,
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
