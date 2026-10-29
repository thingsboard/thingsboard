// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.service.security.exception.IpNotAllowedException;
import org.thingsboard.server.service.security.ip.SysAdminIpPolicy;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.io.IOException;

/**
 * Rejects requests of System Administrator-scoped principals (sysadmin JWTs, sysadmin API keys,
 * sysadmin 2FA pre-verification tokens) coming from outside the configured sysadmin IP allowlist.
 * Runs after the authentication filters, so it sees every authenticated principal regardless of
 * how the credentials were presented.
 */
@Component
@RequiredArgsConstructor
public class SysAdminIpFilter extends OncePerRequestFilter {

    private final ThingsboardErrorResponseHandler errorResponseHandler;
    private final SysAdminIpPolicy sysAdminIpPolicy;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        SecurityUser user = getCurrentUser();
        if (user != null) {
            try {
                sysAdminIpPolicy.enforce(user, request, "rest");
            } catch (IpNotAllowedException e) {
                errorResponseHandler.handle(e, response);
                return;
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

    private SecurityUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser user) {
            return user;
        }
        return null;
    }

}
