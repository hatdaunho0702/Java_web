package com.dienmay.entity.nhom5.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReplyMessageRequest {
    @NotBlank(message = "Nội dung trả lời không được trống")
    private String replyContent;
}
