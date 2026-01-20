package com.ecommerce.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refresh token request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshRequest {
    
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
