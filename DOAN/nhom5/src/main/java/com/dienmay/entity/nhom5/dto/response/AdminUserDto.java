package com.dienmay.entity.nhom5.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    private String uid;
    private String fullName;
    private String email;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private Boolean isActive;
    private Long orderCount;
}
