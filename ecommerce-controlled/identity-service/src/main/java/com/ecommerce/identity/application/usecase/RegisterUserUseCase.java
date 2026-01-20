package com.ecommerce.identity.application.usecase;

import com.ecommerce.identity.domain.port.KeycloakPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Use case for registering new user.
 * Creates user in Keycloak with default 'customer' role.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterUserUseCase {
    
    private final KeycloakPort keycloakPort;
    
    /**
     * Execute user registration
     * 
     * @param email User email
     * @param username Username
     * @param password Password
     * @param firstName First name (optional)
     * @param lastName Last name (optional)
     * @return Created user ID
     */
    public UUID execute(String email, String username, String password, String firstName, String lastName) {
        log.info("Registering new user: email={}, username={}", email, username);
        
        UUID userId = keycloakPort.createUser(email, username, password, firstName, lastName);
        
        log.info("User registered successfully: userId={}, email={}", userId, email);
        return userId;
    }
}
