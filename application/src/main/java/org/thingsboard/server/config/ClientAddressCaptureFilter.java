// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.config;

import com.google.common.net.InetAddresses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thingsboard.server.service.security.ip.ClientAddressResolver;
import org.thingsboard.server.service.security.ip.InternalForwardingToken;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

/**
 * Captures the TCP peer address and the raw X-Forwarded-For chain from the container's
 * original request — unwrapping any request wrappers, so the values are correct even after
 * ForwardedHeaderFilter has rewritten getRemoteAddr() — and stores the resolved client
 * address as request attributes for security checks downstream. The capture marker lets
 * downstream code distinguish "capture ran and resolution failed (fail closed)" from
 * "capture never ran" (e.g. MockMvc), so a failed resolution can never fall back to the
 * forwarded-header-derived, client-controlled getRemoteAddr() value.
 */
public class ClientAddressCaptureFilter extends OncePerRequestFilter {

    public static final String RESOLVED_CLIENT_ADDRESS_ATTR = "tbResolvedClientAddress";
    public static final String RAW_SOCKET_ADDRESS_ATTR = "tbRawSocketAddress";
    public static final String CAPTURE_DONE_ATTR = "tbClientAddressCaptured";

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final ClientAddressResolver resolver;

    public ClientAddressCaptureFilter(ClientAddressResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpServletRequest raw = unwrap(request);
        InetAddress socketAddress = parseQuietly(raw.getRemoteAddr());
        List<String> forwardedFor = raw.getHeaders(X_FORWARDED_FOR) != null
                ? Collections.list(raw.getHeaders(X_FORWARDED_FOR))
                : List.of();
        boolean internallyForwarded = InternalForwardingToken.matches(raw.getHeader(InternalForwardingToken.HEADER));
        InetAddress resolved = resolver.resolve(socketAddress, forwardedFor, internallyForwarded);
        request.setAttribute(CAPTURE_DONE_ATTR, Boolean.TRUE);
        if (socketAddress != null) {
            request.setAttribute(RAW_SOCKET_ADDRESS_ATTR, socketAddress);
        }
        if (resolved != null) {
            request.setAttribute(RESOLVED_CLIENT_ADDRESS_ATTR, resolved);
        }
        chain.doFilter(request, response);
    }

    /**
     * The resolved client address for this request: {@code null} means the resolution failed
     * and sysadmin-scoped checks must fail closed. Falls back to parsing getRemoteAddr() only
     * when the capture filter never ran on this request (plain test setups).
     */
    public static InetAddress resolvedClientAddress(HttpServletRequest request) {
        return addressAttribute(request, RESOLVED_CLIENT_ADDRESS_ATTR);
    }

    public static InetAddress rawSocketAddress(HttpServletRequest request) {
        return addressAttribute(request, RAW_SOCKET_ADDRESS_ATTR);
    }

    private static InetAddress addressAttribute(HttpServletRequest request, String attribute) {
        if (request.getAttribute(attribute) instanceof InetAddress address) {
            return address;
        }
        if (request.getAttribute(CAPTURE_DONE_ATTR) != null) {
            return null;
        }
        return parseQuietly(request.getRemoteAddr());
    }

    private static HttpServletRequest unwrap(HttpServletRequest request) {
        ServletRequest current = request;
        while (current instanceof ServletRequestWrapper wrapper) {
            current = wrapper.getRequest();
        }
        return current instanceof HttpServletRequest http ? http : request;
    }

    private static InetAddress parseQuietly(String literal) {
        if (literal == null || literal.isBlank()) {
            return null;
        }
        try {
            return InetAddresses.forString(literal);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
