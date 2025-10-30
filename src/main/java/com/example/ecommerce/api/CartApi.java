package com.example.ecommerce.api;

import com.example.ecommerce.dto.cart.CartItemRequest;
import com.example.ecommerce.dto.cart.CartItemResponse;
import com.example.ecommerce.dto.cart.CartResponse;
import com.example.ecommerce.dto.common.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "장바구니", description = "장바구니 관리 API")
public interface CartApi {

    @Operation(
        summary = "장바구니 조회",
        description = "사용자의 장바구니 전체 내역을 조회합니다. 상품명, 가격, 수량, 소계, 재고 상태 및 전체 주문 금액을 포함합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "장바구니 조회 성공",
            content = @Content(schema = @Schema(implementation = CartResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "사용자를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/{userId}")
    ResponseEntity<CartResponse> getCart(
        @Parameter(description = "사용자 ID")
        @PathVariable @Positive Long userId
    );

    @Operation(
        summary = "장바구니에 상품 추가",
        description = "장바구니에 상품을 추가합니다. 담기 시점에 재고를 확인하며, 동일 상품 추가 시 수량이 증가합니다. 재고 초과 수량은 차단됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "장바구니 추가 성공",
            content = @Content(schema = @Schema(implementation = CartItemResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "재고 부족 또는 잘못된 요청",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "상품 또는 사용자를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/{userId}/items")
    ResponseEntity<CartItemResponse> addCartItem(
        @Parameter(description = "사용자 ID")
        @PathVariable @Positive Long userId,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "장바구니 추가 요청",
            required = true,
            content = @Content(schema = @Schema(implementation = CartItemRequest.class))
        )
        @RequestBody @Valid CartItemRequest request
    );

    @Operation(
        summary = "장바구니 상품 수량 변경",
        description = "장바구니의 특정 상품 수량을 변경합니다. 수량 변경 시 재고를 확인하며, 재고 초과 수량 변경은 차단됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "수량 변경 성공",
            content = @Content(schema = @Schema(implementation = CartItemResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "재고 부족 또는 잘못된 요청",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "장바구니 상품을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{userId}/items/{cartItemId}")
    ResponseEntity<CartItemResponse> updateCartItemQuantity(
        @Parameter(description = "사용자 ID")
        @PathVariable @Positive Long userId,
        @Parameter(description = "장바구니 상품 ID")
        @PathVariable @Positive Long cartItemId,
        @Parameter(description = "변경할 수량")
        @RequestParam @Positive Integer quantity
    );

    @Operation(
        summary = "장바구니 상품 삭제",
        description = "장바구니에서 특정 상품을 삭제합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "삭제 성공"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "장바구니 상품을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{userId}/items/{cartItemId}")
    ResponseEntity<Void> deleteCartItem(
        @Parameter(description = "사용자 ID")
        @PathVariable @Positive Long userId,
        @Parameter(description = "장바구니 상품 ID")
        @PathVariable @Positive Long cartItemId
    );

    @Operation(
        summary = "장바구니 전체 비우기",
        description = "사용자의 장바구니를 전체 비웁니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "장바구니 비우기 성공"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "사용자를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @DeleteMapping("/{userId}")
    ResponseEntity<Void> clearCart(
        @Parameter(description = "사용자 ID")
        @PathVariable @Positive Long userId
    );
}
