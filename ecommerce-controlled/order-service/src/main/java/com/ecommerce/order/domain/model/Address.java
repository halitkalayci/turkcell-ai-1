package com.ecommerce.order.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Address value object.
 * Represents a shipping/delivery address.
 * 
 * Business Rules (docs/rules/order-service-rules.md):
 * - street: not blank, max 255 chars
 * - city: not blank, max 100 chars
 * - postalCode: not blank, max 20 chars
 * - country: ISO 3166-1 alpha-2 code (2 uppercase letters)
 */
public record Address(
    String street,
    String city,
    String postalCode,
    String country
) {
    private static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile("^[A-Z]{2}$");
    
    /**
     * Compact constructor with validation.
     */
    public Address {
        Objects.requireNonNull(street, "street must not be null");
        Objects.requireNonNull(city, "city must not be null");
        Objects.requireNonNull(postalCode, "postalCode must not be null");
        Objects.requireNonNull(country, "country must not be null");
        
        if (street.isBlank()) {
            throw new IllegalArgumentException("street must not be blank");
        }
        if (street.length() > 255) {
            throw new IllegalArgumentException(
                "street must not exceed 255 characters, got: " + street.length()
            );
        }
        
        if (city.isBlank()) {
            throw new IllegalArgumentException("city must not be blank");
        }
        if (city.length() > 100) {
            throw new IllegalArgumentException(
                "city must not exceed 100 characters, got: " + city.length()
            );
        }
        
        if (postalCode.isBlank()) {
            throw new IllegalArgumentException("postalCode must not be blank");
        }
        if (postalCode.length() > 20) {
            throw new IllegalArgumentException(
                "postalCode must not exceed 20 characters, got: " + postalCode.length()
            );
        }
        
        if (!COUNTRY_CODE_PATTERN.matcher(country).matches()) {
            throw new IllegalArgumentException(
                "country must be ISO 3166-1 alpha-2 code (2 uppercase letters), got: " + country
            );
        }
    }
}
