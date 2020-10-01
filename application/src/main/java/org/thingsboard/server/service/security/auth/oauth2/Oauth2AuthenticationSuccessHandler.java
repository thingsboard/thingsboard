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
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationInfo;
import org.thingsboard.server.dao.oauth2.OAuth2Service;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenRepository;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.utils.MiscUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component(value = "oauth2AuthenticationSuccessHandler")
public class Oauth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenFactory tokenFactory;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuth2ClientMapperProvider oauth2ClientMapperProvider;
    private final OAuth2Service oAuth2Service;
    private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @Autowired
    public Oauth2AuthenticationSuccessHandler(final JwtTokenFactory tokenFactory,
                                              final RefreshTokenRepository refreshTokenRepository,
                                              final OAuth2ClientMapperProvider oauth2ClientMapperProvider,
                                              final OAuth2Service oAuth2Service,
                                              final OAuth2AuthorizedClientService oAuth2AuthorizedClientService) {
        this.tokenFactory = tokenFactory;
        this.refreshTokenRepository = refreshTokenRepository;
        this.oauth2ClientMapperProvider = oauth2ClientMapperProvider;
        this.oAuth2Service = oAuth2Service;
        this.oAuth2AuthorizedClientService = oAuth2AuthorizedClientService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        String baseUrl = MiscUtils.constructBaseUrl(request);
        try {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;

            OAuth2ClientRegistrationInfo clientRegistration = oAuth2Service.findClientRegistrationInfo(UUID.fromString(token.getAuthorizedClientRegistrationId()));
            OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(
                    token.getAuthorizedClientRegistrationId(),
                    token.getPrincipal().getName());
            OAuth2ClientMapper mapper = oauth2ClientMapperProvider.getOAuth2ClientMapperByType(clientRegistration.getMapperConfig().getType());
            SecurityUser securityUser = mapper.getOrCreateUserByClientPrincipal(token, oAuth2AuthorizedClient.getAccessToken().getTokenValue(),
                    clientRegistration.getMapperConfig());

            JwtToken accessToken = tokenFactory.createAccessJwtToken(securityUser);
            JwtToken refreshToken = refreshTokenRepository.requestRefreshToken(securityUser);

            getRedirectStrategy().sendRedirect(request, response, baseUrl + "/?accessToken=" + accessToken.getToken() + "&refreshToken=" + refreshToken.getToken());
        } catch (Exception e) {
            getRedirectStrategy().sendRedirect(request, response, baseUrl + "/login?loginError=" +
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8.toString()));
        }
    }
}