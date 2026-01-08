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
package org.thingsboard.server.service.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class DefaultDeviceStateManagerTest {

    @Mock
    private DeviceStateService deviceStateServiceMock;
    @Mock
    private TbCallback tbCallbackMock;
    @Mock
    private TbClusterService clusterServiceMock;
    @Mock
    private TbQueueMsgMetadata metadataMock;

    @Mock
    private TbServiceInfoProvider serviceInfoProviderMock;
    @Mock
    private PartitionService partitionServiceMock;

    @Captor
    private ArgumentCaptor<TbQueueCallback> queueCallbackCaptor;

    private DefaultDeviceStateManager deviceStateManager;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("57ab2e6c-bc4c-11ee-a506-0242ac120002"));
    private static final DeviceId DEVICE_ID = DeviceId.fromString("74a9053e-bc4c-11ee-a506-0242ac120002");
    private static final long EVENT_TS = System.currentTimeMillis();
    private static final RuntimeException RUNTIME_EXCEPTION = new RuntimeException("Something bad happened!");
    private static final TopicPartitionInfo MY_TPI = TopicPartitionInfo.builder().myPartition(true).build();
    private static final TopicPartitionInfo EXTERNAL_TPI = TopicPartitionInfo.builder().myPartition(false).build();

    @BeforeEach
    public void setup() {
        deviceStateManager = new DefaultDeviceStateManager(serviceInfoProviderMock, partitionServiceMock, Optional.of(deviceStateServiceMock), clusterServiceMock);
    }

    @ParameterizedTest
    @DisplayName("Given event should be routed to local service and event processed has succeeded, " +
            "when onDeviceX() is called, then should route event to local service and call onSuccess() callback.")
    @MethodSource
    public void givenRoutedToLocalAndProcessingSuccess_whenOnDeviceAction_thenShouldCallLocalServiceAndSuccessCallback(
            BiConsumer<DefaultDeviceStateManager, TbCallback> onDeviceAction, Consumer<DeviceStateService> actionVerification
    ) {
        // GIVEN
        given(serviceInfoProviderMock.isService(ServiceType.TB_CORE)).willReturn(true);
        given(partitionServiceMock.resolve(ServiceType.TB_CORE, TENANT_ID, DEVICE_ID)).willReturn(MY_TPI);

        onDeviceAction.accept(deviceStateManager, tbCallbackMock);

        // THEN
        actionVerification.accept(deviceStateServiceMock);

        then(clusterServiceMock).shouldHaveNoInteractions();
        then(tbCallbackMock).should().onSuccess();
        then(tbCallbackMock).should(never()).onFailure(any());
    }

    private static Stream<Arguments> givenRoutedToLocalAndProcessingSuccess_whenOnDeviceAction_thenShouldCallLocalServiceAndSuccessCallback() {
        return Stream.of(
                Arguments.of(
                        (BiConsumer<DefaultDeviceStateManager, TbCallback>) (deviceStateManager, tbCallbackMock) -> deviceStateManager.onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Consumer<DeviceStateService>) deviceStateServiceMock -> then(deviceStateServiceMock).should().onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (BiConsumer<DefaultDeviceStateManager, TbCallback>) (deviceStateManager, tbCallbackMock) -> deviceStateManager.onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Consumer<DeviceStateService>) deviceStateServiceMock -> then(deviceStateServiceMock).should().onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (BiConsumer<DefaultDeviceStateManager, TbCallback>) (deviceStateManager, tbCallbackMock) -> deviceStateManager.onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Consumer<DeviceStateService>) deviceStateServiceMock -> then(deviceStateServiceMock).should().onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (BiConsumer<DefaultDeviceStateManager, TbCallback>) (deviceStateManager, tbCallbackMock) -> deviceStateManager.onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Consumer<DeviceStateService>) deviceStateServiceMock -> then(deviceStateServiceMock).should().onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (BiConsumer<DefaultDeviceStateManager, TbCallback>) (deviceStateManager, tbCallbackMock) -> deviceStateManager.onDeviceInactivityTimeoutUpdate(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Consumer<DeviceStateService>) deviceStateServiceMock -> then(deviceStateServiceMock).should().onDeviceInactivityTimeoutUpdate(TENANT_ID, DEVICE_ID, EVENT_TS)
                )
        );
    }

    @ParameterizedTest
    @DisplayName("Given event should be routed to local service and event processed has failed, " +
            "when onDeviceX() is called, then should route event to local service and call onFailure() callback.")
    @MethodSource
    public void givenRoutedToLocalAndProcessingFailure_whenOnDeviceAction_thenShouldCallLocalServiceAndFailureCallback(
            Consumer<DeviceStateService> exceptionThrowSetup, BiConsumer<DefaultDeviceStateManager, TbCallback> onDeviceAction, Consumer<DeviceStateService> actionVerification
    ) {
        // GIVEN
        given(serviceInfoProviderMock.isService(ServiceType.TB_CORE)).willReturn(true);
        given(partitionServiceMock.resolve(ServiceType.TB_CORE, TENANT_ID, DEVICE_ID)).willReturn(MY_TPI);

        exceptionThrowSetup.accept(deviceStateServiceMock);

        // WHEN
        onDeviceAction.accept(deviceStateManager, tbCallbackMock);

        // THEN
        actionVerification.accept(deviceStateServiceMock);

        then(clusterServiceMock).shouldHaveNoInteractions();
        then(tbCallbackMock).should(never()).onSuccess();
        then(tbCallbackMock).should().onFailure(RUNTIME_EXCEPTION);
    }

    private static Stream<Arguments> givenRoutedToLocalAndProcessingFailure_whenOnDeviceAction_thenShouldCallLocalServiceAndFailureCallback() {
        return Stream.of(
                Arguments.of(
                        (Consumer<DeviceStateService>) deviceStateServiceMock -> doThrow(RUNTIME_EXCEPTION).when(deviceStateServiceMock).onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS),
                        (BiConsumer<DefaultDeviceStateManager, TbCallback>) (deviceStateManager, tbCallbackMock) -> deviceStateManager.onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Consumer<DeviceStateService>) deviceStateServiceMock -> then(deviceStateServiceMock).should().onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (Consumer<DeviceStateService>) deviceStateServiceMock -> doThrow(RUNTIME_EXCEPTION).when(deviceStateServiceMock).onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS),
                        (BiConsumer<DefaultDeviceStateManager, TbCallback>) (deviceStateManager, tbCallbackMock) -> deviceStateManager.onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Consumer<DeviceStateService>) deviceStateServiceMock -> then(deviceStateServiceMock).should().onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (Consumer<DeviceStateService>) deviceStateServiceMock -> doThrow(RUNTIME_EXCEPTION).when(deviceStateServiceMock).onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS),
                        (BiConsumer<DefaultDeviceStateManager, TbCallback>) (deviceStateManager, tbCallbackMock) -> deviceStateManager.onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Consumer<DeviceStateService>) deviceStateServiceMock -> then(deviceStateServiceMock).should().onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (Consumer<DeviceStateService>) deviceStateServiceMock -> doThrow(RUNTIME_EXCEPTION).when(deviceStateServiceMock).onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS),
                        (BiConsumer<DefaultDeviceStateManager, TbCallback>) (deviceStateManager, tbCallbackMock) -> deviceStateManager.onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Consumer<DeviceStateService>) deviceStateServiceMock -> then(deviceStateServiceMock).should().onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (Consumer<DeviceStateService>) deviceStateServiceMock -> doThrow(RUNTIME_EXCEPTION).when(deviceStateServiceMock).onDeviceInactivityTimeoutUpdate(TENANT_ID, DEVICE_ID, EVENT_TS),
                        (BiConsumer<DefaultDeviceStateManager, TbCallback>) (deviceStateManager, tbCallbackMock) -> deviceStateManager.onDeviceInactivityTimeoutUpdate(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Consumer<DeviceStateService>) deviceStateServiceMock -> then(deviceStateServiceMock).should().onDeviceInactivityTimeoutUpdate(TENANT_ID, DEVICE_ID, EVENT_TS)
                )
        );
    }

    @ParameterizedTest
    @DisplayName("Given event should be routed to external service, " +
            "when onDeviceX() is called, then should send correct queue message to external service with correct callback object.")
    @MethodSource
    public void givenRoutedToExternal_whenOnDeviceAction_thenShouldSendQueueMsgToExternalServiceWithCorrectCallback(
            BiConsumer<DefaultDeviceStateManager, TbCallback> onDeviceAction, BiConsumer<TbClusterService, ArgumentCaptor<TbQueueCallback>> actionVerification
    ) {
        // WHEN
        ReflectionTestUtils.setField(deviceStateManager, "deviceStateService", Optional.empty());
        given(serviceInfoProviderMock.isService(ServiceType.TB_CORE)).willReturn(false);
        given(partitionServiceMock.resolve(ServiceType.TB_CORE, TENANT_ID, DEVICE_ID)).willReturn(EXTERNAL_TPI);

        onDeviceAction.accept(deviceStateManager, tbCallbackMock);

        // THEN
        actionVerification.accept(clusterServiceMock, queueCallbackCaptor);

        TbQueueCallback callback = queueCallbackCaptor.getValue();
        callback.onSuccess(metadataMock);
        then(tbCallbackMock).should().onSuccess();
        callback.onFailure(RUNTIME_EXCEPTION);
        then(tbCallbackMock).should().onFailure(RUNTIME_EXCEPTION);
    }

    private static Stream<Arguments> givenRoutedToExternal_whenOnDeviceAction_thenShouldSendQueueMsgToExternalServiceWithCorrectCallback() {
        return Stream.of(
                Arguments.of(
                        (BiConsumer<DefaultDeviceStateManager, TbCallback>) (deviceStateManager, tbCallbackMock) -> deviceStateManager.onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (BiConsumer<TbClusterService, ArgumentCaptor<TbQueueCallback>>) (clusterServiceMock, queueCallbackCaptor) -> {
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
                            then(clusterServiceMock).should().pushMsgToCore(eq(EXTERNAL_TPI), any(UUID.class), eq(toCoreMsg), queueCallbackCaptor.capture());
                        }
                ),
                Arguments.of(
                        (BiConsumer<DefaultDeviceStateManager, TbCallback>) (deviceStateManager, tbCallbackMock) -> deviceStateManager.onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (BiConsumer<TbClusterService, ArgumentCaptor<TbQueueCallback>>) (clusterServiceMock, queueCallbackCaptor) -> {
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
                            then(clusterServiceMock).should().pushMsgToCore(eq(EXTERNAL_TPI), any(UUID.class), eq(toCoreMsg), queueCallbackCaptor.capture());
                        }
                ),
                Arguments.of(
                        (BiConsumer<DefaultDeviceStateManager, TbCallback>) (deviceStateManager, tbCallbackMock) -> deviceStateManager.onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (BiConsumer<TbClusterService, ArgumentCaptor<TbQueueCallback>>) (clusterServiceMock, queueCallbackCaptor) -> {
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
                            then(clusterServiceMock).should().pushMsgToCore(eq(EXTERNAL_TPI), any(UUID.class), eq(toCoreMsg), queueCallbackCaptor.capture());
                        }
                ),
                Arguments.of(
                        (BiConsumer<DefaultDeviceStateManager, TbCallback>) (deviceStateManager, tbCallbackMock) -> deviceStateManager.onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (BiConsumer<TbClusterService, ArgumentCaptor<TbQueueCallback>>) (clusterServiceMock, queueCallbackCaptor) -> {
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
                            then(clusterServiceMock).should().pushMsgToCore(eq(EXTERNAL_TPI), any(UUID.class), eq(toCoreMsg), queueCallbackCaptor.capture());
                        }
                ),
                Arguments.of(
                        (BiConsumer<DefaultDeviceStateManager, TbCallback>) (deviceStateManager, tbCallbackMock) -> deviceStateManager.onDeviceInactivityTimeoutUpdate(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (BiConsumer<TbClusterService, ArgumentCaptor<TbQueueCallback>>) (clusterServiceMock, queueCallbackCaptor) -> {
                            var deviceInactivityTimeoutUpdateMsg = TransportProtos.DeviceInactivityTimeoutUpdateProto.newBuilder()
                                    .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                                    .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                                    .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                                    .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                                    .setInactivityTimeout(EVENT_TS)
                                    .build();
                            var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                                    .setDeviceInactivityTimeoutUpdateMsg(deviceInactivityTimeoutUpdateMsg)
                                    .build();
                            then(clusterServiceMock).should().pushMsgToCore(eq(EXTERNAL_TPI), any(UUID.class), eq(toCoreMsg), queueCallbackCaptor.capture());
                        }
                )
        );
    }

}
