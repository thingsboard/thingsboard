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

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;

import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DaoSqlTest
public class Oauth2AuthenticationSuccessHandlerTest extends AbstractControllerTest {

    @Autowired
    protected Oauth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    private SecurityUser securityUser;

    @Before
    public void before() {
        UserId userId = new UserId(UUID.randomUUID());
        securityUser = createMockSecurityUser(userId);

        UserService userService = mock(UserService.class);
        User user = new User();
        user.setId(userId);
        user.setEmail("email");
        user.setAuthority(Authority.TENANT_ADMIN);
        when(userService.findUserById(any(), eq(userId))).thenReturn(user);

        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setEnabled(true);
        when(userService.findUserCredentialsByUserId(any(), eq(userId))).thenReturn(userCredentials);
    }

    @Test
    public void testGetRedirectUrl() {
        String urlWithoutParams = "http://localhost:8080/dashboardGroups/3fa13530-6597-11ed-bd76-8bd591f0ec3e";
        String urlWithParams = "http://localhost:8080/dashboardGroups/3fa13530-6597-11ed-bd76-8bd591f0ec3e?state=someState&page=1";

        String redirectUrl = oauth2AuthenticationSuccessHandler.getRedirectUrl(urlWithoutParams, securityUser);
        assertTrue(redirectUrl.contains("/?accessToken="));

        redirectUrl = oauth2AuthenticationSuccessHandler.getRedirectUrl(urlWithParams, securityUser);
        assertTrue(redirectUrl.contains("&accessToken="));
    }

    private SecurityUser createMockSecurityUser(UserId userId) {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setEmail("email");
        securityUser.setUserPrincipal(new UserPrincipal(UserPrincipal.Type.USER_NAME, securityUser.getEmail()));
        securityUser.setAuthority(Authority.CUSTOMER_USER);
        securityUser.setId(userId);
        securityUser.setSessionId(UUID.randomUUID().toString());
        return securityUser;
    }
}