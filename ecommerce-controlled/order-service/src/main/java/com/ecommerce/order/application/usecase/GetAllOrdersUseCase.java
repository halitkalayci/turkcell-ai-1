package com.ecommerce.order.application.usecase;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.domain.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case: List Orders
 * Business Rules: docs/rules/order-service-rules.md#3.3
 * 
 * Responsibilities:
 * - Support filtering by customerId, status, date range
 * - Support pagination (default: page 0, size 20, max 100)
 * - Default sorting: createdAt DESC
 * - Return empty page if no matches (NOT error)
 */
@Service
@Transactional(readOnly = true)
public class GetAllOrdersUseCase {

    private final OrderRepository orderRepository;

    public GetAllOrdersUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Lists orders with optional filtering and pagination.
     * 
     * @param pageable pagination and sorting parameters
     * @return page of orders (may be empty)
     */
    public Page<Order> execute(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }
    
    /**
     * Lists orders filtered by customer ID.
     * 
     * @param customerId customer identifier
     * @param pageable pagination parameters
     * @return page of orders for the customer
     */
    public Page<Order> executeByCustomerId(UUID customerId, Pageable pageable) {
        if (customerId == null) {
            throw new IllegalArgumentException("customerId must not be null");
        }
        return orderRepository.findByCustomerId(customerId, pageable);
    }
    
    /**
     * Lists orders filtered by status.
     * 
     * @param status order status
     * @param pageable pagination parameters
     * @return page of orders with the specified status
     */
    public Page<Order> executeByStatus(OrderStatus status, Pageable pageable) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        return orderRepository.findByStatus(status, pageable);
    }
    
    /**
     * Lists orders filtered by creation date range.
     * 
     * @param createdAfter start of date range (inclusive)
     * @param createdBefore end of date range (inclusive)
     * @param pageable pagination parameters
     * @return page of orders created within the date range
     */
    public Page<Order> executeByDateRange(
        Instant createdAfter, 
        Instant createdBefore, 
        Pageable pageable
    ) {
        return orderRepository.findByCreatedAtBetween(createdAfter, createdBefore, pageable);
    }
}
