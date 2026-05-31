package com.dienmay.entity.nhom5.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageResponse {
    private Long id;
    private String senderName;
    private String senderEmail;
    private String senderPhone;
    private String subject;
    private String message;
    private String status;
    private String replyContent;
    private String repliedAt;
    private boolean hasReply;
    private String createdAt;
    private boolean isFromRegisteredUser;
}
