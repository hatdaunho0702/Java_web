package com.dienmay.entity.nhom5.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContactMessageRequest {
    @NotBlank(message = "Vui lòng nhập họ tên")
    private String senderName;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    private String senderEmail;

    private String senderPhone;

    @NotBlank(message = "Vui lòng nhập chủ đề")
    private String subject;

    @NotBlank(message = "Vui lòng nhập tin nhắn")
    @Size(min = 10, message = "Tin nhắn tối thiểu 10 ký tự")
    private String message;
}
