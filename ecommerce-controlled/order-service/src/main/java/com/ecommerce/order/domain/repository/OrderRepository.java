package com.ecommerce.order.domain.repository;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Order aggregate.
 * Defines persistence operations for orders.
 */
public interface OrderRepository {
    /**
     * Saves an order (create or update).
     */
    Order save(Order order);
    
    /**
     * Finds an order by its ID.
     */
    Optional<Order> findById(UUID id);
    
    /**
     * Finds all orders with pagination.
     */
    Page<Order> findAll(Pageable pageable);
    
    /**
     * Finds orders by customer ID with pagination.
     */
    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);
    
    /**
     * Finds orders by status with pagination.
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    
    /**
     * Finds orders created within a date range with pagination.
     */
    Page<Order> findByCreatedAtBetween(Instant startDate, Instant endDate, Pageable pageable);
    
    /**
     * Deletes an order by its ID.
     */
    void deleteById(UUID id);
}
