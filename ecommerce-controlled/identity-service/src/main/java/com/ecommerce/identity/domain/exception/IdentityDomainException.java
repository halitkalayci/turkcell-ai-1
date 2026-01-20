package com.ecommerce.identity.domain.exception;

/**
 * Base exception for all identity domain exceptions
 */
public class IdentityDomainException extends RuntimeException {
    
    public IdentityDomainException(String message) {
        super(message);
    }
    
    public IdentityDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
