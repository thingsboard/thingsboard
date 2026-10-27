// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.security.ip;

import com.google.common.net.InetAddresses;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.CidrRange;
import org.thingsboard.server.config.SysAdminAllowlistProperties;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the effective client address from the TCP peer address and the X-Forwarded-For chain
 * using the rightmost-untrusted algorithm: forwarded headers are honored only when presented by
 * a configured trusted proxy (loopback is always trusted), and the first address that is not a
 * trusted proxy, scanning right to left, is the client. A malformed entry stops the scan and the
 * last trusted address wins (fail closed), so header forgery can never move the result towards
 * an attacker-chosen address.
 */
@Component
public class ClientAddressResolver {

    private final SysAdminAllowlistProperties properties;

    public ClientAddressResolver(SysAdminAllowlistProperties properties) {
        this.properties = properties;
    }

    public InetAddress resolve(InetAddress socketAddress, List<String> forwardedForValues) {
        if (socketAddress == null || !isTrustedProxy(socketAddress)) {
            return socketAddress;
        }
        List<String> chain = splitChain(forwardedForValues);
        InetAddress lastTrusted = socketAddress;
        for (int i = chain.size() - 1; i >= 0; i--) {
            InetAddress entry = parseQuietly(chain.get(i));
            if (entry == null) {
                return lastTrusted;
            }
            if (isTrustedProxy(entry)) {
                lastTrusted = entry;
            } else {
                return entry;
            }
        }
        return lastTrusted;
    }

    private boolean isTrustedProxy(InetAddress address) {
        if (address.isLoopbackAddress()) {
            return true;
        }
        for (CidrRange range : properties.getTrustedProxyRanges()) {
            if (range.contains(address)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitChain(List<String> headerValues) {
        if (headerValues == null || headerValues.isEmpty()) {
            return List.of();
        }
        List<String> chain = new ArrayList<>();
        for (String headerValue : headerValues) {
            if (headerValue == null) {
                continue;
            }
            for (String item : headerValue.split(",")) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    chain.add(trimmed);
                }
            }
        }
        return chain;
    }

    private static InetAddress parseQuietly(String literal) {
        try {
            return InetAddresses.forString(literal);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
