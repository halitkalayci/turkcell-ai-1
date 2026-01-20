package com.ecommerce.identity.domain.exception;

/**
 * Thrown when user credentials are invalid
 */
public class InvalidCredentialsException extends IdentityDomainException {
    
    public InvalidCredentialsException(String message) {
        super(message);
    }
    
    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
