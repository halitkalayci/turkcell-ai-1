package com.ecommerce.identity.domain.exception;

/**
 * Thrown when user is not found
 */
public class UserNotFoundException extends IdentityDomainException {
    
    public UserNotFoundException(String message) {
        super(message);
    }
}
