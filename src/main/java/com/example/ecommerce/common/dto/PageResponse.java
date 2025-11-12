package com.example.ecommerce.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "페이징 응답")
public record PageResponse<T>(
    @Schema(description = "현재 페이지 데이터")
    List<T> content,

    @Schema(description = "현재 페이지 번호 (0부터 시작)")
    int page,

    @Schema(description = "페이지 크기")
    int size,

    @Schema(description = "전체 요소 개수")
    long totalElements,

    @Schema(description = "전체 페이지 수")
    int totalPages,

    @Schema(description = "첫 페이지 여부")
    boolean first,

    @Schema(description = "마지막 페이지 여부")
    boolean last,

    @Schema(description = "비어있는 페이지 여부")
    boolean empty
) {
    // Factory method from Spring Data Page
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast(),
            page.isEmpty()
        );
    }

    // Factory method for empty page
    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(
            List.of(),
            page,
            size,
            0L,
            0,
            true,
            true,
            true
        );
    }
}
