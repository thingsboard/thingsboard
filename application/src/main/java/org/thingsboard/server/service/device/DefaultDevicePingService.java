/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.device;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.device.DevicePingResponse;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultDevicePingService implements DevicePingService {

    private static final String LAST_ACTIVITY_TIME_KEY = "lastActivityTime";

    private final TimeseriesService timeseriesService;

    @Value("${device.ping.reachability.window.minutes:5}")
    private long reachabilityWindowMinutes;

    @Override
    public DevicePingResponse pingDevice(TenantId tenantId, DeviceId deviceId) {
        Long lastActivityTime = getLastActivityTime(tenantId, deviceId);

        long now = System.currentTimeMillis();
        long windowStart = now - TimeUnit.MINUTES.toMillis(reachabilityWindowMinutes);
        boolean reachable = lastActivityTime != null && lastActivityTime > windowStart;

        DevicePingResponse response = new DevicePingResponse();
        response.setDeviceId(deviceId.getId().toString());
        response.setReachable(reachable);
        response.setLastSeen(lastActivityTime);

        if (lastActivityTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            response.setLastSeenFormatted(sdf.format(new Date(lastActivityTime)));
            long minutesAgo = TimeUnit.MILLISECONDS.toMinutes(now - lastActivityTime);
            response.setMessage(reachable
                    ? "Device is online. Last activity " + minutesAgo + " minute(s) ago."
                    : "Device is offline. Last activity " + minutesAgo + " minute(s) ago.");
        } else {
            response.setLastSeenFormatted("Never");
            response.setMessage("Device has never been active.");
        }

        return response;
    }

    Long getLastActivityTime(TenantId tenantId, DeviceId deviceId) {
        try {
            List<TsKvEntry> lastActivity = timeseriesService.findLatest(tenantId, deviceId, List.of(LAST_ACTIVITY_TIME_KEY)).get();
            if (!lastActivity.isEmpty() && lastActivity.get(0).getValue() != null) {
                return lastActivity.get(0).getLongValue().orElse(null);
            }

            List<TsKvEntry> latestTelemetry = timeseriesService.findAllLatest(tenantId, deviceId).get();
            if (!latestTelemetry.isEmpty()) {
                return latestTelemetry.stream()
                        .map(TsKvEntry::getTs)
                        .max(Comparator.naturalOrder())
                        .orElse(null);
            }
            return null;
        } catch (Exception e) {
            log.debug("Failed to resolve last activity time for device {}", deviceId, e);
            return null;
        }
    }
}
