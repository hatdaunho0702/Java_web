package com.dienmay.entity.nhom5.repository;

import com.dienmay.entity.nhom5.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);

    List<Category> findByParentIsNullAndIsActiveTrue();

    List<Category> findAllByIsActiveTrue();

    @Query("""
            SELECT DISTINCT c
            FROM Category c
            JOIN c.products p
            WHERE c.isActive = true
                AND p.isActive = true
            ORDER BY c.name ASC
            """)
    List<Category> findActiveCategoriesHavingActiveProducts();
}

