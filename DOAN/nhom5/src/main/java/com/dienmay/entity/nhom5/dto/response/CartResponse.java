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
public class CartResponse {
    private Long cartItemId;
    private Long productId;
    private String productName;
    private String thumbnailUrl;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private int stockQty;
}
