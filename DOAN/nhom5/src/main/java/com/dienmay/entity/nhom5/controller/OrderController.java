package com.dienmay.entity.nhom5.controller;

import com.dienmay.entity.nhom5.dto.request.PlaceOrderRequest;
import com.dienmay.entity.nhom5.dto.response.OrderResponse;
import com.dienmay.entity.nhom5.exception.BadRequestException;
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