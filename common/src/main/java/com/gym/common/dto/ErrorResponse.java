package com.gym.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response format across all microservices
 *
 * Example response:
 * {
 *   "status": "BAD_REQUEST",
 *   "message": "Invalid request",
 *   "errors": [
 *     {
 *       "field": "name",
 *       "message": "must not be blank"
 *     }
 *   ],
 *   "timestamp": "2025-03-18T10:30:00",
 *   "path": "/api/v1/exercises"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    /**
     * HTTP status code as string (e.g., "BAD_REQUEST", "NOT_FOUND", "UNAUTHORIZED")
     */
    private String status;
    
    /**
     * Main error message
     */
    private String message;
    
    /**
     * List of field-level validation errors
     */
    private List<FieldError> errors;
    
    /**
     * Timestamp when error occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * Request path that caused the error
     */
    private String path;
    
    /**
     * Trace ID for distributed tracing (injected by API Gateway)
     */
    private String traceId;
    
    /**
     * Nested class for field-level errors
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldError {
        /**
         * Field name that has validation error
         */
        private String field;
        
        /**
         * Error message for this field
         */
        private String message;
        
        /**
         * Rejected value (optional)
         */
        private Object rejectedValue;
    }
}
