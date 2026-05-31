package com.dienmay.entity.nhom5.controller;

import com.dienmay.entity.nhom5.config.FirebaseProperties;
import com.dienmay.entity.nhom5.entity.User;
import com.dienmay.entity.nhom5.exception.ResourceNotFoundException;
import com.dienmay.entity.nhom5.service.AuthService;
import com.dienmay.entity.nhom5.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final FirebaseProperties firebaseProperties;

    public AuthController(AuthService authService, FirebaseProperties firebaseProperties) {
        this.authService = authService;
        this.firebaseProperties = firebaseProperties;
    }

    @GetMapping("/firebase-config")
    public ResponseEntity<Map<String, String>> firebaseConfig() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("apiKey", firebaseProperties.getApiKey());
        config.put("authDomain", firebaseProperties.getAuthDomain());
        config.put("projectId", firebaseProperties.getProjectId());
        return ResponseEntity.ok(config);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String token = body.getOrDefault("idToken", "");
        if (token.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "missing_token",
                    "message", "Thiếu idToken"
            ));
        }

        try {
            User user;
            try {
                user = authService.verifyAndLoadUser(token);
            } catch (ResourceNotFoundException ex) {
                // First-time social login: create/sync local user from Firebase token claims.
                user = authService.syncUser(token);
            }
            CustomUserDetails principal = CustomUserDetails.fromUser(user);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            new HttpSessionSecurityContextRepository().saveContext(context, request, response);
            return ResponseEntity.ok(buildUserResponse(user));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "invalid_token",
                    "message", ex.getMessage()
            ));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String uid = body.getOrDefault("uid", "");
        String email = body.getOrDefault("email", "");
        String fullName = body.getOrDefault("fullName", "");
        String phone = body.getOrDefault("phone", "");
        String avatarUrl = body.getOrDefault("avatarUrl", "");

        if (uid.isBlank() || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "missing_fields",
                    "message", "Thiếu uid hoặc email"
            ));
        }

        User user = authService.createLocalUser(uid, email, fullName, phone, avatarUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildUserResponse(user));
    }

    private Map<String, Object> buildUserResponse(User user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("uid", user.getUid());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("phone", user.getPhone());
        response.put("avatarUrl", user.getAvatarUrl());
        response.put("role", user.getRole() == null ? "CUSTOMER" : user.getRole().name());
        response.put("createdAt", user.getCreatedAt());
        return response;
    }

}