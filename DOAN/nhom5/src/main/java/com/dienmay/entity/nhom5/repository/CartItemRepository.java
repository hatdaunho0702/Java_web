package com.dienmay.entity.nhom5.repository;

import com.dienmay.entity.nhom5.entity.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUser_Uid(String uid);

    Optional<CartItem> findByUser_UidAndProduct_Id(String uid, Long productId);

    void deleteByUser_Uid(String uid);
}
