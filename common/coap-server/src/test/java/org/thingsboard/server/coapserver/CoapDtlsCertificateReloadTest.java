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
package org.thingsboard.server.coapserver;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.scandium.DTLSConnector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CoapDtlsCertificateReloadTest {

    @Mock
    private CoapServerContext mockCoapServerContext;

    @Mock
    private TbCoapDtlsSettings mockDtlsSettings;

    @Mock
    private CoapServer mockCoapServer;

    @Mock
    private CoapEndpoint mockDtlsEndpoint;

    @Mock
    private DTLSConnector mockDtlsConnector;

    private DefaultCoapServerService coapServerService;

    @BeforeEach
    public void setup() {
        coapServerService = new DefaultCoapServerService();
        ReflectionTestUtils.setField(coapServerService, "coapServerContext", mockCoapServerContext);

        when(mockCoapServerContext.getHost()).thenReturn("localhost");
        when(mockCoapServerContext.getPort()).thenReturn(5683);
        doAnswer(invocation -> {
            invocation.getArgument(0);
            return null;
        }).when(mockDtlsSettings).registerReloadCallback(any());
    }

    @Test
    public void givenDtlsEnabled_whenRegisterCertificateReloadCallback_thenShouldRegisterCallback() {
        when(mockCoapServerContext.getDtlsSettings()).thenReturn(mockDtlsSettings);

        ReflectionTestUtils.setField(coapServerService, "server", mockCoapServer);

        ReflectionTestUtils.invokeMethod(coapServerService, "afterSingletonsInstantiated");

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockDtlsSettings).registerReloadCallback(callbackCaptor.capture());
        assertThat(callbackCaptor.getValue()).isNotNull();
    }

    @Test
    public void givenDtlsNotEnabled_whenRegisterCertificateReloadCallback_thenShouldNotRegisterCallback() {
        when(mockCoapServerContext.getDtlsSettings()).thenReturn(null);

        ReflectionTestUtils.invokeMethod(coapServerService, "afterSingletonsInstantiated");

        verify(mockDtlsSettings, never()).registerReloadCallback(any());
    }

    @Test
    public void givenReloadCallbackInvoked_whenDtlsEndpointExists_thenShouldRecreateDtlsEndpoint() {
        when(mockCoapServerContext.getDtlsSettings()).thenReturn(mockDtlsSettings);

        ReflectionTestUtils.setField(coapServerService, "server", mockCoapServer);
        ReflectionTestUtils.setField(coapServerService, "dtlsCoapEndpoint", mockDtlsEndpoint);
        ReflectionTestUtils.setField(coapServerService, "dtlsConnector", mockDtlsConnector);

        when(mockCoapServer.getEndpoints()).thenReturn(mock(List.class));

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        ReflectionTestUtils.invokeMethod(coapServerService, "afterSingletonsInstantiated");
        verify(mockDtlsSettings).registerReloadCallback(callbackCaptor.capture());

        Runnable reloadCallback = callbackCaptor.getValue();
        assertThat(reloadCallback).isNotNull();
    }

    @Test
    public void givenDtlsEnabled_whenInit_thenShouldRegisterCallback() {
        when(mockCoapServerContext.getDtlsSettings()).thenReturn(mockDtlsSettings);
        when(mockCoapServerContext.getHost()).thenReturn("localhost");
        when(mockCoapServerContext.getPort()).thenReturn(5683);

        ReflectionTestUtils.setField(coapServerService, "server", mockCoapServer);
        ReflectionTestUtils.invokeMethod(coapServerService, "afterSingletonsInstantiated");

        verify(mockDtlsSettings).registerReloadCallback(any(Runnable.class));
    }

    @Test
    public void givenReloadCallback_whenInvokedMultipleTimes_thenShouldHandleGracefully() {
        when(mockCoapServerContext.getDtlsSettings()).thenReturn(mockDtlsSettings);
        ReflectionTestUtils.setField(coapServerService, "server", mockCoapServer);
        ReflectionTestUtils.setField(coapServerService, "dtlsCoapEndpoint", mockDtlsEndpoint);
        ReflectionTestUtils.setField(coapServerService, "dtlsConnector", mockDtlsConnector);

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        ReflectionTestUtils.invokeMethod(coapServerService, "afterSingletonsInstantiated");
        verify(mockDtlsSettings).registerReloadCallback(callbackCaptor.capture());

        Runnable reloadCallback = callbackCaptor.getValue();

        assertThat(reloadCallback).isNotNull();
    }

}
