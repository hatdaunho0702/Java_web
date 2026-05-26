package com.dienmay.entity.nhom5.controller;

import com.dienmay.entity.nhom5.service.AuthService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/sync-user")
    public ResponseEntity<Map<String, String>> syncUser(@RequestBody Map<String, String> body) {
        String token = body.getOrDefault("idToken", "");
        String result = authService.syncUser(token);
        return ResponseEntity.ok(Map.of("idToken", result));
    }
}
