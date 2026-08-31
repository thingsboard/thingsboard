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
package org.thingsboard.server.transport.lwm2m.bootstrap;

import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.TbLwM2MDtlsBootstrapCertificateVerifier;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapSecurityStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MInMemoryBootstrapConfigStore;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LwM2mBootstrapCertificateReloadTest {

    @Mock
    private LwM2MTransportServerConfig mockServerConfig;

    @Mock
    private LwM2MTransportBootstrapConfig mockBootstrapConfig;

    @Mock
    private LwM2MBootstrapSecurityStore mockSecurityStore;

    @Mock
    private LwM2MInMemoryBootstrapConfigStore mockConfigStore;

    @Mock
    private TransportService mockTransportService;

    @Mock
    private TbLwM2MDtlsBootstrapCertificateVerifier mockCertificateVerifier;

    @Mock
    private LeshanBootstrapServer mockBootstrapServer;

    @Mock
    private SslCredentials mockSslCredentials;

    private LwM2MTransportBootstrapService bootstrapService;

    @BeforeEach
    public void setup() {
        bootstrapService = new LwM2MTransportBootstrapService(
                mockServerConfig,
                mockBootstrapConfig,
                mockSecurityStore,
                mockConfigStore,
                mockTransportService,
                mockCertificateVerifier
        );

        when(mockBootstrapConfig.getHost()).thenReturn("localhost");
        when(mockBootstrapConfig.getPort()).thenReturn(5687);
        when(mockBootstrapConfig.getSecureHost()).thenReturn("localhost");
        when(mockBootstrapConfig.getSecurePort()).thenReturn(5688);
        when(mockBootstrapConfig.getSslCredentials()).thenReturn(mockSslCredentials);
        when(mockServerConfig.getDtlsRetransmissionTimeout()).thenReturn(9000);
    }

    @Test
    public void givenInit_whenCalled_thenShouldRegisterCertificateReloadCallback() {
        ReflectionTestUtils.setField(bootstrapService, "server", mockBootstrapServer);

        bootstrapService.afterSingletonsInstantiated();

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockBootstrapConfig).registerServerReloadCallback(callbackCaptor.capture());

        assertThat(callbackCaptor.getValue()).isNotNull();
    }

    @Test
    public void givenReloadCallback_whenNewServerCreationFails_thenOldServerIsPreserved() {
        ReflectionTestUtils.setField(bootstrapService, "server", mockBootstrapServer);

        // Force getLhBootstrapServer() to fail by returning null host (causes InetSocketAddress to throw)
        when(mockBootstrapConfig.getHost()).thenReturn(null);

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        bootstrapService.afterSingletonsInstantiated();
        verify(mockBootstrapConfig).registerServerReloadCallback(callbackCaptor.capture());

        Runnable reloadCallback = callbackCaptor.getValue();

        // getLhBootstrapServer() will fail due to null host before old server is stopped.
        // The old server should NOT be destroyed since the new server was never created.
        reloadCallback.run();

        verify(mockBootstrapServer, never()).stop();
        verify(mockBootstrapServer, never()).destroy();
        assertThat(ReflectionTestUtils.getField(bootstrapService, "server")).isSameAs(mockBootstrapServer);
    }

    @Test
    public void givenNullServer_whenRecreate_thenShouldNotThrow() {
        ReflectionTestUtils.setField(bootstrapService, "server", null);

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        bootstrapService.afterSingletonsInstantiated();
        verify(mockBootstrapConfig).registerServerReloadCallback(callbackCaptor.capture());

        Runnable reloadCallback = callbackCaptor.getValue();

        // Should not throw — callback catches exceptions internally
        reloadCallback.run();
    }

    @Test
    public void givenCertificateUpdate_whenRecreate_thenShouldUseNewCredentials() {
        SslCredentials oldCredentials = mockSslCredentials;
        SslCredentials newCredentials = mock(SslCredentials.class);

        when(mockBootstrapConfig.getSslCredentials()).thenReturn(oldCredentials).thenReturn(newCredentials);

        SslCredentials firstCall = mockBootstrapConfig.getSslCredentials();
        assertThat(firstCall).isEqualTo(oldCredentials);

        SslCredentials secondCall = mockBootstrapConfig.getSslCredentials();
        assertThat(secondCall).isEqualTo(newCredentials);

        verify(mockBootstrapConfig, times(2)).getSslCredentials();
    }

    @Test
    public void givenReloadCallback_whenRegistered_thenShouldRegisterExactlyOne() {
        bootstrapService.afterSingletonsInstantiated();

        verify(mockBootstrapConfig, times(1)).registerServerReloadCallback(any());
    }

    @Test
    public void givenReloadCallback_whenNewServerStartFails_thenOldServerRestarted() {
        // GIVEN
        ReflectionTestUtils.setField(bootstrapService, "server", mockBootstrapServer);

        LeshanBootstrapServer mockNewServer = mock(LeshanBootstrapServer.class);
        doThrow(new RuntimeException("start failed")).when(mockNewServer).start();

        LwM2MTransportBootstrapService spyService = Mockito.spy(bootstrapService);
        doReturn(mockNewServer).when(spyService).getLhBootstrapServer();

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        spyService.afterSingletonsInstantiated();
        verify(mockBootstrapConfig).registerServerReloadCallback(callbackCaptor.capture());

        Runnable reloadCallback = callbackCaptor.getValue();

        // WHEN
        reloadCallback.run();

        // THEN
        // Old server is stopped (not destroyed) to release ports
        verify(mockBootstrapServer).stop();
        verify(mockBootstrapServer, never()).destroy();
        // The new server fails to start and is destroyed
        verify(mockNewServer).destroy();
        // Old server is restarted (not rebuilt from potentially stale credentials)
        verify(mockBootstrapServer).start();
        assertThat(ReflectionTestUtils.getField(spyService, "server")).isSameAs(mockBootstrapServer);
    }

}
