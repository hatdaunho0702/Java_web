package com.dienmay.entity.nhom5.dto.request;

import com.dienmay.entity.nhom5.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {
    private Long couponId;
    private String recipientName;
    private String phone;
    private String address;
    private PaymentMethod paymentMethod;
    private String note;
}
