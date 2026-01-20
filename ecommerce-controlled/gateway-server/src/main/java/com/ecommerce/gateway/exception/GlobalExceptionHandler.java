package com.ecommerce.gateway.exception;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.time.Instant;
import java.util.Map;

/**
 * Global Exception Handler for API Gateway
 * 
 * Standardizes error responses for JWT authentication/authorization failures.
 * 
 * Per docs/security/gateway-security.md §1.2:
 * - Token missing → 401 Unauthorized
 * - Token expired → 401 Unauthorized
 * - Invalid signature → 401 Unauthorized
 * - Invalid issuer → 401 Unauthorized
 * - Insufficient roles → 403 Forbidden
 * 
 * Error response format (JSON):
 * {
 *   "timestamp": "2026-01-20T10:30:00Z",
 *   "status": 401,
 *   "error": "Unauthorized",
 *   "message": "JWT token is expired or invalid",
 *   "path": "/api/v1/orders"
 * }
 * 
 * Note: Gateway does NOT expose internal error details for security reasons
 */
@Component
public class GlobalExceptionHandler extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(request, options);
        Throwable error = getError(request);
        
        // Customize error response based on exception type
        if (error instanceof AuthenticationException) {
            errorAttributes.put("status", HttpStatus.UNAUTHORIZED.value());
            errorAttributes.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
            errorAttributes.put("message", "JWT token is expired or invalid");
        } else if (error instanceof AccessDeniedException) {
            errorAttributes.put("status", HttpStatus.FORBIDDEN.value());
            errorAttributes.put("error", HttpStatus.FORBIDDEN.getReasonPhrase());
            errorAttributes.put("message", "Insufficient permissions to access this resource");
        }
        
        // Add timestamp in ISO-8601 format
        errorAttributes.put("timestamp", Instant.now().toString());
        
        // Remove internal error details (stack trace, exception class)
        errorAttributes.remove("trace");
        errorAttributes.remove("exception");
        
        return errorAttributes;
    }
}
