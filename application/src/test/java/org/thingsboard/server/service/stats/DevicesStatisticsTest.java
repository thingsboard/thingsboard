/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.stats.EntityStatistics;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.stats.EntityStatisticsDao;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.apiusage.ApiStatsKey;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.stats.device.DeviceClass;
import org.thingsboard.server.service.stats.device.DeviceStats;
import org.thingsboard.server.service.stats.device.DevicesStatisticsService;
import org.thingsboard.server.service.stats.device.DevicesSummaryStatistics;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.ApiUsageRecordKey.ACTIVE_DEVICES;
import static org.thingsboard.server.common.data.ApiUsageRecordKey.INACTIVE_DEVICES;

@DaoSqlTest
@TestPropertySource(properties = {
        "usage.stats.report.enabled=true",
        "usage.stats.report.enabled_per_entity=true",
        "usage.stats.devices.enabled=true",
        "transport.http.enabled=true",
        "state.defaultStateCheckIntervalInSec=5",
        "usage.stats.report.interval=10"
})
public class DevicesStatisticsTest extends AbstractControllerTest {

    @SpyBean
    private DevicesStatisticsService statisticsService;
    @Autowired
    private TbApiUsageStateService apiUsageStateService;
    @Autowired
    private EntityStatisticsDao entityStatisticsDao;
    @Autowired
    private TimeseriesService timeseriesService;

    private ApiUsageStateId apiUsageStateId;

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
        apiUsageStateId = apiUsageStateService.getApiUsageState(tenantId).getId();
    }

    @Test
    public void testDevicesClassification() throws Exception {
        Device deviceS = createDevice("S", "s");
        Device deviceM = createDevice("M", "m");

        long msgCountS = DeviceClass.S.getMaxDailyMsgCount() - 1;
        for (long i = 0; i < msgCountS; i++) {
            String telemetry = "{\"dp1\": 1, \"dp2\": 2}";
            postTelemetry("s", telemetry);
        }

        long msgCountM = DeviceClass.M.getMaxDailyMsgCount() - 1;
        for (long i = 0; i < msgCountM; i++) {
            String telemetry = "{\"dp1\": 1, \"dp2\": 2}";
            postTelemetry("m", telemetry);
        }

        await().atMost(20, TimeUnit.SECONDS)
                .until(() -> getLatestStats(ApiStatsKey.of(ApiUsageRecordKey.TRANSPORT_MSG_COUNT, deviceS.getUuidId()), true) != null &&
                        getLatestStats(ApiStatsKey.of(ApiUsageRecordKey.TRANSPORT_DP_COUNT, deviceS.getUuidId()), true) != null);

        when(((BaseEntitiesStatisticsService) statisticsService).getCalculationPeriod())
                .thenReturn(Pair.of(
                        LocalDate.now().atStartOfDay().atZone(ZoneOffset.UTC).toInstant().toEpochMilli(),
                        LocalDate.now().atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
                ));
        statisticsService.calculateStats();

        Map<EntityId, EntityStatistics> devicesStats = entityStatisticsDao.findByTenantIdAndEntityType(tenantId, EntityType.DEVICE, new PageLink(10)).getData().stream()
                .collect(Collectors.toMap(EntityStatistics::getEntityId, v -> v));
        assertThat(devicesStats.get(deviceS.getId())).extracting(EntityStatistics::getLatestValue).asInstanceOf(type(DeviceStats.class))
                .satisfies(stats -> {
                    assertThat(stats.getDailyMsgCount()).isEqualTo(msgCountS);
                    assertThat(stats.getDailyDataPointsCount()).isEqualTo(msgCountS * 2);
                    assertThat(stats.getDeviceClass()).isEqualTo(DeviceClass.S);
                });
        assertThat(devicesStats.get(deviceM.getId())).extracting(EntityStatistics::getLatestValue).asInstanceOf(type(DeviceStats.class))
                .satisfies(stats -> {
                    assertThat(stats.getDailyMsgCount()).isEqualTo(msgCountM);
                    assertThat(stats.getDailyDataPointsCount()).isEqualTo(msgCountM * 2);
                    assertThat(stats.getDeviceClass()).isEqualTo(DeviceClass.M);
                });

        loginSysAdmin();
        DevicesSummaryStatistics systemScopeDevicesStatistics = doGet("/api/admin/statistics/devices", DevicesSummaryStatistics.class);
        assertThat(systemScopeDevicesStatistics.getTenantId()).isNull();
        assertThat(systemScopeDevicesStatistics.getCurrentTotalDevicesCount()).isEqualTo(2);
        assertThat(systemScopeDevicesStatistics.getCurrentPerClassDevicesCount().get(DeviceClass.S)).isEqualTo(1);
        assertThat(systemScopeDevicesStatistics.getCurrentPerClassDevicesCount().get(DeviceClass.M)).isEqualTo(1);

        DevicesSummaryStatistics tenantScopeDevicesStatistics = doGet("/api/admin/statistics/devices?tenantId=" + tenantId, DevicesSummaryStatistics.class);
        assertThat(tenantScopeDevicesStatistics.getTenantId()).isEqualTo(tenantId);
        assertThat(tenantScopeDevicesStatistics.getCurrentTotalDevicesCount()).isEqualTo(2);
        assertThat(tenantScopeDevicesStatistics.getCurrentPerClassDevicesCount().get(DeviceClass.S)).isEqualTo(1);
        assertThat(tenantScopeDevicesStatistics.getCurrentPerClassDevicesCount().get(DeviceClass.M)).isEqualTo(1);
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

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> Long.valueOf(0).equals(getLatestStats(ApiStatsKey.of(ACTIVE_DEVICES), false)) &&
                        Long.valueOf(activeDevicesCount + inactiveDevicesCount)
                                .equals(getLatestStats(ApiStatsKey.of(INACTIVE_DEVICES), false)));

        for (Device device : activeDevices) {
            postTelemetry(device.getName(), "{\"dp\":1}");
        }

        await().atMost(15, TimeUnit.SECONDS)
                .until(() -> getLatestStats(ApiStatsKey.of(ACTIVE_DEVICES), false) > 0);
        assertThat(getLatestStats(ApiStatsKey.of(ACTIVE_DEVICES), false)).isEqualTo(activeDevicesCount);
        assertThat(getLatestStats(ApiStatsKey.of(INACTIVE_DEVICES), false)).isEqualTo(inactiveDevicesCount);
    }

    private Long getLatestStats(ApiStatsKey statsKey, boolean hourly) throws Exception {
        return timeseriesService.findLatest(tenantId, apiUsageStateId, List.of(statsKey.getEntryKey(hourly))).get().stream()
                .findFirst().flatMap(KvEntry::getLongValue).orElse(null);
    }

    private void postTelemetry(String accessToken, String json) throws Exception {
        doPost("/api/v1/" + accessToken + "/telemetry", json, new String[0]).andExpect(status().isOk());
    }

}
