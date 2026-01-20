package com.ecommerce.identity.domain.port;

import com.ecommerce.identity.domain.model.AuthToken;
import com.ecommerce.identity.domain.model.User;

import java.util.UUID;

/**
 * Port interface for Keycloak integration.
 * Infrastructure layer implements this interface.
 * Application layer depends on this port (hexagonal architecture).
 */
public interface KeycloakPort {
    
    /**
     * Create new user in Keycloak
     * 
     * @param email User email
     * @param username Username
     * @param password Password
     * @param firstName First name (optional)
     * @param lastName Last name (optional)
     * @return Created user ID
     * @throws UserAlreadyExistsException if user with email/username exists
     * @throws KeycloakServiceException if Keycloak is unavailable
     */
    UUID createUser(String email, String username, String password, String firstName, String lastName);
    
    /**
     * Authenticate user with email and password
     * 
     * @param email User email
     * @param password Password
     * @return Authentication token (access + refresh)
     * @throws InvalidCredentialsException if credentials are invalid
     * @throws KeycloakServiceException if Keycloak is unavailable
     */
    AuthToken authenticate(String email, String password);
    
    /**
     * Refresh access token using refresh token
     * 
     * @param refreshToken Valid refresh token
     * @return New authentication token
     * @throws InvalidTokenException if refresh token is invalid/expired
     * @throws KeycloakServiceException if Keycloak is unavailable
     */
    AuthToken refreshToken(String refreshToken);
    
    /**
     * Get user information by user ID
     * 
     * @param userId User ID
     * @return User information
     * @throws UserNotFoundException if user not found
     * @throws KeycloakServiceException if Keycloak is unavailable
     */
    User getUserById(UUID userId);
}
