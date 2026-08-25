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
package org.thingsboard.server.actors.device;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.common.util.LinkedHashMapRemoveEldest;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.service.transport.TbCoreToTransportService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * Session-map eviction, which needs a realistic concurrent-session limit. Kept apart from
 * {@link DeviceActorMessageProcessorTest}, whose RPC tests mint a session per call and so run with a limit of 1.
 */
public class DeviceActorMessageProcessorSessionLimitTest {

    private static final int MAX_CONCURRENT_SESSIONS_PER_DEVICE = 10;

    ActorSystemContext systemContext;
    DeviceService deviceService;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("ae651b45-2a92-4bdf-9d56-faede44eafb8"));
    DeviceId deviceId = DeviceId.fromString("78bf9b26-74ef-4af2-9cfb-ad6cf24ad2ec");

    DeviceActorMessageProcessor processor;

    @Before
    public void setUp() {
        systemContext = mock(ActorSystemContext.class);
        deviceService = mock(DeviceService.class);
        given(systemContext.getMaxConcurrentSessionsPerDevice()).willReturn(MAX_CONCURRENT_SESSIONS_PER_DEVICE);
        given(systemContext.getDeviceService()).willReturn(deviceService);
        given(systemContext.getRpcSubmitStrategy()).willReturn("BURST");
        processor = new DeviceActorMessageProcessor(systemContext, tenantId, deviceId);
        // Evicting a session notifies the transport that it was closed.
        given(systemContext.getTbCoreToTransportService()).willReturn(mock(TbCoreToTransportService.class));
    }

    @Test
    public void givenSystemContext_whenNewInstance_thenVerifySessionMapMaxSize() {
        assertThat(processor.sessions).isInstanceOf(LinkedHashMapRemoveEldest.class);
        assertThat(processor.sessions.getMaxEntries()).isEqualTo(MAX_CONCURRENT_SESSIONS_PER_DEVICE);
        assertThat(processor.sessions.getRemovalConsumer()).isNotNull();
    }

    @Test
    public void givenFullSessionMap_whenSessionOverflow_thenShouldDeleteAttributeAndRPCSubscriptions() {
        //givenFullSessionMap
        for (int i = 0; i < MAX_CONCURRENT_SESSIONS_PER_DEVICE; i++) {
            UUID sessionID = UUID.randomUUID();
            processor.sessions.put(sessionID, Mockito.mock(SessionInfoMetaData.class, RETURNS_DEEP_STUBS));
            processor.attributeSubscriptions.put(sessionID, Mockito.mock(SessionInfo.class));
            processor.rpcSubscriptions.put(sessionID, Mockito.mock(SessionInfo.class));
        }
        assertThat(processor.sessions.size()).isEqualTo(MAX_CONCURRENT_SESSIONS_PER_DEVICE);
        assertThat(processor.attributeSubscriptions.size()).isEqualTo(MAX_CONCURRENT_SESSIONS_PER_DEVICE);
        assertThat(processor.rpcSubscriptions.size()).isEqualTo(MAX_CONCURRENT_SESSIONS_PER_DEVICE);

        //add one more
        processor.sessions.put(UUID.randomUUID(), Mockito.mock(SessionInfoMetaData.class));

        assertThat(processor.sessions.size()).isEqualTo(MAX_CONCURRENT_SESSIONS_PER_DEVICE);
        assertThat(processor.attributeSubscriptions.size()).isEqualTo(MAX_CONCURRENT_SESSIONS_PER_DEVICE - 1);
        assertThat(processor.rpcSubscriptions.size()).isEqualTo(MAX_CONCURRENT_SESSIONS_PER_DEVICE - 1);

    }
}
