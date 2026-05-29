package com.dienmay.entity.nhom5.repository;

import com.dienmay.entity.nhom5.entity.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    Page<Product> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Product> findByIsActiveTrueAndSalePriceIsNotNull(Pageable pageable);

    Page<Product> findByIsActiveTrueOrderBySoldQtyDesc(Pageable pageable);

        Page<Product> findByIsActiveTrueAndCategory_IdAndIdNotOrderByCreatedAtDesc(
            Long categoryId,
            Long id,
            Pageable pageable
        );

    @Query("SELECT MIN(COALESCE(p.salePrice, p.originalPrice)) FROM Product p WHERE p.isActive = true")
    BigDecimal findMinEffectivePriceForActive();

    @Query("SELECT MAX(COALESCE(p.salePrice, p.originalPrice)) FROM Product p WHERE p.isActive = true")
    BigDecimal findMaxEffectivePriceForActive();

        List<Product> findByIsActiveTrueAndStockQtyLessThanEqual(Integer threshold);
}
