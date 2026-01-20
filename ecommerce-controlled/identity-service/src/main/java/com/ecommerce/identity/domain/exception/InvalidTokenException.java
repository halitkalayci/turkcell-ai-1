package com.ecommerce.identity.domain.exception;

/**
 * Thrown when token is invalid or expired
 */
public class InvalidTokenException extends IdentityDomainException {
    
    public InvalidTokenException(String message) {
        super(message);
    }
    
    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
