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
import java.util.Optional;
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

    public List<CartResponse> getCart(String userUid) {
        return cartItemRepository.findByUser_Uid(userUid)
                .stream()
                .map(item -> {
                    Product p = item.getProduct();
                    BigDecimal unitPrice = p.getSalePrice() != null
                            ? p.getSalePrice()
                            : p.getOriginalPrice();
                    BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                    return CartResponse.builder()
                            .cartItemId(item.getId())
                            .productId(p.getId())
                            .productName(p.getName())
                            .thumbnailUrl(p.getThumbnailUrl())
                            .quantity(item.getQuantity())
                            .unitPrice(unitPrice)
                            .subtotal(subtotal)
                            .stockQty(p.getStockQty())
                            .build();
                })
                .toList();
    }

    @Transactional
    public void addToCart(String userUid, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Số lượng phải lớn hơn 0");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new BadRequestException("Sản phẩm không còn kinh doanh");
        }

        Optional<CartItem> existing = cartItemRepository.findByUser_UidAndProduct_Id(userUid, productId);
        int totalQty = quantity + existing.map(CartItem::getQuantity).orElse(0);
        if (totalQty > product.getStockQty()) {
            throw new BadRequestException("Chỉ còn " + product.getStockQty() + " sản phẩm trong kho");
        }

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(totalQty);
            cartItemRepository.save(item);
            return;
        }

        User user = userRepository.findByUid(userUid)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        cartItemRepository.save(CartItem.builder()
                .user(user)
                .product(product)
                .quantity(quantity)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void updateCartItem(String userUid, Long cartItemId, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Số lượng phải lớn hơn 0");
        }

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ"));

        if (!cartItem.getUser().getUid().equals(userUid)) {
            throw new BadRequestException("Bạn không có quyền sửa giỏ hàng này");
        }

        if (quantity > cartItem.getProduct().getStockQty()) {
            throw new BadRequestException("Chỉ còn " + cartItem.getProduct().getStockQty() + " sản phẩm trong kho");
        }

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
    }

    @Transactional
    public void removeCartItem(String userUid, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ"));
        if (!cartItem.getUser().getUid().equals(userUid)) {
            throw new BadRequestException("Bạn không có quyền xóa sản phẩm trong giỏ này");
        }
        cartItemRepository.delete(cartItem);
    }

    public int countCartItems(String userUid) {
        return cartItemRepository.findByUser_Uid(userUid).size();
    }

    @Transactional
    public void clearCart(String userUid) {
        cartItemRepository.deleteByUser_Uid(userUid);
    }
}
