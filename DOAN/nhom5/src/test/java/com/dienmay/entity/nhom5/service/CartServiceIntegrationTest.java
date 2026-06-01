package com.dienmay.entity.nhom5.service;

import static org.junit.jupiter.api.Assertions.*;

import com.dienmay.entity.nhom5.entity.CartItem;
import com.dienmay.entity.nhom5.entity.Product;
import com.dienmay.entity.nhom5.entity.User;
import com.dienmay.entity.nhom5.repository.CartItemRepository;
import com.dienmay.entity.nhom5.repository.ProductRepository;
import com.dienmay.entity.nhom5.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class CartServiceIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @Transactional
    void testUpdateCartItem() {
        // Find an existing cart item in the database
        CartItem cartItem = cartItemRepository.findAll().stream().findFirst().orElse(null);
        if (cartItem != null) {
            String uid = cartItem.getUser().getUid();
            Long id = cartItem.getId();
            int oldQty = cartItem.getQuantity();
            int newQty = oldQty + 1;
            if (newQty <= cartItem.getProduct().getStockQty()) {
                cartService.updateCartItem(uid, id, newQty);
                CartItem updated = cartItemRepository.findById(id).orElse(null);
                assertNotNull(updated);
                assertEquals(newQty, updated.getQuantity());
                
                // Revert
                cartService.updateCartItem(uid, id, oldQty);
            }
        }
    }
}
