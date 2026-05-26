package com.dienmay.entity.nhom5.repository;

import com.dienmay.entity.nhom5.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);

    List<Category> findByParentIsNullAndIsActiveTrue();

    List<Category> findAllByIsActiveTrue();
}

