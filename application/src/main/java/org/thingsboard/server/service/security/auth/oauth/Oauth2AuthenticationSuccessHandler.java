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
package org.thingsboard.server.service.security.auth.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenRepository;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component(value="oauth2AuthenticationSuccessHandler")
@ConditionalOnProperty(prefix = "security.oauth2", value = "enabled", havingValue = "true")
public class Oauth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final ObjectMapper mapper;
    private final JwtTokenFactory tokenFactory;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SystemSecurityService systemSecurityService;
    private final UserService userService;

    @Autowired
    public Oauth2AuthenticationSuccessHandler(final ObjectMapper mapper,
                                              final JwtTokenFactory tokenFactory,
                                              final RefreshTokenRepository refreshTokenRepository,
                                              final UserService userService,
                                              final SystemSecurityService systemSecurityService) {
        this.mapper = mapper;
        this.tokenFactory = tokenFactory;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userService = userService;
        this.systemSecurityService = systemSecurityService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Object object = authentication.getPrincipal();

        System.out.println(object);

        // active user check

        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, "tenant@thingsboard.org");
        SecurityUser securityUser =  (SecurityUser) authenticateByUsernameAndPassword(principal,"tenant@thingsboard.org", "tenant").getPrincipal();

        JwtToken accessToken = tokenFactory.createAccessJwtToken(securityUser);
        JwtToken refreshToken = refreshTokenRepository.requestRefreshToken(securityUser);

        Map<String, String> tokenMap = new HashMap<String, String>();
        tokenMap.put("token", accessToken.getToken());
        tokenMap.put("refreshToken", refreshToken.getToken());

//        response.setStatus(HttpStatus.OK.value());
//        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//        mapper.writeValue(response.getWriter(), tokenMap);

        request.setAttribute("token", accessToken.getToken());
        response.addHeader("token", accessToken.getToken());

        getRedirectStrategy().sendRedirect(request, response, "http://localhost:4200/?accessToken=" + accessToken.getToken() + "&refreshToken=" + refreshToken.getToken());
    }

    private Authentication authenticateByUsernameAndPassword(UserPrincipal userPrincipal, String username, String password) {
        User user = userService.findUserByEmail(TenantId.SYS_TENANT_ID, username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        try {

            UserCredentials userCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, user.getId());
            if (userCredentials == null) {
                throw new UsernameNotFoundException("User credentials not found");
            }

            try {
                systemSecurityService.validateUserCredentials(user.getTenantId(), userCredentials, username, password);
            } catch (LockedException e) {
                throw e;
            }

            if (user.getAuthority() == null)
                throw new InsufficientAuthenticationException("User has no authority assigned");

            SecurityUser securityUser = new SecurityUser(user, userCredentials.isEnabled(), userPrincipal);
            return new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        } catch (Exception e) {
            throw e;
        }
    }
}