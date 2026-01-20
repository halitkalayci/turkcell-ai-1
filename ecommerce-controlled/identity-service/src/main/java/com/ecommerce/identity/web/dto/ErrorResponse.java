package com.ecommerce.identity.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Error response DTO matching OpenAPI contract
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    private Instant timestamp;
    private Integer status;
    private String error;
    private String message;
    private String path;
    private List<String> details;
}
