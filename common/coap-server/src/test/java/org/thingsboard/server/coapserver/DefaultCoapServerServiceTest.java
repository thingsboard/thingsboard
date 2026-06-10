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
package org.thingsboard.server.coapserver;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.ThingsBoardExecutors;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultCoapServerServiceTest {

    private static final String HOST = "127.0.0.1";

    @Mock
    private CoapServerContext mockCoapServerContext;

    private DefaultCoapServerService service;
    private DatagramSocket occupiedSocket;
    private int occupiedPort;

    @BeforeEach
    public void setUp() throws Exception {
        occupiedSocket = new DatagramSocket(new InetSocketAddress(InetAddress.getByName(HOST), 0));
        occupiedPort = occupiedSocket.getLocalPort();

        service = new DefaultCoapServerService();
        ReflectionTestUtils.setField(service, "coapServerContext", mockCoapServerContext);

        when(mockCoapServerContext.getHost()).thenReturn(HOST);
        when(mockCoapServerContext.getPort()).thenReturn(occupiedPort);
        when(mockCoapServerContext.getDtlsSettings()).thenReturn(null);
    }

    @AfterEach
    public void tearDown() {
        if (occupiedSocket != null && !occupiedSocket.isClosed()) {
            occupiedSocket.close();
        }
    }

    @Test
    public void whenPlainBindFails_thenInitThrowsAndReleasesCoapServer() {
        assertThatThrownBy(() -> service.init())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("None of the server endpoints could be started");

        assertThat(ReflectionTestUtils.getField(service, "server")).isNull();
        assertThat(ReflectionTestUtils.getField(service, "dtlsSessionsExecutor")).isNull();
        assertThat(ReflectionTestUtils.getField(service, "dtlsConnector")).isNull();
        assertThat(ReflectionTestUtils.getField(service, "dtlsCoapEndpoint")).isNull();
        assertThat(ReflectionTestUtils.getField(service, "tbDtlsCertificateVerifier")).isNull();
    }

    @Test
    public void whenDtlsEnabledAndStartFails_thenInitShutsDownDtlsExecutorAndReleasesCoapServer() throws Exception {
        // DTLS enabled: the DTLS endpoint is created and dtlsSessionsExecutor is scheduled before server.start().
        // This exercises the catch's dtlsSessionsExecutor.shutdownNow() branch, which the plain-bind test does not.
        TbCoapDtlsSettings mockDtlsSettings = mock(TbCoapDtlsSettings.class);
        when(mockCoapServerContext.getDtlsSettings()).thenReturn(mockDtlsSettings);

        DtlsConnectorConfig mockDtlsConfig = mock(DtlsConnectorConfig.class);
        when(mockDtlsConfig.getAddress()).thenReturn(new InetSocketAddress(InetAddress.getByName(HOST), occupiedPort + 1));
        TbCoapDtlsCertificateVerifier mockVerifier = mock(TbCoapDtlsCertificateVerifier.class);
        when(mockVerifier.getDtlsSessionReportTimeout()).thenReturn(1800000L);
        when(mockDtlsConfig.getAdvancedCertificateVerifier()).thenReturn(mockVerifier);
        when(mockDtlsSettings.dtlsConnectorConfig(any())).thenReturn(mockDtlsConfig);

        ScheduledExecutorService mockExecutor = mock(ScheduledExecutorService.class);
        Resource mockRoot = mock(Resource.class);

        try (MockedStatic<ThingsBoardExecutors> executorsStatic = mockStatic(ThingsBoardExecutors.class);
             MockedConstruction<CoapServer> serverMock = mockConstruction(CoapServer.class, (server, ctx) -> {
                 when(server.getRoot()).thenReturn(mockRoot);
                 doThrow(new IllegalStateException("None of the server endpoints could be started")).when(server).start();
             });
             MockedConstruction<DTLSConnector> dtlsMock = mockConstruction(DTLSConnector.class);
             MockedConstruction<CoapEndpoint.Builder> builderMock = mockConstruction(CoapEndpoint.Builder.class, (builder, ctx) -> {
                 when(builder.setInetSocketAddress(any())).thenReturn(builder);
                 when(builder.setConfiguration(any())).thenReturn(builder);
                 when(builder.setConnector(any(DTLSConnector.class))).thenReturn(builder);
                 when(builder.build()).thenReturn(mock(CoapEndpoint.class));
             })) {

            executorsStatic.when(() -> ThingsBoardExecutors.newSingleThreadScheduledExecutor(anyString())).thenReturn(mockExecutor);

            assertThatThrownBy(() -> service.init())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("None of the server endpoints could be started");

            // DTLS branch was actually entered and the executor was created...
            verify(mockDtlsSettings).dtlsConnectorConfig(any());
            // ...and the cleanup branch shut it down and destroyed the server.
            verify(mockExecutor).shutdownNow();
            verify(serverMock.constructed().get(0)).destroy();
        }

        assertThat(ReflectionTestUtils.getField(service, "server")).isNull();
        assertThat(ReflectionTestUtils.getField(service, "dtlsSessionsExecutor")).isNull();
        assertThat(ReflectionTestUtils.getField(service, "dtlsConnector")).isNull();
        assertThat(ReflectionTestUtils.getField(service, "dtlsCoapEndpoint")).isNull();
        assertThat(ReflectionTestUtils.getField(service, "tbDtlsCertificateVerifier")).isNull();
    }

}
