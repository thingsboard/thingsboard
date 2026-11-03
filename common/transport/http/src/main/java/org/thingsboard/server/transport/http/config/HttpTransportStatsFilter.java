// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.http.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thingsboard.server.transport.http.HttpTlsTermination;
import org.thingsboard.server.transport.http.HttpTransportContext;

import java.io.IOException;

@RequiredArgsConstructor
public class HttpTransportStatsFilter extends OncePerRequestFilter {

    private final HttpTransportContext transportContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        transportContext.onRequest(HttpTlsTermination.resolve(request));
        chain.doFilter(request, response);
    }

}
