package com.ecommerce.identity.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * User profile response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    
    private UUID userId;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private Boolean emailVerified;
}
