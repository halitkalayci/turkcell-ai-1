package com.ecommerce.identity.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication token containing access token and refresh token.
 * Returned after successful login or token refresh.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthToken {
    
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn;      // Access token lifetime in seconds
    private String tokenType;        // "Bearer"
    private String userId;           // Extracted from token
}
