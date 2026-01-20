package com.ecommerce.order.application.usecase;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case: Get Order by ID
 * Business Rules: docs/rules/order-service-rules.md#3.2
 * 
 * Responsibilities:
 * - Validate orderId format
 * - Retrieve order from repository
 * - Return order or throw exception if not found
 */
@Service
@Transactional(readOnly = true)
public class GetOrderByIdUseCase {

    private final OrderRepository orderRepository;

    public GetOrderByIdUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Retrieves an order by its ID.
     * 
     * @param id order identifier
     * @return order if found
     * @throws IllegalArgumentException if id is null
     * @throws jakarta.persistence.EntityNotFoundException if order not found
     */
    public Order execute(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
        
        return orderRepository.findById(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                "Order not found with id: " + id
            ));
    }
}
