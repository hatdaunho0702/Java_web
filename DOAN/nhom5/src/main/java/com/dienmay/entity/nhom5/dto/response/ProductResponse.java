package com.dienmay.entity.nhom5.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private String thumbnailUrl;
    private Double avgRating;
    private Integer stockQty;
}
