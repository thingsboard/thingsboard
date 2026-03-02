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
package org.thingsboard.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SsrfProtectionValidatorTest {

    // JUnit 5 @ResourceLock ensures that tests modifying SsrfProtectionValidator's static
    // additional blocked hosts never run concurrently with each other (parallel execution is enabled).
    private static final String SYNC_LOCK = "SsrfProtectionValidatorTest";

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.com",
            "https://example.com:8443/path",
            "https://8.8.8.8/dns-query"
    })
    void testAllowedUrls(String url) {
        URI uri = URI.create(url);
        assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1",
            "http://127.0.0.1:8080/path",
            "http://127.1.2.3"
    })
    void testBlockedLoopbackIpv4(String url) {
        URI uri = URI.create(url);
        assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URI is invalid");
    }

    @Test
    void testBlockedLocalhost() {
        URI uri = URI.create("http://localhost/path");
        assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URI is invalid");
    }

    @Test
    void testBlockedIpv6Loopback() {
        URI uri = URI.create("http://[::1]/path");
        assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URI is invalid");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://169.254.169.254/latest/meta-data/",
            "http://169.254.169.254/latest/meta-data/iam/security-credentials/",
            "http://169.254.1.1"
    })
    void testBlockedLinkLocalImds(String url) {
        URI uri = URI.create(url);
        assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URI is invalid");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://10.0.0.1",
            "http://10.255.255.255",
            "http://172.16.0.1",
            "http://172.31.255.255",
            "http://192.168.1.1",
            "http://192.168.0.100:8080/api"
    })
    void testBlockedPrivateRfc1918(String url) {
        URI uri = URI.create(url);
        assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URI is invalid");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // 100.64.0.0/10 — Carrier-Grade NAT (RFC 6598): Alibaba Cloud metadata (100.100.100.200), Tencent Cloud (100.88.222.5)
            "http://100.100.100.200",
            "http://100.88.222.5",
            "http://100.64.0.1",
            "http://100.127.255.255",
            // 192.0.0.0/24 — IANA reserved (RFC 6890): Oracle Cloud alternate metadata (192.0.0.192)
            "http://192.0.0.192",
            "http://192.0.0.1",
            // 168.63.129.16 — Azure WireServer
            "http://168.63.129.16"
    })
    void testBlockedCloudMetadataEndpoints(String url) {
        URI uri = URI.create(url);
        assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URI is invalid");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Just outside 100.64.0.0/10
            "http://100.128.0.1",
            // Just outside 192.0.0.0/24
            "http://192.0.1.1",
            // Adjacent to Azure WireServer
            "http://168.63.129.17"
    })
    void testAllowedNearCloudMetadataBoundaries(String url) {
        URI uri = URI.create(url);
        assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file:///etc/passwd",
            "ftp://internal.host/file"
    })
    void testBlockedSchemes(String url) {
        URI uri = URI.create(url);
        assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("only HTTP and HTTPS schemes are allowed");
    }

    @Test
    void testBlockedZeroAddress() {
        URI uri = URI.create("http://0.0.0.0");
        assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URI is invalid");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://server.internal",
            "http://app.local"
    })
    void testBlockedHostnameSuffixes(String url) {
        URI uri = URI.create(url);
        assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URI is invalid");
    }

    @Test
    void testBlockedNullScheme() {
        URI uri = URI.create("//example.com/path");
        assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("only HTTP and HTTPS schemes are allowed");
    }

    @Test
    void testBlockedEmptyHost() {
        URI uri = URI.create("http:///path");
        assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("hostname is missing");
    }

    @Test
    void testBlockedUnresolvableHostname() {
        URI uri = URI.create("http://host.invalid.tld.that.does.not.exist/path");
        assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("unable to resolve hostname");
    }

    @Test
    void testBlockedLocalhostCaseInsensitive() {
        URI uri = URI.create("http://LOCALHOST/path");
        assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URI is invalid");
    }

    @Test
    void testDisabledAllowsPrivateAddresses() {
        URI uri = URI.create("http://127.0.0.1");
        assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(uri, false));
    }

    @Test
    @ResourceLock(SYNC_LOCK)
    void testAdditionalBlockedSingleIp() {
        try {
            SsrfProtectionValidator.setAdditionalBlockedHosts(List.of("8.8.8.8"));
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("https://8.8.8.8/dns-query"), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
            // Adjacent IP is not blocked
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("https://8.8.8.9"), true));
        } finally {
            SsrfProtectionValidator.setAdditionalBlockedHosts(Collections.emptyList());
        }
    }

    @Test
    @ResourceLock(SYNC_LOCK)
    void testAdditionalBlockedCidrSlash10() {
        try {
            // Use 44.0.0.0/10 (not blocked by default) to verify CIDR /10 matching
            SsrfProtectionValidator.setAdditionalBlockedHosts(List.of("44.0.0.0/10"));
            // Inside the range
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://44.0.1.1"), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
            // Last address in the range
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://44.63.255.255"), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
            // Outside the range
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://44.64.0.1"), true));
        } finally {
            SsrfProtectionValidator.setAdditionalBlockedHosts(Collections.emptyList());
        }
    }

    @Test
    @ResourceLock(SYNC_LOCK)
    void testAdditionalBlockedCidrSlash24() {
        try {
            SsrfProtectionValidator.setAdditionalBlockedHosts(List.of("198.51.100.0/24"));
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://198.51.100.0"), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://198.51.100.255"), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
            // Outside the range
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://198.51.101.0"), true));
        } finally {
            SsrfProtectionValidator.setAdditionalBlockedHosts(Collections.emptyList());
        }
    }

    @Test
    @ResourceLock(SYNC_LOCK)
    void testAdditionalBlockedHostnameViaValidateUri() {
        try {
            SsrfProtectionValidator.setAdditionalBlockedHosts(List.of("evil.corp"));
            URI uri = URI.create("http://evil.corp/api");
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
        } finally {
            SsrfProtectionValidator.setAdditionalBlockedHosts(Collections.emptyList());
        }
    }

    @Test
    @ResourceLock(SYNC_LOCK)
    void testAdditionalBlockedHostnameCaseInsensitive() {
        try {
            SsrfProtectionValidator.setAdditionalBlockedHosts(List.of("My-Service.Corp"));
            URI uri = URI.create("http://my-service.corp/api");
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
        } finally {
            SsrfProtectionValidator.setAdditionalBlockedHosts(Collections.emptyList());
        }
    }

    @Test
    @ResourceLock(SYNC_LOCK)
    void testSetAdditionalBlockedHostsEmptyAndNull() {
        // Should not throw
        SsrfProtectionValidator.setAdditionalBlockedHosts(Collections.emptyList());
        SsrfProtectionValidator.setAdditionalBlockedHosts(null);
    }

    @Test
    void testCidrRangeInvalidPrefixLength() {
        assertThatThrownBy(() -> SsrfProtectionValidator.CidrRange.parse("10.0.0.0/999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid prefix length");
        assertThatThrownBy(() -> SsrfProtectionValidator.CidrRange.parse("10.0.0.0/-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid prefix length");
    }

    @Test
    @ResourceLock(SYNC_LOCK)
    void testAdditionalBlockedCidrViaValidateUri() {
        // 203.0.113.0/24 (TEST-NET-3) is not blocked by default
        URI uri = URI.create("http://203.0.113.1");
        assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true));
        try {
            SsrfProtectionValidator.setAdditionalBlockedHosts(List.of("203.0.113.0/24"));
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(uri, true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
        } finally {
            SsrfProtectionValidator.setAdditionalBlockedHosts(Collections.emptyList());
        }
    }

    @Test
    @ResourceLock(SYNC_LOCK)
    void testAdditionalBlockedMixedConfig() {
        try {
            SsrfProtectionValidator.setAdditionalBlockedHosts(List.of("203.0.113.0/24", "evil.corp", "8.8.8.8"));
            // CIDR range
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://203.0.113.50"), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
            // Hostname
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://evil.corp/api"), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
            // Single IP
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("https://8.8.8.8"), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
            // Not in any additional block list
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("https://1.1.1.1"), true));
        } finally {
            SsrfProtectionValidator.setAdditionalBlockedHosts(Collections.emptyList());
        }
    }

}
