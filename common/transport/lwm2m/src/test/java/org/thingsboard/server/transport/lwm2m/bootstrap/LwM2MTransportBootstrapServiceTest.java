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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.TbLwM2MDtlsBootstrapCertificateVerifier;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapSecurityStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MInMemoryBootstrapConfigStore;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LwM2MTransportBootstrapServiceTest {

    private static final String HOST = "127.0.0.1";

    @Mock
    private LwM2MTransportServerConfig serverConfig;

    @Mock
    private LwM2MTransportBootstrapConfig bootstrapConfig;

    @Mock
    private LwM2MBootstrapSecurityStore lwM2MBootstrapSecurityStore;

    @Mock
    private LwM2MInMemoryBootstrapConfigStore lwM2MInMemoryBootstrapConfigStore;

    @Mock
    private TransportService transportService;

    @Mock
    private TbLwM2MDtlsBootstrapCertificateVerifier certificateVerifier;

    private LwM2MTransportBootstrapService service;
    private DatagramSocket occupiedPlain;
    private DatagramSocket occupiedSecure;

    @BeforeEach
    public void setUp() throws Exception {
        occupiedPlain = new DatagramSocket(new InetSocketAddress(InetAddress.getByName(HOST), 0));
        occupiedSecure = new DatagramSocket(new InetSocketAddress(InetAddress.getByName(HOST), 0));

        when(bootstrapConfig.getHost()).thenReturn(HOST);
        when(bootstrapConfig.getPort()).thenReturn(occupiedPlain.getLocalPort());
        when(bootstrapConfig.getSecureHost()).thenReturn(HOST);
        when(bootstrapConfig.getSecurePort()).thenReturn(occupiedSecure.getLocalPort());
        when(bootstrapConfig.getSslCredentials()).thenReturn(null);

        when(serverConfig.isRecommendedCiphers()).thenReturn(false);
        when(serverConfig.isRecommendedSupportedGroups()).thenReturn(false);
        when(serverConfig.getDtlsRetransmissionTimeout()).thenReturn(9000);
        when(serverConfig.getDtlsCidLength()).thenReturn(null);

        service = new LwM2MTransportBootstrapService(
                serverConfig,
                bootstrapConfig,
                lwM2MBootstrapSecurityStore,
                lwM2MInMemoryBootstrapConfigStore,
                transportService,
                certificateVerifier
        );
    }

    @AfterEach
    public void tearDown() {
        if (occupiedPlain != null && !occupiedPlain.isClosed()) {
            occupiedPlain.close();
        }
        if (occupiedSecure != null && !occupiedSecure.isClosed()) {
            occupiedSecure.close();
        }
    }

    @Test
    public void whenEndpointsFailToStart_thenInitThrowsAndReleasesBootstrapServer() {
        assertThatThrownBy(() -> service.init())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("None of the server endpoints could be started");

        assertThat(ReflectionTestUtils.getField(service, "server")).isNull();
    }

}
