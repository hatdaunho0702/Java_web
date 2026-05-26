package com.dienmay.entity.nhom5.repository;

import com.dienmay.entity.nhom5.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByFirebaseUid(String uid);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
