package com.dienmay.entity.nhom5.controller;

import com.dienmay.entity.nhom5.dto.response.CartResponse;
import com.dienmay.entity.nhom5.entity.User;
import com.dienmay.entity.nhom5.exception.ResourceNotFoundException;
import com.dienmay.entity.nhom5.repository.UserRepository;
import com.dienmay.entity.nhom5.service.CartService;
import com.dienmay.entity.nhom5.security.CustomUserDetails;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
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
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    public CartController(CartService cartService, UserRepository userRepository) {
        this.cartService = cartService;
        this.userRepository = userRepository;
    }

    private User resolveCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal == null) {
            throw new ResourceNotFoundException("Không tìm thấy user hiện tại");
        }
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUser();
        }
        if (principal instanceof String firebaseUid) {
            return userRepository.findByUid(firebaseUid)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        }
        throw new ResourceNotFoundException("Không tìm thấy user hiện tại");
    }

    @GetMapping
    public ResponseEntity<List<CartResponse>> getCart() {
        User user = resolveCurrentUser();
        return ResponseEntity.ok(cartService.getCart(user.getUid()));
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> addToCart(@RequestBody AddToCartRequest req) {
        User user = resolveCurrentUser();
        cartService.addToCart(user.getUid(), req.productId, req.quantity == null ? 1 : req.quantity);
        return ResponseEntity.ok(Map.of("message", "Đã thêm vào giỏ hàng"));
    }

    @PutMapping("/{cartItemId}")
    public ResponseEntity<Map<String, String>> updateCartItem(@PathVariable Long cartItemId,
            @RequestBody UpdateCartRequest req) {
        User user = resolveCurrentUser();
        cartService.updateCartItem(user.getUid(), cartItemId, req.quantity);
        return ResponseEntity.ok(Map.of("message", "Cập nhật số lượng thành công"));
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Map<String, String>> removeCartItem(@PathVariable Long cartItemId) {
        User user = resolveCurrentUser();
        cartService.removeCartItem(user.getUid(), cartItemId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm khỏi giỏ hàng"));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearCart() {
        User user = resolveCurrentUser();
        cartService.clearCart(user.getUid());
        return ResponseEntity.ok(Map.of("message", "Đã xóa toàn bộ giỏ hàng"));
    }

    public static class AddToCartRequest {
        public Long productId;
        public Integer quantity;
    }

    public static class UpdateCartRequest {
        public Integer quantity;
    }
}