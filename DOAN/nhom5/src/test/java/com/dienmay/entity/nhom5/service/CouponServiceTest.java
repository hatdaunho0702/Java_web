package com.dienmay.entity.nhom5.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.dienmay.entity.nhom5.dto.response.CouponValidateResponse;
import com.dienmay.entity.nhom5.entity.Coupon;
import com.dienmay.entity.nhom5.entity.CouponDiscountType;
import com.dienmay.entity.nhom5.repository.CouponRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon percentCoupon;
    private Coupon fixedCoupon;

    @BeforeEach
    void setUp() {
        percentCoupon = Coupon.builder()
                .id(1L)
                .code("PERCENT10")
                .discountType(CouponDiscountType.PERCENT)
                .discountValue(new BigDecimal("10"))
                .minOrderValue(new BigDecimal("100000"))
                .maxDiscount(new BigDecimal("50000"))
                .usageLimit(100)
                .usedCount(0)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .build();

        fixedCoupon = Coupon.builder()
                .id(2L)
                .code("FIXED50K")
                .discountType(CouponDiscountType.FIXED)
                .discountValue(new BigDecimal("50000"))
                .minOrderValue(new BigDecimal("200000"))
                .usageLimit(100)
                .usedCount(0)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .build();
    }

    @Test
    void validateCoupon_Success_Percent() {
        when(couponRepository.findByCode("PERCENT10")).thenReturn(Optional.of(percentCoupon));

        CouponValidateResponse response = couponService.validateCoupon("PERCENT10", new BigDecimal("200000"));

        assertTrue(response.isValid());
        assertEquals(0, response.getDiscountAmount().compareTo(new BigDecimal("20000")));
        assertEquals("Áp dụng mã giảm giá thành công", response.getMessage());
        assertEquals(1L, response.getCouponId());
    }

    @Test
    void validateCoupon_Success_Fixed() {
        when(couponRepository.findByCode("FIXED50K")).thenReturn(Optional.of(fixedCoupon));

        CouponValidateResponse response = couponService.validateCoupon("FIXED50K", new BigDecimal("300000"));

        assertTrue(response.isValid());
        assertEquals(0, response.getDiscountAmount().compareTo(new BigDecimal("50000")));
        assertEquals("Áp dụng mã giảm giá thành công", response.getMessage());
        assertEquals(2L, response.getCouponId());
    }

    @Test
    void validateCoupon_NotFound() {
        when(couponRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        CouponValidateResponse response = couponService.validateCoupon("INVALID", new BigDecimal("200000"));

        assertFalse(response.isValid());
        assertEquals("Mã giảm giá không tồn tại", response.getMessage());
    }

    @Test
    void validateCoupon_Inactive() {
        percentCoupon.setIsActive(false);
        when(couponRepository.findByCode("PERCENT10")).thenReturn(Optional.of(percentCoupon));

        CouponValidateResponse response = couponService.validateCoupon("PERCENT10", new BigDecimal("200000"));

        assertFalse(response.isValid());
        assertEquals("Mã giảm giá đã bị vô hiệu hóa", response.getMessage());
    }

    @Test
    void validateCoupon_Expired() {
        percentCoupon.setEndDate(LocalDateTime.now().minusHours(1));
        when(couponRepository.findByCode("PERCENT10")).thenReturn(Optional.of(percentCoupon));

        CouponValidateResponse response = couponService.validateCoupon("PERCENT10", new BigDecimal("200000"));

        assertFalse(response.isValid());
        assertEquals("Mã giảm giá không nằm trong thời gian áp dụng", response.getMessage());
    }

    @Test
    void validateCoupon_NotStartedYet() {
        percentCoupon.setStartDate(LocalDateTime.now().plusHours(1));
        when(couponRepository.findByCode("PERCENT10")).thenReturn(Optional.of(percentCoupon));

        CouponValidateResponse response = couponService.validateCoupon("PERCENT10", new BigDecimal("200000"));

        assertFalse(response.isValid());
        assertEquals("Mã giảm giá không nằm trong thời gian áp dụng", response.getMessage());
    }

    @Test
    void validateCoupon_BelowMinOrderValue() {
        when(couponRepository.findByCode("PERCENT10")).thenReturn(Optional.of(percentCoupon));

        CouponValidateResponse response = couponService.validateCoupon("PERCENT10", new BigDecimal("50000"));

        assertFalse(response.isValid());
        assertEquals("Giá trị đơn hàng chưa đạt mức tối thiểu", response.getMessage());
    }

    @Test
    void validateCoupon_UsageLimitReached() {
        percentCoupon.setUsedCount(100);
        when(couponRepository.findByCode("PERCENT10")).thenReturn(Optional.of(percentCoupon));

        CouponValidateResponse response = couponService.validateCoupon("PERCENT10", new BigDecimal("200000"));

        assertFalse(response.isValid());
        assertEquals("Mã giảm giá đã hết lượt sử dụng", response.getMessage());
    }

    @Test
    void validateCoupon_PercentCapping() {
        when(couponRepository.findByCode("PERCENT10")).thenReturn(Optional.of(percentCoupon));

        // 10% of 600,000 is 60,000, which exceeds maxDiscount (50,000). Should be capped at 50,000.
        CouponValidateResponse response = couponService.validateCoupon("PERCENT10", new BigDecimal("600000"));

        assertTrue(response.isValid());
        assertEquals(0, response.getDiscountAmount().compareTo(new BigDecimal("50000")));
    }
}
