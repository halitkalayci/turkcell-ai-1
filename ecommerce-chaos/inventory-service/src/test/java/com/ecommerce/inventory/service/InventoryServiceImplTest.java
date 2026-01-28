package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.*;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.exception.InsufficientInventoryException;
import com.ecommerce.inventory.exception.InventoryAlreadyExistsException;
import com.ecommerce.inventory.exception.InventoryNotFoundException;
import com.ecommerce.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Inventory testInventory;
    private CreateInventoryRequest createRequest;

    @BeforeEach
    void setUp() {
        testInventory = new Inventory();
        testInventory.setId(1L);
        testInventory.setProductId("PROD-001");
        testInventory.setProductName("Test Product");
        testInventory.setQuantity(100);
        testInventory.setReservedQuantity(10);
        testInventory.setPrice(99.99);
        testInventory.setCreatedAt(LocalDateTime.now());
        testInventory.setUpdatedAt(LocalDateTime.now());

        createRequest = new CreateInventoryRequest("PROD-001", "Test Product", 100, 99.99);
    }

    @Test
    void createInventory_Success() {
        when(inventoryRepository.existsByProductId(createRequest.getProductId())).thenReturn(false);
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        InventoryResponse response = inventoryService.createInventory(createRequest);

        assertNotNull(response);
        assertEquals("PROD-001", response.getProductId());
        assertEquals("Test Product", response.getProductName());
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
    }

    @Test
    void createInventory_AlreadyExists() {
        when(inventoryRepository.existsByProductId(createRequest.getProductId())).thenReturn(true);

        assertThrows(InventoryAlreadyExistsException.class, () -> {
            inventoryService.createInventory(createRequest);
        });
    }

    @Test
    void getInventoryById_Success() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(testInventory));

        InventoryResponse response = inventoryService.getInventoryById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("PROD-001", response.getProductId());
    }

    @Test
    void getInventoryById_NotFound() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(InventoryNotFoundException.class, () -> {
            inventoryService.getInventoryById(1L);
        });
    }

    @Test
    void getInventoryByProductId_Success() {
        when(inventoryRepository.findByProductId("PROD-001")).thenReturn(Optional.of(testInventory));

        InventoryResponse response = inventoryService.getInventoryByProductId("PROD-001");

        assertNotNull(response);
        assertEquals("PROD-001", response.getProductId());
    }

    @Test
    void getAllInventories_Success() {
        List<Inventory> inventories = Arrays.asList(testInventory);
        when(inventoryRepository.findAll()).thenReturn(inventories);

        List<InventoryResponse> responses = inventoryService.getAllInventories();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("PROD-001", responses.get(0).getProductId());
    }

    @Test
    void updateInventory_Success() {
        UpdateInventoryRequest updateRequest = new UpdateInventoryRequest("Updated Product", 150, 149.99);
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        InventoryResponse response = inventoryService.updateInventory(1L, updateRequest);

        assertNotNull(response);
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
    }

    @Test
    void deleteInventory_Success() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(testInventory));
        doNothing().when(inventoryRepository).delete(testInventory);

        inventoryService.deleteInventory(1L);

        verify(inventoryRepository, times(1)).delete(testInventory);
    }

    @Test
    void checkAvailability_Available() {
        when(inventoryRepository.findByProductId("PROD-001")).thenReturn(Optional.of(testInventory));

        CheckAvailabilityResponse response = inventoryService.checkAvailability("PROD-001", 50);

        assertTrue(response.isAvailable());
        assertEquals(90, response.getAvailableQuantity());
    }

    @Test
    void checkAvailability_NotAvailable() {
        when(inventoryRepository.findByProductId("PROD-001")).thenReturn(Optional.of(testInventory));

        CheckAvailabilityResponse response = inventoryService.checkAvailability("PROD-001", 100);

        assertFalse(response.isAvailable());
        assertEquals(90, response.getAvailableQuantity());
    }

    @Test
    void reserveInventory_Success() {
        ReserveInventoryRequest reserveRequest = new ReserveInventoryRequest("PROD-001", 20);
        when(inventoryRepository.findByProductId("PROD-001")).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        InventoryResponse response = inventoryService.reserveInventory(reserveRequest);

        assertNotNull(response);
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
    }

    @Test
    void reserveInventory_InsufficientInventory() {
        ReserveInventoryRequest reserveRequest = new ReserveInventoryRequest("PROD-001", 100);
        when(inventoryRepository.findByProductId("PROD-001")).thenReturn(Optional.of(testInventory));

        assertThrows(InsufficientInventoryException.class, () -> {
            inventoryService.reserveInventory(reserveRequest);
        });
    }

    @Test
    void releaseReservation_Success() {
        when(inventoryRepository.findByProductId("PROD-001")).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        InventoryResponse response = inventoryService.releaseReservation("PROD-001", 5);

        assertNotNull(response);
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
    }

    @Test
    void confirmReservation_Success() {
        when(inventoryRepository.findByProductId("PROD-001")).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        InventoryResponse response = inventoryService.confirmReservation("PROD-001", 5);

        assertNotNull(response);
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
    }

    @Test
    void addStock_Success() {
        when(inventoryRepository.findByProductId("PROD-001")).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        InventoryResponse response = inventoryService.addStock("PROD-001", 50);

        assertNotNull(response);
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
    }
}
