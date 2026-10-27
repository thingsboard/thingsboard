// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.security.auth.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.config.SysAdminAllowlistProperties;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;
import org.thingsboard.server.service.security.ip.SysAdminIpPolicy;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Oauth2AuthenticationSuccessHandlerAllowlistTest {

    private static final String BLOCKED_IP = "10.0.0.9";
    private static final String ALLOWED_IP = "203.0.113.5";

    @Mock
    private JwtTokenFactory tokenFactory;
    @Mock
    private OAuth2ClientMapperProvider mapperProvider;
    @Mock
    private OAuth2ClientService oAuth2ClientService;
    @Mock
    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
    @Mock
    private HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;
    @Mock
    private SystemSecurityService systemSecurityService;
    @Mock
    private OAuth2ClientMapper mapper;
    @Mock
    private OAuth2AuthenticationToken authentication;

    private Oauth2AuthenticationSuccessHandler handler;
    private SecurityUser securityUser;

    @BeforeEach
    void setUp() {
        SysAdminAllowlistProperties props = new SysAdminAllowlistProperties(List.of(ALLOWED_IP), List.of());
        props.init();
        handler = new Oauth2AuthenticationSuccessHandler(tokenFactory, mapperProvider, oAuth2ClientService,
                oAuth2AuthorizedClientService, cookieRepository, systemSecurityService, new SysAdminIpPolicy(props));

        String registrationId = UUID.randomUUID().toString();
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn(registrationId);
        org.springframework.security.oauth2.core.user.OAuth2User principal = mock(org.springframework.security.oauth2.core.user.OAuth2User.class);
        when(principal.getName()).thenReturn("sysadmin@example.com");
        org.mockito.Mockito.doReturn(principal).when(authentication).getPrincipal();

        OAuth2Client oauth2Client = new OAuth2Client();
        oauth2Client.setMapperConfig(OAuth2MapperConfig.builder().type(MapperType.BASIC).build());
        when(oAuth2ClientService.findOAuth2ClientById(any(), any())).thenReturn(oauth2Client);

        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getTokenValue()).thenReturn("access-token");
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(oAuth2AuthorizedClientService.loadAuthorizedClient(anyString(), anyString())).thenReturn(authorizedClient);

        when(mapperProvider.getOAuth2ClientMapperByType(any())).thenReturn(mapper);

        securityUser = new SecurityUser(new UserId(UUID.randomUUID()));
        securityUser.setAuthority(Authority.SYS_ADMIN);
        securityUser.setTenantId(TenantId.SYS_TENANT_ID);
        when(mapper.getOrCreateUserByClientPrincipal(any(), any(), anyString(), any())).thenReturn(securityUser);

        when(systemSecurityService.getBaseUrl(any(), any(), any(), any())).thenReturn("http://localhost:8080");
        when(tokenFactory.createTokenPair(any())).thenReturn(new JwtPair("jwt-token", "jwt-refresh"));
    }

    @Test
    void sysAdminMappedUserFromBlockedIpGetsNoTokens() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(BLOCKED_IP);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(tokenFactory, never()).createTokenPair(any());
        assertThat(response.getRedirectedUrl()).contains("loginError=");
        assertThat(response.getRedirectedUrl()).doesNotContain("accessToken=");
        // generic failure: the redirect must not reveal that the account is sysadmin-protected
        assertThat(response.getRedirectedUrl()).doesNotContainIgnoringCase("network");
    }

    @Test
    void sysAdminMappedUserFromAllowedIpGetsTokens() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ALLOWED_IP);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(tokenFactory).createTokenPair(securityUser);
        assertThat(response.getRedirectedUrl()).contains("accessToken=jwt-token");
    }

    @Test
    void tenantUserFromBlockedIpGetsTokens() throws Exception {
        securityUser.setAuthority(Authority.TENANT_ADMIN);
        securityUser.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(BLOCKED_IP);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(tokenFactory).createTokenPair(securityUser);
        assertThat(response.getRedirectedUrl()).contains("accessToken=jwt-token");
    }

}
