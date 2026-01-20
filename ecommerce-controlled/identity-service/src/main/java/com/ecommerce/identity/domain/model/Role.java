package com.ecommerce.identity.domain.model;

/**
 * User roles matching Keycloak realm roles.
 * Must match role names configured in Keycloak (realm: example).
 */
public enum Role {
    CUSTOMER,           // Standard user role
    ADMIN,              // System administrator
    ORDER_MANAGER,      // Can manage all orders
    INVENTORY_MANAGER,  // Can manage inventory
    SERVICE_ACCOUNT;    // Service-to-service authentication
    
    /**
     * Convert from Keycloak role string to enum
     */
    public static Role fromString(String roleStr) {
        if (roleStr == null) {
            return null;
        }
        
        // Handle lowercase from Keycloak
        String normalized = roleStr.toUpperCase().replace("-", "_");
        
        try {
            return Role.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Unknown role - ignore or log warning
            return null;
        }
    }
    
    /**
     * Convert to Keycloak role string
     */
    public String toKeycloakRole() {
        return this.name().toLowerCase().replace("_", "-");
    }
}
