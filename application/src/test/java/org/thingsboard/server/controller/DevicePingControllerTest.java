package org.thingsboard.server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Device Ping REST API endpoint
 * 
 * Simplified tests without complex dependencies
 */
@SpringBootTest
@AutoConfigureMockMvc
class DevicePingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceService deviceService;

    private DeviceId testDeviceId;
    private TenantId testTenantId;
    private Device testDevice;
    private String apiEndpoint;

    @BeforeEach
    void setUp() {
        testDeviceId = new DeviceId(UUID.randomUUID());
        testTenantId = new TenantId(UUID.randomUUID());
        apiEndpoint = "/api/device/ping/" + testDeviceId.getId();
        
        testDevice = new Device();
        testDevice.setId(testDeviceId);
        testDevice.setTenantId(testTenantId);
        testDevice.setName("Test Device");
        testDevice.setCreatedTime(System.currentTimeMillis());
    }

    /**
     * Test Case 1: Valid device ping request with authentication
     */
    @Test
    @WithMockUser(username = "tenant@thingsboard.org", authorities = {"TENANT_ADMIN"})
    void testPingEndpoint_WithAuthentication_Returns200() throws Exception {
        // Arrange
        when(deviceService.findDeviceById(any(TenantId.class), eq(testDeviceId)))
            .thenReturn(testDevice);

        // Act & Assert
        mockMvc.perform(get(apiEndpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /**
     * Test Case 2: Request without authentication
     */
    @Test
    void testPingEndpoint_WithoutAuthentication_Returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get(apiEndpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // Verify service was never called
        verify(deviceService, never()).findDeviceById(any(), any());
    }

    /**
     * Test Case 3: Invalid device ID format
     */
    @Test
    @WithMockUser(username = "tenant@thingsboard.org", authorities = {"TENANT_ADMIN"})
    void testPingEndpoint_InvalidDeviceId_Returns400() throws Exception {
        // Arrange
        String invalidEndpoint = "/api/device/ping/invalid-uuid";

        // Act & Assert
        mockMvc.perform(get(invalidEndpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test Case 4: Device not found in database
     */
    @Test
    @WithMockUser(username = "tenant@thingsboard.org", authorities = {"TENANT_ADMIN"})
    void testPingEndpoint_DeviceNotFound_Returns404() throws Exception {
        // Arrange
        when(deviceService.findDeviceById(any(TenantId.class), eq(testDeviceId)))
            .thenReturn(null);

        // Act & Assert
        mockMvc.perform(get(apiEndpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /**
     * Test Case 5: Service throws exception
     */
    @Test
    @WithMockUser(username = "tenant@thingsboard.org", authorities = {"TENANT_ADMIN"})
    void testPingEndpoint_ServiceException_Returns500() throws Exception {
        // Arrange
        when(deviceService.findDeviceById(any(TenantId.class), eq(testDeviceId)))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get(apiEndpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    /**
     * Test Case 6: Content type verification
     */
    @Test
    @WithMockUser(username = "tenant@thingsboard.org", authorities = {"TENANT_ADMIN"})
    void testPingEndpoint_ContentType_IsCorrect() throws Exception {
        // Arrange
        when(deviceService.findDeviceById(any(TenantId.class), eq(testDeviceId)))
            .thenReturn(testDevice);

        // Act & Assert
        mockMvc.perform(get(apiEndpoint))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * Test Case 7: Customer user access
     */
    @Test
    @WithMockUser(username = "customer@thingsboard.org", authorities = {"CUSTOMER_USER"})
    void testPingEndpoint_CustomerUser_CanAccess() throws Exception {
        // Arrange
        when(deviceService.findDeviceById(any(TenantId.class), eq(testDeviceId)))
            .thenReturn(testDevice);

        // Act & Assert
        mockMvc.perform(get(apiEndpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /**
     * Test Case 8: Multiple concurrent requests
     */
    @Test
    @WithMockUser(username = "tenant@thingsboard.org", authorities = {"TENANT_ADMIN"})
    void testPingEndpoint_MultipleRequests_AllSucceed() throws Exception {
        // Arrange
        when(deviceService.findDeviceById(any(TenantId.class), eq(testDeviceId)))
            .thenReturn(testDevice);

        // Act & Assert - simulate 3 concurrent requests
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get(apiEndpoint)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // Verify service was called 3 times
        verify(deviceService, times(3)).findDeviceById(any(TenantId.class), eq(testDeviceId));
    }
}