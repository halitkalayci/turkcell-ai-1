package com.ecommerce.identity.application.usecase;

import com.ecommerce.identity.domain.model.AuthToken;
import com.ecommerce.identity.domain.port.KeycloakPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use case for user login.
 * Authenticates user with Keycloak and returns JWT tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginUserUseCase {
    
    private final KeycloakPort keycloakPort;
    
    /**
     * Execute user login
     * 
     * @param email User email
     * @param password Password
     * @return Authentication token
     */
    public AuthToken execute(String email, String password) {
        log.info("User login attempt: email={}", email);
        
        AuthToken token = keycloakPort.authenticate(email, password);
        
        log.info("User logged in successfully: email={}, userId={}", email, token.getUserId());
        return token;
    }
}
