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
package org.thingsboard.server.service.security.auth.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.dao.oauth2.OAuth2Service;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.service.security.auth.rest.RestAuthenticationDetails;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.PREV_URI_COOKIE_NAME;

@Slf4j
@Component(value = "oauth2AuthenticationSuccessHandler")
@TbCoreComponent
public class Oauth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenFactory tokenFactory;
    private final OAuth2ClientMapperProvider oauth2ClientMapperProvider;
    private final OAuth2Service oAuth2Service;
    private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final SystemSecurityService systemSecurityService;

    @Autowired
    public Oauth2AuthenticationSuccessHandler(final JwtTokenFactory tokenFactory,
                                              final OAuth2ClientMapperProvider oauth2ClientMapperProvider,
                                              final OAuth2Service oAuth2Service,
                                              final OAuth2AuthorizedClientService oAuth2AuthorizedClientService,
                                              final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository,
                                              final SystemSecurityService systemSecurityService) {
        this.tokenFactory = tokenFactory;
        this.oauth2ClientMapperProvider = oauth2ClientMapperProvider;
        this.oAuth2Service = oAuth2Service;
        this.oAuth2AuthorizedClientService = oAuth2AuthorizedClientService;
        this.httpCookieOAuth2AuthorizationRequestRepository = httpCookieOAuth2AuthorizationRequestRepository;
        this.systemSecurityService = systemSecurityService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthorizationRequest authorizationRequest = httpCookieOAuth2AuthorizationRequestRepository.loadAuthorizationRequest(request);
        String callbackUrlScheme = authorizationRequest.getAttribute(TbOAuth2ParameterNames.CALLBACK_URL_SCHEME);
        String baseUrl;
        if (!StringUtils.isEmpty(callbackUrlScheme)) {
            baseUrl = callbackUrlScheme + ":";
        } else {
            baseUrl = this.systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, new CustomerId(EntityId.NULL_UUID), request);
            Optional<Cookie> prevUrlOpt = CookieUtils.getCookie(request, PREV_URI_COOKIE_NAME);
            if (prevUrlOpt.isPresent()) {
                baseUrl += prevUrlOpt.get().getValue();
                CookieUtils.deleteCookie(request, response, PREV_URI_COOKIE_NAME);
            }
        }
        try {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;

            OAuth2Registration registration = oAuth2Service.findRegistration(UUID.fromString(token.getAuthorizedClientRegistrationId()));
            OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(
                    token.getAuthorizedClientRegistrationId(),
                    token.getPrincipal().getName());
            OAuth2ClientMapper mapper = oauth2ClientMapperProvider.getOAuth2ClientMapperByType(registration.getMapperConfig().getType());
            SecurityUser securityUser = mapper.getOrCreateUserByClientPrincipal(request, token, oAuth2AuthorizedClient.getAccessToken().getTokenValue(),
                    registration);

            clearAuthenticationAttributes(request, response);

            getRedirectStrategy().sendRedirect(request, response, getRedirectUrl(baseUrl, securityUser));
            systemSecurityService.logLoginAction(securityUser, new RestAuthenticationDetails(request), ActionType.LOGIN, registration.getName(), null);
        } catch (Exception e) {
            log.debug("Error occurred during processing authentication success result. " +
                    "request [{}], response [{}], authentication [{}]", request, response, authentication, e);
            clearAuthenticationAttributes(request, response);
            String errorPrefix;
            if (!StringUtils.isEmpty(callbackUrlScheme)) {
                errorPrefix = "/?error=";
            } else {
                errorPrefix = "/login?loginError=";
            }
            getRedirectStrategy().sendRedirect(request, response, baseUrl + errorPrefix +
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8.toString()));
        }
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }

    String getRedirectUrl(String baseUrl, SecurityUser securityUser) {
        JwtPair tokenPair = tokenFactory.createTokenPair(securityUser);
        if (baseUrl.indexOf("?") > 0) {
            baseUrl += "&";
        } else {
            baseUrl += "/?";
        }
        return baseUrl + "accessToken=" + tokenPair.getToken() + "&refreshToken=" + tokenPair.getRefreshToken();
    }
}
