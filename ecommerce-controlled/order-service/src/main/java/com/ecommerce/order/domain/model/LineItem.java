package com.ecommerce.order.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * LineItem value object.
 * Represents a single product entry within an order.
 * 
 * Business Rules (docs/rules/order-service-rules.md):
 * - productId: required, valid UUID
 * - quantity: min 1, max 999
 * - unitPrice: min 0.01, scale 2
 */
public record LineItem(
    UUID productId,
    Integer quantity,
    BigDecimal unitPrice
) {
    /**
     * Compact constructor with validation.
     */
    public LineItem {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        
        if (quantity < 1 || quantity > 999) {
            throw new IllegalArgumentException(
                "quantity must be between 1 and 999, got: " + quantity
            );
        }
        
        if (unitPrice.compareTo(BigDecimal.valueOf(0.01)) < 0) {
            throw new IllegalArgumentException(
                "unitPrice must be at least 0.01, got: " + unitPrice
            );
        }
        
        if (unitPrice.scale() > 2) {
            throw new IllegalArgumentException(
                "unitPrice scale must not exceed 2, got: " + unitPrice.scale()
            );
        }
    }
    
    /**
     * Calculate the subtotal for this line item.
     * 
     * @return quantity × unitPrice
     */
    public BigDecimal calculateSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
