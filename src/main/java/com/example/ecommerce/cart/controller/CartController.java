package com.example.ecommerce.cart.controller;

import com.example.ecommerce.cart.dto.CartItemRequest;
import com.example.ecommerce.cart.dto.CartItemResponse;
import com.example.ecommerce.cart.dto.CartResponse;
import com.example.ecommerce.product.domain.status.StockStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/carts")
public class CartController implements CartApi {

    @Override
    public ResponseEntity<CartResponse> getCart(
        @PathVariable @Positive Long userId
    ) {

        // TODO: 서비스 레이어 구현 후 연결
        // Mock 데이터
        List<CartItemResponse> items = List.of();
        CartResponse response = new CartResponse(
            items,
            0L,  // totalAmount
            0L        // discountAmount
        );

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CartItemResponse> addCartItem(
        @PathVariable @Positive Long userId,
        @RequestBody @Valid CartItemRequest request
    ) {
        // TODO: 서비스 레이어 구현 후 연결
        //Mock 데이터
        CartItemResponse response = new CartItemResponse(
            3L,
            request.productId(),
            "Mock 상품명",
            15000L,
            request.quantity(),
            15000L * request.quantity(),
            StockStatus.AVAILABLE
        );

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CartItemResponse> updateCartItemQuantity(
        @PathVariable @Positive Long userId,
        @PathVariable @Positive Long cartItemId,
        @RequestParam @Positive Integer quantity
    ) {
        // TODO: 서비스 레이어 구현 후 연결
        //Mock 데이터
        CartItemResponse response = new CartItemResponse(
            3L,
            1L,
            "Mock 상품명",
            15000L,
            1,
            15000L,
            StockStatus.AVAILABLE
        );

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> deleteCartItem(
        @PathVariable @Positive Long userId,
        @PathVariable @Positive Long cartItemId
    ) {
        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> clearCart(
        @PathVariable @Positive Long userId
    ) {
        // TODO: 서비스 레이어 구현 후 연결
        return ResponseEntity.noContent().build();
    }
}
