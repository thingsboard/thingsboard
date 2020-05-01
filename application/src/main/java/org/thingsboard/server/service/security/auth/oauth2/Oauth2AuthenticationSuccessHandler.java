/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.security.auth.oauth2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.oauth2.OAuth2Client;
import org.thingsboard.server.dao.oauth2.OAuth2Configuration;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenRepository;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(value = "oauth2AuthenticationSuccessHandler")
@ConditionalOnProperty(prefix = "security.oauth2", value = "enabled", havingValue = "true")
public class Oauth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenFactory tokenFactory;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuth2ClientMapperProvider oauth2ClientMapperProvider;
    private final OAuth2Configuration oauth2Configuration;

    @Autowired
    public Oauth2AuthenticationSuccessHandler(final JwtTokenFactory tokenFactory,
                                              final RefreshTokenRepository refreshTokenRepository,
                                              final OAuth2ClientMapperProvider oauth2ClientMapperProvider,
                                              final OAuth2Configuration oauth2Configuration) {
        this.tokenFactory = tokenFactory;
        this.refreshTokenRepository = refreshTokenRepository;
        this.oauth2ClientMapperProvider = oauth2ClientMapperProvider;
        this.oauth2Configuration = oauth2Configuration;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;

        OAuth2Client oauth2Client = oauth2Configuration.getClientByRegistrationId(token.getAuthorizedClientRegistrationId());
        OAuth2ClientMapper mapper = oauth2ClientMapperProvider.getOAuth2ClientMapperByType(oauth2Client.getMapperConfig().getType());
        SecurityUser securityUser = mapper.getOrCreateUserByClientPrincipal(token, oauth2Client.getMapperConfig());

        JwtToken accessToken = tokenFactory.createAccessJwtToken(securityUser);
        JwtToken refreshToken = refreshTokenRepository.requestRefreshToken(securityUser);

        getRedirectStrategy().sendRedirect(request, response, "/?accessToken=" + accessToken.getToken() + "&refreshToken=" + refreshToken.getToken());
    }
}