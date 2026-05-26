package com.dienmay.entity.nhom5.repository;

import com.dienmay.entity.nhom5.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    Page<Product> findByIsActiveTrue(Pageable pageable);

        @Modifying
        @Query("""
                        UPDATE Product p
                        SET p.stockQty = p.stockQty - :qty
                        WHERE p.id = :id
                            AND p.stockQty >= :qty
                        """)
        int decreaseStock(@Param("id") Long id, @Param("qty") int qty);

        @Modifying
        @Query("UPDATE Product p SET p.stockQty = p.stockQty + :qty WHERE p.id = :id")
        int increaseStock(@Param("id") Long id, @Param("qty") int qty);

        @Modifying
        @Query("UPDATE Product p SET p.soldQty = p.soldQty + :qty WHERE p.id = :id")
        int increaseSoldQty(@Param("id") Long id, @Param("qty") int qty);

        @Modifying
        @Query("UPDATE Product p SET p.soldQty = CASE WHEN p.soldQty >= :qty THEN p.soldQty - :qty ELSE 0 END WHERE p.id = :id")
        int decreaseSoldQty(@Param("id") Long id, @Param("qty") int qty);

        List<Product> findByIsActiveTrueAndStockQtyLessThanEqual(Integer threshold);

        List<Product> findByIsActiveTrueOrderBySoldQtyDesc(Pageable pageable);
}
