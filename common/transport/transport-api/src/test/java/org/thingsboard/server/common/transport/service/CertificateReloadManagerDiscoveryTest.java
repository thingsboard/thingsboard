// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.transport.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CertificateReloadManagerDiscoveryTest {

    @TempDir
    Path tempDir;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CertificateReloadManager.class);

    @Test
    public void givenNoSslCredentialsConfigs_whenContextStarts_thenManagerCreatedWithoutScheduler() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CertificateReloadManager.class);
            assertThat(getScheduler(context.getBean(CertificateReloadManager.class))).isNull();
        });
    }

    @Test
    public void givenDisabledSslCredentialsConfig_whenContextStarts_thenSchedulerNotStarted() {
        SslCredentialsConfig config = mockSslCredentialsConfig(false, List.of(tempDir.resolve("server.pem")));
        contextRunner
                .withBean(SslCredentialsConfig.class, () -> config)
                .run(context -> assertThat(getScheduler(context.getBean(CertificateReloadManager.class))).isNull());
    }

    @Test
    public void givenSslCredentialsConfigWithCertificateFile_whenContextStarts_thenSchedulerStarted() {
        SslCredentialsConfig config = mockSslCredentialsConfig(true, List.of(tempDir.resolve("server.pem")));
        contextRunner
                .withBean(SslCredentialsConfig.class, () -> config)
                .run(context -> assertThat(getScheduler(context.getBean(CertificateReloadManager.class))).isNotNull());
    }

    @Test
    public void givenReloadDisabled_whenContextStarts_thenSchedulerNotStarted() {
        SslCredentialsConfig config = mockSslCredentialsConfig(true, List.of(tempDir.resolve("server.pem")));
        contextRunner
                .withPropertyValues("transport.ssl.certificate.reload.enabled=false")
                .withBean(SslCredentialsConfig.class, () -> config)
                .run(context -> assertThat(getScheduler(context.getBean(CertificateReloadManager.class))).isNull());
    }

    private static SslCredentialsConfig mockSslCredentialsConfig(boolean enabled, List<Path> certificateFilePaths) {
        SslCredentials credentials = mock(SslCredentials.class);
        when(credentials.getCertificateFilePaths()).thenReturn(certificateFilePaths);
        SslCredentialsConfig config = mock(SslCredentialsConfig.class);
        when(config.isEnabled()).thenReturn(enabled);
        when(config.getName()).thenReturn("Test SSL Credentials");
        when(config.getCredentials()).thenReturn(credentials);
        return config;
    }

    private static Object getScheduler(CertificateReloadManager manager) {
        return ReflectionTestUtils.getField(manager, "scheduler");
    }

}
