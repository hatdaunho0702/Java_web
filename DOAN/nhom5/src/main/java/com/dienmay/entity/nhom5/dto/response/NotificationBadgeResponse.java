package com.dienmay.entity.nhom5.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationBadgeResponse {
    private long unreadMessages;
    private long lowStockCount;
    private long pendingOrders;
    private long total;       // tổng tất cả
}
