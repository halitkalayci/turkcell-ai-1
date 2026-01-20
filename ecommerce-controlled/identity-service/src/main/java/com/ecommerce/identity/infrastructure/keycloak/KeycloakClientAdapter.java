package com.ecommerce.identity.infrastructure.keycloak;

import com.ecommerce.identity.domain.exception.*;
import com.ecommerce.identity.domain.model.AuthToken;
import com.ecommerce.identity.domain.model.Role;
import com.ecommerce.identity.domain.model.User;
import com.ecommerce.identity.domain.port.KeycloakPort;
import com.ecommerce.identity.infrastructure.config.KeycloakProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Keycloak adapter implementing KeycloakPort.
 * Handles all Keycloak Admin API and Token Endpoint interactions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakClientAdapter implements KeycloakPort {
    
    private final Keycloak keycloakAdminClient;
    private final KeycloakProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Override
    public UUID createUser(String email, String username, String password, String firstName, String lastName) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(properties.getRealm());
            UsersResource usersResource = realmResource.users();
            
            // Check if user already exists
            List<UserRepresentation> existingUsers = usersResource.search(email, true);
            if (!existingUsers.isEmpty()) {
                throw new UserAlreadyExistsException("User with email " + email + " already exists");
            }
            
            // Create user representation
            UserRepresentation user = new UserRepresentation();
            user.setEmail(email);
            user.setUsername(username);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEnabled(true);
            user.setEmailVerified(true); // Auto-verify for MVP
            
            // Set password
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(false);
            user.setCredentials(List.of(credential));
            
            // Create user
            Response response = usersResource.create(user);
            
            if (response.getStatus() != 201) {
                String errorBody = response.readEntity(String.class);
                log.error("Failed to create user: status={}, body={}", response.getStatus(), errorBody);
                throw new KeycloakServiceException("Failed to create user in Keycloak: HTTP " + response.getStatus() + " - " + errorBody);
            }
            
            // Extract user ID from Location header
            String locationHeader = response.getHeaderString("Location");
            String userId = locationHeader.substring(locationHeader.lastIndexOf('/') + 1);
            UUID userUuid = UUID.fromString(userId);
            
            // Assign default 'customer' role
            assignRoleToUser(userUuid, "customer");
            
            log.info("User created successfully in Keycloak: userId={}, email={}", userId, email);
            return userUuid;
            
        } catch (UserAlreadyExistsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating user in Keycloak", e);
            throw new KeycloakServiceException("Failed to create user in Keycloak", e);
        }
    }
    
    @Override
    public AuthToken authenticate(String email, String password) {
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", properties.getAdminClientId());
            body.add("client_secret", properties.getAdminClientSecret());
            body.add("username", email);
            body.add("password", password);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                properties.getTokenEndpoint(),
                request,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();
                
                String accessToken = (String) tokenResponse.get("access_token");
                String refreshToken = (String) tokenResponse.get("refresh_token");
                Integer expiresIn = (Integer) tokenResponse.get("expires_in");
                
                // Extract userId from access token (simplified - parse JWT sub claim)
                String userId = extractUserIdFromToken(accessToken);
                
                return AuthToken.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(expiresIn)
                    .tokenType("Bearer")
                    .userId(userId)
                    .build();
            } else {
                throw new InvalidCredentialsException("Authentication failed");
            }
            
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Invalid credentials for user: {}", email);
            throw new InvalidCredentialsException("Invalid credentials");
        } catch (InvalidCredentialsException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Error authenticating with Keycloak", e);
            throw new KeycloakServiceException("Keycloak service unavailable", e);
        }
    }
    
    @Override
    public AuthToken refreshToken(String refreshToken) {
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", properties.getAdminClientId());
            body.add("client_secret", properties.getAdminClientSecret());
            body.add("refresh_token", refreshToken);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                properties.getTokenEndpoint(),
                request,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();
                
                String accessToken = (String) tokenResponse.get("access_token");
                String newRefreshToken = (String) tokenResponse.get("refresh_token");
                Integer expiresIn = (Integer) tokenResponse.get("expires_in");
                
                String userId = extractUserIdFromToken(accessToken);
                
                return AuthToken.builder()
                    .accessToken(accessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(expiresIn)
                    .tokenType("Bearer")
                    .userId(userId)
                    .build();
            } else {
                throw new InvalidTokenException("Token refresh failed");
            }
            
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Invalid refresh token");
            throw new InvalidTokenException("Invalid or expired refresh token");
        } catch (InvalidTokenException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Error refreshing token with Keycloak", e);
            throw new KeycloakServiceException("Keycloak service unavailable", e);
        }
    }
    
    @Override
    public User getUserById(UUID userId) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(properties.getRealm());
            UsersResource usersResource = realmResource.users();
            
            UserRepresentation userRep = usersResource.get(userId.toString()).toRepresentation();
            
            if (userRep == null) {
                throw new UserNotFoundException("User not found: " + userId);
            }
            
            // Get user roles
            List<RoleRepresentation> realmRoles = usersResource.get(userId.toString())
                .roles()
                .realmLevel()
                .listAll();
            
            List<Role> roles = realmRoles.stream()
                .map(r -> Role.fromString(r.getName()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            return User.builder()
                .userId(userId)
                .email(userRep.getEmail())
                .username(userRep.getUsername())
                .firstName(userRep.getFirstName())
                .lastName(userRep.getLastName())
                .roles(roles)
                .emailVerified(userRep.isEmailVerified() != null ? userRep.isEmailVerified() : false)
                .createdAt(userRep.getCreatedTimestamp() != null ? 
                    Instant.ofEpochMilli(userRep.getCreatedTimestamp()) : Instant.now())
                .build();
            
        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching user from Keycloak: userId={}", userId, e);
            throw new KeycloakServiceException("Failed to fetch user from Keycloak", e);
        }
    }
    
    // Helper methods
    
    private void assignRoleToUser(UUID userId, String roleName) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(properties.getRealm());
            
            // Get role
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            
            // Assign role to user
            realmResource.users().get(userId.toString()).roles().realmLevel().add(List.of(role));
            
            log.debug("Assigned role '{}' to user: {}", roleName, userId);
        } catch (Exception e) {
            log.warn("Failed to assign role '{}' to user: {}", roleName, userId, e);
            // Don't fail user creation if role assignment fails
        }
    }
    
    private String extractUserIdFromToken(String accessToken) {
        try {
            // Simple JWT parsing - extract payload (between first and second dot)
            String[] parts = accessToken.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                
                // Extract "sub" claim (user ID)
                if (payload.contains("\"sub\"")) {
                    int start = payload.indexOf("\"sub\":\"") + 7;
                    int end = payload.indexOf("\"", start);
                    if (start > 6 && end > start) {
                        return payload.substring(start, end);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract userId from token", e);
        }
        return null;
    }
}
