package com.dienmay.entity.nhom5.repository;

import com.dienmay.entity.nhom5.entity.UserAddress;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUser_Uid(String uid);

    void deleteByUser_Uid(String uid);
}