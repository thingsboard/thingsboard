// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.scheduler.sql;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveOtaPackageInfoRequest;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.scheduler.DefaultSchedulerService;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.DataConstants.UPDATE_FIRMWARE;
import static org.thingsboard.server.common.data.msg.TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;
import static org.thingsboard.server.dao.scheduler.BaseSchedulerEventService.getOriginatorId;

@DaoSqlTest
public class SchedulerEventTest extends AbstractControllerTest {

    @Autowired
    DefaultSchedulerService schedulerService;

    @Autowired
    TbServiceInfoProvider serviceInfoProvider;

    @Autowired
    DeviceGroupOtaPackageService deviceGroupOtaPackageService;

    @Before
    public void before() throws Exception {
        loginTenantAdmin();
        Mockito.reset(tbClusterService);
    }

    private static final String testRpc = "{\"method\":\"test\",\"params\":{\"p1\":1,\"p2\":\"2\"}}";
    private static final Boolean PERSISTENT_TEST_VALUE = true;
    private static final Long TIMEOUT_TEST_VALUE = 7000L;

    @Test
    public void sendRpcRequestToDevice() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);

        ListeningScheduledExecutorService mockExecutor = Mockito.mock(ListeningScheduledExecutorService.class);

        AtomicBoolean isScheduled = new AtomicBoolean(false);

        when(mockExecutor.schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS))).thenAnswer(invocation -> {
            if (!isScheduled.get()) {
                isScheduled.set(true);
                Runnable task = (Runnable) invocation.getArguments()[0];
                task.run();
            }

            return any();
        });

        ReflectionTestUtils.setField(schedulerService, "scheduledExecutor", mockExecutor);

        SchedulerEvent schedulerEvent = createSchedulerEvent(savedDevice.getId(), true);
        SchedulerEvent savedSchedulerEvent = doPost("/api/schedulerEvent", schedulerEvent, SchedulerEvent.class);

        verify(tbClusterService, timeout(10000)).pushMsgToRuleEngine(eq(tenantId), eq(getOriginatorId(savedSchedulerEvent)), argThat(tbMsg -> {
                    if (tbMsg.isTypeOf(RPC_CALL_FROM_SERVER_TO_DEVICE)) {
                        assertEquals(tbMsg.getOriginator(), savedDevice.getId());
                        assertEquals(testRpc, tbMsg.getData());
                        assertEquals(serviceInfoProvider.getServiceId(), tbMsg.getMetaData().getValue("originServiceId"));
                        assertThat(Long.parseLong(tbMsg.getMetaData().getValue("expirationTime"))).isCloseTo(System.currentTimeMillis() + TIMEOUT_TEST_VALUE, byLessThan(10000L));
                        assertThat(tbMsg.getMetaData().getValue("persistent")).isEqualTo(PERSISTENT_TEST_VALUE.toString());
                        assertEquals(serviceInfoProvider.getServiceId(), tbMsg.getMetaData().getValue("originServiceId"));
                        return true;
                    }
                    return false;
                }
        ), any());
    }

    @Test
    public void shouldNotSendRpcRequestToDeviceIfSchedulerEventIsDisabled() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);

        ListeningScheduledExecutorService mockExecutor = Mockito.mock(ListeningScheduledExecutorService.class);

        AtomicBoolean isScheduled = new AtomicBoolean(false);

        when(mockExecutor.schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS))).thenAnswer(invocation -> {
            if (!isScheduled.get()) {
                isScheduled.set(true);
                Runnable task = (Runnable) invocation.getArguments()[0];
                task.run();
            }

            return any();
        });

        ReflectionTestUtils.setField(schedulerService, "scheduledExecutor", mockExecutor);

        SchedulerEvent schedulerEvent = createSchedulerEvent(savedDevice.getId(), false);
        SchedulerEvent savedSchedulerEvent = doPost("/api/schedulerEvent", schedulerEvent, SchedulerEvent.class);

        verify(tbClusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(getOriginatorId(savedSchedulerEvent)), argThat(tbMsg -> {
                    return tbMsg.isTypeOf(RPC_CALL_FROM_SERVER_TO_DEVICE);
                }
        ), any());
    }

    @Test
    public void shouldNotUpdateFirmwareOfDeviceGroupOfAnotherTenant() throws Exception {
        DeviceProfileInfo deviceProfile = doGet("/api/deviceProfileInfo/default", DeviceProfileInfo.class);
        SaveOtaPackageInfoRequest firmwareRequest = new SaveOtaPackageInfoRequest();
        firmwareRequest.setDeviceProfileId(new DeviceProfileId(deviceProfile.getId().getId()));
        firmwareRequest.setType(FIRMWARE);
        firmwareRequest.setTitle("My firmware");
        firmwareRequest.setVersion("v1.0");
        firmwareRequest.setUsesUrl(false);
        OtaPackageInfo firmware = doPost("/api/otaPackage", firmwareRequest, OtaPackageInfo.class);

        loginDifferentTenant();
        EntityGroup otherTenantGroup = new EntityGroup();
        otherTenantGroup.setName("Other Tenant Devices");
        otherTenantGroup.setType(EntityType.DEVICE);
        otherTenantGroup = doPost("/api/entityGroup", otherTenantGroup, EntityGroup.class);
        loginTenantAdmin();

        Object originalExecutor = ReflectionTestUtils.getField(schedulerService, "scheduledExecutor");
        ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
        ReflectionTestUtils.setField(schedulerService, "scheduledExecutor", executor);
        try {
            SchedulerEvent schedulerEvent = new SchedulerEvent();
            schedulerEvent.setName("Firmware update");
            schedulerEvent.setType(UPDATE_FIRMWARE);
            schedulerEvent.setEnabled(true);
            ObjectNode schedule = JacksonUtil.newObjectNode();
            schedule.put("startTime", Long.MAX_VALUE);
            schedule.put("timezone", "UTC");
            schedulerEvent.setSchedule(schedule);
            schedulerEvent.setOriginatorId(otherTenantGroup.getId());
            ObjectNode configuration = JacksonUtil.newObjectNode();
            configuration.set("msgBody", JacksonUtil.valueToTree(firmware.getId()));
            schedulerEvent.setConfiguration(configuration);
            SchedulerEvent savedSchedulerEvent = doPost("/api/schedulerEvent", schedulerEvent, SchedulerEvent.class);

            Map<?, ?> eventsMetaData = (Map<?, ?>) ReflectionTestUtils.getField(schedulerService, "eventsMetaData");
            Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> eventsMetaData.containsKey(savedSchedulerEvent.getId()));
            ReflectionTestUtils.invokeMethod(schedulerService, "processEvent", tenantId, savedSchedulerEvent.getId());
        } finally {
            ReflectionTestUtils.setField(schedulerService, "scheduledExecutor", originalExecutor);
            executor.shutdownNow();
        }
        assertThat(deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(otherTenantGroup.getId(), FIRMWARE)).isNull();
    }

    private SchedulerEvent createSchedulerEvent(EntityId originatorId, boolean enabled) {
        SchedulerEvent schedulerEvent = new SchedulerEvent();
        schedulerEvent.setName("TestRpc");
        schedulerEvent.setType("sendRpcRequest");
        schedulerEvent.setEnabled(enabled);
        ObjectNode schedule = JacksonUtil.newObjectNode();
        schedule.put("startTime", Long.MAX_VALUE);
        schedule.put("timezone", "UTC");
        schedulerEvent.setSchedule(schedule);
        schedulerEvent.setOriginatorId(originatorId);

        ObjectNode configuration = JacksonUtil.newObjectNode();
        configuration.put("msgType", RPC_CALL_FROM_SERVER_TO_DEVICE.name());
        configuration.set("msgBody", JacksonUtil.toJsonNode(testRpc));
        ObjectNode metadata = JacksonUtil.newObjectNode();
        metadata.put("persistent", PERSISTENT_TEST_VALUE);
        metadata.put("timeout", TIMEOUT_TEST_VALUE);
        configuration.set("metadata", metadata);
        schedulerEvent.setConfiguration(configuration);

        return schedulerEvent;
    }

}
