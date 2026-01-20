package com.ecommerce.order.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Order aggregate root.
 * Represents a customer order in the e-commerce system.
 * 
 * Business Rules (docs/rules/order-service-rules.md):
 * - Order lifecycle follows state machine (PENDING → CONFIRMED → SHIPPED → DELIVERED)
 * - Terminal states (DELIVERED, CANCELLED) are immutable
 * - Total amount must equal sum of line item subtotals
 * - Line items: min 1, max 50
 * - No duplicate productIds allowed
 */
public class Order {
    private UUID id;
    private String orderNumber;
    private UUID customerId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private Address shippingAddress;
    private List<LineItem> lineItems;
    
    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
    private Instant confirmedAt;
    private Instant shippedAt;
    private Instant deliveredAt;
    private Instant cancelledAt;
    
    // Optional fields
    private String cancellationReason;
    
    /**
     * Private constructor for JPA/framework use.
     */
    protected Order() {
    }
    
    /**
     * Creates a new order with PENDING status.
     * 
     * @param customerId customer identifier
     * @param shippingAddress delivery address
     * @param lineItems order line items
     * @param totalAmount total order amount
     * @return new Order instance
     * @throws IllegalArgumentException if business rules violated
     */
    public static Order create(
        UUID customerId,
        Address shippingAddress,
        List<LineItem> lineItems,
        BigDecimal totalAmount
    ) {
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(shippingAddress, "shippingAddress must not be null");
        Objects.requireNonNull(lineItems, "lineItems must not be null");
        Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        
        validateLineItems(lineItems);
        validateTotalAmount(lineItems, totalAmount);
        
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.orderNumber = generateOrderNumber();
        order.customerId = customerId;
        order.status = OrderStatus.PENDING;
        order.totalAmount = totalAmount;
        order.shippingAddress = shippingAddress;
        order.lineItems = new ArrayList<>(lineItems);
        
        Instant now = Instant.now();
        order.createdAt = now;
        order.updatedAt = now;
        
        return order;
    }
    
    /**
     * Validates line items according to business rules.
     */
    private static void validateLineItems(List<LineItem> lineItems) {
        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("lineItems must contain at least 1 item");
        }
        
        if (lineItems.size() > 50) {
            throw new IllegalArgumentException(
                "lineItems must not exceed 50 items, got: " + lineItems.size()
            );
        }
        
        // Check for duplicate productIds
        Set<UUID> productIds = new HashSet<>();
        for (LineItem item : lineItems) {
            if (!productIds.add(item.productId())) {
                throw new IllegalArgumentException(
                    "duplicate productId not allowed: " + item.productId()
                );
            }
        }
    }
    
    /**
     * Validates that totalAmount matches sum of line items.
     */
    private static void validateTotalAmount(List<LineItem> lineItems, BigDecimal totalAmount) {
        BigDecimal calculated = lineItems.stream()
            .map(LineItem::calculateSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalAmount.compareTo(calculated) != 0) {
            throw new IllegalArgumentException(
                "totalAmount does not match sum of line items. Expected: " + 
                calculated + ", got: " + totalAmount
            );
        }
    }
    
    /**
     * Generates order number in format: ORD-{yyyyMMddHHmmss}-{random5digits}
     */
    private static String generateOrderNumber() {
        String timestamp = java.time.format.DateTimeFormatter
            .ofPattern("yyyyMMddHHmmss")
            .withZone(java.time.ZoneOffset.UTC)
            .format(Instant.now());
        
        int random = 10000 + new Random().nextInt(90000); // 5 digits
        
        return "ORD-" + timestamp + "-" + random;
    }
    
    /**
     * Updates order status following state machine rules.
     * 
     * @param newStatus target status
     * @throws IllegalStateException if transition not allowed
     */
    public void updateStatus(OrderStatus newStatus) {
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        
        if (this.status == newStatus) {
            throw new IllegalStateException(
                "cannot transition to same status: " + newStatus
            );
        }
        
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                "invalid status transition from " + this.status + " to " + newStatus
            );
        }
        
        this.status = newStatus;
        this.updatedAt = Instant.now();
        
        // Set specific timestamp based on target status
        switch (newStatus) {
            case CONFIRMED -> this.confirmedAt = this.updatedAt;
            case SHIPPED -> this.shippedAt = this.updatedAt;
            case DELIVERED -> this.deliveredAt = this.updatedAt;
            case CANCELLED -> this.cancelledAt = this.updatedAt;
        }
    }
    
    /**
     * Cancels the order with optional reason.
     * 
     * @param reason optional cancellation reason
     * @throws IllegalStateException if cancellation not allowed
     */
    public void cancel(String reason) {
        if (!canBeCancelled()) {
            throw new IllegalStateException(
                "cannot cancel order in " + this.status + " status. " +
                "Only PENDING and CONFIRMED orders can be cancelled."
            );
        }
        
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.updatedAt = this.cancelledAt;
        this.cancellationReason = reason;
    }
    
    /**
     * Checks if order can be cancelled.
     * 
     * @return true if status is PENDING or CONFIRMED
     */
    public boolean canBeCancelled() {
        return this.status == OrderStatus.PENDING || 
               this.status == OrderStatus.CONFIRMED;
    }
    
    /**
     * Checks if order is in a terminal state.
     * 
     * @return true if status is DELIVERED or CANCELLED
     */
    public boolean isTerminal() {
        return this.status.isTerminal();
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public String getOrderNumber() {
        return orderNumber;
    }
    
    public UUID getCustomerId() {
        return customerId;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public Address getShippingAddress() {
        return shippingAddress;
    }
    
    public List<LineItem> getLineItems() {
        return Collections.unmodifiableList(lineItems);
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public Instant getConfirmedAt() {
        return confirmedAt;
    }
    
    public Instant getShippedAt() {
        return shippedAt;
    }
    
    public Instant getDeliveredAt() {
        return deliveredAt;
    }
    
    public Instant getCancelledAt() {
        return cancelledAt;
    }
    
    public String getCancellationReason() {
        return cancellationReason;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", orderNumber='" + orderNumber + '\'' +
                ", status=" + status +
                ", customerId=" + customerId +
                ", totalAmount=" + totalAmount +
                '}';
    }
}
