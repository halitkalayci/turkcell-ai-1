package com.ecommerce.identity.domain.exception;

/**
 * Thrown when user with same email/username already exists
 */
public class UserAlreadyExistsException extends IdentityDomainException {
    
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
