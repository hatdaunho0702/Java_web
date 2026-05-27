package com.dienmay.entity.nhom5.repository;

import com.dienmay.entity.nhom5.entity.Order;
import com.dienmay.entity.nhom5.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUser_UidOrderByCreatedAtDesc(String uid, Pageable pageable);

    List<Order> findByUser_UidAndStatus(String uid, OrderStatus status);

    long countByStatus(OrderStatus status);

        @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0)
            FROM Order o
            WHERE o.status = :status
              AND o.createdAt >= :fromDate
              AND o.createdAt < :toDate
            """)
        BigDecimal sumRevenueByStatusBetween(
            @Param("status") OrderStatus status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
        );

        @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
        List<Object[]> countOrdersByStatusGroup();
}
