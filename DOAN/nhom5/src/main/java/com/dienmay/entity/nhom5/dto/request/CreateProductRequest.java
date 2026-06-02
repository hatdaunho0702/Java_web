package com.dienmay.entity.nhom5.dto.request;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {
    private String name;
    private String slug;
    private String description;
    private Long categoryId;
    private Long brandId;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private Integer stockQty;
    private Boolean isActive;
    private Boolean isFeatured;
    private List<String> remainImages;
    private List<ProductSpecRequest> specs;
}
