package com.ecommerce.order.application.usecase;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case: Cancel Order
 * Business Rules: docs/rules/order-service-rules.md#3.4
 * 
 * Responsibilities:
 * - Retrieve order by ID
 * - Validate cancellation eligibility (PENDING or CONFIRMED status)
 * - Cancel order with optional reason
 * - Persist changes
 * - Return updated order
 */
@Service
public class CancelOrderUseCase {

    private final OrderRepository orderRepository;

    public CancelOrderUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Cancels an order.
     * 
     * @param id order identifier
     * @param reason optional cancellation reason
     * @return cancelled order
     * @throws IllegalArgumentException if id is null
     * @throws jakarta.persistence.EntityNotFoundException if order not found
     * @throws IllegalStateException if cancellation not allowed (wrong status)
     */
    @Transactional
    public Order execute(UUID id, String reason) {
        if (id == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
        
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                "Order not found with id: " + id
            ));
        
        // Domain model enforces cancellation rules
        order.cancel(reason);
        
        return orderRepository.save(order);
    }
}
