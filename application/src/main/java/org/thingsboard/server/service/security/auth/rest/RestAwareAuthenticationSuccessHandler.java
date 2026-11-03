// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.rest;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.service.security.auth.MfaAuthenticationToken;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component(value = "defaultAuthenticationSuccessHandler")
@RequiredArgsConstructor
public class RestAwareAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtTokenFactory tokenFactory;
    private final TwoFaConfigManager twoFaConfigManager;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        JwtPair tokenPair = new JwtPair();

        if (authentication instanceof MfaAuthenticationToken) {
            int preVerificationTokenLifetime = twoFaConfigManager.getPlatformTwoFaSettings(securityUser.getTenantId(), true)
                    .flatMap(settings -> Optional.ofNullable(settings.getTotalAllowedTimeForVerification())
                            .filter(time -> time > 0))
                    .orElse((int) TimeUnit.MINUTES.toSeconds(30));
            tokenPair.setToken(tokenFactory.createPreVerificationToken(securityUser, preVerificationTokenLifetime).getToken());
            tokenPair.setRefreshToken(null);
            tokenPair.setScope(Authority.PRE_VERIFICATION_TOKEN);
        } else {
            tokenPair = tokenFactory.createTokenPair(securityUser);
        }

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        JacksonUtil.writeValue(response.getWriter(), tokenPair);

        clearAuthenticationAttributes(request);
    }

    /**
     * Removes temporary authentication-related data which may have been stored
     * in the session during the authentication process..
     *
     */
    protected final void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return;
        }

        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }
}
