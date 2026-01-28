package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.*;
import com.ecommerce.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryResponse> createInventory(@Valid @RequestBody CreateInventoryRequest request) {
        InventoryResponse response = inventoryService.createInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryResponse> getInventoryById(@PathVariable Long id) {
        InventoryResponse response = inventoryService.getInventoryById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable String productId) {
        InventoryResponse response = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAllInventories() {
        List<InventoryResponse> responses = inventoryService.getAllInventories();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryResponse> updateInventory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInventoryRequest request) {
        InventoryResponse response = inventoryService.updateInventory(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInventory(@PathVariable Long id) {
        inventoryService.deleteInventory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-availability")
    public ResponseEntity<CheckAvailabilityResponse> checkAvailability(
            @RequestParam String productId,
            @RequestParam Integer quantity) {
        CheckAvailabilityResponse response = inventoryService.checkAvailability(productId, quantity);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reserve")
    public ResponseEntity<InventoryResponse> reserveInventory(@Valid @RequestBody ReserveInventoryRequest request) {
        InventoryResponse response = inventoryService.reserveInventory(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/release")
    public ResponseEntity<InventoryResponse> releaseReservation(
            @RequestParam String productId,
            @RequestParam Integer quantity) {
        InventoryResponse response = inventoryService.releaseReservation(productId, quantity);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<InventoryResponse> confirmReservation(
            @RequestParam String productId,
            @RequestParam Integer quantity) {
        InventoryResponse response = inventoryService.confirmReservation(productId, quantity);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add-stock")
    public ResponseEntity<InventoryResponse> addStock(
            @RequestParam String productId,
            @RequestParam Integer quantity) {
        InventoryResponse response = inventoryService.addStock(productId, quantity);
        return ResponseEntity.ok(response);
    }
}
