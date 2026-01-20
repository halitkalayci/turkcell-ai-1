package com.ecommerce.identity.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refresh token response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshResponse {
    
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn;
    private String tokenType;
}
