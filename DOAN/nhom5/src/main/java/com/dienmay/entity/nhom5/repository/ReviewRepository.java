package com.dienmay.entity.nhom5.repository;

import com.dienmay.entity.nhom5.entity.Review;
import com.dienmay.entity.nhom5.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductId(Long productId, Pageable pageable);

    boolean existsByProductIdAndUser_UidAndOrderId(Long productId, String uid, Long orderId);

    Page<Review> findByUser_Uid(String uid, Pageable pageable);

    boolean existsByUser_UidAndProductIdAndOrderId(String uid, Long productId, Long orderId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.status = :status")
    Double findAverageRatingByProductIdAndStatus(
            @Param("productId") Long productId,
            @Param("status") ReviewStatus status
    );
}
