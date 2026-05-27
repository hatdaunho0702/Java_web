package com.dienmay.entity.nhom5.service;

import com.dienmay.entity.nhom5.exception.ResourceNotFoundException;
import com.dienmay.entity.nhom5.entity.Role;
import com.dienmay.entity.nhom5.entity.User;
import com.dienmay.entity.nhom5.repository.UserRepository;
import java.time.LocalDateTime;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional // Đảm bảo đồng bộ an toàn xuống SQLite
public class AuthService {

    private final UserRepository userRepository;

    public FirebaseToken verifyFirebaseToken(String idToken) {
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (Exception ex) {
            log.warn("Lỗi giải mã Firebase idToken: {}", ex.getMessage());
            throw new IllegalArgumentException("Invalid Firebase idToken");
        }
    }

    public User createLocalUser(String uid, String email, String fullName, String phone) {
        return createLocalUser(uid, email, fullName, phone, null);
    }

    public User createLocalUser(String uid, String email, String fullName, String phone, String avatarUrl) {
        String normalizedUid = normalize(uid);
        String normalizedEmail = normalize(email);
        String normalizedFullName = normalize(fullName);
        String normalizedPhone = normalize(phone);
        String normalizedAvatarUrl = normalize(avatarUrl);

        if (normalizedUid.isBlank()) {
            throw new IllegalArgumentException("uid is required");
        }

        User user = userRepository.findByUid(normalizedUid)
                .orElseGet(() -> User.builder()
                        .uid(normalizedUid)
                        .email(normalizedEmail)
                        .fullName(normalizedFullName.isBlank() ? normalizedEmail : normalizedFullName)
                        .phone(normalizedPhone)
                        .avatarUrl(normalizedAvatarUrl)
                    .role(Role.CUSTOMER)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());

        if (!normalizedEmail.isBlank()) {
            user.setEmail(normalizedEmail);
        }
        if (!normalizedFullName.isBlank()) {
            user.setFullName(normalizedFullName);
        } else if (user.getFullName() == null || user.getFullName().isBlank()) {
            user.setFullName(normalizedEmail);
        }
        if (!normalizedPhone.isBlank()) {
            user.setPhone(normalizedPhone);
        }
        if (!normalizedAvatarUrl.isBlank()) {
            user.setAvatarUrl(normalizedAvatarUrl);
        }
        if (user.getRole() == null) {
            user.setRole(Role.CUSTOMER);
        }
        if (user.getIsActive() == null) {
            user.setIsActive(true);
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public User loadUserByUid(String uid) {
        String normalizedUid = normalize(uid);
        if (normalizedUid.isBlank()) {
            throw new IllegalArgumentException("uid is required");
        }
        return userRepository.findByUid(normalizedUid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User verifyAndLoadUser(String idToken) {
        FirebaseToken decoded = verifyFirebaseToken(idToken);
        User user = loadUserByUid(decoded.getUid());
        log.info("Firebase token hợp lệ. Đã nạp user SQLite cho uid={}", decoded.getUid());
        return user;
    }

    public User syncUser(String firebaseUid, String email, String displayName, String photoUrl) {
        return createLocalUser(firebaseUid, email, displayName, null, photoUrl);
    }

    public User syncUser(String idToken) {
        FirebaseToken decoded = verifyFirebaseToken(idToken);
        return createLocalUser(decoded.getUid(), decoded.getEmail(), decoded.getName(), null, decoded.getPicture());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}