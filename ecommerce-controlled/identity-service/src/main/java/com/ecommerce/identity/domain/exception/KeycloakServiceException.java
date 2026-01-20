package com.ecommerce.identity.domain.exception;

/**
 * Thrown when Keycloak service is unavailable or returns error
 */
public class KeycloakServiceException extends IdentityDomainException {
    
    public KeycloakServiceException(String message) {
        super(message);
    }
    
    public KeycloakServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
