package com.dienmay.entity.nhom5.controller.admin;

import com.dienmay.entity.nhom5.dto.response.AdminOrderDto;
import com.dienmay.entity.nhom5.dto.response.AdminUserDto;
import com.dienmay.entity.nhom5.entity.Order;
import com.dienmay.entity.nhom5.entity.User;
import com.dienmay.entity.nhom5.exception.ResourceNotFoundException;
import com.dienmay.entity.nhom5.repository.OrderRepository;
import com.dienmay.entity.nhom5.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @GetMapping
    public ResponseEntity<Page<AdminUserDto>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> users;
        if (keyword != null && !keyword.isBlank()) {
            users = userRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(keyword, keyword, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        List<AdminUserDto> content = users.getContent().stream().map(u -> {
            long userOrders = orderRepository.findByUser_UidOrderByCreatedAtDesc(u.getUid(), PageRequest.of(0,1)).getTotalElements();
            return AdminUserDto.builder()
                    .uid(u.getUid())
                    .fullName(u.getFullName())
                    .email(u.getEmail())
                    .avatarUrl(u.getAvatarUrl())
                    .createdAt(u.getCreatedAt())
                    .isActive(u.getIsActive())
                    .orderCount(userOrders)
                    .build();
        }).collect(Collectors.toList());

        Page<AdminUserDto> dtoPage = new PageImpl<>(content, pageable, users.getTotalElements());
        return ResponseEntity.ok(dtoPage);
    }

    @PutMapping("/{id}/lock")
    public ResponseEntity<Void> lockUser(@PathVariable String id) {
        User u = userRepository.findByUid(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        u.setIsActive(false);
        userRepository.save(u);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/unlock")
    public ResponseEntity<Void> unlockUser(@PathVariable String id) {
        User u = userRepository.findByUid(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        u.setIsActive(true);
        userRepository.save(u);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<Page<AdminOrderDto>> getUserOrders(@PathVariable String id,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        userRepository.findByUid(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = orderRepository.findByUser_UidOrderByCreatedAtDesc(id, pageable);
        Page<AdminOrderDto> dto = orders.map(o -> AdminOrderDto.builder()
            .id(o.getId())
            .orderCode(o.getOrderCode())
            .recipientName(o.getRecipientName())
            .createdAt(o.getCreatedAt())
            .status(o.getStatus() == null ? null : o.getStatus().name())
            .totalAmount(o.getTotalAmount())
            .build());
        return ResponseEntity.ok(dto);
    }
}
