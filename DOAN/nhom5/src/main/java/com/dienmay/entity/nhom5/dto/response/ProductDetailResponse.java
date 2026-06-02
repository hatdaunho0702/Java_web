package com.dienmay.entity.nhom5.dto.response;

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
public class ProductDetailResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private Integer stockQty;
    private boolean active;
    private boolean featured;
    private String thumbnailUrl;
    private Double avgRating;
    private Double averageRating;
    private Integer reviewCount;
    private String categoryName;
    private String brandName;
    private Long categoryId;
    private Long brandId;
    private List<String> imageUrls;
    private List<com.dienmay.entity.nhom5.entity.ProductImage> images;
    private List<ProductSpecDto> specs;
    private List<ReviewDto> reviews;
    private List<ProductResponse> related;
}
