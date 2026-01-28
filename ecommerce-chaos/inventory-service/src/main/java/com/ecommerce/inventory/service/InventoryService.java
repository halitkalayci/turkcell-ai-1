package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.*;
import com.ecommerce.inventory.entity.Inventory;

import java.util.List;

public interface InventoryService {
    
    InventoryResponse createInventory(CreateInventoryRequest request);
    
    InventoryResponse getInventoryById(Long id);
    
    InventoryResponse getInventoryByProductId(String productId);
    
    List<InventoryResponse> getAllInventories();
    
    InventoryResponse updateInventory(Long id, UpdateInventoryRequest request);
    
    void deleteInventory(Long id);
    
    CheckAvailabilityResponse checkAvailability(String productId, Integer quantity);
    
    InventoryResponse reserveInventory(ReserveInventoryRequest request);
    
    InventoryResponse releaseReservation(String productId, Integer quantity);
    
    InventoryResponse confirmReservation(String productId, Integer quantity);
    
    InventoryResponse addStock(String productId, Integer quantity);
}
