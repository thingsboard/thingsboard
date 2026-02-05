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
package org.thingsboard.server.common.transport.config.ssl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SslCredentialsConfigTest {

    @Mock
    private SslCredentials mockCredentials;

    private SslCredentialsConfig config;

    @BeforeEach
    public void setup() {
        config = new SslCredentialsConfig("Test SSL Config", false);
    }

    @Test
    public void givenConfig_whenCreated_thenShouldHaveCorrectName() {
        assertThat(config.getName()).isEqualTo("Test SSL Config");
        assertThat(config.isTrustsOnly()).isFalse();
    }

    @Test
    public void givenTrustsOnlyConfig_whenCreated_thenShouldHaveCorrectTrustsOnly() {
        SslCredentialsConfig trustsOnlyConfig = new SslCredentialsConfig("Trust Config", true);
        assertThat(trustsOnlyConfig.isTrustsOnly()).isTrue();
    }

    @Test
    public void givenCallback_whenRegistered_thenShouldBeStoredInList() {
        AtomicInteger callCount = new AtomicInteger(0);

        config.registerReloadCallback(callCount::incrementAndGet);
        config.setCredentials(mockCredentials);

        try {
            doNothing().when(mockCredentials).reload(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        config.onCertificateFileChanged();

        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    public void givenMultipleCallbacks_whenCertificateChanged_thenAllShouldBeCalled() throws Exception {
        AtomicInteger callback1Count = new AtomicInteger(0);
        AtomicInteger callback2Count = new AtomicInteger(0);
        AtomicInteger callback3Count = new AtomicInteger(0);

        config.registerReloadCallback(callback1Count::incrementAndGet);
        config.registerReloadCallback(callback2Count::incrementAndGet);
        config.registerReloadCallback(callback3Count::incrementAndGet);

        config.setCredentials(mockCredentials);
        doNothing().when(mockCredentials).reload(false);

        config.onCertificateFileChanged();

        assertThat(callback1Count.get()).isEqualTo(1);
        assertThat(callback2Count.get()).isEqualTo(1);
        assertThat(callback3Count.get()).isEqualTo(1);
    }

    @Test
    public void givenCallbackThrowsException_whenCertificateChanged_thenOtherCallbacksShouldStillBeCalled() throws Exception {
        AtomicInteger callback1Count = new AtomicInteger(0);
        AtomicInteger callback2Count = new AtomicInteger(0);

        config.registerReloadCallback(() -> {
            callback1Count.incrementAndGet();
            throw new RuntimeException("Simulated callback failure");
        });
        config.registerReloadCallback(callback2Count::incrementAndGet);

        config.setCredentials(mockCredentials);
        doNothing().when(mockCredentials).reload(false);

        config.onCertificateFileChanged();

        assertThat(callback1Count.get()).isEqualTo(1);
        assertThat(callback2Count.get()).isEqualTo(1);
    }

    @Test
    public void givenCredentialsReloadFails_whenCertificateChanged_thenCallbacksShouldNotBeCalled() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);

        config.registerReloadCallback(callbackCount::incrementAndGet);
        config.setCredentials(mockCredentials);

        doThrow(new RuntimeException("Simulated reload failure")).when(mockCredentials).reload(false);

        config.onCertificateFileChanged();

        assertThat(callbackCount.get()).isEqualTo(0);
    }

    @Test
    public void givenCertificateChanged_whenCredentialsReloadSucceeds_thenShouldCallReload() throws Exception {
        config.setCredentials(mockCredentials);
        doNothing().when(mockCredentials).reload(false);

        config.onCertificateFileChanged();

        verify(mockCredentials).reload(false);
    }

    @Test
    public void givenTrustsOnlyConfig_whenCertificateChanged_thenShouldReloadWithTrustsOnlyTrue() throws Exception {
        SslCredentialsConfig trustsOnlyConfig = new SslCredentialsConfig("Trust Config", true);
        trustsOnlyConfig.setCredentials(mockCredentials);
        doNothing().when(mockCredentials).reload(true);

        trustsOnlyConfig.onCertificateFileChanged();

        verify(mockCredentials).reload(true);
    }

    @Test
    public void givenConcurrentCallbackRegistrations_whenCertificateChanged_thenShouldHandleSafely() throws Exception {
        AtomicInteger totalCallbacks = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    config.registerReloadCallback(totalCallbacks::incrementAndGet);
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

        config.setCredentials(mockCredentials);
        doNothing().when(mockCredentials).reload(false);

        config.onCertificateFileChanged();

        assertThat(totalCallbacks.get()).isEqualTo(10);
    }

}
