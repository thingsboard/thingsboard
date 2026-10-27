// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthExceptionHandler extends OncePerRequestFilter {

    private final ThingsboardErrorResponseHandler errorResponseHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        try {
            filterChain.doFilter(request, response);
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            errorResponseHandler.handle(e, response);
        }
    }

}
