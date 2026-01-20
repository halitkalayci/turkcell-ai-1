package com.ecommerce.identity.application.usecase;

import com.ecommerce.identity.domain.model.User;
import com.ecommerce.identity.domain.port.KeycloakPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Use case for getting user profile.
 * Retrieves user information from Keycloak.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetUserProfileUseCase {
    
    private final KeycloakPort keycloakPort;
    
    /**
     * Execute get user profile
     * 
     * @param userId User ID from JWT token
     * @return User profile
     */
    public User execute(UUID userId) {
        log.debug("Fetching user profile: userId={}", userId);
        
        User user = keycloakPort.getUserById(userId);
        
        log.debug("User profile retrieved: userId={}, email={}", userId, user.getEmail());
        return user;
    }
}
