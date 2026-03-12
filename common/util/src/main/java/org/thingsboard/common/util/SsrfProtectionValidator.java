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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SsrfProtectionValidator {

    private static volatile boolean enabled;
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final Set<String> BLOCKED_HOSTNAMES = Set.of("localhost");
    private static final Set<String> BLOCKED_HOSTNAME_SUFFIXES = Set.of(".internal", ".local");

    private static volatile AdditionalBlockedHosts additionalBlocked = AdditionalBlockedHosts.EMPTY;

    // Well-known cloud metadata endpoints not covered by the JDK checks (isLoopback, isSiteLocal, isLinkLocal)
    private static final List<CidrRange> CLOUD_METADATA_RANGES = List.of(
            CidrRange.of("100.64.0.0", 10),   // Carrier-Grade NAT (RFC 6598); Alibaba Cloud and Tencent Cloud metadata
            CidrRange.of("192.0.0.0", 24),     // IANA reserved (RFC 6890); Oracle Cloud alternate metadata endpoint
            CidrRange.of("168.63.129.16", 32)  // Azure WireServer
    );

    public static void validateUri(URI uri) {
        validateUri(uri, enabled);
    }

    static void validateUri(URI uri, boolean ssrfProtectionEnabled) {
        if (!ssrfProtectionEnabled) {
            return;
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new RuntimeException("URI is invalid: only HTTP and HTTPS schemes are allowed, got: " + scheme);
        }

        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new RuntimeException("URI is invalid: hostname is missing");
        }

        String hostLower = host.toLowerCase();
        if (BLOCKED_HOSTNAMES.contains(hostLower) || additionalBlocked.hostnames.contains(hostLower)) {
            throwBlockedHost(host);
        }
        for (String suffix : BLOCKED_HOSTNAME_SUFFIXES) {
            if (hostLower.endsWith(suffix)) {
                throwBlockedHost(host);
            }
        }
        // Block IPv6 loopback literal in URL (e.g. http://[::1]/)
        if ("[::1]".equals(host) || "::1".equals(host)) {
            throwBlockedHost(host);
        }

        validateResolvedAddresses(host);
    }

    private static void validateResolvedAddresses(String host) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new RuntimeException("URI is invalid: unable to resolve hostname '" + host + "'", e);
        }

        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                log.debug("Blocked request to host '{}' resolved to '{}'", host, address.getHostAddress());
                throwBlockedHost(host);
            }
        }
    }

    private static boolean isBlockedAddress(InetAddress address) {
        // Covers 127.0.0.0/8 and ::1
        if (address.isLoopbackAddress()) {
            return true;
        }
        // Covers 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
        if (address.isSiteLocalAddress()) {
            return true;
        }
        // Covers 169.254.0.0/16 and fe80::/10
        if (address.isLinkLocalAddress()) {
            return true;
        }
        // Covers 0.0.0.0
        if (address.isAnyLocalAddress()) {
            return true;
        }
        // Additional check for IPv6 unique local addresses (fc00::/7)
        byte[] addr = address.getAddress();
        if (addr.length == 16) {
            int firstByte = addr[0] & 0xFF;
            // fc00::/7 means first 7 bits are 1111110, so first byte is 0xFC or 0xFD
            if (firstByte == 0xFC || firstByte == 0xFD) {
                return true;
            }
        }
        for (CidrRange cidr : CLOUD_METADATA_RANGES) {
            if (cidr.contains(address)) {
                return true;
            }
        }
        // Check additional configured CIDR ranges
        for (CidrRange cidr : additionalBlocked.cidrRanges) {
            if (cidr.contains(address)) {
                return true;
            }
        }
        return false;
    }

    private static void throwBlockedHost(String host) {
        throw new RuntimeException("URI is invalid: host '" + host + "' is not allowed");
    }

    public static void setEnabled(boolean enabled) {
        SsrfProtectionValidator.enabled = enabled;
    }

    public static void setAdditionalBlockedHosts(List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            additionalBlocked = AdditionalBlockedHosts.EMPTY;
            return;
        }
        List<CidrRange> cidrRanges = new ArrayList<>();
        Set<String> hostnames = new HashSet<>();
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.contains("/") || isIpLiteral(trimmed)) {
                try {
                    cidrRanges.add(CidrRange.parse(trimmed));
                } catch (Exception e) {
                    log.warn("Failed to parse CIDR/IP entry '{}': {}", trimmed, e.getMessage());
                }
            } else {
                hostnames.add(trimmed.toLowerCase());
            }
        }
        additionalBlocked = new AdditionalBlockedHosts(
                Collections.unmodifiableList(cidrRanges),
                Collections.unmodifiableSet(hostnames));
        log.info("SSRF additional blocked hosts configured: {} CIDR range(s), {} hostname(s)", cidrRanges.size(), hostnames.size());
    }

    private static boolean isIpLiteral(String entry) {
        // IPv4 starts with a digit, IPv6 contains ':'
        return !entry.isEmpty() && (Character.isDigit(entry.charAt(0)) || entry.contains(":"));
    }

    record AdditionalBlockedHosts(List<CidrRange> cidrRanges, Set<String> hostnames) {
        static final AdditionalBlockedHosts EMPTY = new AdditionalBlockedHosts(Collections.emptyList(), Collections.emptySet());
    }

    record CidrRange(byte[] network, int prefixLength) {

        static CidrRange of(String ip, int prefixLength) {
            try {
                byte[] addr = InetAddress.getByName(ip).getAddress();
                if (prefixLength < 0 || prefixLength > addr.length * 8) {
                    throw new IllegalArgumentException("Invalid prefix length: " + prefixLength + " for " + ip);
                }
                return new CidrRange(addr, prefixLength);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Invalid IP: " + ip, e);
            }
        }

        static CidrRange parse(String entry) throws UnknownHostException {
            int slashIndex = entry.indexOf('/');
            if (slashIndex >= 0) {
                String ip = entry.substring(0, slashIndex);
                int prefix = Integer.parseInt(entry.substring(slashIndex + 1));
                byte[] addr = InetAddress.getByName(ip).getAddress();
                if (prefix < 0 || prefix > addr.length * 8) {
                    throw new IllegalArgumentException("Invalid prefix length: " + prefix + " for " + entry);
                }
                return new CidrRange(addr, prefix);
            } else {
                byte[] addr = InetAddress.getByName(entry).getAddress();
                return new CidrRange(addr, addr.length * 8);
            }
        }

        boolean contains(InetAddress address) {
            byte[] addr = address.getAddress();
            if (addr.length != network.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (addr[i] != network[i]) {
                    return false;
                }
            }
            if (remainingBits > 0 && fullBytes < addr.length) {
                int mask = 0xFF << (8 - remainingBits);
                if ((addr[fullBytes] & mask) != (network[fullBytes] & mask)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            try {
                return InetAddress.getByAddress(network).getHostAddress() + "/" + prefixLength;
            } catch (UnknownHostException e) {
                return "invalid/" + prefixLength;
            }
        }
    }

}
