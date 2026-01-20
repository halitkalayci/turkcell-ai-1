package com.ecommerce.identity.application.usecase;

import com.ecommerce.identity.domain.model.AuthToken;
import com.ecommerce.identity.domain.port.KeycloakPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use case for refreshing access token.
 * Exchanges refresh token for new access token.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenUseCase {
    
    private final KeycloakPort keycloakPort;
    
    /**
     * Execute token refresh
     * 
     * @param refreshToken Valid refresh token
     * @return New authentication token
     */
    public AuthToken execute(String refreshToken) {
        log.debug("Token refresh attempt");
        
        AuthToken token = keycloakPort.refreshToken(refreshToken);
        
        log.debug("Token refreshed successfully: userId={}", token.getUserId());
        return token;
    }
}
