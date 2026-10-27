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
        assertThat(resolver.resolve(addr("198.51.100.7"), List.of("1.2.3.4"), false))
                .isEqualTo(addr("198.51.100.7"));
    }

    @Test
    void trustedProxyUsesRightmostUntrusted() {
        ClientAddressResolver resolver = resolver("10.0.0.0/8");
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of("1.2.3.4, 10.0.0.3"), false))
                .isEqualTo(addr("1.2.3.4"));
    }

    @Test
    void clientControlledLeftmostIgnored() {
        ClientAddressResolver resolver = resolver("10.0.0.0/8");
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of("9.9.9.9, 1.2.3.4"), false))
                .isEqualTo(addr("1.2.3.4"));
    }

    @Test
    void loopbackNotTrustedWithoutInternalForwardingToken() {
        // a server-side process (e.g. an HTTP rule node) calling the node itself must not be able
        // to steer the resolved address with a forged header
        ClientAddressResolver resolver = resolver();
        assertThat(resolver.resolve(addr("127.0.0.1"), List.of("1.2.3.4"), false))
                .isEqualTo(addr("127.0.0.1"));
        assertThat(resolver.resolve(addr("::1"), List.of("1.2.3.4"), false))
                .isEqualTo(addr("::1"));
    }

    @Test
    void loopbackTrustedWithInternalForwardingToken() {
        // the AI loopback executor authenticates its forwarding with the per-boot internal token
        ClientAddressResolver resolver = resolver();
        assertThat(resolver.resolve(addr("127.0.0.1"), List.of("1.2.3.4"), true))
                .isEqualTo(addr("1.2.3.4"));
    }

    @Test
    void explicitlyConfiguredLoopbackProxyIsTrusted() {
        ClientAddressResolver resolver = resolver("127.0.0.0/8");
        assertThat(resolver.resolve(addr("127.0.0.1"), List.of("1.2.3.4"), false))
                .isEqualTo(addr("1.2.3.4"));
    }

    @Test
    void allEntriesTrustedFallsBackToLeftmostTrusted() {
        ClientAddressResolver resolver = resolver("10.0.0.0/8");
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of("10.0.0.5, 10.0.0.3"), false))
                .isEqualTo(addr("10.0.0.5"));
    }

    @Test
    void noXffFromTrustedProxyReturnsProxyAddress() {
        ClientAddressResolver resolver = resolver("10.0.0.0/8");
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of(), false))
                .isEqualTo(addr("10.0.0.2"));
        assertThat(resolver.resolve(addr("10.0.0.2"), null, false))
                .isEqualTo(addr("10.0.0.2"));
    }

    @Test
    void malformedXffResolvesToNullAndBlocks() {
        // a malformed entry means the chain cannot be trusted at all: null = blocked for
        // sysadmin-scoped principals, never an address that might be allowlisted
        ClientAddressResolver resolver = resolver("10.0.0.0/8");
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of("unknown"), false)).isNull();
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of("1.2.3.4:5678"), false)).isNull();
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of("[2001:db8::1]:443"), false)).isNull();
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of("garbage, 10.0.0.3"), false)).isNull();
    }

    @Test
    void nullSocketResolvesToNull() {
        ClientAddressResolver resolver = resolver("10.0.0.0/8");
        assertThat(resolver.resolve(null, List.of("1.2.3.4"), false)).isNull();
    }

    @Test
    void multipleXffHeadersConcatenated() {
        ClientAddressResolver resolver = resolver("10.0.0.0/8");
        assertThat(resolver.resolve(addr("10.0.0.2"), List.of("1.2.3.4", "10.0.0.3"), false))
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
