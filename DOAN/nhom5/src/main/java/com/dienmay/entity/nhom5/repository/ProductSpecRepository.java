package com.dienmay.entity.nhom5.repository;

import com.dienmay.entity.nhom5.entity.ProductSpec;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSpecRepository extends JpaRepository<ProductSpec, Long> {

    List<ProductSpec> findByProductId(Long productId);

    void deleteByProductId(Long productId);
}
