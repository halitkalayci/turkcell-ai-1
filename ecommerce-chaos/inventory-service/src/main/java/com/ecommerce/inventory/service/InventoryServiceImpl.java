package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.*;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.exception.InsufficientInventoryException;
import com.ecommerce.inventory.exception.InventoryAlreadyExistsException;
import com.ecommerce.inventory.exception.InventoryNotFoundException;
import com.ecommerce.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        log.info("Creating inventory for product: {}", request.getProductId());
        
        if (inventoryRepository.existsByProductId(request.getProductId())) {
            throw new InventoryAlreadyExistsException("Inventory already exists for product: " + request.getProductId());
        }

        Inventory inventory = new Inventory();
        inventory.setProductId(request.getProductId());
        inventory.setProductName(request.getProductName());
        inventory.setQuantity(request.getQuantity());
        inventory.setReservedQuantity(0);
        inventory.setPrice(request.getPrice());

        Inventory savedInventory = inventoryRepository.save(inventory);
        log.info("Inventory created successfully for product: {}", savedInventory.getProductId());
        
        return mapToResponse(savedInventory);
    }

    @Override
    public InventoryResponse getInventoryById(Long id) {
        log.info("Fetching inventory by id: {}", id);
        Inventory inventory = findInventoryById(id);
        return mapToResponse(inventory);
    }

    @Override
    public InventoryResponse getInventoryByProductId(String productId) {
        log.info("Fetching inventory by product id: {}", productId);
        Inventory inventory = findInventoryByProductId(productId);
        return mapToResponse(inventory);
    }

    @Override
    public List<InventoryResponse> getAllInventories() {
        log.info("Fetching all inventories");
        return inventoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InventoryResponse updateInventory(Long id, UpdateInventoryRequest request) {
        log.info("Updating inventory with id: {}", id);
        Inventory inventory = findInventoryById(id);

        if (request.getProductName() != null) {
            inventory.setProductName(request.getProductName());
        }
        if (request.getQuantity() != null) {
            inventory.setQuantity(request.getQuantity());
        }
        if (request.getPrice() != null) {
            inventory.setPrice(request.getPrice());
        }

        Inventory updatedInventory = inventoryRepository.save(inventory);
        log.info("Inventory updated successfully for id: {}", id);
        
        return mapToResponse(updatedInventory);
    }

    @Override
    @Transactional
    public void deleteInventory(Long id) {
        log.info("Deleting inventory with id: {}", id);
        Inventory inventory = findInventoryById(id);
        inventoryRepository.delete(inventory);
        log.info("Inventory deleted successfully for id: {}", id);
    }

    @Override
    public CheckAvailabilityResponse checkAvailability(String productId, Integer quantity) {
        log.info("Checking availability for product: {}, quantity: {}", productId, quantity);
        Inventory inventory = findInventoryByProductId(productId);
        
        Integer availableQuantity = inventory.getAvailableQuantity();
        boolean available = availableQuantity >= quantity;
        
        log.info("Availability check result - Product: {}, Available: {}, Requested: {}, Can fulfill: {}", 
                productId, availableQuantity, quantity, available);
        
        return new CheckAvailabilityResponse(productId, available, availableQuantity, quantity);
    }

    @Override
    @Transactional
    public InventoryResponse reserveInventory(ReserveInventoryRequest request) {
        log.info("Reserving inventory for product: {}, quantity: {}", request.getProductId(), request.getQuantity());
        Inventory inventory = findInventoryByProductId(request.getProductId());

        if (!inventory.canReserve(request.getQuantity())) {
            throw new InsufficientInventoryException(
                    String.format("Insufficient inventory for product %s. Available: %d, Requested: %d",
                            request.getProductId(), inventory.getAvailableQuantity(), request.getQuantity()));
        }

        inventory.reserve(request.getQuantity());
        Inventory savedInventory = inventoryRepository.save(inventory);
        
        log.info("Inventory reserved successfully for product: {}", request.getProductId());
        return mapToResponse(savedInventory);
    }

    @Override
    @Transactional
    public InventoryResponse releaseReservation(String productId, Integer quantity) {
        log.info("Releasing reservation for product: {}, quantity: {}", productId, quantity);
        Inventory inventory = findInventoryByProductId(productId);

        inventory.releaseReservation(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);
        
        log.info("Reservation released successfully for product: {}", productId);
        return mapToResponse(savedInventory);
    }

    @Override
    @Transactional
    public InventoryResponse confirmReservation(String productId, Integer quantity) {
        log.info("Confirming reservation for product: {}, quantity: {}", productId, quantity);
        Inventory inventory = findInventoryByProductId(productId);

        inventory.confirmReservation(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);
        
        log.info("Reservation confirmed successfully for product: {}", productId);
        return mapToResponse(savedInventory);
    }

    @Override
    @Transactional
    public InventoryResponse addStock(String productId, Integer quantity) {
        log.info("Adding stock for product: {}, quantity: {}", productId, quantity);
        Inventory inventory = findInventoryByProductId(productId);

        inventory.setQuantity(inventory.getQuantity() + quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);
        
        log.info("Stock added successfully for product: {}", productId);
        return mapToResponse(savedInventory);
    }

    private Inventory findInventoryById(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found with id: " + id));
    }

    private Inventory findInventoryByProductId(String productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId));
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getProductName(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getPrice(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }
}
