package com.dienmay.entity.nhom5.controller;

import com.dienmay.entity.nhom5.dto.request.ContactMessageRequest;
import com.dienmay.entity.nhom5.dto.response.ContactMessageResponse;
import com.dienmay.entity.nhom5.service.ContactMessageService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactMessageService contactMessageService;

    // Gửi tin nhắn (ai cũng gửi được)
    // Nếu đã login → gắn uid
    @PostMapping
    public ResponseEntity<?> sendMessage(@Valid @RequestBody ContactMessageRequest req) {
        String uid = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            uid = (String) auth.getPrincipal();
        }

        ContactMessageResponse res = contactMessageService.sendMessage(uid, req);
        return ResponseEntity.status(201).body(Map.of(
            "success", true,
            "message", "Tin nhắn đã được gửi thành công!",
            "id", res.getId()
        ));
    }
    // User đã login xem lịch sử tin nhắn của mình
    @GetMapping("/my-messages")
    public ResponseEntity<?> getMyMessages() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.ok(List.of());
        }
        String uid = (String) auth.getPrincipal();
        return ResponseEntity.ok(contactMessageService.getMyMessages(uid));
    }
}
