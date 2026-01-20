package com.ecommerce.order.application.usecase;

import com.ecommerce.order.domain.model.Address;
import com.ecommerce.order.domain.model.LineItem;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Use case: Create Order
 * Business Rules: docs/rules/order-service-rules.md#3.1
 * 
 * Responsibilities:
 * - Validate request data (delegated to domain model)
 * - Create order with PENDING status
 * - Persist order
 * - Return created order
 */
@Service
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;

    public CreateOrderUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Creates a new order.
     * 
     * @param customerId customer identifier
     * @param shippingAddress delivery address
     * @param lineItems order line items (min 1, max 50)
     * @param totalAmount total order amount
     * @return created order with PENDING status
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public Order execute(
        UUID customerId,
        Address shippingAddress,
        List<LineItem> lineItems,
        BigDecimal totalAmount
    ) {
        // Sync iletişim ile, inventory-serviceden sipariş verilmesi talep edilen ürünlerin stok kontrolü
        // yapılmalı.
        
        // Domain model enforces all business rules
        Order order = Order.create(
            customerId,
            shippingAddress,
            lineItems,
            totalAmount
        );
        
        return orderRepository.save(order);
    }
}
