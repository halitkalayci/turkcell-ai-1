package com.ecommerce.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Header Sanitization Filter
 * 
 * Security responsibility: Prevent header injection attacks
 * 
 * Removes client-injected security headers BEFORE any processing.
 * This ensures clients cannot forge identity by injecting X-User-* headers.
 * 
 * Per docs/security/gateway-security.md §2.2:
 * "Gateway MUST remove these headers from client requests"
 * 
 * Execution order: HIGHEST_PRECEDENCE (runs first)
 * 
 * Stripped headers:
 * - X-User-Id
 * - X-User-Email
 * - X-User-Roles
 * - X-Request-Id (will be regenerated later)
 */
@Component
public class HeaderSanitizationFilter implements GlobalFilter, Ordered {

    private static final String[] SECURITY_HEADERS_TO_STRIP = {
        "X-User-Id",
        "X-User-Email",
        "X-User-Roles",
        "X-Request-Id"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Remove all security headers from incoming request
        ServerHttpRequest.Builder mutatedRequest = request.mutate();
        for (String header : SECURITY_HEADERS_TO_STRIP) {
            mutatedRequest.headers(headers -> headers.remove(header));
        }
        
        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(mutatedRequest.build())
            .build();
        
        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        // Run before all other filters (highest priority)
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
