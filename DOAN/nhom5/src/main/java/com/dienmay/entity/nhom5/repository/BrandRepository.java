package com.dienmay.entity.nhom5.repository;

import com.dienmay.entity.nhom5.entity.Brand;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    Optional<Brand> findByName(String name);

    List<Brand> findAllByIsActiveTrueOrderByNameAsc();
}

