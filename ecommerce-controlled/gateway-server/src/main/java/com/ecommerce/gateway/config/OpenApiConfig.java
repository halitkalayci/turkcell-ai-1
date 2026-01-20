package com.ecommerce.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI Configuration for Swagger UI with OAuth2 Authorization
 * 
 * Enables "Authorize" button in Swagger UI for testing JWT-protected endpoints.
 * 
 * Configuration source:
 * - Token URL: Keycloak token endpoint (password grant)
 * - Authorization URL: Keycloak authorization endpoint
 * - Scopes: openid, email, profile
 * 
 * Usage:
 * 1. Open Swagger UI: http://localhost:8080/swagger-ui.html
 * 2. Click "Authorize" button
 * 3. Enter Keycloak credentials
 * 4. Execute protected endpoints with Bearer token
 * 
 * Per AGENTS.md: Gateway contains NO business logic, only routing + security
 */
@Configuration
public class OpenApiConfig {

    @Value("${KEYCLOAK_ISSUER_URI:http://localhost:8181/realms/example}")
    private String issuerUri;

    @Bean
    public OpenAPI customOpenAPI() {
        // Construct Keycloak OAuth2 endpoints
        String tokenUrl = issuerUri + "/protocol/openid-connect/token";
        String authorizationUrl = issuerUri + "/protocol/openid-connect/auth";

        return new OpenAPI()
            .info(new Info()
                .title("E-Commerce API Gateway")
                .version("1.0.0")
                .description("API Gateway for E-Commerce Microservices\n\n" +
                    "**Security:** OAuth2 + JWT\n\n" +
                    "**Keycloak Realm:** example\n\n" +
                    "**Public Endpoints:**\n" +
                    "- POST /api/v1/identity/login\n" +
                    "- POST /api/v1/identity/register\n" +
                    "- POST /api/v1/identity/refresh\n" +
                    "- GET /actuator/health\n\n" +
                    "**Protected Endpoints:** All others (requires valid JWT)")
            )
            .addServersItem(new Server()
                .url("http://localhost:8080")
                .description("Local Gateway")
            )
            .components(new Components()
                .addSecuritySchemes("keycloak_oauth2", new SecurityScheme()
                    .type(SecurityScheme.Type.OAUTH2)
                    .description("Keycloak OAuth2 authentication")
                    .flows(new OAuthFlows()
                        .password(new OAuthFlow()
                            .tokenUrl(tokenUrl)
                            .authorizationUrl(authorizationUrl)
                            .refreshUrl(tokenUrl)
                        )
                    )
                )
                .addSecuritySchemes("bearer_jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT Bearer token (manually obtained from Keycloak)")
                )
            )
            .addSecurityItem(new SecurityRequirement()
                .addList("keycloak_oauth2")
                .addList("bearer_jwt")
            );
    }
}
