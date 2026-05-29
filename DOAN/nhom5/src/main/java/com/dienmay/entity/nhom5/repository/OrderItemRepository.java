package com.dienmay.entity.nhom5.repository;

import com.dienmay.entity.nhom5.entity.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("""
            SELECT oi
            FROM OrderItem oi
            WHERE oi.product.id = :productId
                AND oi.order.user.uid = :uid
                AND oi.order.status <> com.dienmay.entity.nhom5.entity.OrderStatus.CANCELLED
            ORDER BY oi.order.createdAt DESC
            """)
    List<OrderItem> findPurchasedItemsByUserAndProduct(
            @Param("uid") String uid,
            @Param("productId") Long productId
    );
}
