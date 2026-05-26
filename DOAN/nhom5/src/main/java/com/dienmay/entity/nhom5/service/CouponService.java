package com.dienmay.entity.nhom5.service;

import com.dienmay.entity.nhom5.dto.response.CouponValidateResponse;
import com.dienmay.entity.nhom5.entity.Coupon;
import com.dienmay.entity.nhom5.entity.CouponDiscountType;
import com.dienmay.entity.nhom5.repository.CouponRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponValidateResponse validateCoupon(String code, BigDecimal orderTotal) {
        Coupon coupon = couponRepository.findByCode(code).orElse(null);
        if (coupon == null) {
            return CouponValidateResponse.builder()
                    .valid(false)
                    .discountAmount(BigDecimal.ZERO)
                    .message("Mã giảm giá không tồn tại")
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            return invalid("Mã giảm giá đã bị vô hiệu hóa");
        }
        if (coupon.getStartDate().isAfter(now) || coupon.getEndDate().isBefore(now)) {
            return invalid("Mã giảm giá không nằm trong thời gian áp dụng");
        }
        if (coupon.getMinOrderValue() != null && orderTotal.compareTo(coupon.getMinOrderValue()) < 0) {
            return invalid("Giá trị đơn hàng chưa đạt mức tối thiểu");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return invalid("Mã giảm giá đã hết lượt sử dụng");
        }

        BigDecimal discountAmount;
        if (coupon.getDiscountType() == CouponDiscountType.PERCENT) {
            discountAmount = orderTotal
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscount() != null) {
                discountAmount = discountAmount.min(coupon.getMaxDiscount());
            }
        } else {
            discountAmount = coupon.getDiscountValue();
        }
        discountAmount = discountAmount.min(orderTotal);

        return CouponValidateResponse.builder()
                .valid(true)
                .discountAmount(discountAmount)
                .message("Áp dụng mã giảm giá thành công")
                .build();
    }

    private CouponValidateResponse invalid(String message) {
        return CouponValidateResponse.builder()
                .valid(false)
                .discountAmount(BigDecimal.ZERO)
                .message(message)
                .build();
    }
}
