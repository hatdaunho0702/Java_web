package com.dienmay.entity.nhom5.controller.admin;

import com.dienmay.entity.nhom5.entity.Coupon;
import com.dienmay.entity.nhom5.entity.CouponDiscountType;
import com.dienmay.entity.nhom5.repository.CouponRepository;
import com.dienmay.entity.nhom5.exception.ResourceNotFoundException;
import com.dienmay.entity.nhom5.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponRepository couponRepository;

    @GetMapping
    public ResponseEntity<List<Coupon>> listCoupons() {
        return ResponseEntity.ok(couponRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Coupon> createCoupon(@RequestBody CouponRequest req) {
        validateCouponRequest(req);

        Coupon coupon = Coupon.builder()
                .code(req.getCode().trim().toUpperCase())
                .description(req.getDescription())
                .discountType(CouponDiscountType.valueOf(req.getDiscountType()))
                .discountValue(req.getDiscountValue())
                .minOrderValue(req.getMinOrderValue())
                .maxDiscount(req.getMaxDiscount())
                .usageLimit(req.getUsageLimit())
                .usedCount(0)
                .startDate(req.getStartDate() != null ? req.getStartDate() : LocalDateTime.now())
                .endDate(req.getEndDate() != null ? req.getEndDate() : LocalDateTime.now().plusMonths(1))
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .createdAt(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(couponRepository.save(coupon));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Coupon> updateCoupon(@PathVariable Long id, @RequestBody CouponRequest req) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã giảm giá"));

        validateCouponRequest(req);

        coupon.setCode(req.getCode().trim().toUpperCase());
        coupon.setDescription(req.getDescription());
        coupon.setDiscountType(CouponDiscountType.valueOf(req.getDiscountType()));
        coupon.setDiscountValue(req.getDiscountValue());
        coupon.setMinOrderValue(req.getMinOrderValue());
        coupon.setMaxDiscount(req.getMaxDiscount());
        coupon.setUsageLimit(req.getUsageLimit());
        if (req.getStartDate() != null) coupon.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) coupon.setEndDate(req.getEndDate());
        coupon.setIsActive(req.getIsActive() != null ? req.getIsActive() : coupon.getIsActive());

        return ResponseEntity.ok(couponRepository.save(coupon));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã giảm giá"));
        couponRepository.delete(coupon);
        return ResponseEntity.noContent().build();
    }

    private void validateCouponRequest(CouponRequest req) {
        if (req.getCode() == null || req.getCode().isBlank()) {
            throw new BadRequestException("Mã coupon không được để trống");
        }
        if (req.getDiscountType() == null || req.getDiscountType().isBlank()) {
            throw new BadRequestException("Kiểu giảm giá không được để trống");
        }
        try {
            CouponDiscountType.valueOf(req.getDiscountType());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Kiểu giảm giá không hợp lệ (PERCENT hoặc FIXED)");
        }
        if (req.getDiscountValue() == null || req.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Giá trị giảm giá phải lớn hơn 0");
        }
        if (CouponDiscountType.valueOf(req.getDiscountType()) == CouponDiscountType.PERCENT 
                && req.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("Giá trị giảm phần trăm không được vượt quá 100%");
        }
    }

    @Data
    public static class CouponRequest {
        private String code;
        private String description;
        private String discountType;
        private BigDecimal discountValue;
        private BigDecimal minOrderValue;
        private BigDecimal maxDiscount;
        private Integer usageLimit;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Boolean isActive;
    }
}
