/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.security.auth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserAuthDetails;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.event.UserCredentialsInvalidationEvent;
import org.thingsboard.server.common.data.security.event.UserSessionInvalidationEvent;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.security.auth.jwt.JwtAuthenticationProvider;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenAuthenticationProvider;
import org.thingsboard.server.service.security.exception.JwtExpiredTokenException;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;
import org.thingsboard.server.service.user.cache.UserAuthDetailsCache;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TokenOutdatingTest.class, loader = SpringBootContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ComponentScan({"org.thingsboard.server"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DaoSqlTest
@TestPropertySource(properties = {
        "security.jwt.tokenIssuer=test.io",
        "security.jwt.tokenSigningKey=secret",
        "security.jwt.tokenExpirationTime=600",
        "security.jwt.refreshTokenExpTime=15",
        // explicitly set the wrong value to check that it is NOT used.
        "cache.specs.userSessionsInvalidation.timeToLiveInMinutes=2"
})
public class TokenOutdatingTest {
    private JwtAuthenticationProvider accessTokenAuthenticationProvider;
    private RefreshTokenAuthenticationProvider refreshTokenAuthenticationProvider;

    @Autowired
    private TokenOutdatingService tokenOutdatingService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private JwtTokenFactory tokenFactory;
    private SecurityUser securityUser;

    @Before
    public void setUp() {
        UserId userId = new UserId(UUID.randomUUID());
        securityUser = createMockSecurityUser(userId);

        UserAuthDetailsCache userAuthDetailsCache = mock(UserAuthDetailsCache.class);

        User user = new User();
        user.setId(userId);
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setEmail("email");
        when(userAuthDetailsCache.getUserAuthDetails(any(), eq(userId))).thenReturn(new UserAuthDetails(user, true));

        accessTokenAuthenticationProvider = new JwtAuthenticationProvider(tokenFactory, tokenOutdatingService);
        refreshTokenAuthenticationProvider = new RefreshTokenAuthenticationProvider(tokenFactory, userAuthDetailsCache, mock(CustomerService.class), tokenOutdatingService);
    }

    @Test
    public void testOutdateOldUserTokens() throws Exception {
        JwtToken jwtToken = tokenFactory.createAccessJwtToken(securityUser);

        // Token outdatage time is rounded to 1 sec. Need to wait before outdating so that outdatage time is strictly after token issue time
        SECONDS.sleep(1);
        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));
        assertTrue(tokenOutdatingService.isOutdated(jwtToken.token(), securityUser.getId()));

        SECONDS.sleep(1);

        JwtToken newJwtToken = tokenFactory.createAccessJwtToken(securityUser);
        assertFalse(tokenOutdatingService.isOutdated(newJwtToken.token(), securityUser.getId()));
    }

    @Test
    public void testAuthenticateWithOutdatedAccessToken() throws InterruptedException {
        RawAccessJwtToken accessJwtToken = getRawJwtToken(tokenFactory.createAccessJwtToken(securityUser));

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(accessJwtToken));
        });

        SECONDS.sleep(1);
        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));

        assertThrows(JwtExpiredTokenException.class, () -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(accessJwtToken));
        });
    }

    @Test
    public void testAuthenticateWithOutdatedRefreshToken() throws InterruptedException {
        RawAccessJwtToken refreshJwtToken = getRawJwtToken(tokenFactory.createRefreshToken(securityUser));

        assertDoesNotThrow(() -> {
            refreshTokenAuthenticationProvider.authenticate(new RefreshAuthenticationToken(refreshJwtToken));
        });

        SECONDS.sleep(1);
        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));

        assertThrows(CredentialsExpiredException.class, () -> {
            refreshTokenAuthenticationProvider.authenticate(new RefreshAuthenticationToken(refreshJwtToken));
        });
    }

    // This test takes too long to run and is basically testing the cache logic
//    @Test
//    public void testTokensOutdatageTimeRemovalFromCache() throws Exception {
//        JwtToken jwtToken = tokenFactory.createAccessJwtToken(securityUser);
//
//        SECONDS.sleep(1);
//        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));
//
//        SECONDS.sleep(1);
//
//        assertTrue(tokenOutdatingService.isOutdated(jwtToken, securityUser.getId()));
//
//        SECONDS.sleep(30); // refreshTokenExpTime/2
//
//        assertTrue(tokenOutdatingService.isOutdated(jwtToken, securityUser.getId()));
//
//        SECONDS.sleep(30 + 1); // refreshTokenExpTime/2 + 1
//
//        assertFalse(tokenOutdatingService.isOutdated(jwtToken, securityUser.getId()));
//    }

    @Test
    public void testOnlyOneTokenExpired() throws InterruptedException {
        JwtToken jwtToken = tokenFactory.createAccessJwtToken(securityUser);

        SecurityUser anotherSecurityUser = new SecurityUser(securityUser, securityUser.isEnabled(), securityUser.getUserPrincipal());
        JwtToken anotherJwtToken = tokenFactory.createAccessJwtToken(anotherSecurityUser);

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(jwtToken)));
        });

        SECONDS.sleep(1);

        eventPublisher.publishEvent(new UserSessionInvalidationEvent(securityUser.getSessionId()));

        assertThrows(JwtExpiredTokenException.class, () -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(jwtToken)));
        });

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(anotherJwtToken)));
        });
    }

    @Test
    public void testResetAllSessions() throws InterruptedException {
        JwtToken jwtToken = tokenFactory.createAccessJwtToken(securityUser);

        SecurityUser anotherSecurityUser = new SecurityUser(securityUser, securityUser.isEnabled(), securityUser.getUserPrincipal());
        JwtToken anotherJwtToken = tokenFactory.createAccessJwtToken(anotherSecurityUser);

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(jwtToken)));
        });

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(anotherJwtToken)));
        });

        SECONDS.sleep(1);

        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));

        assertThrows(JwtExpiredTokenException.class, () -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(jwtToken)));
        });

        assertThrows(JwtExpiredTokenException.class, () -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(anotherJwtToken)));
        });
    }


    private RawAccessJwtToken getRawJwtToken(JwtToken token) {
        return new RawAccessJwtToken(token.token());
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
