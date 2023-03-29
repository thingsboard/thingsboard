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

import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.stats.EntityStatistics;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.stats.EntityStatisticsDao;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.apiusage.ApiStatsKey;
import org.thingsboard.server.service.apiusage.BaseApiUsageState;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.stats.device.DeviceClass;
import org.thingsboard.server.service.stats.device.DeviceStats;
import org.thingsboard.server.service.stats.device.DevicesStatisticsService;
import org.thingsboard.server.service.stats.device.DevicesSummaryStatistics;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.ApiUsageRecordKey.ACTIVE_DEVICES;
import static org.thingsboard.server.common.data.ApiUsageRecordKey.INACTIVE_DEVICES;
import static org.thingsboard.server.common.msg.tools.SchedulerUtils.getStartOfCurrentHour;

@DaoSqlTest
@TestPropertySource(properties = {
        "usage.stats.report.enabled=true",
        "usage.stats.report.enabled_per_entity=true",
        "usage.stats.devices.enabled=true",
        "transport.http.enabled=true",
        "state.defaultStateCheckIntervalInSec=2",
        "usage.stats.report.interval=2"
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
    @Autowired
    private DeviceService deviceService;

    private ApiUsageStateId apiUsageStateId;

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
        apiUsageStateId = apiUsageStateService.getApiUsageState(tenantId).getId();

        when(((BaseEntitiesStatisticsService) statisticsService).getCalculationPeriod())
                .thenReturn(Pair.of(
                        getStartOfCurrentDay(),
                        getStartOfCurrentDay() + TimeUnit.DAYS.toMillis(1)
                ));
    }

    @After
    public void afterEach() {
        Mockito.reset(statisticsService);
        entityStatisticsDao.deleteByTsBefore(System.currentTimeMillis());
    }

    @Test
    public void testDevicesClassification() throws Exception {
        Device deviceS = createDevice("s", "s");
        Device deviceM = createDevice("m", "m");

        Pair<Long, Long> msgsAndDpsS = generateTelemetryAccordingToClasses(Map.of(DeviceClass.S, List.of(deviceS))).get(DeviceClass.S);
        Pair<Long, Long> msgsAndDpsM = generateTelemetryAccordingToClasses(Map.of(DeviceClass.M, List.of(deviceM))).get(DeviceClass.M);

        statisticsService.calculateStats();

        Map<EntityId, EntityStatistics> devicesStats = entityStatisticsDao.findByTenantIdAndEntityType(tenantId, EntityType.DEVICE, new PageLink(10)).getData().stream()
                .collect(Collectors.toMap(EntityStatistics::getEntityId, v -> v));
        assertThat(devicesStats.get(deviceS.getId())).extracting(EntityStatistics::getLatestValue).asInstanceOf(type(DeviceStats.class))
                .satisfies(stats -> {
                    assertThat(stats.getDailyMsgCount()).isEqualTo(msgsAndDpsS.getFirst());
                    assertThat(stats.getDailyDataPointsCount()).isEqualTo(msgsAndDpsS.getSecond());
                    assertThat(stats.getDeviceClass()).isEqualTo(DeviceClass.S);
                });
        assertThat(devicesStats.get(deviceM.getId())).extracting(EntityStatistics::getLatestValue).asInstanceOf(type(DeviceStats.class))
                .satisfies(stats -> {
                    assertThat(stats.getDailyMsgCount()).isEqualTo(msgsAndDpsM.getFirst());
                    assertThat(stats.getDailyDataPointsCount()).isEqualTo(msgsAndDpsM.getSecond());
                    assertThat(stats.getDeviceClass()).isEqualTo(DeviceClass.M);
                });

        loginSysAdmin();
        DevicesSummaryStatistics systemScopeDevicesStatistics = doGet("/api/admin/statistics/devices?periodTs=" + System.currentTimeMillis(), DevicesSummaryStatistics.class);
        assertThat(systemScopeDevicesStatistics.getTenantId()).isNull();
        assertThat(systemScopeDevicesStatistics.getTotalDevicesCount()).isEqualTo(2);
        assertThat(systemScopeDevicesStatistics.getPerClassDevicesCount().get(DeviceClass.S)).isEqualTo(1);
        assertThat(systemScopeDevicesStatistics.getPerClassDevicesCount().get(DeviceClass.M)).isEqualTo(1);

        DevicesSummaryStatistics tenantScopeDevicesStatistics = doGet("/api/admin/statistics/devices?periodTs=" + System.currentTimeMillis() + "&tenantId=" + tenantId, DevicesSummaryStatistics.class);
        assertThat(tenantScopeDevicesStatistics.getTenantId()).isEqualTo(tenantId);
        assertThat(tenantScopeDevicesStatistics.getTotalDevicesCount()).isEqualTo(2);
        assertThat(tenantScopeDevicesStatistics.getPerClassDevicesCount().get(DeviceClass.S)).isEqualTo(1);
        assertThat(tenantScopeDevicesStatistics.getPerClassDevicesCount().get(DeviceClass.M)).isEqualTo(1);
    }

    @Test
    public void testDevicesClassificationWithDeletedDevices() throws Exception {
        Map<DeviceClass, List<Device>> devices = new HashMap<>();
        int devicesOfEachClass = 5;
        Set<DeviceClass> classes = Set.of(DeviceClass.S, DeviceClass.M);
        int totalCount = classes.size() * devicesOfEachClass;

        Map<DeviceId, DeviceClass> devicesClasses = new HashMap<>();
        for (DeviceClass deviceClass : classes) {
            devices.put(deviceClass, new ArrayList<>());
            for (int i = 0; i < devicesOfEachClass; i++) {
                String name = deviceClass + "_" + i;
                Device device = createDevice(name, name);
                devices.get(deviceClass).add(device);
                devicesClasses.put(device.getId(), deviceClass);
            }
        }
        generateTelemetryAccordingToClasses(devices);

        statisticsService.calculateStats();
        List<EntityStatistics> stats = entityStatisticsDao.findByTenantIdAndEntityType(tenantId, EntityType.DEVICE, new PageLink(100)).getData();
        assertThat(stats).size().isEqualTo(totalCount);

        // so that stats timestamp is updated for existing devices
        when(((BaseEntitiesStatisticsService) statisticsService).getCalculationPeriod())
                .thenReturn(Pair.of(
                        getStartOfCurrentHour(),
                        getStartOfCurrentHour() + 40_000
                ));

        Map<DeviceId, DeviceClass> deletedDevices = new HashMap<>();
        devices.forEach((deviceClass, classDevices) -> {
            for (int i = 0; i < 2; i++) {
                Device device = classDevices.remove(i);
                deviceService.deleteDevice(tenantId, device.getId());
                deletedDevices.put(device.getId(), deviceClass);
            }
        });

        statisticsService.calculateStats();
        stats = entityStatisticsDao.findByTenantIdAndEntityType(tenantId, EntityType.DEVICE, new PageLink(100)).getData();
        assertThat(stats).size().isEqualTo(totalCount);

        Map<EntityId, EntityStatistics> perDeviceStats = stats.stream().collect(Collectors.toMap(EntityStatistics::getEntityId, s -> s));
        long statsTsForDeletedDevices = getStartOfCurrentDay();
        long statsTsForPresentDevices = getStartOfCurrentHour();

        perDeviceStats.forEach((deviceId, deviceStats) -> {
            DeviceStats statsValue = (DeviceStats) deviceStats.getLatestValue();
            assertThat(statsValue.getDeviceClass()).isEqualTo(devicesClasses.get(deviceId));

            if (deletedDevices.containsKey(deviceId)) {
                assertThat(deviceStats.getTs()).isEqualTo(statsTsForDeletedDevices);
            } else {
                assertThat(deviceStats.getTs()).isEqualTo(statsTsForPresentDevices);
            }
        });

        entityStatisticsDao.deleteByTsBefore(statsTsForPresentDevices - 1);
        stats = entityStatisticsDao.findByTenantIdAndEntityType(tenantId, EntityType.DEVICE, new PageLink(100)).getData();
        assertThat(stats).size().isEqualTo(totalCount - deletedDevices.size());
    }

    @Test
    public void testDevicesActivityStats() throws Exception {
        setStaticFieldValue(BaseApiUsageState.class, "gaugeReportInterval", TimeUnit.SECONDS.toMillis(1));
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
                .until(() -> Long.valueOf(0).equals(getLatestStats(ApiStatsKey.of(ACTIVE_DEVICES), false)) &&
                        Long.valueOf(activeDevicesCount + inactiveDevicesCount)
                                .equals(getLatestStats(ApiStatsKey.of(INACTIVE_DEVICES), false)));

        for (Device device : activeDevices) {
            postTelemetry(device.getName(), "{\"dp\":1}");
        }

        await().atMost(20, TimeUnit.SECONDS)
                .until(() -> getLatestStats(ApiStatsKey.of(ACTIVE_DEVICES), false) == activeDevicesCount &&
                        getLatestStats(ApiStatsKey.of(INACTIVE_DEVICES), false) == inactiveDevicesCount);
    }

    private Map<DeviceClass, Pair<Long, Long>> generateTelemetryAccordingToClasses(Map<DeviceClass, List<Device>> devices) throws Exception {
        Map<DeviceClass, Pair<Long, Long>> msgsAndDps = new HashMap<>();
        devices.forEach((deviceClass, classDevices) -> {
            long msgCount = deviceClass.getMaxDailyMsgCount() - 1;
            long dpCount = msgCount * 2;
            for (Device device : classDevices) {
                for (long i = 0; i < msgCount; i++) {
                    String telemetry = "{\"dp1\": 1, \"dp2\": 2}";
                    postTelemetry(device.getName(), telemetry);
                }
            }
            await().atMost(20, TimeUnit.SECONDS)
                    .until(() -> classDevices.stream().allMatch(device -> Long.valueOf(msgCount).equals(getLatestStats(ApiStatsKey.of(ApiUsageRecordKey.TRANSPORT_MSG_COUNT, device.getUuidId()), true)) &&
                            Long.valueOf(dpCount).equals(getLatestStats(ApiStatsKey.of(ApiUsageRecordKey.TRANSPORT_DP_COUNT, device.getUuidId()), true))));
            msgsAndDps.put(deviceClass, Pair.of(msgCount, dpCount));
        });
        return msgsAndDps;
    }

    private long getStartOfCurrentDay() {
        return LocalDate.now().atStartOfDay().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    @SneakyThrows
    private Long getLatestStats(ApiStatsKey statsKey, boolean hourly) {
        return timeseriesService.findLatest(tenantId, apiUsageStateId, List.of(statsKey.getEntryKey(hourly))).get().stream()
                .findFirst().flatMap(KvEntry::getLongValue).orElse(null);
    }

    @SneakyThrows
    private void postTelemetry(String accessToken, String json) {
        doPost("/api/v1/" + accessToken + "/telemetry", json, new String[0]).andExpect(status().isOk());
    }

}
