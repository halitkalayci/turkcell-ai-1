package com.ecommerce.identity.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * User domain model representing authenticated user information.
 * This is NOT a JPA entity - it's a pure domain model.
 * Data comes from Keycloak JWT claims or Admin API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    private UUID userId;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private List<Role> roles;
    private boolean emailVerified;
    private Instant createdAt;
    
    /**
     * Check if user has specific role
     */
    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }
    
    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }
    
    /**
     * Check if user is customer
     */
    public boolean isCustomer() {
        return hasRole(Role.CUSTOMER);
    }
    
    /**
     * Get full name
     */
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return username;
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
}
