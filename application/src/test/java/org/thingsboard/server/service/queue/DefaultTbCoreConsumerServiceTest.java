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
package org.thingsboard.server.service.queue;

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
import org.thingsboard.server.service.state.DeviceStateService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class DefaultTbCoreConsumerServiceTest {

    @Mock
    private DeviceStateService stateServiceMock;
    @Mock
    private TbCoreConsumerStats statsMock;

    @Mock
    private TbCallback tbCallbackMock;

    @Mock
    private DefaultTbCoreConsumerService defaultTbCoreConsumerServiceMock;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stateService", stateServiceMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
    }

    @Test
    public void givenConnectMsg_whenForwardToStateService_thenOnDeviceConnectAndCallbackAreCalled() {
        // GIVEN
        var tenantId = new TenantId(UUID.randomUUID());
        var deviceId = new DeviceId(UUID.randomUUID());
        var time = System.currentTimeMillis();

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
        then(stateServiceMock).should(times(1)).onDeviceConnect(tenantId, deviceId, time);
        then(tbCallbackMock).should(times(1)).onSuccess();
    }

    @Test
    public void givenOnDeviceConnectThrowsException_whenForwardToStateService_thenOnlyCallbackOnFailureIsCalled() {
        // GIVEN
        var tenantId = new TenantId(UUID.randomUUID());
        var deviceId = new DeviceId(UUID.randomUUID());
        var time = System.currentTimeMillis();

        var connectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastConnectTime(time)
                .build();

        var runtimeException = new RuntimeException("Something bad happened!");

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(connectMsg, tbCallbackMock);
        doThrow(runtimeException).when(stateServiceMock).onDeviceConnect(tenantId, deviceId, time);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(connectMsg, tbCallbackMock);

        // THEN
        then(tbCallbackMock).should(never()).onSuccess();
        then(tbCallbackMock).should(times(1)).onFailure(runtimeException);
    }

    @Test
    public void givenActivityMsgAndStatsAreEnabled_whenForwardToStateService_thenOnDeviceActivityAndCallbackAreCalled() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", true);

        var tenantId = new TenantId(UUID.randomUUID());
        var deviceId = new DeviceId(UUID.randomUUID());
        var time = System.currentTimeMillis();

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
        then(statsMock).should(times(1)).log(activityMsg);
        then(stateServiceMock).should(times(1)).onDeviceActivity(tenantId, deviceId, time);
        then(tbCallbackMock).should(times(1)).onSuccess();
    }

    @Test
    public void givenOnDeviceActivityThrowsException_whenForwardToStateService_thenOnlyCallbackOnFailureIsCalled() {
        // GIVEN
        var tenantId = new TenantId(UUID.randomUUID());
        var deviceId = new DeviceId(UUID.randomUUID());
        var time = System.currentTimeMillis();

        var activityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastActivityTime(time)
                .build();

        var runtimeException = new RuntimeException("Something bad happened!");

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(activityMsg, tbCallbackMock);
        doThrow(runtimeException).when(stateServiceMock).onDeviceActivity(tenantId, deviceId, time);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(activityMsg, tbCallbackMock);

        // THEN
        then(tbCallbackMock).should(never()).onSuccess();

        var exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        then(tbCallbackMock).should(times(1)).onFailure(exceptionCaptor.capture());

        assertThat(exceptionCaptor.getValue())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to update device activity for device [" + deviceId.getId() + "]!");
    }

    @Test
    public void givenDisconnectMsg_whenForwardToStateService_thenOnDeviceDisconnectAndCallbackAreCalled() {
        // GIVEN
        var tenantId = new TenantId(UUID.randomUUID());
        var deviceId = new DeviceId(UUID.randomUUID());
        var time = System.currentTimeMillis();

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
        then(stateServiceMock).should(times(1)).onDeviceDisconnect(tenantId, deviceId, time);
        then(tbCallbackMock).should(times(1)).onSuccess();
    }

    @Test
    public void givenOnDeviceDisconnectThrowsException_whenForwardToStateService_thenOnlyCallbackOnFailureIsCalled() {
        // GIVEN
        var tenantId = new TenantId(UUID.randomUUID());
        var deviceId = new DeviceId(UUID.randomUUID());
        var time = System.currentTimeMillis();

        var disconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastDisconnectTime(time)
                .build();

        var runtimeException = new RuntimeException("Something bad happened!");

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(disconnectMsg, tbCallbackMock);
        doThrow(runtimeException).when(stateServiceMock).onDeviceDisconnect(tenantId, deviceId, time);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(disconnectMsg, tbCallbackMock);

        // THEN
        then(tbCallbackMock).should(never()).onSuccess();
        then(tbCallbackMock).should(times(1)).onFailure(runtimeException);
    }

    @Test
    public void givenInactivityMsg_whenForwardToStateService_thenOnDeviceInactivityAndCallbackAreCalled() {
        // GIVEN
        var tenantId = new TenantId(UUID.randomUUID());
        var deviceId = new DeviceId(UUID.randomUUID());
        var time = System.currentTimeMillis();

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
        then(stateServiceMock).should(times(1)).onDeviceInactivity(tenantId, deviceId, time);
        then(tbCallbackMock).should(times(1)).onSuccess();
    }

    @Test
    public void givenOnDeviceInactivityThrowsException_whenForwardToStateService_thenOnlyCallbackOnFailureIsCalled() {
        // GIVEN
        var tenantId = new TenantId(UUID.randomUUID());
        var deviceId = new DeviceId(UUID.randomUUID());
        var time = System.currentTimeMillis();

        var inactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastInactivityTime(time)
                .build();

        var runtimeException = new RuntimeException("Something bad happened!");

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(inactivityMsg, tbCallbackMock);
        doThrow(runtimeException).when(stateServiceMock).onDeviceInactivity(tenantId, deviceId, time);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(inactivityMsg, tbCallbackMock);

        // THEN
        then(tbCallbackMock).should(never()).onSuccess();
        then(tbCallbackMock).should(times(1)).onFailure(runtimeException);
    }

}
