package com.dienmay.entity.nhom5.controller;

import com.dienmay.entity.nhom5.dto.response.CartResponse;
import com.dienmay.entity.nhom5.entity.User;
import com.dienmay.entity.nhom5.exception.BadRequestException;
import com.dienmay.entity.nhom5.exception.ResourceNotFoundException;
import com.dienmay.entity.nhom5.repository.UserRepository;
import com.dienmay.entity.nhom5.service.CartService;
import com.dienmay.entity.nhom5.security.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    private String getCurrentFirebaseUidOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) {
            return null;
        }
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUser().getUid();
        }
        if (principal instanceof String firebaseUid) {
            return firebaseUid;
        }
        return null;
    }

    private String getCurrentUserUid() {
        String firebaseUid = getCurrentFirebaseUidOrNull();
        if (firebaseUid == null) {
            throw new BadRequestException("Vui lòng đăng nhập");
        }
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        return user.getId();
    }

    @GetMapping
    public ResponseEntity<List<CartResponse>> getCart() {
        String firebaseUid = getCurrentFirebaseUidOrNull();
        if (firebaseUid == null) {
            return ResponseEntity.ok(List.of());
        }
        String userUid = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"))
                .getId();
        return ResponseEntity.ok(cartService.getCart(userUid));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addToCart(@Valid @RequestBody CartItemRequest req) {
        String userUid = getCurrentUserUid();
        cartService.addToCart(userUid, req.getProductId(), req.getQuantity());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Đã thêm vào giỏ hàng", "success", true));
    }

    @PutMapping("/{cartItemId}")
    public ResponseEntity<Map<String, Object>> updateCartItem(
            @PathVariable Long cartItemId,
            @RequestBody Map<String, Integer> body) {
        String userUid = getCurrentUserUid();
        Integer qty = body.get("quantity");
        if (qty == null || qty < 1) {
            throw new BadRequestException("Số lượng phải lớn hơn hoặc bằng 1");
        }
        cartService.updateCartItem(userUid, cartItemId, qty);
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật", "success", true));
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Void> removeCartItem(@PathVariable Long cartItemId) {
        String userUid = getCurrentUserUid();
        cartService.removeCartItem(userUid, cartItemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clearCart() {
        String userUid = getCurrentUserUid();
        cartService.clearCart(userUid);
        return ResponseEntity.ok(Map.of("message", "Đã xóa toàn bộ giỏ hàng", "success", true));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> countCart() {
        String firebaseUid = getCurrentFirebaseUidOrNull();
        if (firebaseUid == null) {
            return ResponseEntity.ok(Map.of("count", 0));
        }
        String userUid = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"))
                .getId();
        return ResponseEntity.ok(Map.of("count", cartService.countCartItems(userUid)));
    }

    public static class CartItemRequest {
        @NotNull(message = "productId là bắt buộc")
        private Long productId;

        @Min(value = 1, message = "quantity tối thiểu là 1")
        @Max(value = 99, message = "quantity tối đa là 99")
        private int quantity;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}