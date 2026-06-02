package com.dienmay.entity.nhom5.controller;

import com.dienmay.entity.nhom5.service.AiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody List<Map<String, String>> history) {
        String reply = aiChatService.generateReply(history);
        return ResponseEntity.ok(Map.of("reply", reply));
    }
}
