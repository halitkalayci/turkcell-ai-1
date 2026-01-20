package com.ecommerce.identity.web.controller;

import com.ecommerce.identity.application.usecase.GetUserProfileUseCase;
import com.ecommerce.identity.application.usecase.LoginUserUseCase;
import com.ecommerce.identity.application.usecase.RefreshTokenUseCase;
import com.ecommerce.identity.application.usecase.RegisterUserUseCase;
import com.ecommerce.identity.domain.model.AuthToken;
import com.ecommerce.identity.domain.model.User;
import com.ecommerce.identity.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Identity Controller - handles authentication and user profile endpoints
 */
@RestController
@RequestMapping("/api/v1/identity")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Identity Service", description = "User authentication and profile management")
public class IdentityController {
    
    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final GetUserProfileUseCase getUserProfileUseCase;
    
    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Creates a new user in Keycloak with default 'customer' role")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request: email={}, username={}", request.getEmail(), request.getUsername());
        
        UUID userId = registerUserUseCase.execute(
            request.getEmail(),
            request.getUsername(),
            request.getPassword(),
            request.getFirstName(),
            request.getLastName()
        );
        
        RegisterResponse response = RegisterResponse.builder()
            .userId(userId)
            .email(request.getEmail())
            .username(request.getUsername())
            .message("User registered successfully")
            .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user and returns JWT access token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request: email={}", request.getEmail());
        
        AuthToken token = loginUserUseCase.execute(request.getEmail(), request.getPassword());
        
        LoginResponse response = LoginResponse.builder()
            .accessToken(token.getAccessToken())
            .refreshToken(token.getRefreshToken())
            .expiresIn(token.getExpiresIn())
            .tokenType(token.getTokenType())
            .userId(token.getUserId())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Exchanges refresh token for new access token")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        log.debug("Token refresh request");
        
        AuthToken token = refreshTokenUseCase.execute(request.getRefreshToken());
        
        RefreshResponse response = RefreshResponse.builder()
            .accessToken(token.getAccessToken())
            .refreshToken(token.getRefreshToken())
            .expiresIn(token.getExpiresIn())
            .tokenType(token.getTokenType())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    @Operation(
        summary = "Get authenticated user profile",
        description = "Returns profile information for the authenticated user",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserProfileResponse> getUserProfile(Authentication authentication) {
        // Extract userId from JWT token
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userIdStr = jwt.getSubject();
        UUID userId = UUID.fromString(userIdStr);
        
        log.debug("Get user profile: userId={}", userId);
        
        User user = getUserProfileUseCase.execute(userId);
        
        UserProfileResponse response = UserProfileResponse.builder()
            .userId(user.getUserId())
            .email(user.getEmail())
            .username(user.getUsername())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .roles(user.getRoles().stream()
                .map(role -> role.toKeycloakRole())
                .collect(Collectors.toList()))
            .emailVerified(user.isEmailVerified())
            .build();
        
        return ResponseEntity.ok(response);
    }
}
