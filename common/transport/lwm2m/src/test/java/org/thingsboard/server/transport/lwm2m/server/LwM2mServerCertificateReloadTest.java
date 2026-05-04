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
package org.thingsboard.server.transport.lwm2m.server;

import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.observation.ObservationService;
import org.eclipse.leshan.server.registration.RegistrationService;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.send.SendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MAuthorizer;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MDtlsCertificateVerifier;
import org.thingsboard.server.transport.lwm2m.server.store.TbSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LwM2mServerCertificateReloadTest {

    @Mock
    private LwM2mTransportContext mockContext;

    @Mock
    private LwM2MTransportServerConfig mockConfig;

    @Mock
    private OtaPackageDataCache mockOtaCache;

    @Mock
    private LwM2mUplinkMsgHandler mockHandler;

    @Mock
    private RegistrationStore mockRegistrationStore;

    @Mock
    private TbSecurityStore mockSecurityStore;

    @Mock
    private TbLwM2MDtlsCertificateVerifier mockCertificateVerifier;

    @Mock
    private TbLwM2MAuthorizer mockAuthorizer;

    @Mock
    private LwM2mVersionedModelProvider mockModelProvider;

    @Mock
    private LeshanServer mockLeshanServer;

    @Mock
    private RegistrationService mockRegistrationService;

    @Mock
    private ObservationService mockObservationService;

    @Mock
    private SendService mockSendService;

    @Mock
    private SslCredentials mockSslCredentials;

    private DefaultLwM2mTransportService lwm2mTransportService;

    @BeforeEach
    public void setup() {
        lwm2mTransportService = new DefaultLwM2mTransportService(
                mockContext,
                mockConfig,
                mockOtaCache,
                mockHandler,
                mockRegistrationStore,
                mockSecurityStore,
                mockCertificateVerifier,
                mockAuthorizer,
                mockModelProvider
        );

        when(mockConfig.getHost()).thenReturn("localhost");
        when(mockConfig.getPort()).thenReturn(5683);
        when(mockConfig.getSecureHost()).thenReturn("localhost");
        when(mockConfig.getSecurePort()).thenReturn(5684);
        when(mockConfig.getSslCredentials()).thenReturn(mockSslCredentials);

        when(mockLeshanServer.getRegistrationService()).thenReturn(mockRegistrationService);
        when(mockLeshanServer.getObservationService()).thenReturn(mockObservationService);
        when(mockLeshanServer.getSendService()).thenReturn(mockSendService);
    }

    @Test
    public void givenRegisterCertificateReloadCallback_whenInvoked_thenShouldRegisterCallback() {
        lwm2mTransportService.afterSingletonsInstantiated();

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockConfig).registerServerReloadCallback(callbackCaptor.capture());
        assertThat(callbackCaptor.getValue()).isNotNull();
    }

    @Test
    public void givenReloadCallback_whenNewServerCreationFails_thenOldServerIsPreserved() {
        lwm2mTransportService.afterSingletonsInstantiated();

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockConfig).registerServerReloadCallback(callbackCaptor.capture());
        Runnable reloadCallback = callbackCaptor.getValue();

        ReflectionTestUtils.setField(lwm2mTransportService, "server", mockLeshanServer);

        // Force getLhServer() to fail by returning null host (causes InetSocketAddress to throw)
        when(mockConfig.getHost()).thenReturn(null);

        // With create-then-swap, the old server should NOT be stopped/destroyed if the new one fails to build.
        reloadCallback.run();

        verify(mockLeshanServer, never()).stop();
        verify(mockLeshanServer, never()).destroy();
        // Old server should still be the active one
        assertThat(ReflectionTestUtils.getField(lwm2mTransportService, "server")).isSameAs(mockLeshanServer);
    }

    @Test
    public void givenServerWithListeners_whenNewServerCreationFails_thenListenersArePreserved() {
        lwm2mTransportService.afterSingletonsInstantiated();

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockConfig).registerServerReloadCallback(callbackCaptor.capture());

        ReflectionTestUtils.setField(lwm2mTransportService, "server", mockLeshanServer);

        LwM2mServerListener serverListener = new LwM2mServerListener(mockHandler);
        ReflectionTestUtils.setField(lwm2mTransportService, "serverListener", serverListener);

        // Force getLhServer() to fail by returning null host
        when(mockConfig.getHost()).thenReturn(null);

        // Invoke the callback — new server creation will fail, old listeners should stay
        callbackCaptor.getValue().run();

        verify(mockRegistrationService, never()).removeListener(any());
    }

    @Test
    public void givenMultipleReloadCallbacks_whenInvoked_thenShouldRegisterExactlyOne() {
        lwm2mTransportService.afterSingletonsInstantiated();

        verify(mockConfig, times(1)).registerServerReloadCallback(any());
    }

    @Test
    public void givenCertificateReload_whenServerNull_thenShouldNotThrow() {
        lwm2mTransportService.afterSingletonsInstantiated();

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockConfig).registerServerReloadCallback(callbackCaptor.capture());

        ReflectionTestUtils.setField(lwm2mTransportService, "server", null);

        // Should not throw - callback catches exceptions internally
        callbackCaptor.getValue().run();
    }

}
