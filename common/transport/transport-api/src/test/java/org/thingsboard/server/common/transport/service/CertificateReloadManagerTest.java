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
package org.thingsboard.server.common.transport.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class CertificateReloadManagerTest {

    @TempDir
    Path tempDir;

    private CertificateReloadManager certificateReloadManager;
    private Path certFile;

    @BeforeEach
    public void setup() throws IOException {
        certificateReloadManager = new CertificateReloadManager();

        certFile = tempDir.resolve("test-cert.pem");
        Files.writeString(certFile, "-----BEGIN CERTIFICATE-----\nTEST_CERT_V1\n-----END CERTIFICATE-----\n");
    }

    @AfterEach
    public void teardown() throws Exception {
        if (certificateReloadManager != null) {
            certificateReloadManager.destroy();
        }
    }

    private void writeFileAndAwaitMtimeChange(Path path, String content, long baselineMtime) throws IOException {
        Files.writeString(path, content);
        await().atMost(2, SECONDS)
                .pollInterval(10, MILLISECONDS)
                .until(() -> Files.getLastModifiedTime(path).toMillis() != baselineMtime);
    }

    private long mtime(Path path) throws IOException {
        return Files.getLastModifiedTime(path).toMillis();
    }

    @Test
    public void givenCertificateFileChanged_whenCheckForChanges_thenShouldTriggerReload() throws Exception {
        AtomicInteger reloadCount = new AtomicInteger(0);

        certificateReloadManager.registerWatcher("test-cert", certFile, reloadCount::incrementAndGet);

        long baseline = mtime(certFile);
        writeFileAndAwaitMtimeChange(certFile, "-----BEGIN CERTIFICATE-----\nTEST_CERT_V2_MODIFIED\n-----END CERTIFICATE-----\n", baseline);

        ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");

        assertThat(reloadCount.get()).isEqualTo(1);
    }

    @Test
    public void givenCertificateFileUnchanged_whenCheckForChanges_thenShouldNotTriggerReload() throws Exception {
        AtomicInteger reloadCount = new AtomicInteger(0);

        certificateReloadManager.registerWatcher("test-cert", certFile, reloadCount::incrementAndGet);

        ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");
        ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");

        assertThat(reloadCount.get()).isEqualTo(0);
    }

    @Test
    public void givenOnlyTimestampChanged_whenCheckForChanges_thenShouldNotTriggerReload() throws Exception {
        AtomicInteger reloadCount = new AtomicInteger(0);

        certificateReloadManager.registerWatcher("test-cert", certFile, reloadCount::incrementAndGet);

        long bumpedMtime = Files.getLastModifiedTime(certFile).toMillis() + 5_000L;
        Files.setLastModifiedTime(certFile, FileTime.fromMillis(bumpedMtime));

        ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");

        assertThat(reloadCount.get()).isEqualTo(0);
    }

    @Test
    public void givenWatcherRegistered_whenFileDeleted_thenShouldNotCrash() throws Exception {
        AtomicInteger reloadCount = new AtomicInteger(0);

        certificateReloadManager.registerWatcher("test-cert", certFile, reloadCount::incrementAndGet);

        Files.delete(certFile);

        ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");

        assertThat(reloadCount.get()).isEqualTo(1);
    }

    @Test
    public void givenWatcherRegistered_whenShutdown_thenShouldStopScheduler() throws Exception {
        certificateReloadManager.registerWatcher("test-cert", certFile, () -> {});
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ReflectionTestUtils.setField(certificateReloadManager, "scheduler", scheduler);

        certificateReloadManager.destroy();

        assertThat(scheduler.isShutdown()).isTrue();
        assertThat(scheduler.isTerminated()).isTrue();
    }

    @Test
    public void givenMultipleCertificateFiles_whenOneChanges_thenShouldTriggerReload() throws Exception {
        Path keyFile = tempDir.resolve("test-key.pem");
        Files.writeString(keyFile, "-----BEGIN PRIVATE KEY-----\nTEST_KEY_V1\n-----END PRIVATE KEY-----\n");

        AtomicInteger certReloadCount = new AtomicInteger(0);
        AtomicInteger keyReloadCount = new AtomicInteger(0);

        certificateReloadManager.registerWatcher("test-cert", certFile, certReloadCount::incrementAndGet);
        certificateReloadManager.registerWatcher("test-key", keyFile, keyReloadCount::incrementAndGet);

        long baseline = mtime(keyFile);
        writeFileAndAwaitMtimeChange(keyFile, "-----BEGIN PRIVATE KEY-----\nTEST_KEY_V2_MODIFIED\n-----END PRIVATE KEY-----\n", baseline);

        ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");

        assertThat(keyReloadCount.get()).isEqualTo(1);
        assertThat(certReloadCount.get()).isEqualTo(0);
    }

    @Test
    public void givenMultipleWatchers_whenCheckCertificates_thenShouldCheckAll() throws Exception {
        Path cert2File = tempDir.resolve("test-cert2.pem");
        Files.writeString(cert2File, "-----BEGIN CERTIFICATE-----\nTEST_CERT2_V1\n-----END CERTIFICATE-----\n");

        AtomicInteger reload1Count = new AtomicInteger(0);
        AtomicInteger reload2Count = new AtomicInteger(0);

        certificateReloadManager.registerWatcher("test-cert1", certFile, reload1Count::incrementAndGet);
        certificateReloadManager.registerWatcher("test-cert2", cert2File, reload2Count::incrementAndGet);

        long baseline1 = mtime(certFile);
        long baseline2 = mtime(cert2File);
        writeFileAndAwaitMtimeChange(certFile, "-----BEGIN CERTIFICATE-----\nMODIFIED1\n-----END CERTIFICATE-----\n", baseline1);
        writeFileAndAwaitMtimeChange(cert2File, "-----BEGIN CERTIFICATE-----\nMODIFIED2\n-----END CERTIFICATE-----\n", baseline2);

        ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");

        assertThat(reload1Count.get()).isEqualTo(1);
        assertThat(reload2Count.get()).isEqualTo(1);
    }

    @Test
    public void givenCallbackThrowsException_whenCheckForChanges_thenShouldContinueWithOtherWatchers() throws Exception {
        Path cert2File = tempDir.resolve("test-cert2.pem");
        Files.writeString(cert2File, "-----BEGIN CERTIFICATE-----\nTEST_CERT2_V1\n-----END CERTIFICATE-----\n");

        AtomicInteger reload2Count = new AtomicInteger(0);

        certificateReloadManager.registerWatcher("test-cert1", certFile, () -> {
            throw new RuntimeException("Simulated reload failure");
        });
        certificateReloadManager.registerWatcher("test-cert2", cert2File, reload2Count::incrementAndGet);

        long baseline1 = mtime(certFile);
        long baseline2 = mtime(cert2File);
        writeFileAndAwaitMtimeChange(certFile, "-----BEGIN CERTIFICATE-----\nMODIFIED1\n-----END CERTIFICATE-----\n", baseline1);
        writeFileAndAwaitMtimeChange(cert2File, "-----BEGIN CERTIFICATE-----\nMODIFIED2\n-----END CERTIFICATE-----\n", baseline2);

        ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");

        assertThat(reload2Count.get()).isEqualTo(1);
    }

    @Test
    public void givenFileDeletedAndRecreated_whenCheckForChanges_thenShouldTriggerReload() throws Exception {
        AtomicInteger reloadCount = new AtomicInteger(0);

        certificateReloadManager.registerWatcher("test-cert", certFile, reloadCount::incrementAndGet);

        Files.delete(certFile);
        ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");
        assertThat(reloadCount.get()).isEqualTo(1);

        Files.writeString(certFile, "-----BEGIN CERTIFICATE-----\nNEW_CERT\n-----END CERTIFICATE-----\n");
        ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");
        assertThat(reloadCount.get()).isEqualTo(2);
    }

    @Test
    public void givenRapidFileModifications_whenCheckForChanges_thenShouldDetectLatestChange() throws Exception {
        AtomicInteger reloadCount = new AtomicInteger(0);

        certificateReloadManager.registerWatcher("test-cert", certFile, reloadCount::incrementAndGet);

        long baseline = mtime(certFile);
        for (int i = 0; i < 5; i++) {
            Files.writeString(certFile, "-----BEGIN CERTIFICATE-----\nCERT_VERSION_" + i + "\n-----END CERTIFICATE-----\n");
        }
        await().atMost(2, SECONDS)
                .pollInterval(10, MILLISECONDS)
                .until(() -> mtime(certFile) != baseline);

        ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");

        assertThat(reloadCount.get()).isEqualTo(1);
    }

    @Test
    public void givenConcurrentChecks_whenCheckForChanges_thenShouldReloadExactlyOnce() throws Exception {
        AtomicInteger reloadCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(5);

        certificateReloadManager.registerWatcher("test-cert", certFile, reloadCount::incrementAndGet);

        long baseline = mtime(certFile);
        writeFileAndAwaitMtimeChange(certFile, "-----BEGIN CERTIFICATE-----\nMODIFIED\n-----END CERTIFICATE-----\n", baseline);

        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");
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
        assertThat(reloadCount.get()).isEqualTo(1);
    }

    @Test
    public void givenSameContentRewritten_whenCheckForChanges_thenShouldNotTriggerReload() throws Exception {
        AtomicInteger reloadCount = new AtomicInteger(0);
        String originalContent = Files.readString(certFile);

        certificateReloadManager.registerWatcher("test-cert", certFile, reloadCount::incrementAndGet);

        long baseline = mtime(certFile);
        writeFileAndAwaitMtimeChange(certFile, originalContent, baseline);

        ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");

        assertThat(reloadCount.get()).isEqualTo(0);
    }

    @Test
    public void givenCallbackFailsRepeatedly_whenMaxFailuresReached_thenShouldStopRetrying() throws Exception {
        AtomicInteger reloadAttempts = new AtomicInteger(0);

        certificateReloadManager.registerWatcher("test-cert", certFile, () -> {
            reloadAttempts.incrementAndGet();
            throw new RuntimeException("Persistent failure");
        });

        long baseline = mtime(certFile);
        writeFileAndAwaitMtimeChange(certFile, "-----BEGIN CERTIFICATE-----\nBAD_CERT\n-----END CERTIFICATE-----\n", baseline);

        for (int i = 0; i < 15; i++) {
            ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");
        }

        assertThat(reloadAttempts.get()).isEqualTo(10);
    }

    @Test
    public void givenCallbackFailedPreviously_whenFileChangesAgain_thenShouldResetAndRetry() throws Exception {
        AtomicInteger reloadAttempts = new AtomicInteger(0);
        AtomicInteger shouldFail = new AtomicInteger(1);

        certificateReloadManager.registerWatcher("test-cert", certFile, () -> {
            reloadAttempts.incrementAndGet();
            if (shouldFail.get() == 1) {
                throw new RuntimeException("Transient failure");
            }
        });

        long baseline = mtime(certFile);
        writeFileAndAwaitMtimeChange(certFile, "-----BEGIN CERTIFICATE-----\nBAD_CERT\n-----END CERTIFICATE-----\n", baseline);

        ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");
        assertThat(reloadAttempts.get()).isEqualTo(1);

        shouldFail.set(0);
        long baseline2 = mtime(certFile);
        writeFileAndAwaitMtimeChange(certFile, "-----BEGIN CERTIFICATE-----\nGOOD_CERT\n-----END CERTIFICATE-----\n", baseline2);

        ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");
        assertThat(reloadAttempts.get()).isEqualTo(2);
    }

    @Test
    public void givenCallbackHitMaxFailures_whenFileChangesToNewContent_thenShouldResetAndRetry() throws Exception {
        AtomicInteger reloadAttempts = new AtomicInteger(0);
        AtomicInteger shouldFail = new AtomicInteger(1);

        certificateReloadManager.registerWatcher("test-cert", certFile, () -> {
            reloadAttempts.incrementAndGet();
            if (shouldFail.get() == 1) {
                throw new RuntimeException("Persistent failure");
            }
        });

        long baseline = mtime(certFile);
        writeFileAndAwaitMtimeChange(certFile, "-----BEGIN CERTIFICATE-----\nBAD_CERT\n-----END CERTIFICATE-----\n", baseline);

        for (int i = 0; i < 15; i++) {
            ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");
        }
        assertThat(reloadAttempts.get()).isEqualTo(10);

        shouldFail.set(0);
        long baseline2 = mtime(certFile);
        writeFileAndAwaitMtimeChange(certFile, "-----BEGIN CERTIFICATE-----\nFIXED_CERT\n-----END CERTIFICATE-----\n", baseline2);

        ReflectionTestUtils.invokeMethod(certificateReloadManager, "checkCertificates");
        assertThat(reloadAttempts.get()).isEqualTo(11);
    }

}
