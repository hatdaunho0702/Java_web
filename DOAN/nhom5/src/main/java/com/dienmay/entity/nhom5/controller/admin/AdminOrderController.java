package com.dienmay.entity.nhom5.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    @GetMapping
    public ResponseEntity<String> dashboardOrders() {
        return ResponseEntity.ok("admin orders endpoint");
    }
}
