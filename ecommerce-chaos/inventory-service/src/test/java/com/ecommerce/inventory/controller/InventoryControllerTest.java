package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.*;
import com.ecommerce.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    private InventoryResponse testResponse;
    private CreateInventoryRequest createRequest;

    @BeforeEach
    void setUp() {
        testResponse = new InventoryResponse(
                1L, "PROD-001", "Test Product", 100, 10, 90, 99.99,
                LocalDateTime.now(), LocalDateTime.now()
        );

        createRequest = new CreateInventoryRequest("PROD-001", "Test Product", 100, 99.99);
    }

    @Test
    void createInventory_Success() throws Exception {
        when(inventoryService.createInventory(any(CreateInventoryRequest.class))).thenReturn(testResponse);

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value("PROD-001"))
                .andExpect(jsonPath("$.productName").value("Test Product"));
    }

    @Test
    void getInventoryById_Success() throws Exception {
        when(inventoryService.getInventoryById(1L)).thenReturn(testResponse);

        mockMvc.perform(get("/api/inventory/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("PROD-001"));
    }

    @Test
    void getInventoryByProductId_Success() throws Exception {
        when(inventoryService.getInventoryByProductId("PROD-001")).thenReturn(testResponse);

        mockMvc.perform(get("/api/inventory/product/PROD-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("PROD-001"));
    }

    @Test
    void getAllInventories_Success() throws Exception {
        List<InventoryResponse> responses = Arrays.asList(testResponse);
        when(inventoryService.getAllInventories()).thenReturn(responses);

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value("PROD-001"));
    }

    @Test
    void updateInventory_Success() throws Exception {
        UpdateInventoryRequest updateRequest = new UpdateInventoryRequest("Updated Product", 150, 149.99);
        when(inventoryService.updateInventory(eq(1L), any(UpdateInventoryRequest.class))).thenReturn(testResponse);

        mockMvc.perform(put("/api/inventory/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteInventory_Success() throws Exception {
        mockMvc.perform(delete("/api/inventory/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void checkAvailability_Success() throws Exception {
        CheckAvailabilityResponse availabilityResponse = new CheckAvailabilityResponse("PROD-001", true, 90, 50);
        when(inventoryService.checkAvailability("PROD-001", 50)).thenReturn(availabilityResponse);

        mockMvc.perform(get("/api/inventory/check-availability")
                        .param("productId", "PROD-001")
                        .param("quantity", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.availableQuantity").value(90));
    }

    @Test
    void reserveInventory_Success() throws Exception {
        ReserveInventoryRequest reserveRequest = new ReserveInventoryRequest("PROD-001", 20);
        when(inventoryService.reserveInventory(any(ReserveInventoryRequest.class))).thenReturn(testResponse);

        mockMvc.perform(post("/api/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserveRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void releaseReservation_Success() throws Exception {
        when(inventoryService.releaseReservation("PROD-001", 10)).thenReturn(testResponse);

        mockMvc.perform(post("/api/inventory/release")
                        .param("productId", "PROD-001")
                        .param("quantity", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void confirmReservation_Success() throws Exception {
        when(inventoryService.confirmReservation("PROD-001", 10)).thenReturn(testResponse);

        mockMvc.perform(post("/api/inventory/confirm")
                        .param("productId", "PROD-001")
                        .param("quantity", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void addStock_Success() throws Exception {
        when(inventoryService.addStock("PROD-001", 50)).thenReturn(testResponse);

        mockMvc.perform(post("/api/inventory/add-stock")
                        .param("productId", "PROD-001")
                        .param("quantity", "50"))
                .andExpect(status().isOk());
    }
}
