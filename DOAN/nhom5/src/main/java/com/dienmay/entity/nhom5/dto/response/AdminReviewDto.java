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
public class AdminReviewDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private String userUid;
    private String userName;
    private String userEmail;
    private Integer rating;
    private String comment;
    private String status;
    private LocalDateTime createdAt;
}
