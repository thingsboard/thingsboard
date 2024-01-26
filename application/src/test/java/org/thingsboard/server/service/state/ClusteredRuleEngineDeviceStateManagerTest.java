/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;

import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class ClusteredRuleEngineDeviceStateManagerTest {

    @Mock
    private static TbClusterService tbClusterServiceMock;
    @Mock
    private static TbCallback tbCallbackMock;
    @Mock
    private static TbQueueMsgMetadata metadataMock;
    @Captor
    private static ArgumentCaptor<TbQueueCallback> queueCallbackCaptor;
    private static ClusteredRuleEngineDeviceStateManager deviceStateManager;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("57ab2e6c-bc4c-11ee-a506-0242ac120002"));
    private static final DeviceId DEVICE_ID = DeviceId.fromString("74a9053e-bc4c-11ee-a506-0242ac120002");
    private static final long EVENT_TS = System.currentTimeMillis();

    @BeforeEach
    public void setup() {
        deviceStateManager = new ClusteredRuleEngineDeviceStateManager(tbClusterServiceMock);
    }

    @ParameterizedTest
    @MethodSource
    public void givenProcessingSuccess_whenOnDeviceAction_thenCallsDeviceStateServiceAndOnSuccessCallback(Runnable onDeviceAction, Runnable actionVerification) {
        // WHEN
        onDeviceAction.run();

        // THEN
        actionVerification.run();

        TbQueueCallback callback = queueCallbackCaptor.getValue();
        callback.onSuccess(metadataMock);
        then(tbCallbackMock).should().onSuccess();

        var runtimeException = new RuntimeException("Something bad happened!");
        callback.onFailure(runtimeException);
        then(tbCallbackMock).should().onFailure(runtimeException);
    }

    private static Stream<Arguments> givenProcessingSuccess_whenOnDeviceAction_thenCallsDeviceStateServiceAndOnSuccessCallback() {
        return Stream.of(
                Arguments.of(
                        (Runnable) () -> deviceStateManager.onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> {
                            var deviceConnectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                                    .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                                    .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                                    .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                                    .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                                    .setLastConnectTime(EVENT_TS)
                                    .build();
                            var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                                    .setDeviceConnectMsg(deviceConnectMsg)
                                    .build();
                            then(tbClusterServiceMock).should().pushMsgToCore(eq(TENANT_ID), eq(DEVICE_ID), eq(toCoreMsg), queueCallbackCaptor.capture());
                        }
                ),
                Arguments.of(
                        (Runnable) () -> deviceStateManager.onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> {
                            var deviceActivityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                                    .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                                    .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                                    .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                                    .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                                    .setLastActivityTime(EVENT_TS)
                                    .build();
                            var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                                    .setDeviceActivityMsg(deviceActivityMsg)
                                    .build();
                            then(tbClusterServiceMock).should().pushMsgToCore(eq(TENANT_ID), eq(DEVICE_ID), eq(toCoreMsg), queueCallbackCaptor.capture());
                        }
                ),
                Arguments.of(
                        (Runnable) () -> deviceStateManager.onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> {
                            var deviceDisconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                                    .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                                    .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                                    .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                                    .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                                    .setLastDisconnectTime(EVENT_TS)
                                    .build();
                            var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                                    .setDeviceDisconnectMsg(deviceDisconnectMsg)
                                    .build();
                            then(tbClusterServiceMock).should().pushMsgToCore(eq(TENANT_ID), eq(DEVICE_ID), eq(toCoreMsg), queueCallbackCaptor.capture());
                        }
                ),
                Arguments.of(
                        (Runnable) () -> deviceStateManager.onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> {
                            var deviceInactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                                    .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                                    .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                                    .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                                    .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                                    .setLastInactivityTime(EVENT_TS)
                                    .build();
                            var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                                    .setDeviceInactivityMsg(deviceInactivityMsg)
                                    .build();
                            then(tbClusterServiceMock).should().pushMsgToCore(eq(TENANT_ID), eq(DEVICE_ID), eq(toCoreMsg), queueCallbackCaptor.capture());
                        }
                )
        );
    }

}
