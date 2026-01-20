package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.application.usecase.DecrementStockUseCase;
import com.ecommerce.inventory.dto.DecrementStockRequest;
import com.ecommerce.inventory.infrastructure.messaging.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Internal controller for stock operations.
 * NOT exposed via API Gateway - internal use only.
 * 
 * Endpoint: POST /internal/stock/decrement-by-product
 * Per user request for inventory-service.
 */
@RestController
@RequestMapping("/internal/stock")
@Slf4j
public class InternalStockController {

    private final DecrementStockUseCase decrementStockUseCase;

    public InternalStockController(DecrementStockUseCase decrementStockUseCase) {
        this.decrementStockUseCase = decrementStockUseCase;
    }

    /**
     * Decrement stock for a product.
     * Internal endpoint for testing or direct invocation.
     * 
     * @param request decrement request with productId and quantity
     * @return success response
     */
    @PostMapping("/decrement-by-product")
    public ResponseEntity<String> decrementStockByProduct(
        @RequestBody DecrementStockRequest request
    ) {
        log.info("Decrement stock request: productId={}, quantity={}", 
            request.getProductId(), request.getQuantity());
        
        // Create synthetic event for internal call
        OrderCreatedEvent syntheticEvent = OrderCreatedEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType("ManualStockDecrement")
            .version("1")
            .timestamp(OffsetDateTime.now())
            .orderId(UUID.randomUUID())
            .customerId(UUID.randomUUID())
            .lineItems(List.of(
                OrderCreatedEvent.LineItem.builder()
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .build()
            ))
            .build();
        
        decrementStockUseCase.execute(syntheticEvent);
        
        return ResponseEntity.ok("Stock decremented successfully");
    }
}
