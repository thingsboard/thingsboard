/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class RateLimitProcessingFilter extends GenericFilterBean {

    @Autowired
    private ThingsboardErrorResponseHandler errorResponseHandler;

    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    private final ConcurrentMap<TenantId, TbRateLimits> perTenantLimits = new ConcurrentHashMap<>();
    private final ConcurrentMap<CustomerId, TbRateLimits> perCustomerLimits = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        SecurityUser user = getCurrentUser();
        if (user != null && !user.isSystemAdmin()) {
            var profile = tenantProfileCache.get(user.getTenantId());
            if (profile == null) {
                log.debug("[{}] Failed to lookup tenant profile", user.getTenantId());
                errorResponseHandler.handle(new BadCredentialsException("Failed to lookup tenant profile"), (HttpServletResponse) response);
                return;
            }
            var profileConfiguration = profile.getDefaultProfileConfiguration();
            if (!checkRateLimits(user.getTenantId(), profileConfiguration.getTenantServerRestLimitsConfiguration(), perTenantLimits, response)) {
                return;
            }
            if (user.isCustomerUser()) {
                if (!checkRateLimits(user.getCustomerId(), profileConfiguration.getCustomerServerRestLimitsConfiguration(), perCustomerLimits, response)) {
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private <I extends EntityId> boolean checkRateLimits(I ownerId, String rateLimitConfig, Map<I, TbRateLimits> rateLimitsMap, ServletResponse response) {
        if (StringUtils.isNotEmpty(rateLimitConfig)) {
            TbRateLimits rateLimits = rateLimitsMap.get(ownerId);
            if (rateLimits == null || !rateLimits.getConfiguration().equals(rateLimitConfig)) {
                rateLimits = new TbRateLimits(rateLimitConfig);
                rateLimitsMap.put(ownerId, rateLimits);
            }

            if (!rateLimits.tryConsume()) {
                errorResponseHandler.handle(new TbRateLimitsException(ownerId.getEntityType()), (HttpServletResponse) response);
                return false;
            }
        } else {
            rateLimitsMap.remove(ownerId);
        }

        return true;
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
