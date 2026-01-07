/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DaoSqlTest
public class Oauth2AuthenticationSuccessHandlerTest extends AbstractControllerTest {

    @Autowired
    private Oauth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    @Mock
    private JwtTokenFactory jwtTokenFactory;

    private SecurityUser securityUser;

    @Before
    public void before() {
        UserId userId = new UserId(UUID.randomUUID());
        securityUser = new SecurityUser(userId);
        when(jwtTokenFactory.createTokenPair(eq(securityUser))).thenReturn(new JwtPair("testAccessToken", "testRefreshToken"));
    }

    @Test
    public void testGetRedirectUrl() {
        JwtPair jwtPair = jwtTokenFactory.createTokenPair(securityUser);

        String urlWithoutParams = "http://localhost:8080/dashboardGroups/3fa13530-6597-11ed-bd76-8bd591f0ec3e";
        String urlWithParams = "http://localhost:8080/dashboardGroups/3fa13530-6597-11ed-bd76-8bd591f0ec3e?state=someState&page=1";

        String redirectUrl = oauth2AuthenticationSuccessHandler.getRedirectUrl(urlWithoutParams, jwtPair);
        String expectedUrl = urlWithoutParams + "/?accessToken=" + jwtPair.getToken() + "&refreshToken=" + jwtPair.getRefreshToken();
        assertEquals(expectedUrl, redirectUrl);

        redirectUrl = oauth2AuthenticationSuccessHandler.getRedirectUrl(urlWithParams, jwtPair);
        expectedUrl = urlWithParams + "&accessToken=" + jwtPair.getToken() + "&refreshToken=" + jwtPair.getRefreshToken();
        assertEquals(expectedUrl, redirectUrl);
    }
}