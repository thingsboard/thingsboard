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
package org.thingsboard.server.common.transport.config.ssl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SslCredentialsWebServerCustomizerTest {

    @Mock
    private ServerProperties mockServerProperties;

    @Mock
    private SslCredentialsConfig mockCredentialsConfig;

    @Mock
    private SslCredentials mockCredentials;

    @Mock
    private KeyStore mockKeyStore;

    private SslCredentialsWebServerCustomizer customizer;

    @BeforeEach
    public void setup() throws Exception {
        customizer = new SslCredentialsWebServerCustomizer(mockServerProperties);
        ReflectionTestUtils.setField(customizer, "httpServerSslCredentialsConfig", mockCredentialsConfig);

        when(mockCredentialsConfig.getCredentials()).thenReturn(mockCredentials);
        when(mockCredentials.getKeyStore()).thenReturn(mockKeyStore);
        when(mockCredentials.getKeyPassword()).thenReturn("password");
        when(mockCredentials.getKeyAlias()).thenReturn("server");

        X509Certificate mockCert = mock(X509Certificate.class);
        when(mockCert.getEncoded()).thenReturn("TEST_CERT_DATA".getBytes());
        when(mockCredentials.getCertificateChain()).thenReturn(new X509Certificate[]{mockCert});
    }

    @Test
    public void givenInitialized_whenAfterSingletonsInstantiated_thenShouldRegisterReloadCallback() {
        customizer.afterSingletonsInstantiated();

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockCredentialsConfig).registerReloadCallback(callbackCaptor.capture());
        assertThat(callbackCaptor.getValue()).isNotNull();
    }

    @Test
    public void givenReloadCallback_whenInvoked_thenShouldReloadCertificates() {
        customizer.afterSingletonsInstantiated();

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockCredentialsConfig).registerReloadCallback(callbackCaptor.capture());
        Runnable reloadCallback = callbackCaptor.getValue();

        reloadCallback.run();

        verify(mockCredentialsConfig, times(1)).getCredentials();
    }

    @Test
    public void givenSslBundles_whenGetBundle_thenShouldReturnValidBundle() {
        SslBundles sslBundles = customizer.sslBundles();

        SslBundle bundle = sslBundles.getBundle("default");

        assertThat(bundle).isNotNull();
    }

    @Test
    public void givenSslBundles_whenGetBundleNames_thenShouldReturnDefault() {
        SslBundles sslBundles = customizer.sslBundles();

        List<String> bundleNames = sslBundles.getBundleNames();

        assertThat(bundleNames).containsExactly("default");
    }

    @Test
    public void givenSslBundles_whenAddUpdateHandler_thenShouldRegisterHandler() {
        SslBundles sslBundles = customizer.sslBundles();
        AtomicInteger handlerCallCount = new AtomicInteger(0);
        Consumer<SslBundle> handler = bundle -> handlerCallCount.incrementAndGet();

        sslBundles.addBundleUpdateHandler("default", handler);

        customizer.afterSingletonsInstantiated();
        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockCredentialsConfig).registerReloadCallback(callbackCaptor.capture());
        callbackCaptor.getValue().run();

        assertThat(handlerCallCount.get()).isEqualTo(1);
    }

    @Test
    public void givenSslBundles_whenAddUpdateHandlerForWrongBundle_thenShouldNotRegister() {
        SslBundles sslBundles = customizer.sslBundles();
        AtomicInteger handlerCallCount = new AtomicInteger(0);
        Consumer<SslBundle> handler = bundle -> handlerCallCount.incrementAndGet();

        sslBundles.addBundleUpdateHandler("wrong-bundle", handler);

        customizer.afterSingletonsInstantiated();
        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockCredentialsConfig).registerReloadCallback(callbackCaptor.capture());
        callbackCaptor.getValue().run();

        assertThat(handlerCallCount.get()).isEqualTo(0);
    }

    @Test
    public void givenMultipleUpdateHandlers_whenReload_thenShouldNotifyAll() {
        SslBundles sslBundles = customizer.sslBundles();
        AtomicInteger handler1CallCount = new AtomicInteger(0);
        AtomicInteger handler2CallCount = new AtomicInteger(0);
        AtomicInteger handler3CallCount = new AtomicInteger(0);

        sslBundles.addBundleUpdateHandler("default", bundle -> handler1CallCount.incrementAndGet());
        sslBundles.addBundleUpdateHandler("default", bundle -> handler2CallCount.incrementAndGet());
        sslBundles.addBundleUpdateHandler("default", bundle -> handler3CallCount.incrementAndGet());

        customizer.afterSingletonsInstantiated();
        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockCredentialsConfig).registerReloadCallback(callbackCaptor.capture());

        callbackCaptor.getValue().run();

        assertThat(handler1CallCount.get()).isEqualTo(1);
        assertThat(handler2CallCount.get()).isEqualTo(1);
        assertThat(handler3CallCount.get()).isEqualTo(1);
    }

    @Test
    public void givenMultipleReloads_whenTriggered_thenShouldNotifyHandlersEachTime() {
        SslBundles sslBundles = customizer.sslBundles();
        AtomicInteger handlerCallCount = new AtomicInteger(0);
        sslBundles.addBundleUpdateHandler("default", bundle -> handlerCallCount.incrementAndGet());

        customizer.afterSingletonsInstantiated();
        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockCredentialsConfig).registerReloadCallback(callbackCaptor.capture());
        Runnable reloadCallback = callbackCaptor.getValue();

        reloadCallback.run();
        reloadCallback.run();
        reloadCallback.run();

        assertThat(handlerCallCount.get()).isEqualTo(3);
    }

    @Test
    public void givenUpdateHandlerThrowsException_whenReload_thenShouldContinueNotifyingOtherHandlers() {
        SslBundles sslBundles = customizer.sslBundles();
        AtomicInteger handler1CallCount = new AtomicInteger(0);
        AtomicInteger handler2CallCount = new AtomicInteger(0);

        sslBundles.addBundleUpdateHandler("default", bundle -> {
            handler1CallCount.incrementAndGet();
            throw new RuntimeException("Handler 1 failed");
        });
        sslBundles.addBundleUpdateHandler("default", bundle -> handler2CallCount.incrementAndGet());

        customizer.afterSingletonsInstantiated();
        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockCredentialsConfig).registerReloadCallback(callbackCaptor.capture());

        callbackCaptor.getValue().run();

        assertThat(handler1CallCount.get()).isEqualTo(1);
        assertThat(handler2CallCount.get()).isEqualTo(1);
    }

    @Test
    public void givenConcurrentReloads_whenTriggered_thenShouldHandleThreadSafely() throws Exception {
        SslBundles sslBundles = customizer.sslBundles();
        AtomicInteger handlerCallCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(5);

        sslBundles.addBundleUpdateHandler("default", bundle -> handlerCallCount.incrementAndGet());

        customizer.afterSingletonsInstantiated();
        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockCredentialsConfig).registerReloadCallback(callbackCaptor.capture());
        Runnable reloadCallback = callbackCaptor.getValue();

        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    reloadCallback.run();
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
        assertThat(handlerCallCount.get()).isEqualTo(5);
    }

    @Test
    public void givenReloadWithFailingCredentials_whenInvoked_thenShouldHandleGracefully() {
        when(mockCredentialsConfig.getCredentials()).thenThrow(new RuntimeException("Failed to load credentials"));

        customizer.afterSingletonsInstantiated();
        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockCredentialsConfig).registerReloadCallback(callbackCaptor.capture());

        callbackCaptor.getValue().run();
    }

    @Test
    public void givenSslBundle_whenGetBundleMultipleTimes_thenShouldReturnFreshBundle() {
        SslBundles sslBundles = customizer.sslBundles();

        SslBundle bundle1 = sslBundles.getBundle("default");
        SslBundle bundle2 = sslBundles.getBundle("default");

        assertThat(bundle1).isNotNull();
        assertThat(bundle2).isNotNull();
    }

    @Test
    public void givenHttpServerSslCredentials_whenCreateBean_thenShouldReturnConfig() {
        SslCredentialsConfig config = customizer.httpServerSslCredentials();

        assertThat(config).isNotNull();
        assertThat(config.getName()).isEqualTo("HTTP Server SSL Credentials");
        assertThat(config.isTrustsOnly()).isFalse();
    }

}
