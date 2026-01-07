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
package org.thingsboard.server.service.queue;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.ruleengine.RuleEngineCallService;
import org.thingsboard.server.service.state.DeviceStateService;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class DefaultTbCoreConsumerServiceTest {

    @Mock
    private DeviceStateService stateServiceMock;
    @Mock
    private TbCoreConsumerStats statsMock;
    @Mock
    private RuleEngineCallService ruleEngineCallServiceMock;

    @Mock
    private TbCallback tbCallbackMock;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());
    private final long time = System.currentTimeMillis();

    private ListeningExecutorService executor;

    @Mock
    private DefaultTbCoreConsumerService defaultTbCoreConsumerServiceMock;

    @BeforeEach
    public void setup() {
        executor = MoreExecutors.newDirectExecutorService();
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stateService", stateServiceMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "deviceActivityEventsExecutor", executor);
    }

    @AfterEach
    public void cleanup() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10L, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    public void givenProcessingSuccess_whenForwardingDeviceStateMsgToStateService_thenOnSuccessCallbackIsCalled() {
        // GIVEN
        var stateMsg = TransportProtos.DeviceStateServiceMsgProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setAdded(true)
                .setUpdated(false)
                .setDeleted(false)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(stateMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(stateMsg, tbCallbackMock);

        // THEN
        then(stateServiceMock).should().onQueueMsg(stateMsg, tbCallbackMock);
    }

    @Test
    public void givenStatsEnabled_whenForwardingDeviceStateMsgToStateService_thenStatsAreRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", true);

        var stateMsg = TransportProtos.DeviceStateServiceMsgProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setAdded(true)
                .setUpdated(false)
                .setDeleted(false)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(stateMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(stateMsg, tbCallbackMock);

        // THEN
        then(statsMock).should().log(stateMsg);
    }

    @Test
    public void givenStatsDisabled_whenForwardingDeviceStateMsgToStateService_thenStatsAreNotRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", false);

        var stateMsg = TransportProtos.DeviceStateServiceMsgProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setAdded(true)
                .setUpdated(false)
                .setDeleted(false)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(stateMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(stateMsg, tbCallbackMock);

        // THEN
        then(statsMock).should(never()).log(stateMsg);
    }

    @Test
    public void givenProcessingSuccess_whenForwardingConnectMsgToStateService_thenOnSuccessCallbackIsCalled() {
        // GIVEN
        var connectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastConnectTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(connectMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(connectMsg, tbCallbackMock);

        // THEN
        then(stateServiceMock).should().onDeviceConnect(tenantId, deviceId, time);
        then(tbCallbackMock).should().onSuccess();
        then(tbCallbackMock).should(never()).onFailure(any());
    }

    @Test
    public void givenProcessingFailure_whenForwardingConnectMsgToStateService_thenOnFailureCallbackIsCalled() {
        // GIVEN
        var connectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastConnectTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(connectMsg, tbCallbackMock);

        var runtimeException = new RuntimeException("Something bad happened!");
        doThrow(runtimeException).when(stateServiceMock).onDeviceConnect(tenantId, deviceId, time);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(connectMsg, tbCallbackMock);

        // THEN
        then(tbCallbackMock).should(never()).onSuccess();
        then(tbCallbackMock).should().onFailure(runtimeException);
    }

    @Test
    public void givenStatsEnabled_whenForwardingConnectMsgToStateService_thenStatsAreRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", true);

        var connectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastConnectTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(connectMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(connectMsg, tbCallbackMock);

        // THEN
        then(statsMock).should().log(connectMsg);
    }

    @Test
    public void givenStatsDisabled_whenForwardingConnectMsgToStateService_thenStatsAreNotRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", false);

        var connectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastConnectTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(connectMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(connectMsg, tbCallbackMock);

        // THEN
        then(statsMock).should(never()).log(connectMsg);
    }

    @Test
    public void givenProcessingSuccess_whenForwardingActivityMsgToStateService_thenOnSuccessCallbackIsCalled() {
        // GIVEN
        var activityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastActivityTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(activityMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(activityMsg, tbCallbackMock);

        // THEN
        then(stateServiceMock).should().onDeviceActivity(tenantId, deviceId, time);
        then(tbCallbackMock).should().onSuccess();
        then(tbCallbackMock).should(never()).onFailure(any());
    }

    @Test
    public void givenProcessingFailure_whenForwardingActivityMsgToStateService_thenOnFailureCallbackIsCalled() {
        // GIVEN
        var activityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastActivityTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(activityMsg, tbCallbackMock);

        var runtimeException = new RuntimeException("Something bad happened!");
        doThrow(runtimeException).when(stateServiceMock).onDeviceActivity(tenantId, deviceId, time);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(activityMsg, tbCallbackMock);

        // THEN
        then(tbCallbackMock).should(never()).onSuccess();

        var exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        then(tbCallbackMock).should().onFailure(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to update device activity for device [" + deviceId.getId() + "]!")
                .hasCause(runtimeException);
    }

    @Test
    public void givenStatsEnabled_whenForwardingActivityMsgToStateService_thenStatsAreRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", true);

        var activityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastActivityTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(activityMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(activityMsg, tbCallbackMock);

        // THEN
        then(statsMock).should().log(activityMsg);
    }

    @Test
    public void givenStatsDisabled_whenForwardingActivityMsgToStateService_thenStatsAreNotRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", false);

        var activityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastActivityTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(activityMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(activityMsg, tbCallbackMock);

        // THEN
        then(statsMock).should(never()).log(activityMsg);
    }

    @Test
    public void givenProcessingSuccess_whenForwardingDisconnectMsgToStateService_thenOnSuccessCallbackIsCalled() {
        // GIVEN
        var disconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastDisconnectTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(disconnectMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(disconnectMsg, tbCallbackMock);

        // THEN
        then(stateServiceMock).should().onDeviceDisconnect(tenantId, deviceId, time);
        then(tbCallbackMock).should().onSuccess();
        then(tbCallbackMock).should(never()).onFailure(any());
    }

    @Test
    public void givenProcessingFailure_whenForwardingDisconnectMsgToStateService_thenOnFailureCallbackIsCalled() {
        // GIVEN
        var disconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastDisconnectTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(disconnectMsg, tbCallbackMock);

        var runtimeException = new RuntimeException("Something bad happened!");
        doThrow(runtimeException).when(stateServiceMock).onDeviceDisconnect(tenantId, deviceId, time);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(disconnectMsg, tbCallbackMock);

        // THEN
        then(tbCallbackMock).should(never()).onSuccess();
        then(tbCallbackMock).should().onFailure(runtimeException);
    }

    @Test
    public void givenStatsEnabled_whenForwardingDisconnectMsgToStateService_thenStatsAreRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", true);

        var disconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastDisconnectTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(disconnectMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(disconnectMsg, tbCallbackMock);

        // THEN
        then(statsMock).should().log(disconnectMsg);
    }

    @Test
    public void givenStatsDisabled_whenForwardingDisconnectMsgToStateService_thenStatsAreNotRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", false);

        var disconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastDisconnectTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(disconnectMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(disconnectMsg, tbCallbackMock);

        // THEN
        then(statsMock).should(never()).log(disconnectMsg);
    }

    @Test
    public void givenProcessingSuccess_whenForwardingInactivityMsgToStateService_thenOnSuccessCallbackIsCalled() {
        // GIVEN
        var inactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastInactivityTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(inactivityMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(inactivityMsg, tbCallbackMock);

        // THEN
        then(stateServiceMock).should().onDeviceInactivity(tenantId, deviceId, time);
        then(tbCallbackMock).should().onSuccess();
        then(tbCallbackMock).should(never()).onFailure(any());
    }

    @Test
    public void givenProcessingFailure_whenForwardingInactivityMsgToStateService_thenOnFailureCallbackIsCalled() {
        // GIVEN
        var inactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastInactivityTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(inactivityMsg, tbCallbackMock);

        var runtimeException = new RuntimeException("Something bad happened!");
        doThrow(runtimeException).when(stateServiceMock).onDeviceInactivity(tenantId, deviceId, time);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(inactivityMsg, tbCallbackMock);

        // THEN
        then(tbCallbackMock).should(never()).onSuccess();
        then(tbCallbackMock).should().onFailure(runtimeException);
    }

    @Test
    public void givenStatsEnabled_whenForwardingInactivityMsgToStateService_thenStatsAreRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", true);

        var inactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastInactivityTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(inactivityMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(inactivityMsg, tbCallbackMock);

        // THEN
        then(statsMock).should().log(inactivityMsg);
    }

    @Test
    public void givenStatsDisabled_whenForwardingInactivityMsgToStateService_thenStatsAreNotRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", false);

        var inactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastInactivityTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(inactivityMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(inactivityMsg, tbCallbackMock);

        // THEN
        then(statsMock).should(never()).log(inactivityMsg);
    }

    @Test
    public void givenProcessingSuccess_whenForwardingInactivityTimeoutUpdateMsgToStateService_thenOnSuccessCallbackIsCalled() {
        // GIVEN
        var inactivityTimeoutUpdateMsg = TransportProtos.DeviceInactivityTimeoutUpdateProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setInactivityTimeout(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(inactivityTimeoutUpdateMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(inactivityTimeoutUpdateMsg, tbCallbackMock);

        // THEN
        then(stateServiceMock).should().onDeviceInactivityTimeoutUpdate(tenantId, deviceId, time);
        then(tbCallbackMock).should().onSuccess();
        then(tbCallbackMock).should(never()).onFailure(any());
    }

    @Test
    public void givenProcessingFailure_whenForwardingInactivityTimeoutUpdateMsgToStateService_thenOnFailureCallbackIsCalled() {
        // GIVEN
        var inactivityTimeoutUpdateMsg = TransportProtos.DeviceInactivityTimeoutUpdateProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setInactivityTimeout(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(inactivityTimeoutUpdateMsg, tbCallbackMock);

        var runtimeException = new RuntimeException("Something bad happened!");
        doThrow(runtimeException).when(stateServiceMock).onDeviceInactivityTimeoutUpdate(tenantId, deviceId, time);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(inactivityTimeoutUpdateMsg, tbCallbackMock);

        // THEN
        then(tbCallbackMock).should(never()).onSuccess();
        then(tbCallbackMock).should().onFailure(runtimeException);
    }

    @Test
    public void givenStatsEnabled_whenForwardingInactivityTimeoutUpdateMsgToStateService_thenStatsAreRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", true);

        var inactivityTimeoutUpdateMsg = TransportProtos.DeviceInactivityTimeoutUpdateProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setInactivityTimeout(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(inactivityTimeoutUpdateMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(inactivityTimeoutUpdateMsg, tbCallbackMock);

        // THEN
        then(statsMock).should().log(inactivityTimeoutUpdateMsg);
    }

    @Test
    public void givenStatsDisabled_whenForwardingInactivityTimeoutUpdateMsgToStateService_thenStatsAreNotRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", false);

        var inactivityTimeoutUpdateMsg = TransportProtos.DeviceInactivityTimeoutUpdateProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setInactivityTimeout(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(inactivityTimeoutUpdateMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(inactivityTimeoutUpdateMsg, tbCallbackMock);

        // THEN
        then(statsMock).should(never()).log(inactivityTimeoutUpdateMsg);
    }

    @Test
    public void givenRestApiCallResponseMsgProto_whenForwardToRuleEngineCallService_thenCallOnQueueMsg() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "ruleEngineCallService", ruleEngineCallServiceMock);
        var restApiCallResponseMsgProto = TransportProtos.RestApiCallResponseMsgProto.getDefaultInstance();
        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToRuleEngineCallService(restApiCallResponseMsgProto, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToRuleEngineCallService(restApiCallResponseMsgProto, tbCallbackMock);

        // THEN
        then(ruleEngineCallServiceMock).should().onQueueMsg(restApiCallResponseMsgProto, tbCallbackMock);
    }

}
