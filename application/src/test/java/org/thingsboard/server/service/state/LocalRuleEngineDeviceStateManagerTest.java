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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;

import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class LocalRuleEngineDeviceStateManagerTest {

    @Mock
    private static DeviceStateService deviceStateServiceMock;
    @Mock
    private static TbCallback tbCallbackMock;
    private static LocalRuleEngineDeviceStateManager deviceStateManager;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("57ab2e6c-bc4c-11ee-a506-0242ac120002"));
    private static final DeviceId DEVICE_ID = DeviceId.fromString("74a9053e-bc4c-11ee-a506-0242ac120002");
    private static final long EVENT_TS = System.currentTimeMillis();
    private static final RuntimeException RUNTIME_EXCEPTION = new RuntimeException("Something bad happened!");

    @BeforeEach
    public void setup() {
        deviceStateManager = new LocalRuleEngineDeviceStateManager(deviceStateServiceMock);
    }

    @ParameterizedTest
    @MethodSource
    public void givenProcessingSuccess_whenOnDeviceAction_thenCallsDeviceStateServiceAndOnSuccessCallback(Runnable onDeviceAction, Runnable actionVerification) {
        // WHEN
        onDeviceAction.run();

        // THEN
        actionVerification.run();
        then(tbCallbackMock).should().onSuccess();
        then(tbCallbackMock).should(never()).onFailure(any());
    }

    private static Stream<Arguments> givenProcessingSuccess_whenOnDeviceAction_thenCallsDeviceStateServiceAndOnSuccessCallback() {
        return Stream.of(
                Arguments.of(
                        (Runnable) () -> deviceStateManager.onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> then(deviceStateServiceMock).should().onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (Runnable) () -> deviceStateManager.onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> then(deviceStateServiceMock).should().onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (Runnable) () -> deviceStateManager.onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> then(deviceStateServiceMock).should().onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (Runnable) () -> deviceStateManager.onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> then(deviceStateServiceMock).should().onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    public void givenProcessingFailure_whenOnDeviceAction_thenCallsDeviceStateServiceAndOnFailureCallback(
            Runnable exceptionThrowSetup, Runnable onDeviceAction, Runnable actionVerification
    ) {
        // GIVEN
        exceptionThrowSetup.run();

        // WHEN
        onDeviceAction.run();

        // THEN
        actionVerification.run();
        then(tbCallbackMock).should(never()).onSuccess();
        then(tbCallbackMock).should().onFailure(RUNTIME_EXCEPTION);
    }

    private static Stream<Arguments> givenProcessingFailure_whenOnDeviceAction_thenCallsDeviceStateServiceAndOnFailureCallback() {
        return Stream.of(
                Arguments.of(
                        (Runnable) () -> doThrow(RUNTIME_EXCEPTION).when(deviceStateServiceMock).onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS),
                        (Runnable) () -> deviceStateManager.onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> then(deviceStateServiceMock).should().onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (Runnable) () -> doThrow(RUNTIME_EXCEPTION).when(deviceStateServiceMock).onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS),
                        (Runnable) () -> deviceStateManager.onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> then(deviceStateServiceMock).should().onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (Runnable) () -> doThrow(RUNTIME_EXCEPTION).when(deviceStateServiceMock).onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS),
                        (Runnable) () -> deviceStateManager.onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> then(deviceStateServiceMock).should().onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (Runnable) () -> doThrow(RUNTIME_EXCEPTION).when(deviceStateServiceMock).onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS),
                        (Runnable) () -> deviceStateManager.onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> then(deviceStateServiceMock).should().onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS)
                )
        );
    }

}
