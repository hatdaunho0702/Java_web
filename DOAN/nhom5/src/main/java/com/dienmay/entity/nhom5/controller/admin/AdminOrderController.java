package com.dienmay.entity.nhom5.controller.admin;

import com.dienmay.entity.nhom5.dto.request.UpdateOrderStatusRequest;
import com.dienmay.entity.nhom5.dto.response.AdminOrderDto;
import com.dienmay.entity.nhom5.entity.Order;
import com.dienmay.entity.nhom5.entity.OrderStatus;
import com.dienmay.entity.nhom5.exception.BadRequestException;
import com.dienmay.entity.nhom5.security.CustomUserDetails;
import com.dienmay.entity.nhom5.service.OrderService;
import com.dienmay.entity.nhom5.repository.OrderRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<AdminOrderDto>> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        OrderStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = OrderStatus.valueOf(status);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Trạng thái không hợp lệ");
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = orderRepository.adminSearch(statusEnum, (keyword == null || keyword.isBlank()) ? null : keyword, pageable);

        List<AdminOrderDto> content = orders.getContent().stream().map(o -> AdminOrderDto.builder()
                .id(o.getId())
                .orderCode(o.getOrderCode())
                .recipientName(o.getRecipientName())
                .createdAt(o.getCreatedAt())
                .totalAmount(o.getTotalAmount())
                .status(o.getStatus() == null ? null : o.getStatus().name())
                .build()).collect(Collectors.toList());

        Page<AdminOrderDto> dtoPage = new PageImpl<>(content, pageable, orders.getTotalElements());
        return ResponseEntity.ok(dtoPage);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestBody UpdateOrderStatusRequest req) {
        if (req == null || req.getStatus() == null) {
            throw new BadRequestException("Missing status");
        }
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(req.getStatus());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Trạng thái không hợp lệ");
        }

        String adminUid = getCurrentFirebaseUid();
        orderService.updateOrderStatus(id, newStatus, adminUid);
        return ResponseEntity.ok().build();
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
