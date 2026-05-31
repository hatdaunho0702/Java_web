package com.dienmay.entity.nhom5.controller.admin;

import com.dienmay.entity.nhom5.entity.Category;
import com.dienmay.entity.nhom5.repository.CategoryRepository;
import com.dienmay.entity.nhom5.exception.ResourceNotFoundException;
import com.dienmay.entity.nhom5.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<Category>> listCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody CategoryRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BadRequestException("Tên danh mục không được để trống");
        }
        if (req.getSlug() == null || req.getSlug().isBlank()) {
            throw new BadRequestException("Slug danh mục không được để trống");
        }

        if (categoryRepository.findBySlug(req.getSlug().trim().toLowerCase()).isPresent()) {
            throw new BadRequestException("Slug danh mục đã tồn tại trong hệ thống");
        }
        
        Category parent = null;
        if (req.getParentId() != null) {
            parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục cha"));
        }

        Category category = Category.builder()
                .name(req.getName().trim())
                .slug(req.getSlug().trim().toLowerCase())
                .parent(parent)
                .iconUrl(req.getIconUrl())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .createdAt(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(categoryRepository.save(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest req) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        if (req.getName() == null || req.getName().isBlank()) {
            throw new BadRequestException("Tên danh mục không được để trống");
        }
        if (req.getSlug() == null || req.getSlug().isBlank()) {
            throw new BadRequestException("Slug danh mục không được để trống");
        }

        categoryRepository.findBySlug(req.getSlug().trim().toLowerCase()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BadRequestException("Slug danh mục đã tồn tại trong hệ thống");
            }
        });

        Category parent = null;
        if (req.getParentId() != null) {
            if (req.getParentId().equals(id)) {
                throw new BadRequestException("Danh mục cha không thể là chính nó");
            }
            parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục cha"));
        }

        category.setName(req.getName().trim());
        category.setSlug(req.getSlug().trim().toLowerCase());
        category.setParent(parent);
        category.setIconUrl(req.getIconUrl());
        category.setIsActive(req.getIsActive() != null ? req.getIsActive() : category.getIsActive());

        return ResponseEntity.ok(categoryRepository.save(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
        
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new BadRequestException("Không thể xóa danh mục đang có sản phẩm. Hãy chuyển sản phẩm sang danh mục khác trước.");
        }
        
        categoryRepository.delete(category);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class CategoryRequest {
        private String name;
        private String slug;
        private Long parentId;
        private String iconUrl;
        private Boolean isActive;
    }
}
