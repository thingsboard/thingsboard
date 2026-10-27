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

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

/**
 * Captures the TCP peer address and the raw X-Forwarded-For chain from the container's
 * original request — unwrapping any request wrappers, so the values are correct even after
 * ForwardedHeaderFilter has rewritten getRemoteAddr() — and stores the resolved client
 * address as request attributes for security checks downstream.
 */
public class ClientAddressCaptureFilter extends OncePerRequestFilter {

    public static final String RESOLVED_CLIENT_ADDRESS_ATTR = "tbResolvedClientAddress";
    public static final String RAW_SOCKET_ADDRESS_ATTR = "tbRawSocketAddress";

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
        InetAddress resolved = resolver.resolve(socketAddress, forwardedFor);
        if (socketAddress != null) {
            request.setAttribute(RAW_SOCKET_ADDRESS_ATTR, socketAddress);
        }
        if (resolved != null) {
            request.setAttribute(RESOLVED_CLIENT_ADDRESS_ATTR, resolved);
        }
        chain.doFilter(request, response);
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
