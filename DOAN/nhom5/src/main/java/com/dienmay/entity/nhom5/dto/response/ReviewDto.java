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
public class ReviewDto {
    private Long id;
    private Integer rating;
    private String comment;
    private String userName;
    private String userAvatarUrl;
    private LocalDateTime createdAt;
}
