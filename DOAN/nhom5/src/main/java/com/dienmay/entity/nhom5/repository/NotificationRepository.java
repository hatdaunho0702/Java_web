package com.dienmay.entity.nhom5.repository;

import com.dienmay.entity.nhom5.entity.Notification;
import com.dienmay.entity.nhom5.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    long countByTypeAndIsRead(NotificationType type, Boolean isRead);
}
