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
package org.thingsboard.server.transport.lwm2m.server;

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MAuthorizer;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MDtlsCertificateVerifier;
import org.thingsboard.server.transport.lwm2m.server.store.TbSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultLwM2mTransportServiceTest {

    @Mock
    private LwM2mTransportContext context;

    @Mock
    private LwM2MTransportServerConfig config;
    @Mock
    private OtaPackageDataCache otaPackageDataCache;
    @Mock
    private LwM2mUplinkMsgHandler handler;
    @Mock
    private CaliforniumRegistrationStore registrationStore;
    @Mock
    private TbSecurityStore securityStore;
    @Mock
    private TbLwM2MDtlsCertificateVerifier certificateVerifier;
    @Mock
    private TbLwM2MAuthorizer authorizer;
    @Mock
    private LwM2mVersionedModelProvider modelProvider;


    @Test
    public void getLHServer_creates_ConnectionIdGenerator_when_connection_id_length_not_null(){
        final Integer CONNECTION_ID_LENGTH = 6;
        when(config.getDtlsConnectionIdLength()).thenReturn(CONNECTION_ID_LENGTH);
        var lwm2mService = createLwM2MService();

        LeshanServer server = ReflectionTestUtils.invokeMethod(lwm2mService, "getLhServer");

        assertThat(server).isNotNull();
        var securedEndpoint = (CoapEndpoint) ReflectionTestUtils.getField(server, "securedEndpoint");
        assertThat(securedEndpoint).isNotNull();

        var config = (DtlsConnectorConfig) ReflectionTestUtils.getField(securedEndpoint.getConnector(), "config");
        assertThat(config).isNotNull();
        assertThat(config.getConnectionIdGenerator()).isNotNull();
        assertThat((Integer) ReflectionTestUtils.getField(config.getConnectionIdGenerator(), "connectionIdLength"))
                .isEqualTo(CONNECTION_ID_LENGTH);
    }

    @Test
    public void getLHServer_creates_no_ConnectionIdGenerator_when_connection_id_length_is_null(){
        when(config.getDtlsConnectionIdLength()).thenReturn(null);
        var lwm2mService = createLwM2MService();

        LeshanServer server = ReflectionTestUtils.invokeMethod(lwm2mService, "getLhServer");

        assertThat(server).isNotNull();
        var securedEndpoint = (CoapEndpoint) ReflectionTestUtils.getField(server, "securedEndpoint");
        assertThat(securedEndpoint).isNotNull();
        var config = (DtlsConnectorConfig) ReflectionTestUtils.getField(securedEndpoint.getConnector(), "config");
        assertThat(config).isNotNull();
        assertThat(config.getConnectionIdGenerator()).isNull();
    }

    private DefaultLwM2mTransportService createLwM2MService() {
        setDefaultConfigVariables();
        return new DefaultLwM2mTransportService(context, config, otaPackageDataCache, handler, registrationStore,
                securityStore, certificateVerifier, authorizer, modelProvider);
    }

    private void setDefaultConfigVariables(){
        when(config.getPort()).thenReturn(5683);
        when(config.getSecurePort()).thenReturn(5684);
        when(config.isRecommendedCiphers()).thenReturn(false);
        when(config.getDtlsRetransmissionTimeout()).thenReturn(9000);
    }


}