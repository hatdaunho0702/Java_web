package com.dienmay.entity.nhom5.service;

import com.dienmay.entity.nhom5.entity.Role;
import com.dienmay.entity.nhom5.entity.User;
import com.dienmay.entity.nhom5.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    @Transactional
    public User syncUser(String firebaseUid, String email, String displayName, String photoUrl) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> User.builder()
                        .firebaseUid(firebaseUid)
                        .email(email)
                        .fullName(displayName != null && !displayName.isBlank() ? displayName : email)
                        .avatarUrl(photoUrl)
                        .role(Role.CUSTOMER)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());

        if (user.getId() == null) {
            return userRepository.save(user);
        }

        user.setUpdatedAt(LocalDateTime.now());
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            user.setEmail(email);
        }
        if (user.getFirebaseUid() == null || user.getFirebaseUid().isBlank()) {
            user.setFirebaseUid(firebaseUid);
        }
        return userRepository.save(user);
    }

    public String syncUser(String idToken) {
        return idToken;
    }
}
