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
package org.thingsboard.server.service.security.auth.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenRepository;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;
import org.thingsboard.server.service.security.auth.mfa.config.account.TwoFactorAuthAccountConfig;
import org.thingsboard.server.service.security.model.JwtTokenPair;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;

@Component(value = "defaultAuthenticationSuccessHandler")
@RequiredArgsConstructor
@Slf4j
public class RestAwareAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final ObjectMapper mapper;
    private final JwtTokenFactory tokenFactory;
    private final TwoFactorAuthService twoFactorAuthService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        JwtTokenPair tokenPair;

        // fixme: check if this handler is not called when token is refreshed
        Optional<TwoFactorAuthAccountConfig> twoFaAccountConfig = twoFactorAuthService.getTwoFaAccountConfig(securityUser.getTenantId(), securityUser.getId());
        if (twoFaAccountConfig.isPresent()) {
            try {
                twoFactorAuthService.processByTwoFaProvider(securityUser.getTenantId(), twoFaAccountConfig.get().getProviderType(),
                        (provider, providerConfig) -> {
                            provider.prepareVerificationCode(securityUser, providerConfig, twoFaAccountConfig.get());
                        });
                tokenPair = new JwtTokenPair();
                tokenPair.setToken(tokenFactory.createPreVerificationToken(securityUser).getToken());
                tokenPair.setScope(Authority.PRE_VERIFICATION_TOKEN);
            } catch (Exception e) {
                log.error("Failed to process 2FA for user {}. Falling back to plain auth", securityUser.getId(), e);
                tokenPair = tokenFactory.createTokenPair(securityUser);
            }
        } else {
            tokenPair = tokenFactory.createTokenPair(securityUser);
        }

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        mapper.writeValue(response.getWriter(), tokenPair);

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
