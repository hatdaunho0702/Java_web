package com.dienmay.entity.nhom5.repository;

import com.dienmay.entity.nhom5.entity.ContactMessage;
import com.dienmay.entity.nhom5.entity.ContactStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    // Đếm tin chưa đọc (cho badge admin)
    long countByStatus(ContactStatus status);

    // Lấy tất cả, mới nhất trước
    Page<ContactMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Filter theo status
    Page<ContactMessage> findByStatusOrderByCreatedAtDesc(ContactStatus status, Pageable pageable);

    // Lấy theo sender uid (user xem lịch sử)
    List<ContactMessage> findBySenderUidOrderByCreatedAtDesc(String senderUid);

    // Đếm unread của admin (notification)
    @Query("SELECT COUNT(m) FROM ContactMessage m WHERE m.status = com.dienmay.entity.nhom5.entity.ContactStatus.UNREAD")
    long countUnread();
}
