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

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultDevicePingServiceTest {

    @Mock
    TimeseriesService timeseriesService;

    @InjectMocks
    DefaultDevicePingService service;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("ce31b8c0-da96-11f0-888b-7f458e3ae7a2"));
    private static final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("784f394c-42b6-435a-983c-b7beff2784f9"));

    @Test
    void pingDevice_shouldBeReachable_whenLastActivityWithinWindow() throws Exception {
        ReflectionTestUtils.setField(service, "reachabilityWindowMinutes", 5L);

        long now = System.currentTimeMillis();
        TsKvEntry lastActivity = new BasicTsKvEntry(now - 60_000, new LongDataEntry("lastActivityTime", now - 60_000));

        when(timeseriesService.findLatest(eq(TENANT_ID), eq(DEVICE_ID), any(List.class)))
                .thenReturn(Futures.immediateFuture(List.of(lastActivity)));

        var resp = service.pingDevice(TENANT_ID, DEVICE_ID);

        assertThat(resp.getDeviceId()).isEqualTo(DEVICE_ID.getId().toString());
        assertThat(resp.isReachable()).isTrue();
        assertThat(resp.getLastSeen()).isNotNull();
    }

    @Test
    void pingDevice_shouldNotBeReachable_whenLastActivityOutsideWindow() throws Exception {
        ReflectionTestUtils.setField(service, "reachabilityWindowMinutes", 5L);

        long now = System.currentTimeMillis();
        TsKvEntry lastActivity = new BasicTsKvEntry(now - 10 * 60_000, new LongDataEntry("lastActivityTime", now - 10 * 60_000));

        when(timeseriesService.findLatest(eq(TENANT_ID), eq(DEVICE_ID), any(List.class)))
                .thenReturn(Futures.immediateFuture(List.of(lastActivity)));

        var resp = service.pingDevice(TENANT_ID, DEVICE_ID);

        assertThat(resp.isReachable()).isFalse();
        assertThat(resp.getLastSeen()).isNotNull();
    }

    @Test
    void pingDevice_shouldFallbackToLatestTs_whenNoLastActivityKey() throws Exception {
        ReflectionTestUtils.setField(service, "reachabilityWindowMinutes", 5L);

        when(timeseriesService.findLatest(eq(TENANT_ID), eq(DEVICE_ID), any(List.class)))
                .thenReturn(Futures.immediateFuture(List.of()));

        long ts1 = System.currentTimeMillis() - 3 * 60_000;
        long ts2 = System.currentTimeMillis() - 2 * 60_000;
        TsKvEntry e1 = new BasicTsKvEntry(ts1, new LongDataEntry("temperature", 25L));
        TsKvEntry e2 = new BasicTsKvEntry(ts2, new LongDataEntry("humidity", 40L));

        when(timeseriesService.findAllLatest(eq(TENANT_ID), eq(DEVICE_ID)))
                .thenReturn(Futures.immediateFuture(List.of(e1, e2)));

        var resp = service.pingDevice(TENANT_ID, DEVICE_ID);

        assertThat(resp.getLastSeen()).isEqualTo(ts2);
        assertThat(resp.isReachable()).isTrue();
    }
}
