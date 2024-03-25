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
package org.thingsboard.server.transport.lwm2m.bootstrap;

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.TbLwM2MDtlsBootstrapCertificateVerifier;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapSecurityStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MInMemoryBootstrapConfigStore;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.when;

@ExtendWith(MockitoExtension.class)
public class LwM2MTransportBootstrapServiceTest {

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

    @Disabled // fixme: nick
    @Test
    public void getLHServer_creates_ConnectionIdGenerator_when_connection_id_length_not_null(){
        final Integer CONNECTION_ID_LENGTH = 6;
        when(serverConfig.getDtlsCidLength()).thenReturn(CONNECTION_ID_LENGTH);
        var lwM2MBootstrapService = createLwM2MBootstrapService();

        var server = lwM2MBootstrapService.getLhBootstrapServer();
        var securedEndpoint = (CoapEndpoint) ReflectionTestUtils.getField(server, "securedEndpoint");
        assertThat(securedEndpoint).isNotNull();

        var config = (DtlsConnectorConfig) ReflectionTestUtils.getField(securedEndpoint.getConnector(), "config");
        assertThat(config).isNotNull();
        assertThat(config.getConnectionIdGenerator()).isNotNull();
        assertThat((Integer) ReflectionTestUtils.getField(config.getConnectionIdGenerator(), "connectionIdLength"))
                .isEqualTo(CONNECTION_ID_LENGTH);
    }

    @Disabled // fixme: nick
    @Test
    public void getLHServer_creates_no_ConnectionIdGenerator_when_connection_id_length_is_null(){
        when(serverConfig.getDtlsCidLength()).thenReturn(null);
        var lwM2MBootstrapService = createLwM2MBootstrapService();

        var server = lwM2MBootstrapService.getLhBootstrapServer();
        var securedEndpoint = (CoapEndpoint) ReflectionTestUtils.getField(server, "securedEndpoint");
        assertThat(securedEndpoint).isNotNull();

        var config = (DtlsConnectorConfig) ReflectionTestUtils.getField(securedEndpoint.getConnector(), "config");
        assertThat(config).isNotNull();
        assertThat(config.getConnectionIdGenerator()).isNull();
    }

    private LwM2MTransportBootstrapService createLwM2MBootstrapService() {
        setDefaultConfigVariables();
        return new LwM2MTransportBootstrapService(serverConfig, bootstrapConfig, lwM2MBootstrapSecurityStore,
                lwM2MInMemoryBootstrapConfigStore, transportService, certificateVerifier);
    }

    private void setDefaultConfigVariables(){
        when(bootstrapConfig.getPort()).thenReturn(5683);
        when(bootstrapConfig.getSecurePort()).thenReturn(5684);
        when(serverConfig.isRecommendedCiphers()).thenReturn(false);
        when(serverConfig.getDtlsRetransmissionTimeout()).thenReturn(9000);
    }


}