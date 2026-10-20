// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util.itext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.common.util.SsrfProtectionValidator;
import org.xhtmlrenderer.pdf.ITextOutputDevice;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class PdfReportUserAgentSsrfTest {

    private PdfReportUserAgent userAgent;

    @BeforeEach
    void setUp() {
        ITextOutputDevice outputDevice = mock(ITextOutputDevice.class);
        userAgent = new PdfReportUserAgent(null, null, outputDevice, 1, 800);
    }

    @AfterEach
    void tearDown() {
        SsrfProtectionValidator.setEnabled(false);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file:///etc/hosts",
            "file:///etc/passwd",
            "ftp://internal.host/file"
    })
    void testBlockedSchemesThrow(String uri) {
        SsrfProtectionValidator.setEnabled(true);
        assertThatThrownBy(() -> userAgent.resolveAndOpenStream(uri))
                .isInstanceOf(PdfReportUserAgent.PdfReportImageException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://169.254.169.254/latest/meta-data/",
            "http://localhost/secret",
            "http://127.0.0.1/admin",
            "http://10.0.0.1/internal",
            "http://192.168.1.1/config",
            "http://[::1]/path"
    })
    void testBlockedHostsThrow(String uri) {
        SsrfProtectionValidator.setEnabled(true);
        assertThatThrownBy(() -> userAgent.resolveAndOpenStream(uri))
                .isInstanceOf(PdfReportUserAgent.PdfReportImageException.class);
    }

    @Test
    void testInvalidUriThrows() {
        SsrfProtectionValidator.setEnabled(true);
        assertThatThrownBy(() -> userAgent.resolveAndOpenStream("://broken uri{"))
                .isInstanceOf(PdfReportUserAgent.PdfReportImageException.class);
    }

    @Test
    void testJarSchemeNotBlocked() {
        SsrfProtectionValidator.setEnabled(true);
        String jarUri = "jar:file:/some/path/flying-saucer-core.jar!/resources/css/XhtmlNamespaceHandler.css";
        // Must not throw — jar: URIs are used by Flying Saucer for internal resources
        userAgent.resolveAndOpenStream(jarUri);
    }

    @Test
    void testFileUriBlockedButReadableWhenDisabled(@TempDir Path tempDir) throws Exception {
        Path tempFile = Files.createFile(tempDir.resolve("test.txt"));
        Files.writeString(tempFile, "test content");
        String fileUri = tempFile.toUri().toString();

        SsrfProtectionValidator.setEnabled(true);
        assertThatThrownBy(() -> userAgent.resolveAndOpenStream(fileUri))
                .isInstanceOf(PdfReportUserAgent.PdfReportImageException.class);

        SsrfProtectionValidator.setEnabled(false);
        try (InputStream is = userAgent.resolveAndOpenStream(fileUri)) {
            assertThat(is).as("file:// URI should be readable when SSRF protection is off").isNotNull();
        }
    }

}
