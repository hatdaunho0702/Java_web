package com.dienmay.entity.nhom5.exception;

import java.time.LocalDateTime;

public record ErrorResponse(String error, String message, LocalDateTime timestamp) {
}
