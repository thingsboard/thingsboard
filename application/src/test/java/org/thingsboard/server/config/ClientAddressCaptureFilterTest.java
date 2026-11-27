// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.config;

import com.google.common.net.InetAddresses;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.thingsboard.server.service.security.ip.ClientAddressResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientAddressCaptureFilterTest {

    @Test
    void capturesSocketAndResolvedAddresses() throws Exception {
        ClientAddressCaptureFilter filter = filter("10.0.0.0/8");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(request.getAttribute(ClientAddressCaptureFilter.RAW_SOCKET_ADDRESS_ATTR))
                .isEqualTo(InetAddresses.forString("10.0.0.2"));
        assertThat(request.getAttribute(ClientAddressCaptureFilter.RESOLVED_CLIENT_ADDRESS_ATTR))
                .isEqualTo(InetAddresses.forString("1.2.3.4"));
    }

    @Test
    void xffIgnoredWithoutTrustedProxies() throws Exception {
        ClientAddressCaptureFilter filter = filter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.7");
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(request.getAttribute(ClientAddressCaptureFilter.RESOLVED_CLIENT_ADDRESS_ATTR))
                .isEqualTo(InetAddresses.forString("198.51.100.7"));
    }

    @Test
    void loopbackWithoutInternalTokenNotTrusted() throws Exception {
        ClientAddressCaptureFilter filter = filter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(request.getAttribute(ClientAddressCaptureFilter.RESOLVED_CLIENT_ADDRESS_ATTR))
                .isEqualTo(InetAddresses.forString("127.0.0.1"));
    }

    @Test
    void loopbackWithInternalTokenForwardsClientAddress() throws Exception {
        ClientAddressCaptureFilter filter = filter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        request.addHeader(org.thingsboard.server.service.security.ip.InternalForwardingToken.HEADER,
                org.thingsboard.server.service.security.ip.InternalForwardingToken.value());
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(request.getAttribute(ClientAddressCaptureFilter.RESOLVED_CLIENT_ADDRESS_ATTR))
                .isEqualTo(InetAddresses.forString("1.2.3.4"));
    }

    @Test
    void failedResolutionCannotFallBackToSteeredRemoteAddr() throws Exception {
        // trusted proxy sends a malformed XFF entry; a wrapper (standing in for ForwardedHeaderFilter)
        // reports a client-chosen getRemoteAddr(); the helper must fail closed, not be steered
        ClientAddressCaptureFilter filter = filter("10.0.0.0/8");
        MockHttpServletRequest raw = new MockHttpServletRequest();
        raw.setRemoteAddr("10.0.0.2");
        raw.addHeader("X-Forwarded-For", "garbage");
        jakarta.servlet.http.HttpServletRequestWrapper wrapper = new jakarta.servlet.http.HttpServletRequestWrapper(raw) {
            @Override
            public String getRemoteAddr() {
                return "203.0.113.5";
            }
        };
        filter.doFilter(wrapper, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(raw.getAttribute(ClientAddressCaptureFilter.RESOLVED_CLIENT_ADDRESS_ATTR)).isNull();
        assertThat(ClientAddressCaptureFilter.resolvedClientAddress(wrapper)).isNull();
    }

    @Test
    void helperFallsBackToRemoteAddrOnlyWhenCaptureNeverRan() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("9.9.9.9");
        assertThat(ClientAddressCaptureFilter.resolvedClientAddress(request))
                .isEqualTo(InetAddresses.forString("9.9.9.9"));
        request.setAttribute(ClientAddressCaptureFilter.CAPTURE_DONE_ATTR, Boolean.TRUE);
        assertThat(ClientAddressCaptureFilter.resolvedClientAddress(request)).isNull();
    }

    @Test
    void unwrapsRequestWrappersToReadRawSocketAddress() throws Exception {
        // simulates running after ForwardedHeaderFilter: the wrapper lies about the remote address
        ClientAddressCaptureFilter filter = filter();
        MockHttpServletRequest raw = new MockHttpServletRequest();
        raw.setRemoteAddr("198.51.100.7");
        raw.addHeader("X-Forwarded-For", "1.2.3.4");
        jakarta.servlet.http.HttpServletRequestWrapper wrapper = new jakarta.servlet.http.HttpServletRequestWrapper(raw) {
            @Override
            public String getRemoteAddr() {
                return "1.2.3.4";
            }
        };
        filter.doFilter(wrapper, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(raw.getAttribute(ClientAddressCaptureFilter.RAW_SOCKET_ADDRESS_ATTR))
                .isEqualTo(InetAddresses.forString("198.51.100.7"));
        assertThat(raw.getAttribute(ClientAddressCaptureFilter.RESOLVED_CLIENT_ADDRESS_ATTR))
                .isEqualTo(InetAddresses.forString("198.51.100.7"));
    }

    private static ClientAddressCaptureFilter filter(String... trustedProxies) {
        SysAdminAllowlistProperties props = new SysAdminAllowlistProperties(List.of(), List.of(trustedProxies));
        props.init();
        return new ClientAddressCaptureFilter(new ClientAddressResolver(props));
    }

}
