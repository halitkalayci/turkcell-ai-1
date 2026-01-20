package com.ecommerce.order.domain.model;

/**
 * Order lifecycle states.
 * Represents the current state of an order in its lifecycle.
 * 
 * State Transitions (see docs/rules/order-service-rules.md):
 * <pre>
 * PENDING → CONFIRMED → SHIPPED → DELIVERED
 *    ↓           ↓          ↓
 * CANCELLED   CANCELLED  CANCELLED
 * </pre>
 * 
 * Terminal States: DELIVERED, CANCELLED (immutable)
 */
public enum OrderStatus {
    /**
     * Order created, awaiting confirmation.
     * Initial state for all new orders.
     */
    PENDING,
    
    /**
     * Order confirmed (payment validated), awaiting fulfillment.
     * Can transition from: PENDING
     */
    CONFIRMED,
    
    /**
     * Order dispatched to customer.
     * Can transition from: CONFIRMED
     */
    SHIPPED,
    
    /**
     * Order successfully delivered.
     * Terminal state - no further transitions allowed.
     * Can transition from: SHIPPED
     */
    DELIVERED,
    
    /**
     * Order cancelled by customer or system.
     * Terminal state - no further transitions allowed.
     * Can transition from: PENDING, CONFIRMED, SHIPPED (rare)
     */
    CANCELLED;
    
    /**
     * Checks if this status is a terminal state.
     * Terminal states are immutable and cannot transition to any other state.
     * 
     * @return true if this is a terminal state (DELIVERED or CANCELLED)
     */
    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }
    
    /**
     * Checks if transition to target status is allowed.
     * Enforces state machine rules from business requirements.
     * 
     * @param target the target status to transition to
     * @return true if transition is allowed
     */
    public boolean canTransitionTo(OrderStatus target) {
        if (this == target) {
            return false; // No self-transitions
        }
        
        if (this.isTerminal()) {
            return false; // Terminal states cannot transition
        }
        
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == SHIPPED || target == CANCELLED;
            case SHIPPED -> target == DELIVERED || target == CANCELLED;
            default -> false;
        };
    }
}
