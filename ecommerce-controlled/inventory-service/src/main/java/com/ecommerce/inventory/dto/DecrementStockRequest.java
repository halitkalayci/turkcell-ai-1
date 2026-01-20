package com.ecommerce.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for internal stock decrement endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecrementStockRequest {
    private UUID productId;
    private Integer quantity;
}
