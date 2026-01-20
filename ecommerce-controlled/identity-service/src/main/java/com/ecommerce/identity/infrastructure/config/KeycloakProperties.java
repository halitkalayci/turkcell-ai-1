package com.ecommerce.identity.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Keycloak configuration properties.
 * Values provided by human via environment variables.
 */
@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Data
public class KeycloakProperties {
    
    private String realm;
    private String authServerUrl;
    private String adminClientId;
    private String adminClientSecret;
    private String tokenEndpoint;
    private String adminRealm = "master";
}
