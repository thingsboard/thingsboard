// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.subscription.SubscriptionService;

import java.io.IOException;

/**
 * Advertises non-production mode on API responses. For an API-first deployment this is the only visible
 * non-production signal, since the watermark is a UI affordance. It is a signal, not an enforcement
 * mechanism - any reverse proxy can strip it, so no control may depend on it reaching the client.
 */
@Component
@RequiredArgsConstructor
public class NonProductionHeaderFilter extends OncePerRequestFilter {

    static final String NON_PRODUCTION_HEADER = "X-ThingsBoard-Non-Production";

    private final SubscriptionService subscriptionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (subscriptionService.isDevelopment(TenantId.SYS_TENANT_ID)) {
            response.setHeader(NON_PRODUCTION_HEADER, "true");
        }
        chain.doFilter(request, response);
    }
}
