package com.authmat.application.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant errorTimestamp,
        String message,
        int statusCode,
        String requestPath,
        Map<String,String> validationErrors) { }
