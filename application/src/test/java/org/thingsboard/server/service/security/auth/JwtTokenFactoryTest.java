/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import io.jsonwebtoken.Claims;
import org.junit.BeforeClass;
import org.junit.Test;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

public class JwtTokenFactoryTest {

    private static JwtTokenFactory tokenFactory;
    private static JwtSettings jwtSettings;

    @BeforeClass
    public static void beforeAll() {
        jwtSettings = new JwtSettings();
        jwtSettings.setTokenIssuer("tb");
        jwtSettings.setTokenSigningKey("abewafaf");
        jwtSettings.setTokenExpirationTime((int) TimeUnit.HOURS.toSeconds(2));
        jwtSettings.setRefreshTokenExpTime((int) TimeUnit.DAYS.toSeconds(7));

        JwtSettingsService jwtSettingsService = mock(JwtSettingsService.class);
        willReturn(jwtSettings).given(jwtSettingsService).getJwtSettings();

        tokenFactory = new JwtTokenFactory(jwtSettingsService);
    }

    @Test
    public void testCreateAndParseAccessJwtToken() {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(new UserId(UUID.randomUUID()));
        securityUser.setEmail("tenant@thingsboard.org");
        securityUser.setAuthority(Authority.TENANT_ADMIN);
        securityUser.setTenantId(new TenantId(UUID.randomUUID()));
        securityUser.setEnabled(true);
        securityUser.setFirstName("A");
        securityUser.setLastName("B");
        securityUser.setUserPrincipal(new UserPrincipal(UserPrincipal.Type.USER_NAME, securityUser.getEmail()));
        securityUser.setCustomerId(new CustomerId(UUID.randomUUID()));

        testCreateAndParseAccessJwtToken(securityUser);

        securityUser = new SecurityUser(securityUser, true, new UserPrincipal(UserPrincipal.Type.PUBLIC_ID, securityUser.getEmail()));
        securityUser.setFirstName(null);
        securityUser.setLastName(null);
        securityUser.setCustomerId(null);

        testCreateAndParseAccessJwtToken(securityUser);
    }

    public void testCreateAndParseAccessJwtToken(SecurityUser securityUser) {
        AccessJwtToken accessToken = tokenFactory.createAccessJwtToken(securityUser);
        checkExpirationTime(accessToken, jwtSettings.getTokenExpirationTime());

        SecurityUser parsedSecurityUser = tokenFactory.parseAccessJwtToken(new RawAccessJwtToken(accessToken.getToken()));
        assertThat(parsedSecurityUser.getId()).isEqualTo(securityUser.getId());
        assertThat(parsedSecurityUser.getEmail()).isEqualTo(securityUser.getEmail());
        assertThat(parsedSecurityUser.getUserPrincipal()).matches(userPrincipal -> {
            return userPrincipal.getType().equals(securityUser.getUserPrincipal().getType())
                    && userPrincipal.getValue().equals(securityUser.getUserPrincipal().getValue());
        });
        assertThat(parsedSecurityUser.getAuthorities()).isEqualTo(securityUser.getAuthorities());
        assertThat(parsedSecurityUser.isEnabled()).isEqualTo(securityUser.isEnabled());
        assertThat(parsedSecurityUser.getTenantId()).isEqualTo(securityUser.getTenantId());
        assertThat(parsedSecurityUser.getCustomerId()).isEqualTo(securityUser.getCustomerId());
        assertThat(parsedSecurityUser.getFirstName()).isEqualTo(securityUser.getFirstName());
        assertThat(parsedSecurityUser.getLastName()).isEqualTo(securityUser.getLastName());
    }

    @Test
    public void testCreateAndParseRefreshJwtToken() {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(new UserId(UUID.randomUUID()));
        securityUser.setEmail("tenant@thingsboard.org");
        securityUser.setAuthority(Authority.TENANT_ADMIN);
        securityUser.setUserPrincipal(new UserPrincipal(UserPrincipal.Type.USER_NAME, securityUser.getEmail()));
        securityUser.setEnabled(true);
        securityUser.setTenantId(new TenantId(UUID.randomUUID()));
        securityUser.setCustomerId(new CustomerId(UUID.randomUUID()));

        JwtToken refreshToken = tokenFactory.createRefreshToken(securityUser);
        checkExpirationTime(refreshToken, jwtSettings.getRefreshTokenExpTime());

        SecurityUser parsedSecurityUser = tokenFactory.parseRefreshToken(new RawAccessJwtToken(refreshToken.getToken()));
        assertThat(parsedSecurityUser.getId()).isEqualTo(securityUser.getId());
        assertThat(parsedSecurityUser.getUserPrincipal()).matches(userPrincipal -> {
            return userPrincipal.getType().equals(securityUser.getUserPrincipal().getType())
                    && userPrincipal.getValue().equals(securityUser.getUserPrincipal().getValue());
        });
        assertThat(parsedSecurityUser.getAuthority()).isNull();
    }

    @Test
    public void testCreateAndParsePreVerificationJwtToken() {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(new UserId(UUID.randomUUID()));
        securityUser.setEmail("tenant@thingsboard.org");
        securityUser.setAuthority(Authority.TENANT_ADMIN);
        securityUser.setUserPrincipal(new UserPrincipal(UserPrincipal.Type.USER_NAME, securityUser.getEmail()));
        securityUser.setEnabled(true);
        securityUser.setTenantId(new TenantId(UUID.randomUUID()));
        securityUser.setCustomerId(new CustomerId(UUID.randomUUID()));

        int tokenLifetime = (int) TimeUnit.MINUTES.toSeconds(30);
        JwtToken preVerificationToken = tokenFactory.createPreVerificationToken(securityUser, tokenLifetime);
        checkExpirationTime(preVerificationToken, tokenLifetime);

        SecurityUser parsedSecurityUser = tokenFactory.parseAccessJwtToken(new RawAccessJwtToken(preVerificationToken.getToken()));
        assertThat(parsedSecurityUser.getId()).isEqualTo(securityUser.getId());
        assertThat(parsedSecurityUser.getAuthority()).isEqualTo(Authority.PRE_VERIFICATION_TOKEN);
        assertThat(parsedSecurityUser.getTenantId()).isEqualTo(securityUser.getTenantId());
        assertThat(parsedSecurityUser.getCustomerId()).isEqualTo(securityUser.getCustomerId());
        assertThat(parsedSecurityUser.getUserPrincipal()).matches(userPrincipal -> {
            return userPrincipal.getType() == UserPrincipal.Type.USER_NAME
                    && userPrincipal.getValue().equals(securityUser.getUserPrincipal().getValue());
        });
    }

    private void checkExpirationTime(JwtToken jwtToken, int tokenLifetime) {
        Claims claims = tokenFactory.parseTokenClaims(jwtToken).getBody();
        assertThat(claims.getExpiration()).matches(actualExpirationTime -> {
            Calendar expirationTime = Calendar.getInstance();
            expirationTime.setTime(new Date());
            expirationTime.add(Calendar.SECOND, tokenLifetime);
            if (actualExpirationTime.equals(expirationTime.getTime())) {
                return true;
            } else if (actualExpirationTime.before(expirationTime.getTime())) {
                int gap = 2;
                expirationTime.add(Calendar.SECOND, -gap);
                return actualExpirationTime.after(expirationTime.getTime());
            } else {
                return false;
            }
        });
    }

}
