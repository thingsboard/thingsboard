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

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable SSRF protection configuration. Replaces the global mutable static state
 * previously held in {@link SsrfProtectionValidator}.
 * <p>
 * Instances are created at startup (from Spring {@code @Value} properties) and passed
 * through the call chain, eliminating shared mutable state and test-pollution issues.
 */
@Slf4j
public final class SsrfProtectionConfig {

    public static final SsrfProtectionConfig DISABLED = new SsrfProtectionConfig(
            false, List.of(), Set.of(), List.of(), Set.of());

    private final boolean enabled;
    private final List<SsrfProtectionValidator.CidrRange> allowedCidrRanges;
    private final Set<String> allowedHostnames;
    private final List<SsrfProtectionValidator.CidrRange> additionalBlockedCidrRanges;
    private final Set<String> additionalBlockedHostnames;

    SsrfProtectionConfig(boolean enabled,
                         List<SsrfProtectionValidator.CidrRange> allowedCidrRanges,
                         Set<String> allowedHostnames,
                         List<SsrfProtectionValidator.CidrRange> additionalBlockedCidrRanges,
                         Set<String> additionalBlockedHostnames) {
        this.enabled = enabled;
        this.allowedCidrRanges = allowedCidrRanges;
        this.allowedHostnames = allowedHostnames;
        this.additionalBlockedCidrRanges = additionalBlockedCidrRanges;
        this.additionalBlockedHostnames = additionalBlockedHostnames;
    }

    /**
     * Creates a config from raw property values (as read from Spring {@code @Value}).
     */
    public static SsrfProtectionConfig of(boolean enabled,
                                           List<String> allowedHosts,
                                           List<String> additionalBlockedHosts) {
        ParsedEntries allowed = parseEntries(allowedHosts);
        ParsedEntries blocked = parseEntries(additionalBlockedHosts);
        if (!allowed.cidrRanges.isEmpty() || !allowed.hostnames.isEmpty()) {
            log.info("SSRF allowed hosts configured: {} CIDR range(s), {} hostname(s)",
                    allowed.cidrRanges.size(), allowed.hostnames.size());
        }
        if (!blocked.cidrRanges.isEmpty() || !blocked.hostnames.isEmpty()) {
            log.info("SSRF additional blocked hosts configured: {} CIDR range(s), {} hostname(s)",
                    blocked.cidrRanges.size(), blocked.hostnames.size());
        }
        return new SsrfProtectionConfig(enabled,
                allowed.cidrRanges, allowed.hostnames,
                blocked.cidrRanges, blocked.hostnames);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<SsrfProtectionValidator.CidrRange> getAllowedCidrRanges() {
        return allowedCidrRanges;
    }

    public Set<String> getAllowedHostnames() {
        return allowedHostnames;
    }

    public List<SsrfProtectionValidator.CidrRange> getAdditionalBlockedCidrRanges() {
        return additionalBlockedCidrRanges;
    }

    public Set<String> getAdditionalBlockedHostnames() {
        return additionalBlockedHostnames;
    }

    private static ParsedEntries parseEntries(List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return ParsedEntries.EMPTY;
        }
        List<SsrfProtectionValidator.CidrRange> cidrRanges = new ArrayList<>();
        Set<String> hostnames = new HashSet<>();
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.contains("/") || isIpLiteral(trimmed)) {
                try {
                    cidrRanges.add(SsrfProtectionValidator.CidrRange.parse(trimmed));
                } catch (Exception e) {
                    log.warn("Failed to parse CIDR/IP entry '{}': {}", trimmed, e.getMessage());
                }
            } else {
                hostnames.add(trimmed.toLowerCase());
            }
        }
        return new ParsedEntries(
                Collections.unmodifiableList(cidrRanges),
                Collections.unmodifiableSet(hostnames));
    }

    private static boolean isIpLiteral(String entry) {
        return !entry.isEmpty() && (Character.isDigit(entry.charAt(0)) || entry.contains(":"));
    }

    private record ParsedEntries(List<SsrfProtectionValidator.CidrRange> cidrRanges, Set<String> hostnames) {
        static final ParsedEntries EMPTY = new ParsedEntries(Collections.emptyList(), Collections.emptySet());
    }

}
