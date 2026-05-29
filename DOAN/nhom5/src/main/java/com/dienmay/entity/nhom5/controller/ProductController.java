package com.dienmay.entity.nhom5.controller;

import com.dienmay.entity.nhom5.dto.request.ProductFilterRequest;
import com.dienmay.entity.nhom5.dto.request.CreateReviewRequest;
import com.dienmay.entity.nhom5.dto.response.ProductDetailResponse;
import com.dienmay.entity.nhom5.dto.response.ReviewDto;
import com.dienmay.entity.nhom5.exception.BadRequestException;
import com.dienmay.entity.nhom5.security.CustomUserDetails;
import com.dienmay.entity.nhom5.service.ProductService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "categoryId", required = false) List<Long> categoryIds,
            @RequestParam(name = "brandId", required = false) List<Long> brandIds,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .keyword(keyword)
                .categoryIds(categoryIds)
                .brandIds(brandsOrEmpty(brandIds))
                .categoryId(firstOrNull(categoryIds))
                .brandId(firstOrNull(brandIds))
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .sort(sort)
                .page(page)
                .size(size)
                .build();

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return ResponseEntity.ok(productService.getProductsPayload(filter, pageable));
    }

    @GetMapping("/featured")
    public ResponseEntity<Map<String, Object>> getFeaturedProducts() {
        return ResponseEntity.ok(productService.getFeaturedProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<Map<String, Object>> getProductReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Page<ReviewDto> result = productService.getProductReviews(id, PageRequest.of(Math.max(page, 0), Math.max(size, 1)));
        return ResponseEntity.ok(Map.of(
                "content", result.getContent(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "currentPage", result.getNumber()
        ));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<Map<String, Object>> createReview(
            @PathVariable Long id,
            @Valid @RequestBody CreateReviewRequest request) {
        String firebaseUid = getCurrentFirebaseUid();
        ReviewDto review = productService.createReview(id, firebaseUid, request);
        return ResponseEntity.ok(Map.of(
                "message", "Đã gửi đánh giá, chờ duyệt",
                "success", true,
                "review", review
        ));
    }

    private String getCurrentFirebaseUid() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String uid && !"anonymousUser".equals(uid)) {
            return uid;
        }
        if (principal instanceof CustomUserDetails cud && cud.getUser() != null) {
            return cud.getUser().getUid();
        }
        throw new BadRequestException("Vui lòng đăng nhập");
    }

    private Long firstOrNull(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.get(0);
    }

    private List<Long> brandsOrEmpty(List<Long> ids) {
        return ids == null ? List.of() : ids;
    }
}
