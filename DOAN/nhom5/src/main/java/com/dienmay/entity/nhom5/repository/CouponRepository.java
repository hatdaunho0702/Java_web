package com.dienmay.entity.nhom5.repository;

import com.dienmay.entity.nhom5.entity.Coupon;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    @Query("""
            SELECT c
            FROM Coupon c
            WHERE c.code = :code
              AND c.isActive = true
              AND c.startDate <= :now
              AND c.endDate >= :now
              AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)
            """)
    Optional<Coupon> findValidCoupon(
            @Param("code") String code,
            @Param("now") LocalDateTime now
    );
}
