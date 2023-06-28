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
package org.thingsboard.server.service.state;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.sql.query.EntityQueryRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.INACTIVITY_TIMEOUT;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDeviceStateServiceTest {

    @Mock
    DeviceService deviceService;
    @Mock
    AttributesService attributesService;
    @Mock
    TimeseriesService tsService;
    @Mock
    TbClusterService clusterService;
    @Mock
    PartitionService partitionService;
    @Mock
    DeviceStateData deviceStateDataMock;
    @Mock
    EntityQueryRepository entityQueryRepository;

    DeviceId deviceId = DeviceId.fromString("00797a3b-7aeb-4b5b-b57a-c2a810d0f112");

    DefaultDeviceStateService service;

    @Before
    public void setUp() {
        service = spy(new DefaultDeviceStateService(deviceService, attributesService, tsService, clusterService, partitionService, entityQueryRepository, null, null, mock(NotificationRuleProcessor.class)));
    }

    @Test
    public void givenDeviceIdFromDeviceStatesMap_whenGetOrFetchDeviceStateData_thenNoStackOverflow() {
        service.deviceStates.put(deviceId, deviceStateDataMock);
        DeviceStateData deviceStateData = service.getOrFetchDeviceStateData(deviceId);
        assertThat(deviceStateData, is(deviceStateDataMock));
        Mockito.verify(service, never()).fetchDeviceStateDataUsingEntityDataQuery(deviceId);
    }

    @Test
    public void givenDeviceIdWithoutDeviceStateInMap_whenGetOrFetchDeviceStateData_thenFetchDeviceStateData() {
        service.deviceStates.clear();
        willReturn(deviceStateDataMock).given(service).fetchDeviceStateDataUsingEntityDataQuery(deviceId);
        DeviceStateData deviceStateData = service.getOrFetchDeviceStateData(deviceId);
        assertThat(deviceStateData, is(deviceStateDataMock));
        Mockito.verify(service, times(1)).fetchDeviceStateDataUsingEntityDataQuery(deviceId);
    }

    @Test
    public void givenPersistToTelemetryAndDefaultInactivityTimeoutFetched_whenTransformingToDeviceStateData_thenTryGetInactivityFromAttribute() {
        var defaultInactivityTimeoutInSec = 60L;
        var latest =
                Map.of(
                        EntityKeyType.TIME_SERIES, Map.of(INACTIVITY_TIMEOUT, new TsValue(0, Long.toString(defaultInactivityTimeoutInSec * 1000))),
                        EntityKeyType.SERVER_ATTRIBUTE, Map.of(INACTIVITY_TIMEOUT, new TsValue(0, Long.toString(5000L)))
                );

        process(latest, defaultInactivityTimeoutInSec);
    }

    @Test
    public void givenPersistToTelemetryAndNoInactivityTimeoutFetchedFromTimeSeries_whenTransformingToDeviceStateData_thenTryGetInactivityFromAttribute() {
        var defaultInactivityTimeoutInSec = 60L;
        var latest =
                Map.of(
                        EntityKeyType.SERVER_ATTRIBUTE, Map.of(INACTIVITY_TIMEOUT, new TsValue(0, Long.toString(5000L)))
                );

        process(latest, defaultInactivityTimeoutInSec);
    }

    private void process(Map<EntityKeyType, Map<String, TsValue>> latest, long defaultInactivityTimeoutInSec) {
        service.setDefaultInactivityTimeoutInSec(defaultInactivityTimeoutInSec);
        service.setDefaultInactivityTimeoutMs(defaultInactivityTimeoutInSec * 1000);
        service.setPersistToTelemetry(true);

        var deviceUuid = UUID.randomUUID();
        var deviceId = new DeviceId(deviceUuid);

        DeviceStateData deviceStateData = service.toDeviceStateData(new EntityData(deviceId, latest, Map.of()), new DeviceIdInfo(TenantId.SYS_TENANT_ID.getId(), UUID.randomUUID(), deviceUuid));

        Assert.assertEquals(5000L, deviceStateData.getState().getInactivityTimeout());
    }

    @Test
    public void givenUpdateInactivityTimeoutAndThenNoStateChange() throws Exception {
        TelemetrySubscriptionService telemetrySubscriptionService = Mockito.mock(TelemetrySubscriptionService.class);
        ReflectionTestUtils.setField(service, "tsSubService", telemetrySubscriptionService);
        ReflectionTestUtils.setField(service, "defaultStateCheckIntervalInSec", 60);
        ReflectionTestUtils.setField(service, "defaultActivityStatsIntervalInSec", 60);
        ReflectionTestUtils.setField(service, "defaultInactivityTimeoutMs", 60000);
        ReflectionTestUtils.setField(service, "initFetchPackSize", 10);

        Mockito.when(entityQueryRepository.findEntityDataByQueryInternal(Mockito.any())).thenReturn(new PageData<>());

        service.init();
        var tenantId = new TenantId(UUID.randomUUID());
        var tpi = TopicPartitionInfo.builder().myPartition(true).build();
        Mockito.when(partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId)).thenReturn(tpi);

        var deviceIdInfo = new DeviceIdInfo(tenantId.getId(), null, deviceId.getId());

        Mockito.when(deviceService.findDeviceIdInfos(Mockito.any()))
                .thenReturn(new PageData<>(List.of(deviceIdInfo), 0, 1, false));

        Method method = AbstractPartitionBasedService.class.getDeclaredMethod("initStateFromDB", Set.class);
        method.setAccessible(true);
        method.invoke(service, Collections.singleton(tpi));

        service.onAddedPartitions(Collections.singleton(tpi));

        DeviceState deviceState = DeviceState.builder().build();

        DeviceStateData deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(deviceState)
                .metaData(new TbMsgMetaData())
                .build();

        service.deviceStates.put(deviceId, deviceStateData);

        service.onDeviceActivity(tenantId, deviceId, System.currentTimeMillis());

        Mockito.verify(telemetrySubscriptionService, Mockito.times(1)).saveAttrAndNotify(Mockito.any(), Mockito.eq(deviceId), Mockito.any(), Mockito.eq("active"), Mockito.eq(true), Mockito.any());

        Mockito.reset(telemetrySubscriptionService);

        service.onDeviceInactivityTimeoutUpdate(tenantId, deviceId, 1);

        Mockito.verify(telemetrySubscriptionService, Mockito.never()).saveAttrAndNotify(Mockito.any(), Mockito.eq(deviceId), Mockito.any(), Mockito.eq("active"), Mockito.eq(false), Mockito.any());
        service.onDeviceInactivityTimeoutUpdate(tenantId, deviceId, 60000);

        Mockito.verify(telemetrySubscriptionService, Mockito.never()).saveAttrAndNotify(Mockito.any(), Mockito.eq(deviceId), Mockito.any(), Mockito.eq("active"), Mockito.eq(true), Mockito.any());
    }

}