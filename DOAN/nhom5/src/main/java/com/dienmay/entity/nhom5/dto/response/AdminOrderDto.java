package com.dienmay.entity.nhom5.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderDto {
    private Long id;
    private String orderCode;
    private String recipientName;
    private LocalDateTime createdAt;
    private BigDecimal totalAmount;
    private String status;
}
