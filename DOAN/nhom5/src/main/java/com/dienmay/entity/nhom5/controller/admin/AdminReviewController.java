package com.dienmay.entity.nhom5.controller.admin;

import com.dienmay.entity.nhom5.dto.response.AdminReviewDto;
import com.dienmay.entity.nhom5.entity.Review;
import com.dienmay.entity.nhom5.exception.ResourceNotFoundException;
import com.dienmay.entity.nhom5.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewRepository reviewRepository;

    @GetMapping
    public ResponseEntity<Page<AdminReviewDto>> listReviews(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviews;
        if (keyword != null && !keyword.isBlank()) {
            reviews = reviewRepository.searchReviews(keyword.trim(), pageable);
        } else {
            reviews = reviewRepository.findAll(pageable);
        }

        Page<AdminReviewDto> dtoPage = reviews.map(r -> AdminReviewDto.builder()
                .id(r.getId())
                .productId(r.getProduct() != null ? r.getProduct().getId() : null)
                .productName(r.getProduct() != null ? r.getProduct().getName() : null)
                .productSlug(r.getProduct() != null ? r.getProduct().getSlug() : null)
                .userUid(r.getUser() != null ? r.getUser().getUid() : null)
                .userName(r.getUser() != null ? r.getUser().getFullName() : "Khách hàng")
                .userEmail(r.getUser() != null ? r.getUser().getEmail() : "")
                .rating(r.getRating())
                .comment(r.getComment())
                .status(r.getStatus() != null ? r.getStatus().name() : "APPROVED")
                .createdAt(r.getCreatedAt())
                .build());

        return ResponseEntity.ok(dtoPage);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy đánh giá");
        }
        reviewRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
