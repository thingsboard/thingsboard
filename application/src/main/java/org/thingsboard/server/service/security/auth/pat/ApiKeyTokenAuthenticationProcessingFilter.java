// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.pat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.thingsboard.server.service.security.auth.extractor.TokenExtractor;
import org.thingsboard.server.service.security.model.token.ApiKeyAuthRequest;

import java.io.IOException;

import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.API_KEY_HEADER_PREFIX;
import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.AUTHORIZATION_HEADER;
import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.AUTHORIZATION_HEADER_V2;

public class ApiKeyTokenAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {

    private final AuthenticationFailureHandler failureHandler;
    private final TokenExtractor tokenExtractor;

    @Autowired
    public ApiKeyTokenAuthenticationProcessingFilter(AuthenticationFailureHandler failureHandler,
                                                     @Qualifier("apiKeyHeaderTokenExtractor") TokenExtractor tokenExtractor, RequestMatcher matcher) {
        super(matcher);
        this.failureHandler = failureHandler;
        this.tokenExtractor = tokenExtractor;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String apiKeyValue = tokenExtractor.extract(request);
        ApiKeyAuthRequest apiKeyAuthRequest = new ApiKeyAuthRequest(apiKeyValue);
        return getAuthenticationManager().authenticate(new ApiKeyAuthenticationToken(apiKeyAuthRequest));
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        if (!super.requiresAuthentication(request, response)) {
            return false;
        }
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null) {
            header = request.getHeader(AUTHORIZATION_HEADER_V2);
        }
        return header != null && header.startsWith(API_KEY_HEADER_PREFIX);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        failureHandler.onAuthenticationFailure(request, response, failed);
    }

}
