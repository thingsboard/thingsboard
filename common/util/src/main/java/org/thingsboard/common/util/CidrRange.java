// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.common.util;

import com.google.common.net.InetAddresses;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * An IPv4 or IPv6 address range in CIDR notation. Parsing accepts IP literals only
 * and never performs DNS resolution, so it is safe for security-sensitive matching.
 */
public record CidrRange(byte[] network, int prefixLength) {

    private static final int IPV4_MAPPED_PREFIX_LENGTH = 12;

    /**
     * Parses an IP literal ("203.0.113.10", "2001:db8::1") or CIDR block ("198.51.100.0/24", "2001:db8::/48").
     * Hostnames and malformed input are rejected with {@link IllegalArgumentException}; no DNS lookup is ever made.
     */
    public static CidrRange parseLiteral(String entry) {
        if (entry == null || entry.isBlank()) {
            throw new IllegalArgumentException("Empty IP/CIDR entry");
        }
        String trimmed = entry.trim();
        int slashIndex = trimmed.indexOf('/');
        String ip = slashIndex >= 0 ? trimmed.substring(0, slashIndex) : trimmed;
        InetAddress address;
        try {
            address = InetAddresses.forString(ip);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Not an IP literal or CIDR block: " + trimmed);
        }
        byte[] addr = address.getAddress();
        int prefixLength;
        if (slashIndex >= 0) {
            try {
                prefixLength = Integer.parseInt(trimmed.substring(slashIndex + 1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid prefix length in: " + trimmed);
            }
            if (prefixLength < 0 || prefixLength > addr.length * 8) {
                throw new IllegalArgumentException("Invalid prefix length: " + prefixLength + " for " + trimmed);
            }
        } else {
            prefixLength = addr.length * 8;
        }
        return new CidrRange(addr, prefixLength);
    }

    public static CidrRange of(InetAddress address, int prefixLength) {
        byte[] addr = address.getAddress();
        if (prefixLength < 0 || prefixLength > addr.length * 8) {
            throw new IllegalArgumentException("Invalid prefix length: " + prefixLength + " for " + address.getHostAddress());
        }
        return new CidrRange(addr, prefixLength);
    }

    public boolean contains(InetAddress address) {
        byte[] addr = address.getAddress();
        if (addr.length == 16 && network.length == 4) {
            byte[] unmapped = unmapIpv4(addr);
            if (unmapped == null) {
                return false;
            }
            addr = unmapped;
        } else if (addr.length != network.length) {
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

    private static byte[] unmapIpv4(byte[] ipv6) {
        for (int i = 0; i < 10; i++) {
            if (ipv6[i] != 0) {
                return null;
            }
        }
        if (ipv6[10] != (byte) 0xff || ipv6[11] != (byte) 0xff) {
            return null;
        }
        byte[] ipv4 = new byte[4];
        System.arraycopy(ipv6, IPV4_MAPPED_PREFIX_LENGTH, ipv4, 0, 4);
        return ipv4;
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
