// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CidrRangeTest {

    @Test
    void parsesIpv4HostAndCidr() {
        assertThat(CidrRange.parseLiteral("203.0.113.10").contains(addr("203.0.113.10"))).isTrue();
        assertThat(CidrRange.parseLiteral("203.0.113.10").contains(addr("203.0.113.11"))).isFalse();
        assertThat(CidrRange.parseLiteral("198.51.100.0/24").contains(addr("198.51.100.7"))).isTrue();
    }

    @Test
    void parsesIpv6HostAndCidr() {
        assertThat(CidrRange.parseLiteral("::1").contains(addr("::1"))).isTrue();
        assertThat(CidrRange.parseLiteral("2001:db8::/48").contains(addr("2001:db8:0:1::5"))).isTrue();
        assertThat(CidrRange.parseLiteral("2001:db8::/48").contains(addr("2001:db9::1"))).isFalse();
    }

    @Test
    void rejectsHostname() {
        assertThatThrownBy(() -> CidrRange.parseLiteral("example.com"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CidrRange.parseLiteral("localhost/8"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"203.0.113.0/33", "", " ", "10.0.0.0/-1", "not-an-ip", "999.1.1.1", "10.0.0.0/", "10.0.0.0/x", "2001:db8::/129"})
    void rejectsGarbage(String entry) {
        assertThatThrownBy(() -> CidrRange.parseLiteral(entry))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void containsIpv4() {
        CidrRange range = CidrRange.parseLiteral("198.51.100.0/24");
        assertThat(range.contains(addr("198.51.100.1"))).isTrue();
        assertThat(range.contains(addr("198.51.100.255"))).isTrue();
        assertThat(range.contains(addr("198.51.101.1"))).isFalse();
    }

    @Test
    void containsIpv6() {
        CidrRange range = CidrRange.parseLiteral("2001:db8:0:0::/64");
        assertThat(range.contains(addr("2001:db8::abcd"))).isTrue();
        assertThat(range.contains(addr("2001:db8:0:1::1"))).isFalse();
    }

    @Test
    void ipv4MappedIpv6MatchesIpv4Range() throws Exception {
        CidrRange range = CidrRange.parseLiteral("198.51.100.0/24");
        // raw IPv4-mapped IPv6 bytes: ::ffff:198.51.100.7
        byte[] mapped = new byte[16];
        mapped[10] = (byte) 0xff;
        mapped[11] = (byte) 0xff;
        mapped[12] = (byte) 198;
        mapped[13] = (byte) 51;
        mapped[14] = (byte) 100;
        mapped[15] = 7;
        assertThat(range.contains(InetAddress.getByAddress(mapped))).isTrue();
        // and the literal form parses to a range that still matches plain IPv4
        assertThat(CidrRange.parseLiteral("::ffff:198.51.100.7").contains(addr("198.51.100.7"))).isTrue();
    }

    @Test
    void zeroPrefixMatchesEverythingInFamily() {
        CidrRange v4all = CidrRange.parseLiteral("0.0.0.0/0");
        assertThat(v4all.contains(addr("8.8.8.8"))).isTrue();
        assertThat(v4all.contains(addr("2001:db8::1"))).isFalse();
        CidrRange v6all = CidrRange.parseLiteral("::/0");
        assertThat(v6all.contains(addr("2001:db8::1"))).isTrue();
        assertThat(v6all.contains(addr("8.8.8.8"))).isFalse();
    }

    private static InetAddress addr(String literal) {
        return com.google.common.net.InetAddresses.forString(literal);
    }

}
