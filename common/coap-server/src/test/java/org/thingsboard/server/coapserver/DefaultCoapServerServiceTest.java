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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

}
