package com.dienmay.entity.nhom5.controller.admin;

import com.dienmay.entity.nhom5.dto.request.ReplyMessageRequest;
import com.dienmay.entity.nhom5.entity.ContactStatus;
import com.dienmay.entity.nhom5.service.ContactMessageService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/contact")
@RequiredArgsConstructor
public class AdminContactController {

    private final ContactMessageService contactMessageService;

    // Lấy danh sách (filter theo status)
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ContactStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = ContactStatus.valueOf(status.toUpperCase());
            } catch (Exception e) {
                /* ignore */
            }
        }
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(contactMessageService.getAll(st, pageable));
    }

    // Xem chi tiết (đánh dấu đã đọc)
    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(contactMessageService.getDetail(id));
    }

    // Trả lời
    @PostMapping("/{id}/reply")
    public ResponseEntity<?> reply(
            @PathVariable Long id,
            @Valid @RequestBody ReplyMessageRequest req) {
        String adminUid = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(contactMessageService.reply(id, adminUid, req));
    }

    // Đổi trạng thái
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        ContactStatus st = ContactStatus.valueOf(body.get("status").toUpperCase());
        contactMessageService.updateStatus(id, st);
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái"));
    }

    // Xóa tin nhắn
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        contactMessageService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Badge count (cho navbar admin)
    @GetMapping("/badge")
    public ResponseEntity<?> getBadge() {
        return ResponseEntity.ok(contactMessageService.getBadge());
    }
}
