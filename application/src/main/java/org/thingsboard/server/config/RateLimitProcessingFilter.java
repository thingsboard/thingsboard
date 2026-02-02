/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.TenantProfileNotFoundException;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitProcessingFilter extends OncePerRequestFilter {

    private final ThingsboardErrorResponseHandler errorResponseHandler;
    private final RateLimitService rateLimitService;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        SecurityUser user = getCurrentUser();
        if (user != null && !user.isSystemAdmin()) {
            try {
                if (!rateLimitService.checkRateLimit(LimitedApi.REST_REQUESTS_PER_TENANT, user.getTenantId())) {
                    rateLimitExceeded(EntityType.TENANT, response);
                    return;
                }
            } catch (TenantProfileNotFoundException e) {
                log.debug("[{}] Failed to lookup tenant profile", user.getTenantId());
                errorResponseHandler.handle(new BadCredentialsException("Failed to lookup tenant profile"), response);
                return;
            }

            if (user.isCustomerUser()) {
                if (!rateLimitService.checkRateLimit(LimitedApi.REST_REQUESTS_PER_CUSTOMER, user.getTenantId(), user.getCustomerId())) {
                    rateLimitExceeded(EntityType.CUSTOMER, response);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    private void rateLimitExceeded(EntityType type, HttpServletResponse response) {
        errorResponseHandler.handle(new TbRateLimitsException(type), response);
    }

    protected SecurityUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        } else {
            return null;
        }
    }

}
