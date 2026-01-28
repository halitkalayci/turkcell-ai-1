package com.ecommerce.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckAvailabilityResponse {
    private String productId;
    private boolean available;
    private Integer availableQuantity;
    private Integer requestedQuantity;
}
