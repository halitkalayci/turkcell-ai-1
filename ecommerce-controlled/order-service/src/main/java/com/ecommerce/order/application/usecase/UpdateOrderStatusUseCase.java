package com.ecommerce.order.application.usecase;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case: Update Order Status
 * Business Rules: docs/rules/order-service-rules.md#3.5
 * 
 * Responsibilities:
 * - Retrieve order by ID
 * - Validate state transition
 * - Update order status
 * - Set appropriate timestamps
 * - Persist changes
 * - Return updated order
 */
@Service
public class UpdateOrderStatusUseCase {

    private final OrderRepository orderRepository;

    public UpdateOrderStatusUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Updates order status following state machine rules.
     * 
     * @param id order identifier
     * @param newStatus target status
     * @return updated order
     * @throws IllegalArgumentException if id or newStatus is null
     * @throws jakarta.persistence.EntityNotFoundException if order not found
     * @throws IllegalStateException if transition not allowed
     */
    @Transactional
    public Order execute(UUID id, OrderStatus newStatus) {
        if (id == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("newStatus must not be null");
        }
        
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                "Order not found with id: " + id
            ));
        
        // Domain model enforces state transition rules
        order.updateStatus(newStatus);
        
        return orderRepository.save(order);
    }
}
