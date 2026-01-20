package com.ecommerce.order.application.usecase;

import com.ecommerce.order.application.service.OutboxService;
import com.ecommerce.order.domain.event.OrderCreatedEvent;
import com.ecommerce.order.domain.model.Address;
import com.ecommerce.order.domain.model.LineItem;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case: Create Order
 * Business Rules: docs/rules/order-service-rules.md#3.1
 * 
 * Responsibilities:
 * - Validate stock availability (SYNC call to inventory-service)
 * - Validate request data (delegated to domain model)
 * - Create order with PENDING status
 * - Persist order
 * - Write OrderCreated event to outbox (same transaction)
 * - Return created order
 */
@Service
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final com.ecommerce.order.application.port.StockCheckPort stockCheckPort;
    private final OutboxService outboxService;

    public CreateOrderUseCase(
            OrderRepository orderRepository,
            com.ecommerce.order.application.port.StockCheckPort stockCheckPort,
            OutboxService outboxService
    ) {
        this.orderRepository = orderRepository;
        this.stockCheckPort = stockCheckPort;
        this.outboxService = outboxService;
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
        // SYNC stock validation (blocking call to inventory-service)
        // Per AGENTS.md §7.1: Blocking calls MUST be SYNC (Feign)
        for (LineItem lineItem : lineItems) {
            stockCheckPort.validateStockAvailability(
                lineItem.productId(),
                lineItem.quantity()
            );
        }
        
        // Domain model enforces all business rules
        Order order = Order.create(
            customerId,
            shippingAddress,
            lineItems,
            totalAmount
        );
        
        // Persist order
        Order savedOrder = orderRepository.save(order);
        
        // Write OrderCreated event to outbox (same transaction)
        // Per docs/events/outbox-pattern.md and AGENTS.md §7.8
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType("OrderCreated")
            .version("1")
            .timestamp(OffsetDateTime.now())
            .orderId(savedOrder.id())
            .customerId(savedOrder.customerId())
            .lineItems(savedOrder.lineItems().stream()
                .map(li -> OrderCreatedEvent.LineItem.builder()
                    .productId(li.productId())
                    .quantity(li.quantity())
                    .build())
                .collect(Collectors.toList()))
            .build();
        
        outboxService.writeOrderCreatedEvent(event);
        
        return savedOrder;
    }
}
