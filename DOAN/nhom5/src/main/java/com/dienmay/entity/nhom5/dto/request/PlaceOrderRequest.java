package com.dienmay.entity.nhom5.dto.request;

import com.dienmay.entity.nhom5.entity.PaymentMethod;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {
    private String couponCode;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private PaymentMethod paymentMethod;
    private String note;
    private List<OrderItemRequest> items;
}
