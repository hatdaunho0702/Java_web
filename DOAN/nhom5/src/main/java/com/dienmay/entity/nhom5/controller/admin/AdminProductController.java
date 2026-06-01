package com.dienmay.entity.nhom5.controller.admin;

import com.dienmay.entity.nhom5.dto.request.CreateProductRequest;
import com.dienmay.entity.nhom5.dto.response.ProductDetailResponse;
import com.dienmay.entity.nhom5.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDetailResponse> createProduct(
            @RequestParam("product") String productJson,
            @RequestParam(value = "images", required = false) MultipartFile[] images) throws Exception {
        
        ObjectMapper mapper = new ObjectMapper();
        CreateProductRequest req = mapper.readValue(productJson, CreateProductRequest.class);
        
        ProductDetailResponse res = productService.createProduct(req, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDetailResponse> updateProduct(
            @PathVariable Long id,
            @RequestParam("product") String productJson,
            @RequestParam(value = "images", required = false) MultipartFile[] images) throws Exception {
        
        ObjectMapper mapper = new ObjectMapper();
        CreateProductRequest req = mapper.readValue(productJson, CreateProductRequest.class);
        
        ProductDetailResponse res = productService.updateProduct(id, req, images);
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
