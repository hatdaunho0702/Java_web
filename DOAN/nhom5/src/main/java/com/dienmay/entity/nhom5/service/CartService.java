package com.dienmay.entity.nhom5.service;

import com.dienmay.entity.nhom5.dto.response.CartResponse;
import com.dienmay.entity.nhom5.entity.CartItem;
import com.dienmay.entity.nhom5.entity.Product;
import com.dienmay.entity.nhom5.entity.User;
import com.dienmay.entity.nhom5.exception.BadRequestException;
import com.dienmay.entity.nhom5.exception.ResourceNotFoundException;
import com.dienmay.entity.nhom5.repository.CartItemRepository;
import com.dienmay.entity.nhom5.repository.ProductRepository;
import com.dienmay.entity.nhom5.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public List<CartResponse> getCart(Long userId) {
        return cartItemRepository.findByUserId(userId)
                .stream()
                .map(item -> {
                    BigDecimal unitPrice = item.getProduct().getSalePrice() != null
                            ? item.getProduct().getSalePrice()
                            : item.getProduct().getOriginalPrice();
                    BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                    return CartResponse.builder()
                            .cartItemId(item.getId())
                            .productId(item.getProduct().getId())
                            .productName(item.getProduct().getName())
                            .thumbnailUrl(item.getProduct().getThumbnailUrl())
                            .quantity(item.getQuantity())
                            .unitPrice(unitPrice)
                            .subtotal(subtotal)
                            .stockQty(item.getProduct().getStockQty())
                            .build();
                })
                .toList();
    }

    @Transactional
    public void addToCart(Long userId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Số lượng phải lớn hơn 0");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new BadRequestException("Sản phẩm đang tạm ngưng bán");
        }

        if (product.getStockQty() < quantity) {
            throw new BadRequestException("Số lượng yêu cầu vượt quá tồn kho");
        }

        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(userId, productId).orElse(null);
        if (cartItem != null) {
            int newQty = cartItem.getQuantity() + quantity;
            if (newQty > product.getStockQty()) {
                throw new BadRequestException("Số lượng trong giỏ vượt quá tồn kho");
            }
            cartItem.setQuantity(newQty);
            cartItemRepository.save(cartItem);
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        cartItemRepository.save(CartItem.builder()
                .user(user)
                .product(product)
                .quantity(quantity)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void updateCartItem(Long userId, Long cartItemId, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Số lượng phải lớn hơn 0");
        }

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ"));

        if (!cartItem.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền sửa giỏ hàng này");
        }

        if (cartItem.getProduct().getStockQty() < quantity) {
            throw new BadRequestException("Số lượng yêu cầu vượt quá tồn kho");
        }

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
    }

    @Transactional
    public void removeCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ"));
        if (!cartItem.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền xóa sản phẩm trong giỏ này");
        }
        cartItemRepository.delete(cartItem);
    }
}
