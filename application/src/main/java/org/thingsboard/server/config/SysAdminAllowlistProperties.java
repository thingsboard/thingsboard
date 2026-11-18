// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.CidrRange;

import java.util.List;

/**
 * Static configuration of the System Administrator IP allowlist. An empty allowed-addresses
 * list (the default) means no restriction. Entries must be IP literals or CIDR blocks;
 * a malformed entry aborts startup rather than silently weakening the policy.
 */
@Slf4j
@Component
public class SysAdminAllowlistProperties {

    private final List<String> allowedAddresses;
    private final List<String> trustedProxies;
    private final String forwardHeadersStrategy;

    private List<CidrRange> allowedRanges;
    private List<CidrRange> trustedProxyRanges;

    @org.springframework.beans.factory.annotation.Autowired
    public SysAdminAllowlistProperties(
            @Value("${security.sysadmin_allowlist.allowed_addresses:}") List<String> allowedAddresses,
            @Value("${security.sysadmin_allowlist.trusted_proxies:}") List<String> trustedProxies,
            @Value("${server.forward_headers_strategy:framework}") String forwardHeadersStrategy) {
        this.allowedAddresses = allowedAddresses;
        this.trustedProxies = trustedProxies;
        this.forwardHeadersStrategy = forwardHeadersStrategy;
    }

    public SysAdminAllowlistProperties(List<String> allowedAddresses, List<String> trustedProxies) {
        this(allowedAddresses, trustedProxies, "framework");
    }

    @PostConstruct
    public void init() {
        allowedRanges = parse(allowedAddresses, "security.sysadmin_allowlist.allowed_addresses");
        trustedProxyRanges = parse(trustedProxies, "security.sysadmin_allowlist.trusted_proxies");
        if (!allowedRanges.isEmpty() && "native".equalsIgnoreCase(forwardHeadersStrategy)) {
            // under the native strategy the servlet container rewrites the peer address from
            // forwarded headers below this application's capture, using the container's own
            // trusted ranges - the allowlist could then be bypassed from any private network
            throw new IllegalStateException("security.sysadmin_allowlist cannot be enforced with "
                    + "server.forward_headers_strategy=native; use the default 'framework' strategy and "
                    + "configure security.sysadmin_allowlist.trusted_proxies instead");
        }
        if (allowedRanges.isEmpty()) {
            log.info("Sysadmin IP allowlist is disabled (no allowed addresses configured)");
        } else {
            log.info("Sysadmin IP allowlist is ENABLED: {} allowed range(s), {} trusted proxy range(s)",
                    allowedRanges.size(), trustedProxyRanges.size());
            log.debug("Sysadmin IP allowlist ranges: allowed={}, trustedProxies={}", allowedRanges, trustedProxyRanges);
            allowedRanges.stream()
                    .filter(range -> range.prefixLength() == 0)
                    .forEach(range -> log.warn("Sysadmin IP allowlist contains '{}', which allows the entire address family", range));
        }
    }

    public boolean isEnforced() {
        return !allowedRanges.isEmpty();
    }

    public List<CidrRange> getAllowedRanges() {
        return allowedRanges;
    }

    public List<CidrRange> getTrustedProxyRanges() {
        return trustedProxyRanges;
    }

    private static List<CidrRange> parse(List<String> entries, String propertyName) {
        if (entries == null) {
            return List.of();
        }
        try {
            return entries.stream()
                    .filter(entry -> entry != null && !entry.isBlank())
                    .map(CidrRange::parseLiteral)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid " + propertyName + " configuration: " + e.getMessage(), e);
        }
    }

}
