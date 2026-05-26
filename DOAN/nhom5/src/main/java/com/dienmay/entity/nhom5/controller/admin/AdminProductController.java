package com.dienmay.entity.nhom5.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    @GetMapping
    public ResponseEntity<String> dashboardProducts() {
        return ResponseEntity.ok("admin products endpoint");
    }
}
