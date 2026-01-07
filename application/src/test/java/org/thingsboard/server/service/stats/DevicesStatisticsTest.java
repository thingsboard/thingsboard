/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.stats;

import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.state.DeviceStateService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DaoSqlTest
@TestPropertySource(properties = {
        "usage.stats.report.enabled=true",
        "usage.stats.report.interval=2",
        "usage.stats.gauge_report_interval=1",
        "usage.stats.devices.report_interval=3",
        "state.defaultStateCheckIntervalInSec=3",
        "state.defaultInactivityTimeoutInSec=10",
})
public class DevicesStatisticsTest extends AbstractControllerTest {

    @Autowired
    private TbApiUsageStateService apiUsageStateService;
    @Autowired
    private TimeseriesService timeseriesService;
    @Autowired
    private DeviceStateService deviceStateService;

    private ApiUsageStateId apiUsageStateId;

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
        apiUsageStateId = apiUsageStateService.getApiUsageState(tenantId).getId();
    }

    @Test
    public void testDevicesActivityStats() throws Exception {
        int activeDevicesCount = 5;
        List<Device> activeDevices = new ArrayList<>();
        for (int i = 1; i <= activeDevicesCount; i++) {
            String name = "active_device_" + i;
            Device device = createDevice(name, name);
            activeDevices.add(device);
        }
        int inactiveDevicesCount = 10;
        List<Device> inactiveDevices = new ArrayList<>();
        for (int i = 1; i <= inactiveDevicesCount; i++) {
            String name = "inactive_device_" + i;
            Device device = createDevice(name, name);
            inactiveDevices.add(device);
        }

        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(getLatestStats(ApiUsageRecordKey.ACTIVE_DEVICES, false)).isZero();
                    assertThat(getLatestStats(ApiUsageRecordKey.ACTIVE_DEVICES, true)).isZero();
                    assertThat(getLatestStats(ApiUsageRecordKey.INACTIVE_DEVICES, false)).isEqualTo(activeDevicesCount + inactiveDevicesCount);
                    assertThat(getLatestStats(ApiUsageRecordKey.INACTIVE_DEVICES, true)).isEqualTo(activeDevicesCount + inactiveDevicesCount);
                });

        for (Device device : activeDevices) {
            deviceStateService.onDeviceActivity(tenantId, device.getId(), System.currentTimeMillis());
        }

        await().atMost(40, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(getLatestStats(ApiUsageRecordKey.ACTIVE_DEVICES, false)).isEqualTo(activeDevicesCount);
                    assertThat(getLatestStats(ApiUsageRecordKey.ACTIVE_DEVICES, true)).isEqualTo(activeDevicesCount);
                    assertThat(getLatestStats(ApiUsageRecordKey.INACTIVE_DEVICES, false)).isEqualTo(inactiveDevicesCount);
                });
    }

    @SneakyThrows
    private Long getLatestStats(ApiUsageRecordKey key, boolean hourly) {
        return timeseriesService.findLatest(tenantId, apiUsageStateId, List.of(key.getApiCountKey() + (hourly ? "Hourly" : "")))
                .get().stream().findFirst().flatMap(KvEntry::getLongValue).orElse(null);
    }

}
