package com.ecommerce.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;

/**
 * Security Configuration for API Gateway
 * 
 * Responsibilities:
 * - JWT validation via OAuth2 Resource Server
 * - Public endpoint definitions
 * - Protected route enforcement (authentication-only)
 * - CORS integration
 * 
 * Per AGENTS.md:
 * - Gateway contains NO business logic
 * - Gateway validates JWT at entry point
 * - Downstream services trust gateway-injected headers
 * 
 * Per docs/security/gateway-security.md:
 * - Validate: issuer, signature, expiration
 * - Extract: sub, email, realm_access.roles
 * - Forward: X-User-Id, X-User-Email, X-User-Roles headers
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Security filter chain for JWT validation
     * 
     * Public endpoints (no authentication required):
     * - /actuator/health - Health checks
     * - /api/v1/identity/login - User login
     * - /api/v1/identity/register - User registration
     * - /api/v1/identity/refresh - Token refresh
     * - /v3/api-docs/** - OpenAPI documentation
     * - /swagger-ui.html - Swagger UI entry
     * - /swagger-ui/** - Swagger UI assets
     * - /webjars/** - Swagger UI dependencies
     * 
     * Protected endpoints (authentication required):
     * - All other routes
     * 
     * Authorization strategy: Authentication-only (no role checks)
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            // Disable CSRF for stateless JWT authentication
            .csrf(csrf -> csrf.disable())
            
            // Disable HTTP Basic (prevents default 401 challenge on public endpoints)
            .httpBasic(httpBasic -> httpBasic.disable())
            
            // Disable form login
            .formLogin(formLogin -> formLogin.disable())
            
            // Enable anonymous authentication for public endpoints
            .anonymous(anonymous -> anonymous.principal("anonymousUser"))
            
            // Authorization rules
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints - no authentication
                .pathMatchers(HttpMethod.GET, "/actuator/**").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/identity/login").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/identity/register").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/identity/refresh").permitAll()
                
                // Swagger/OpenAPI public access
                .pathMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/webjars/**"
                ).permitAll()
                
                // All other endpoints require authentication
                .anyExchange().authenticated()
            )
            
            // OAuth2 Resource Server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {
                    // JWT validation configured in application.yml:
                    // - issuer-uri: http://localhost:8181/realms/example
                    // - jwk-set-uri: http://localhost:8181/realms/example/protocol/openid-connect/certs
                })
                // Return 401 for missing/invalid JWT on protected endpoints
                .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
            );

        return http.build();
    }
}
