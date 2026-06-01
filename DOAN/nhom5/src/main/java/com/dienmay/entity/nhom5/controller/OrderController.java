package com.dienmay.entity.nhom5.controller;

import com.dienmay.entity.nhom5.dto.request.PlaceOrderRequest;
import com.dienmay.entity.nhom5.dto.response.OrderResponse;
import com.dienmay.entity.nhom5.exception.BadRequestException;
import com.dienmay.entity.nhom5.exception.ResourceNotFoundException;
import com.dienmay.entity.nhom5.repository.OrderRepository;
import com.dienmay.entity.nhom5.security.CustomUserDetails;
import com.dienmay.entity.nhom5.service.OrderService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        String firebaseUid = getCurrentFirebaseUid();
        OrderResponse response = orderService.placeOrder(firebaseUid, request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public ResponseEntity<PageImpl<Object>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String firebaseUid = getCurrentFirebaseUid();
        Pageable pageable = PageRequest.of(page, size);
        var ordersPage = orderRepository.findByUser_UidOrderByCreatedAtDesc(firebaseUid, pageable);
        var content = ordersPage.getContent().stream()
                .map(o -> (Object) Map.of(
                        "id",          o.getId(),
                        "orderCode",   o.getOrderCode(),
                        "status",      o.getStatus() != null ? o.getStatus().name() : "",
                        "totalAmount", o.getTotalAmount(),
                        "createdAt",   o.getCreatedAt()
                ))
                .toList();
        PageImpl<Object> result = new PageImpl<>(content, pageable, ordersPage.getTotalElements());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/preview")
    public ResponseEntity<Map<String, String>> previewUnavailable() {
        return ResponseEntity.ok(Map.of("message", "preview not implemented"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOrderDetail(@PathVariable Long id) {
        String firebaseUid = getCurrentFirebaseUid();
        var order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));

        if (!order.getUser().getUid().equals(firebaseUid)) {
            throw new BadRequestException("Bạn không có quyền xem đơn hàng này");
        }

        var items = order.getItems().stream()
                .map(item -> Map.of(
                        "id", item.getId(),
                        "productId", item.getProduct().getId(),
                        "productName", item.getProductName(),
                        "productImage", item.getProductImage() != null ? item.getProductImage() : "",
                        "unitPrice", item.getUnitPrice(),
                        "quantity", item.getQuantity(),
                        "subtotal", item.getSubtotal()
                ))
                .toList();

        var result = Map.ofEntries(
                Map.entry("id", order.getId()),
                Map.entry("orderCode", order.getOrderCode()),
                Map.entry("recipientName", order.getRecipientName()),
                Map.entry("recipientPhone", order.getRecipientPhone()),
                Map.entry("shippingAddress", order.getShippingAddress()),
                Map.entry("subtotal", order.getSubtotal()),
                Map.entry("discountAmount", order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO),
                Map.entry("shippingFee", order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO),
                Map.entry("totalAmount", order.getTotalAmount()),
                Map.entry("status", order.getStatus() != null ? order.getStatus().name() : ""),
                Map.entry("paymentMethod", order.getPaymentMethod() != null ? order.getPaymentMethod().name() : ""),
                Map.entry("paymentStatus", order.getPaymentStatus() != null ? order.getPaymentStatus().name() : ""),
                Map.entry("note", order.getNote() != null ? order.getNote() : ""),
                Map.entry("createdAt", order.getCreatedAt()),
                Map.entry("items", items)
        );

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable Long orderId) {
        String firebaseUid = getCurrentFirebaseUid();
        orderService.cancelOrder(orderId, firebaseUid);
        return ResponseEntity.ok(Map.of("message", "Đã hủy đơn hàng thành công", "success", true));
    }

    private String getCurrentFirebaseUid() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String uid && !"anonymousUser".equals(uid)) {
            return uid;
        }
        if (principal instanceof CustomUserDetails cud && cud.getUser() != null) {
            return cud.getUser().getUid();
        }
        throw new BadRequestException("Vui lòng đăng nhập");
    }
}