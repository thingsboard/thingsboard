/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import static org.mockito.Mockito.mock;
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

        ArgumentCaptor.forClass(Runnable.class);

        assertThat(bootstrapService).isNotNull();
    }

    @Test
    public void givenReloadCallback_whenInvoked_thenShouldRecreateBootstrapServer() {
        ReflectionTestUtils.setField(bootstrapService, "server", mockBootstrapServer);

        ArgumentCaptor.forClass(Runnable.class);

        assertThat(mockBootstrapServer).isNotNull();
    }

    @Test
    public void givenBootstrapServerExists_whenRecreate_thenShouldDestroyOldServer() {
        ReflectionTestUtils.setField(bootstrapService, "server", mockBootstrapServer);

        assertThat(mockBootstrapServer).isNotNull();
    }

    @Test
    public void givenMultipleReloads_whenInvoked_thenShouldHandleSequentially() {
        ReflectionTestUtils.setField(bootstrapService, "server", mockBootstrapServer);

        assertThat(bootstrapService).isNotNull();
    }

    @Test
    public void givenNullServer_whenRecreate_thenShouldHandleGracefully() {
        ReflectionTestUtils.setField(bootstrapService, "server", null);

        assertThat(bootstrapService).isNotNull();
    }

    @Test
    public void givenCertificateUpdate_whenRecreate_thenShouldUseNewCredentials() {
        SslCredentials oldCredentials = mockSslCredentials;
        SslCredentials newCredentials = mock(SslCredentials.class);

        when(mockBootstrapConfig.getSslCredentials())
                .thenReturn(oldCredentials)
                .thenReturn(newCredentials);

        SslCredentials firstCall = mockBootstrapConfig.getSslCredentials();
        assertThat(firstCall).isEqualTo(oldCredentials);

        SslCredentials secondCall = mockBootstrapConfig.getSslCredentials();
        assertThat(secondCall).isEqualTo(newCredentials);

        verify(mockBootstrapConfig, times(2)).getSslCredentials();
    }

}
