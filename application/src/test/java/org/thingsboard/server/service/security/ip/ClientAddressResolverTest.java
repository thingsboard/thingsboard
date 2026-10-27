// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.security.ip;

import com.google.common.net.InetAddresses;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.config.SysAdminAllowlistProperties;

import java.net.InetAddress;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientAddressResolverTest {

    @Test
    void noTrustedProxiesIgnoresXff() {
        ClientAddressResolver resolver = resolver();
        assertThat(resolver.resolve(addr("198.51.100.7"), List.of("1.2.3.4")))
                .isEqualTo(addr("198.51.100.7"));
    }

    @Test
    void trustedProxyUsesRightmostUntrusted() {
        ClientAddressResolver resolver = resolver("10.0.0.0/8");
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of("1.2.3.4, 10.0.0.3")))
                .isEqualTo(addr("1.2.3.4"));
    }

    @Test
    void clientControlledLeftmostIgnored() {
        ClientAddressResolver resolver = resolver("10.0.0.0/8");
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of("9.9.9.9, 1.2.3.4")))
                .isEqualTo(addr("1.2.3.4"));
    }

    @Test
    void loopbackSocketImplicitlyTrusted() {
        ClientAddressResolver resolver = resolver();
        assertThat(resolver.resolve(addr("127.0.0.1"), List.of("1.2.3.4")))
                .isEqualTo(addr("1.2.3.4"));
        assertThat(resolver.resolve(addr("::1"), List.of("1.2.3.4")))
                .isEqualTo(addr("1.2.3.4"));
    }

    @Test
    void allEntriesTrustedFallsBackToLeftmostTrusted() {
        ClientAddressResolver resolver = resolver("10.0.0.0/8");
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of("10.0.0.5, 10.0.0.3")))
                .isEqualTo(addr("10.0.0.5"));
    }

    @Test
    void noXffFromTrustedProxyReturnsProxyAddress() {
        ClientAddressResolver resolver = resolver("10.0.0.0/8");
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of()))
                .isEqualTo(addr("10.0.0.2"));
        assertThat(resolver.resolve(addr("10.0.0.2"), null))
                .isEqualTo(addr("10.0.0.2"));
    }

    @Test
    void malformedXffFailsClosed() {
        ClientAddressResolver resolver = resolver("10.0.0.0/8");
        // never throws; returns an address on the trusted side of the malformed entry
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of("unknown"))).isEqualTo(addr("10.0.0.2"));
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of("1.2.3.4:5678"))).isEqualTo(addr("10.0.0.2"));
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of(" "))).isEqualTo(addr("10.0.0.2"));
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of("[2001:db8::1]:443"))).isEqualTo(addr("10.0.0.2"));
        // malformed deeper in the chain: entries right of it are already consumed
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of("garbage, 10.0.0.3")))
                .isEqualTo(addr("10.0.0.3"));
    }

    @Test
    void multipleXffHeadersConcatenated() {
        ClientAddressResolver resolver = resolver("10.0.0.0/8");
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of("1.2.3.4", "10.0.0.3")))
                .isEqualTo(addr("1.2.3.4"));
    }

    private static ClientAddressResolver resolver(String... trustedProxies) {
        SysAdminAllowlistProperties props = new SysAdminAllowlistProperties(List.of(), List.of(trustedProxies));
        props.init();
        return new ClientAddressResolver(props);
    }

    private static InetAddress addr(String literal) {
        return InetAddresses.forString(literal);
    }

}
