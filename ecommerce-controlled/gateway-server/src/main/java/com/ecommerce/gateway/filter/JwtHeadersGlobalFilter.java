package com.ecommerce.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * JWT Headers Global Filter
 * 
 * Extracts user context from validated JWT and injects downstream headers for ALL routes.
 * 
 * Per docs/security/gateway-security.md §2.1:
 * "After successful validation, gateway MUST forward:
 * - Authorization: Bearer <ORIGINAL_JWT> (preserved automatically)
 * - X-User-Id: <extracted_from_sub>
 * - X-User-Email: <extracted_from_email>
 * - X-User-Roles: <comma_separated_roles>
 * - X-Forwarded-For: <client_ip>
 * - X-Request-Id: <generated_uuid>"
 * 
 * Execution: Runs after Spring Security JWT validation
 * Order: HIGHEST_PRECEDENCE + 100 (after security, before other filters)
 * 
 * Trust boundary: Downstream services MAY trust these headers
 * (per AGENTS.md §8 and gateway-security.md §2.3)
 */
@Component
public class JwtHeadersGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
            .filter(principal -> principal instanceof JwtAuthenticationToken)
            .cast(JwtAuthenticationToken.class)
            .flatMap(authentication -> {
                Jwt jwt = authentication.getToken();
                
                // Extract user context from JWT claims
                String userId = jwt.getClaimAsString("sub");
                String email = jwt.getClaimAsString("email");
                List<String> roles = extractRoles(jwt);
                String rolesString = String.join(",", roles);
                
                // Generate request tracking ID
                String requestId = UUID.randomUUID().toString();
                
                // Get client IP (check X-Forwarded-For first, then remote address)
                String clientIp = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-Forwarded-For");
                if (clientIp == null && exchange.getRequest().getRemoteAddress() != null) {
                    clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                }
                if (clientIp == null) {
                    clientIp = "unknown";
                }
                
                // Inject security headers for downstream services
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId != null ? userId : "anonymous")
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-User-Roles", rolesString)
                    .header("X-Forwarded-For", clientIp)
                    .header("X-Request-Id", requestId)
                    .build();
                
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            })
            .switchIfEmpty(chain.filter(exchange)); // If no JWT (public endpoint), continue without headers
    }

    /**
     * Extract roles from JWT realm_access.roles claim
     * 
     * Keycloak JWT structure:
     * {
     *   "realm_access": {
     *     "roles": ["customer", "admin"]
     *   }
     * }
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        try {
            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof java.util.Map) {
                Object roles = ((java.util.Map<String, Object>) realmAccess).get("roles");
                if (roles instanceof List) {
                    return (List<String>) roles;
                }
            }
        } catch (Exception e) {
            // Log warning but don't fail - roles are optional
            // In production, consider using a logger here
        }
        return List.of();
    }

    @Override
    public int getOrder() {
        // Run after Spring Security filters (which are at HIGHEST_PRECEDENCE)
        // but before other gateway filters
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
