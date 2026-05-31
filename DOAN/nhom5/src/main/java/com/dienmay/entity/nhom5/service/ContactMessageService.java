package com.dienmay.entity.nhom5.service;

import com.dienmay.entity.nhom5.dto.request.ContactMessageRequest;
import com.dienmay.entity.nhom5.dto.request.ReplyMessageRequest;
import com.dienmay.entity.nhom5.dto.response.ContactMessageResponse;
import com.dienmay.entity.nhom5.dto.response.NotificationBadgeResponse;
import com.dienmay.entity.nhom5.entity.ContactMessage;
import com.dienmay.entity.nhom5.entity.ContactStatus;
import com.dienmay.entity.nhom5.entity.Notification;
import com.dienmay.entity.nhom5.entity.NotificationType;
import com.dienmay.entity.nhom5.entity.OrderStatus;
import com.dienmay.entity.nhom5.entity.TargetRole;
import com.dienmay.entity.nhom5.exception.ResourceNotFoundException;
import com.dienmay.entity.nhom5.repository.ContactMessageRepository;
import com.dienmay.entity.nhom5.repository.NotificationRepository;
import com.dienmay.entity.nhom5.repository.OrderRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;
    private final NotificationRepository notificationRepository;
    private final OrderRepository orderRepository;

    // ── GỬI TIN NHẮN (user/guest) ──────────
    @Transactional
    public ContactMessageResponse sendMessage(String senderUid, ContactMessageRequest req) {
        ContactMessage msg = ContactMessage.builder()
                .senderUid(senderUid)
                .senderName(req.getSenderName())
                .senderEmail(req.getSenderEmail())
                .senderPhone(req.getSenderPhone())
                .subject(req.getSubject())
                .message(req.getMessage())
                .status(ContactStatus.UNREAD)
                .build();

        ContactMessage saved = contactMessageRepository.save(msg);

        // Tạo notification cho admin
        String shortMessage = req.getMessage().substring(0, Math.min(50, req.getMessage().length())) + "...";
        Notification notif = Notification.builder()
                .type(NotificationType.SYSTEM)
                .title("Tin nhắn mới từ " + req.getSenderName())
                .message(req.getSubject() + ": " + shortMessage)
                .targetRole(TargetRole.ADMIN)
                .refId(saved.getId())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notif);

        log.info("New contact message from {} id={}", req.getSenderEmail(), saved.getId());
        return mapToResponse(saved);
    }

    // ── ADMIN: LẤY DANH SÁCH ───────────────
    public Page<ContactMessageResponse> getAll(ContactStatus status, Pageable pageable) {
        Page<ContactMessage> page = status != null
                ? contactMessageRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : contactMessageRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(this::mapToResponse);
    }

    // ── ADMIN: XEM CHI TIẾT + ĐÁNH DẤU ĐỌC ─
    @Transactional
    public ContactMessageResponse getDetail(Long id) {
        ContactMessage msg = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin nhắn"));
        
        // Đánh dấu đã đọc
        if (msg.getStatus() == ContactStatus.UNREAD) {
            msg.setStatus(ContactStatus.READ);
            contactMessageRepository.save(msg);
        }
        return mapToResponse(msg);
    }

    // ── ADMIN: TRẢ LỜI ─────────────────────
    @Transactional
    public ContactMessageResponse reply(Long id, String adminUid, ReplyMessageRequest req) {
        ContactMessage msg = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin nhắn"));

        msg.setReplyContent(req.getReplyContent());
        msg.setRepliedAt(LocalDateTime.now());
        msg.setRepliedBy(adminUid);
        msg.setStatus(ContactStatus.REPLIED);
        contactMessageRepository.save(msg);

        log.info("Admin replied to message id={}", id);
        return mapToResponse(msg);
    }

    // ── ADMIN: ĐỔI TRẠNG THÁI ──────────────
    @Transactional
    public void updateStatus(Long id, ContactStatus status) {
        ContactMessage msg = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin nhắn"));
        msg.setStatus(status);
        contactMessageRepository.save(msg);
    }

    // ── ADMIN: XÓA ─────────────────────────
    @Transactional
    public void delete(Long id) {
        if (!contactMessageRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy tin nhắn");
        }
        contactMessageRepository.deleteById(id);
    }

    // ── BADGE COUNT (cho navbar admin) ──────
    public NotificationBadgeResponse getBadge() {
        long unread = contactMessageRepository.countUnread();
        long lowStock = notificationRepository.countByTypeAndIsRead(NotificationType.LOW_STOCK, false);
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);

        return NotificationBadgeResponse.builder()
                .unreadMessages(unread)
                .lowStockCount(lowStock)
                .pendingOrders(pendingOrders)
                .total(unread + lowStock + pendingOrders)
                .build();
    }

    // ── USER: XEM LỊCH SỬ GỬI ──────────────
    public List<ContactMessageResponse> getMyMessages(String uid) {
        return contactMessageRepository.findBySenderUidOrderByCreatedAtDesc(uid)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── MAPPER ──────────────────────────────
    private ContactMessageResponse mapToResponse(ContactMessage msg) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return ContactMessageResponse.builder()
                .id(msg.getId())
                .senderName(msg.getSenderName())
                .senderEmail(msg.getSenderEmail())
                .senderPhone(msg.getSenderPhone())
                .subject(msg.getSubject())
                .message(msg.getMessage())
                .status(msg.getStatus().name())
                .replyContent(msg.getReplyContent())
                .repliedAt(msg.getRepliedAt() != null ? msg.getRepliedAt().format(fmt) : null)
                .hasReply(msg.getReplyContent() != null)
                .createdAt(msg.getCreatedAt() != null ? msg.getCreatedAt().format(fmt) : null)
                .isFromRegisteredUser(msg.getSenderUid() != null)
                .build();
    }
}
