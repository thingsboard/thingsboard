// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thingsboard.server.common.data.setup.SystemSetupState;
import org.thingsboard.server.exception.SystemSetupIncompleteException;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.service.setup.SystemSetupService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SystemSetupFilter extends OncePerRequestFilter {
    // Same package as ThingsboardSecurityConfiguration -> reference its entry point constants without an import.

    private static final RequestMatcher SETUP_PROTECTED_REQUEST =
            new OrRequestMatcher(ThingsboardSecurityConfiguration.SETUP_PROTECTED_ENTRY_POINTS);
    private static final RequestMatcher SETUP_ALLOWED_REQUEST =
            new OrRequestMatcher(ThingsboardSecurityConfiguration.SETUP_ALLOWED_ENTRY_POINTS);

    private final SystemSetupService systemSetupService;
    private final ThingsboardErrorResponseHandler errorResponseHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (!isSetupProtected(request)) {
            chain.doFilter(request, response);
            return;
        }
        SystemSetupState state = systemSetupService.getState();
        if (state == SystemSetupState.READY) {
            chain.doFilter(request, response);
            return;
        }
        errorResponseHandler.handle(new SystemSetupIncompleteException(state), response);
    }

    private boolean isSetupProtected(HttpServletRequest request) {
        if (CorsUtils.isPreFlightRequest(request)) {
            // A preflight carries no credentials and performs nothing; the request that follows is checked
            // here on its own merits. Locking one would be worse than pointless: an error response carries no
            // Access-Control-Allow-Origin header, so a browser on another origin would see a network-level
            // failure rather than the lock, and the front end signs the user out on it. A CorsFilter answers
            // preflights ahead of this filter in every SecurityFilterChain, but this bean is also registered
            // directly with the servlet container - how the device API reaches it - which promises nothing.
            return false;
        }
        // The patterns are relative to the context path, so they match the same way under a non-root
        // server.servlet.context-path.
        return SETUP_PROTECTED_REQUEST.matches(request) && !SETUP_ALLOWED_REQUEST.matches(request);
    }

}
