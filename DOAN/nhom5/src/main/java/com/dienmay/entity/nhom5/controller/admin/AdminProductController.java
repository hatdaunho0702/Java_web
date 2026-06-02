package com.dienmay.entity.nhom5.controller.admin;

import com.dienmay.entity.nhom5.dto.request.CreateProductRequest;
import com.dienmay.entity.nhom5.dto.response.ProductDetailResponse;
import com.dienmay.entity.nhom5.exception.ResourceNotFoundException;
import com.dienmay.entity.nhom5.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Slf4j
public class AdminProductController {

    private final ProductService productService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProduct(
            @RequestPart("data") @Valid CreateProductRequest req,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {
        try {
            ProductDetailResponse res = productService.createProduct(req, images);
            return ResponseEntity.status(HttpStatus.CREATED).body(res);
        } catch (Exception e) {
            log.error("Create product error", e);
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestPart("data") @Valid CreateProductRequest req,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {
        try {
            ProductDetailResponse res = productService.updateProduct(id, req, images);
            return ResponseEntity.ok(res);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Update product error id={}", id, e);
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            String message = productService.deleteProduct(id);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Delete product error id={}", id, e);
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<?> toggleActive(@PathVariable Long id) {
        try {
            productService.toggleActive(id);
            return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái sản phẩm"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Toggle active error id={}", id, e);
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }
}
