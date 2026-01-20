package com.ecommerce.inventory.application.usecase;

import com.ecommerce.inventory.domain.model.Inventory;
import com.ecommerce.inventory.domain.repository.InventoryRepository;
import com.ecommerce.inventory.exception.InventoryNotFoundException;
import com.ecommerce.inventory.infrastructure.messaging.event.OrderCreatedEvent;
import com.ecommerce.inventory.infrastructure.persistence.entity.InboxEntity;
import com.ecommerce.inventory.infrastructure.persistence.repository.InboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Use case: Decrement stock in response to OrderCreated event.
 * Per docs/events/idempotency.md
 * 
 * Implements inbox pattern for idempotency.
 */
@Service
@Slf4j
public class DecrementStockUseCase {

    private final InventoryRepository inventoryRepository;
    private final InboxRepository inboxRepository;

    public DecrementStockUseCase(
        InventoryRepository inventoryRepository,
        InboxRepository inboxRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.inboxRepository = inboxRepository;
    }

    /**
     * Process OrderCreated event with idempotency.
     * 
     * @param event OrderCreated event
     * @throws InventoryNotFoundException if product not found
     * @throws IllegalArgumentException if insufficient stock
     */
    @Transactional
    public void execute(OrderCreatedEvent event) {
        try {
            // Step 1: Try to insert into inbox (duplicate detection)
            InboxEntity inbox = InboxEntity.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .processedAt(Instant.now())
                .build();
            
            inboxRepository.save(inbox);
            
            log.info("Event marked as processing: eventId={}", event.getEventId());
            
        } catch (DataIntegrityViolationException e) {
            // Duplicate event detected - skip processing
            log.info("Event already processed, skipping: eventId={}", event.getEventId());
            return;
        }
        
        // Step 2: Perform business logic (stock decrement)
        for (OrderCreatedEvent.LineItem lineItem : event.getLineItems()) {
            Inventory inventory = inventoryRepository.findByProductId(lineItem.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException(
                    "Product not found: " + lineItem.getProductId()));
            
            // Decrement stock
            inventory.decreaseQuantity(lineItem.getQuantity());
            
            // Persist changes
            inventoryRepository.save(inventory);
            
            log.info("Stock decremented: productId={}, quantity={}, remainingStock={}", 
                lineItem.getProductId(), lineItem.getQuantity(), inventory.getQuantity());
        }
    }
}
