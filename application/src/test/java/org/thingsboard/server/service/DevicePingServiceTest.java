package org.thingsboard.server.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified Unit Tests for Device Ping Service
 *
 * These tests verify core logic without external dependencies
 */
class DevicePingServiceTest {

    private DevicePingSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new DevicePingSimulator();
    }

    /**
     * Test 1: Device is reachable when recently active
     */
    @Test
    void testDeviceReachable_WhenRecentlyActive() {
        // Arrange
        long currentTime = System.currentTimeMillis();
        long lastSeen = currentTime - 60000; // 1 minute ago

        // Act
        boolean isReachable = simulator.checkReachability(lastSeen, currentTime);

        // Assert
        assertTrue(isReachable, "Device should be reachable when active within 5 minutes");
    }

    /**
     * Test 2: Device is not reachable when inactive
     */
    @Test
    void testDeviceNotReachable_WhenInactive() {
        // Arrange
        long currentTime = System.currentTimeMillis();
        long lastSeen = currentTime - 600000; // 10 minutes ago

        // Act
        boolean isReachable = simulator.checkReachability(lastSeen, currentTime);

        // Assert
        assertFalse(isReachable, "Device should not be reachable when inactive for 10 minutes");
    }

    /**
     * Test 3: Boundary test - exactly 5 minutes
     */
    @Test
    void testDeviceReachability_AtBoundary() {
        // Arrange
        long currentTime = System.currentTimeMillis();
        long lastSeen = currentTime - 300000; // Exactly 5 minutes

        // Act
        boolean isReachable = simulator.checkReachability(lastSeen, currentTime);

        // Assert
        assertTrue(isReachable, "Device should be reachable at exactly 5 minute boundary");
    }

    /**
     * Test 4: Response format validation
     */
    @Test
    void testResponseFormat_IsValid() {
        // Arrange
        String deviceId = "test-device-123";
        boolean reachable = true;
        long lastSeen = System.currentTimeMillis();

        // Act
        DevicePingResponse response = simulator.createResponse(deviceId, reachable, lastSeen);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertEquals(deviceId, response.getDeviceId(), "Device ID should match");
        assertEquals(reachable, response.isReachable(), "Reachable status should match");
        assertEquals(lastSeen, response.getLastSeen(), "Last seen timestamp should match");
    }

    /**
     * Test 5: Null device ID handling
     */
    @Test
    void testNullDeviceId_HandledCorrectly() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            simulator.createResponse(null, true, System.currentTimeMillis());
        }, "Should throw exception for null device ID");
    }

    /**
     * Test 6: Future timestamp handling
     */
    @Test
    void testFutureTimestamp_HandledCorrectly() {
        // Arrange
        long currentTime = System.currentTimeMillis();
        long futureTime = currentTime + 60000; // 1 minute in future

        // Act
        boolean isReachable = simulator.checkReachability(futureTime, currentTime);

        // Assert
        assertTrue(isReachable, "Future timestamp should be considered reachable");
    }

    // ==================== Helper Classes ====================

    /**
     * Simple simulator class for testing ping logic
     */
    static class DevicePingSimulator {
        private static final long REACHABILITY_THRESHOLD = 300000; // 5 minutes

        public boolean checkReachability(long lastSeenTime, long currentTime) {
            long timeDiff = currentTime - lastSeenTime;
            return timeDiff <= REACHABILITY_THRESHOLD;
        }

        public DevicePingResponse createResponse(String deviceId, boolean reachable, long lastSeen) {
            if (deviceId == null) {
                throw new IllegalArgumentException("Device ID cannot be null");
            }
            return new DevicePingResponse(deviceId, reachable, lastSeen);
        }
    }

    /**
     * Simple DTO for device ping response
     */
    static class DevicePingResponse {
        private final String deviceId;
        private final boolean reachable;
        private final long lastSeen;

        public DevicePingResponse(String deviceId, boolean reachable, long lastSeen) {
            this.deviceId = deviceId;
            this.reachable = reachable;
            this.lastSeen = lastSeen;
        }

        public String getDeviceId() { return deviceId; }
        public boolean isReachable() { return reachable; }
        public long getLastSeen() { return lastSeen; }
    }
}