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
package org.thingsboard.server.transport.mqtt;

import io.netty.handler.ssl.SslHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MqttSslHandlerProviderTest {

    @Mock
    private SslCredentialsConfig mockCredentialsConfig;

    @Mock
    private SslCredentials mockCredentials;

    @Mock
    private TransportService mockTransportService;

    private MqttSslHandlerProvider sslHandlerProvider;

    @BeforeEach
    public void setup() throws Exception {
        sslHandlerProvider = new MqttSslHandlerProvider();
        ReflectionTestUtils.setField(sslHandlerProvider, "mqttSslCredentialsConfig", mockCredentialsConfig);
        ReflectionTestUtils.setField(sslHandlerProvider, "transportService", mockTransportService);
        ReflectionTestUtils.setField(sslHandlerProvider, "sslProtocol", "TLSv1.2");

        KeyManagerFactory mockKmf = mock(KeyManagerFactory.class);
        TrustManagerFactory mockTmf = mock(TrustManagerFactory.class);
        X509TrustManager mockTrustManager = mock(X509TrustManager.class);

        when(mockCredentialsConfig.getCredentials()).thenReturn(mockCredentials);
        when(mockCredentials.createKeyManagerFactory()).thenReturn(mockKmf);
        when(mockCredentials.createTrustManagerFactory()).thenReturn(mockTmf);
        when(mockKmf.getKeyManagers()).thenReturn(new KeyManager[0]);
        when(mockTmf.getTrustManagers()).thenReturn(new TrustManager[]{mockTrustManager});
    }

    @Test
    public void givenInitialized_whenGetSslHandler_thenShouldCreateSSLContext() {
        sslHandlerProvider.afterSingletonsInstantiated();

        SslHandler handler1 = sslHandlerProvider.getSslHandler();
        SslHandler handler2 = sslHandlerProvider.getSslHandler();

        assertThat(handler1).isNotNull();
        assertThat(handler2).isNotNull();
        assertThat(handler1).isNotSameAs(handler2);

        SSLContext context = (SSLContext) ReflectionTestUtils.getField(sslHandlerProvider, "sslContext");
        assertThat(context).isNotNull();
    }

    @Test
    public void givenCertificatesReloaded_whenGetSslHandler_thenShouldRecreateSSLContext() {
        sslHandlerProvider.afterSingletonsInstantiated();

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockCredentialsConfig).registerReloadCallback(callbackCaptor.capture());
        Runnable reloadCallback = callbackCaptor.getValue();

        SslHandler handler1 = sslHandlerProvider.getSslHandler();
        SSLContext initialContext = (SSLContext) ReflectionTestUtils.getField(sslHandlerProvider, "sslContext");
        assertThat(initialContext).isNotNull();

        reloadCallback.run();

        assertThat(handler1).isNotNull();
        SSLContext contextAfterReload = (SSLContext) ReflectionTestUtils.getField(sslHandlerProvider, "sslContext");
        assertThat(contextAfterReload).isNull();

        SslHandler handler2 = sslHandlerProvider.getSslHandler();
        SSLContext newContext = (SSLContext) ReflectionTestUtils.getField(sslHandlerProvider, "sslContext");

        assertThat(handler2).isNotNull();
        assertThat(newContext).isNotNull();
        assertThat(newContext).isNotSameAs(initialContext);
    }

    @Test
    public void givenConcurrentGetSslHandlerCalls_whenSSLContextNull_thenShouldCreateOnlyOnce() throws Exception {
        sslHandlerProvider.afterSingletonsInstantiated();

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockCredentialsConfig).registerReloadCallback(callbackCaptor.capture());
        callbackCaptor.getValue().run();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    SslHandler handler = sslHandlerProvider.getSslHandler();
                    assertThat(handler).isNotNull();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);

        assertThat(completed).isTrue();
        SSLContext context = (SSLContext) ReflectionTestUtils.getField(sslHandlerProvider, "sslContext");
        assertThat(context).isNotNull();
    }

    @Test
    public void givenReloadCallback_whenInvoked_thenShouldInvalidateSSLContext() {
        sslHandlerProvider.afterSingletonsInstantiated();

        sslHandlerProvider.getSslHandler();
        SSLContext initialContext = (SSLContext) ReflectionTestUtils.getField(sslHandlerProvider, "sslContext");
        assertThat(initialContext).isNotNull();

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockCredentialsConfig).registerReloadCallback(callbackCaptor.capture());

        callbackCaptor.getValue().run();

        SSLContext contextAfterReload = (SSLContext) ReflectionTestUtils.getField(sslHandlerProvider, "sslContext");
        assertThat(contextAfterReload).isNull();
    }

    @Test
    public void givenMultipleReloads_whenGetSslHandler_thenShouldRecreateEachTime() {
        sslHandlerProvider.afterSingletonsInstantiated();

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockCredentialsConfig).registerReloadCallback(callbackCaptor.capture());
        Runnable reloadCallback = callbackCaptor.getValue();

        SSLContext context1;
        SSLContext context2;
        SSLContext context3;

        sslHandlerProvider.getSslHandler();
        context1 = (SSLContext) ReflectionTestUtils.getField(sslHandlerProvider, "sslContext");
        assertThat(context1).isNotNull();

        reloadCallback.run();
        sslHandlerProvider.getSslHandler();
        context2 = (SSLContext) ReflectionTestUtils.getField(sslHandlerProvider, "sslContext");
        assertThat(context2).isNotNull();
        assertThat(context2).isNotSameAs(context1);

        reloadCallback.run();
        sslHandlerProvider.getSslHandler();
        context3 = (SSLContext) ReflectionTestUtils.getField(sslHandlerProvider, "sslContext");
        assertThat(context3).isNotNull();
        assertThat(context3).isNotSameAs(context2);
        assertThat(context3).isNotSameAs(context1);
    }

}
