package com.dienmay.entity.nhom5.controller;

import com.dienmay.entity.nhom5.dto.response.CouponValidateResponse;
import com.dienmay.entity.nhom5.service.CouponService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/validate")
    public ResponseEntity<CouponValidateResponse> validate(@Valid @RequestBody CouponValidateRequest request) {
        CouponValidateResponse response = couponService.validateCoupon(request.getCode(), request.getOrderTotal());
        return ResponseEntity.ok(response);
    }

    @Data
    public static class CouponValidateRequest {
        @NotBlank(message = "Mã coupon không được để trống")
        private String code;

        @NotNull(message = "Tổng đơn hàng là bắt buộc")
        private BigDecimal orderTotal;
    }
}
