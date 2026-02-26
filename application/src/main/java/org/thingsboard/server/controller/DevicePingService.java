package org.thingsboard.server.controller;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for checking device reachability and last seen status
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DevicePingService {

    private final DeviceService deviceService;
    private final TimeseriesService timeseriesService;
    private final AttributesService attributesService;

    private static final long TIMEOUT_SECONDS = 5L;

    /**
     * Ping a device and get its reachability status
     * 
     * @param tenantId Tenant ID
     * @param deviceId Device ID
     * @return DevicePingResponse containing reachability and last seen timestamp
     */
    public DevicePingResponse pingDevice(TenantId tenantId, DeviceId deviceId) {
        log.debug("Pinging device [{}] for tenant [{}]", deviceId, tenantId);

        // Verify device exists
        Device device = deviceService.findDeviceById(tenantId, deviceId);
        if (device == null) {
            log.warn("Device [{}] not found", deviceId);
            return new DevicePingResponse(deviceId.getId(), null);
        }

        // Try to get lastActivityTime from server attributes (most reliable)
        Long lastSeen = getLastActivityTime(tenantId, deviceId);

        // If not found in attributes, try to get from latest telemetry
        if (lastSeen == null) {
            lastSeen = getLastTelemetryTimestamp(tenantId, deviceId);
        }

        // If still null, use device creation time as absolute fallback
        if (lastSeen == null) {
            lastSeen = device.getCreatedTime();
            log.debug("Using device creation time as fallback for device [{}]", deviceId);
        }

        log.debug("Device [{}] last seen at [{}]", deviceId, lastSeen);

        return new DevicePingResponse(deviceId.getId(), lastSeen);
    }

    /**
     * Get last activity time from server-side attributes
     * This is the most reliable source as it's updated by the platform
     */
    private Long getLastActivityTime(TenantId tenantId, DeviceId deviceId) {
        try {
            // Try multiple common attribute names that might contain last activity
            String[] attributeKeys = {"lastActivityTime", "lastConnectTime", "inactivityAlarmTime", "active"};
            
            for (String key : attributeKeys) {
                try {
                    ListenableFuture<Optional<AttributeKvEntry>> futureAttr = attributesService.find(
                        tenantId, 
                        deviceId, 
                        AttributeScope.SERVER_SCOPE,
                        key
                    );

                    Optional<AttributeKvEntry> attributeOpt = futureAttr.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    
                    if (attributeOpt.isPresent()) {
                        AttributeKvEntry attr = attributeOpt.get();
                        
                        // Try to get long value (timestamp)
                        Optional<Long> longValue = attr.getLongValue();
                        if (longValue.isPresent() && longValue.get() > 0) {
                            log.debug("Found lastActivityTime from attribute [{}]: {}", key, longValue.get());
                            return longValue.get();
                        }
                    }
                } catch (TimeoutException e) {
                    log.warn("Timeout fetching attribute [{}] for device [{}]", key, deviceId);
                }
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while fetching attributes for device [{}]", deviceId, e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("Error fetching attributes for device [{}]", deviceId, e);
        }
        
        return null;
    }

    /**
     * Get timestamp of the latest telemetry entry as fallback
     * This checks when the device last sent any data
     */
    private Long getLastTelemetryTimestamp(TenantId tenantId, DeviceId deviceId) {
        try {
            // Get latest telemetry for any key (within last 30 days)
            long endTs = System.currentTimeMillis();
            long startTs = endTs - (30L * 24 * 60 * 60 * 1000); // 30 days ago

            ListenableFuture<List<TsKvEntry>> latestFuture = timeseriesService.findLatest(
                tenantId,
                deviceId,
                java.util.Collections.emptyList() // Empty list means all keys
            );

            List<TsKvEntry> tsKvEntries = latestFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (tsKvEntries != null && !tsKvEntries.isEmpty()) {
                // Find the most recent timestamp among all telemetry keys
                long maxTimestamp = tsKvEntries.stream()
                    .mapToLong(TsKvEntry::getTs)
                    .max()
                    .orElse(0L);
                
                if (maxTimestamp > 0) {
                    log.debug("Found latest telemetry timestamp for device [{}]: {}", deviceId, maxTimestamp);
                    return maxTimestamp;
                }
            }
        } catch (TimeoutException e) {
            log.warn("Timeout fetching telemetry for device [{}]", deviceId);
        } catch (InterruptedException e) {
            log.error("Interrupted while fetching telemetry for device [{}]", deviceId, e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("Error fetching telemetry for device [{}]", deviceId, e);
        }
        
        return null;
    }
}