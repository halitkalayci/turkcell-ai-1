package com.ecommerce.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer reservedQuantity = 0;

    @Column(nullable = false)
    private Double price;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (reservedQuantity == null) {
            reservedQuantity = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    public boolean canReserve(Integer amount) {
        return getAvailableQuantity() >= amount;
    }

    public void reserve(Integer amount) {
        if (!canReserve(amount)) {
            throw new IllegalStateException("Insufficient inventory to reserve");
        }
        this.reservedQuantity += amount;
    }

    public void releaseReservation(Integer amount) {
        if (this.reservedQuantity < amount) {
            throw new IllegalStateException("Cannot release more than reserved");
        }
        this.reservedQuantity -= amount;
    }

    public void confirmReservation(Integer amount) {
        if (this.reservedQuantity < amount) {
            throw new IllegalStateException("Cannot confirm more than reserved");
        }
        this.reservedQuantity -= amount;
        this.quantity -= amount;
    }
}
