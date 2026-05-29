package com.dienmay.entity.nhom5.repository;

import com.dienmay.entity.nhom5.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUid(String uid);

    default Optional<User> findByFirebaseUid(String uid) {
        return findByUid(uid);
    }

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUid(String uid);

    org.springframework.data.domain.Page<User> findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String email, String fullName, org.springframework.data.domain.Pageable pageable
    );
}
