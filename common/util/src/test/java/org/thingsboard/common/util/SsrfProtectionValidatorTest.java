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

import java.net.InetAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ResourceLock("SsrfProtectionValidator") // to avoid race conditions when modifying SsrfProtectionValidator's static configuration
public class SsrfProtectionValidatorTest {

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

    // --- Allow-list tests ---

    @Test
    void testAllowListCidrAllowsPrivateAddress() {
        try {
            SsrfProtectionValidator.setAllowedHosts(List.of("192.168.1.0/24"));
            // 192.168.1.1 is normally blocked (site-local), but allow-listed
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://192.168.1.1"), true));
            // Other private ranges remain blocked
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://10.0.0.1"), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
        } finally {
            SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
        }
    }

    @Test
    void testAllowListHostnameBypassesSuffixCheck() {
        try {
            SsrfProtectionValidator.setAllowedHosts(List.of("my-device.local"));
            // .local suffix is normally blocked, but allow-listed hostname passes
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://my-device.local/api"), true));
            // Other .local hostnames remain blocked
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://other-device.local/api"), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
        } finally {
            SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
        }
    }

    @Test
    void testAllowListPrecedenceOverBlockList() {
        try {
            // Block 8.8.8.0/24 via additional-blocked, but allow 8.8.8.8 via allow-list
            SsrfProtectionValidator.setAdditionalBlockedHosts(List.of("8.8.8.0/24"));
            SsrfProtectionValidator.setAllowedHosts(List.of("8.8.8.8"));
            // Allow-list should win
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("https://8.8.8.8"), true));
            // Adjacent IP still blocked
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("https://8.8.8.9"), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
        } finally {
            SsrfProtectionValidator.setAdditionalBlockedHosts(Collections.emptyList());
            SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
        }
    }

    @Test
    void testIsBlockedAddressPublicApi() throws Exception {
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        assertThat(SsrfProtectionValidator.isBlockedAddress(loopback)).isTrue();

        InetAddress publicIp = InetAddress.getByName("8.8.8.8");
        assertThat(SsrfProtectionValidator.isBlockedAddress(publicIp)).isFalse();

        // Allow-listed private address
        try {
            SsrfProtectionValidator.setAllowedHosts(List.of("10.0.0.0/8"));
            InetAddress privateIp = InetAddress.getByName("10.1.2.3");
            assertThat(SsrfProtectionValidator.isBlockedAddress(privateIp)).isFalse();
        } finally {
            SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
        }
    }

    @Test
    void testIsEnabledAccessor() {
        boolean original = SsrfProtectionValidator.isEnabled();
        try {
            SsrfProtectionValidator.setEnabled(true);
            assertThat(SsrfProtectionValidator.isEnabled()).isTrue();
            SsrfProtectionValidator.setEnabled(false);
            assertThat(SsrfProtectionValidator.isEnabled()).isFalse();
        } finally {
            SsrfProtectionValidator.setEnabled(original);
        }
    }

    @Test
    void testSetAllowedHostsEmptyAndNull() {
        // Should not throw
        SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
        SsrfProtectionValidator.setAllowedHosts(null);
    }

    @Test
    void testIsHostnameAllowed() {
        try {
            SsrfProtectionValidator.setAllowedHosts(List.of("my-device.local", "Internal-Server.Corp"));
            assertThat(SsrfProtectionValidator.isHostnameAllowed("my-device.local")).isTrue();
            assertThat(SsrfProtectionValidator.isHostnameAllowed("MY-DEVICE.LOCAL")).isTrue(); // case-insensitive
            assertThat(SsrfProtectionValidator.isHostnameAllowed("internal-server.corp")).isTrue();
            assertThat(SsrfProtectionValidator.isHostnameAllowed("other-device.local")).isFalse();
            assertThat(SsrfProtectionValidator.isHostnameAllowed("example.com")).isFalse();
        } finally {
            SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
        }
    }

    @Test
    void testIsHostnameAllowedEmptyList() {
        SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
        assertThat(SsrfProtectionValidator.isHostnameAllowed("anything")).isFalse();
    }

    @Test
    void testValidateUriUsesStaticEnabledFlag() {
        boolean original = SsrfProtectionValidator.isEnabled();
        try {
            // When enabled, loopback is blocked via the public one-arg overload
            SsrfProtectionValidator.setEnabled(true);
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://127.0.0.1")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");

            // When disabled, loopback passes
            SsrfProtectionValidator.setEnabled(false);
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://127.0.0.1")));
        } finally {
            SsrfProtectionValidator.setEnabled(original);
        }
    }

    @Test
    void testAllowListHostnameCaseInsensitive() {
        try {
            SsrfProtectionValidator.setAllowedHosts(List.of("My-Device.LOCAL"));
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://my-device.local/api"), true));
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://MY-DEVICE.LOCAL/api"), true));
        } finally {
            SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
        }
    }

    @Test
    void testAllowListOverridesCloudMetadataRange() {
        try {
            // 169.254.169.254 is link-local (blocked by default), allow-list should override
            SsrfProtectionValidator.setAllowedHosts(List.of("169.254.169.254"));
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://169.254.169.254/latest/meta-data/"), true));
            // Other link-local still blocked
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://169.254.1.1"), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
        } finally {
            SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
        }
    }

    @Test
    void testAllowListOverridesLoopback() {
        try {
            SsrfProtectionValidator.setAllowedHosts(List.of("127.0.0.0/8"));
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://127.0.0.1"), true));
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://127.1.2.3"), true));
        } finally {
            SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
        }
    }

    @Test
    void testAllowListCidrBoundary() {
        try {
            SsrfProtectionValidator.setAllowedHosts(List.of("192.168.1.0/24"));
            // Last address in range
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://192.168.1.255"), true));
            // First address outside range
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://192.168.2.0"), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
            // Different subnet entirely
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://192.168.0.1"), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
        } finally {
            SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
        }
    }

    @Test
    void testBlockedIpv6UniqueLocal() throws Exception {
        // fc00::/7 covers fc00:: through fdff::
        InetAddress fc00 = InetAddress.getByName("fc00::1");
        assertThat(SsrfProtectionValidator.isBlockedAddress(fc00)).isTrue();

        InetAddress fdAddr = InetAddress.getByName("fd12:3456:789a::1");
        assertThat(SsrfProtectionValidator.isBlockedAddress(fdAddr)).isTrue();

        // fe00:: is NOT in fc00::/7 (it's in fe80::/10 link-local, but fe00:: without the 80 bits is different)
        // 2001:db8:: is a public documentation prefix, not blocked
        InetAddress publicV6 = InetAddress.getByName("2001:db8::1");
        assertThat(SsrfProtectionValidator.isBlockedAddress(publicV6)).isFalse();
    }

    @Test
    void testParseHostEntriesWithWhitespaceAndBlanks() {
        try {
            SsrfProtectionValidator.setAllowedHosts(List.of("  192.168.1.0/24  ", "", "  ", "my-host.corp"));
            // Trimmed CIDR works
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://192.168.1.1"), true));
            // Trimmed hostname works
            assertThat(SsrfProtectionValidator.isHostnameAllowed("my-host.corp")).isTrue();
        } finally {
            SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
        }
    }

    @Test
    void testSetAllowedHostsReplacePrevious() {
        try {
            SsrfProtectionValidator.setAllowedHosts(List.of("192.168.1.0/24"));
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://192.168.1.1"), true));

            // Replace with different range
            SsrfProtectionValidator.setAllowedHosts(List.of("10.0.0.0/8"));
            // Old range no longer allowed
            assertThatThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://192.168.1.1"), true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("URI is invalid");
            // New range allowed
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://10.1.2.3"), true));
        } finally {
            SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
        }
    }

    @Test
    void testAllowListHostnameBypassesBlockedHostname() {
        try {
            // "localhost" is in BLOCKED_HOSTNAMES; allow-listing it should let it through
            SsrfProtectionValidator.setAllowedHosts(List.of("localhost"));
            assertThatNoException().isThrownBy(() -> SsrfProtectionValidator.validateUri(URI.create("http://localhost/path"), true));
        } finally {
            SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
        }
    }

}
