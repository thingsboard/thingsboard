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
package org.thingsboard.server.service.security.model.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ClaimsBuilder;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.thingsboard.server.service.security.exception.JwtExpiredTokenException;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenFactory {

    public static int KEY_LENGTH = Jwts.SIG.HS512.getKeyBitLength();

    private static final String SCOPES = "scopes";
    private static final String USER_ID = "userId";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String ENABLED = "enabled";
    private static final String IS_PUBLIC = "isPublic";
    private static final String TENANT_ID = "tenantId";
    private static final String CUSTOMER_ID = "customerId";
    private static final String SESSION_ID = "sessionId";

    @Lazy
    private final JwtSettingsService jwtSettingsService;

    private volatile JwtParser jwtParser;
    private volatile SecretKey secretKey;

    /**
     * Factory method for issuing new JWT Tokens.
     */
    public AccessJwtToken createAccessJwtToken(SecurityUser securityUser) {
        if (securityUser.getAuthority() == null) {
            throw new IllegalArgumentException("User doesn't have any privileges");
        }

        UserPrincipal principal = securityUser.getUserPrincipal();

        JwtBuilder jwtBuilder = setUpToken(securityUser, securityUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList()), jwtSettingsService.getJwtSettings().getTokenExpirationTime());
        jwtBuilder.claim(FIRST_NAME, securityUser.getFirstName())
                .claim(LAST_NAME, securityUser.getLastName())
                .claim(ENABLED, securityUser.isEnabled())
                .claim(IS_PUBLIC, principal.getType() == UserPrincipal.Type.PUBLIC_ID);
        if (securityUser.getTenantId() != null) {
            jwtBuilder.claim(TENANT_ID, securityUser.getTenantId().getId().toString());
        }
        if (securityUser.getCustomerId() != null) {
            jwtBuilder.claim(CUSTOMER_ID, securityUser.getCustomerId().getId().toString());
        }

        String token = jwtBuilder.compact();

        return new AccessJwtToken(token);
    }

    public SecurityUser parseAccessJwtToken(String token) {
        Jws<Claims> jwsClaims = parseTokenClaims(token);
        Claims claims = jwsClaims.getPayload();
        String subject = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> scopes = claims.get(SCOPES, List.class);
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("JWT Token doesn't have any scopes");
        }

        Authority authority = Authority.parse(scopes.get(0));

        SecurityUser securityUser = new SecurityUser(new UserId(UUID.fromString(claims.get(USER_ID, String.class))));
        securityUser.setEmail(subject);
        securityUser.setAuthority(authority);
        String tenantId = claims.get(TENANT_ID, String.class);

        if (tenantId != null) {
            securityUser.setTenantId(TenantId.fromUUID(UUID.fromString(tenantId)));
        } else if (authority == Authority.SYS_ADMIN) {
            securityUser.setTenantId(TenantId.SYS_TENANT_ID);
        }
        String customerId = claims.get(CUSTOMER_ID, String.class);
        if (customerId != null) {
            securityUser.setCustomerId(new CustomerId(UUID.fromString(customerId)));
        }
        if (claims.get(SESSION_ID, String.class) != null) {
            securityUser.setSessionId(claims.get(SESSION_ID, String.class));
        }

        boolean isPublic = false;
        if (authority != Authority.PRE_VERIFICATION_TOKEN && authority != Authority.MFA_CONFIGURATION_TOKEN) {
            securityUser.setFirstName(claims.get(FIRST_NAME, String.class));
            securityUser.setLastName(claims.get(LAST_NAME, String.class));
            securityUser.setEnabled(claims.get(ENABLED, Boolean.class));
            isPublic = claims.get(IS_PUBLIC, Boolean.class);
        }
        UserPrincipal principal = new UserPrincipal(isPublic ? UserPrincipal.Type.PUBLIC_ID : UserPrincipal.Type.USER_NAME, subject);
        securityUser.setUserPrincipal(principal);
        return securityUser;
    }

    public JwtToken createRefreshToken(SecurityUser securityUser) {
        UserPrincipal principal = securityUser.getUserPrincipal();

        String token = setUpToken(securityUser, Collections.singletonList(Authority.REFRESH_TOKEN.name()), jwtSettingsService.getJwtSettings().getRefreshTokenExpTime())
                .claim(IS_PUBLIC, principal.getType() == UserPrincipal.Type.PUBLIC_ID)
                .id(UUID.randomUUID().toString()).compact();

        return new AccessJwtToken(token);
    }

    public SecurityUser parseRefreshToken(String token) {
        Jws<Claims> jwsClaims = parseTokenClaims(token);
        Claims claims = jwsClaims.getPayload();
        String subject = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> scopes = claims.get(SCOPES, List.class);
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("Refresh Token doesn't have any scopes");
        }
        if (!scopes.get(0).equals(Authority.REFRESH_TOKEN.name())) {
            throw new IllegalArgumentException("Invalid Refresh Token scope");
        }
        boolean isPublic = claims.get(IS_PUBLIC, Boolean.class);
        UserPrincipal principal = new UserPrincipal(isPublic ? UserPrincipal.Type.PUBLIC_ID : UserPrincipal.Type.USER_NAME, subject);
        SecurityUser securityUser = new SecurityUser(new UserId(UUID.fromString(claims.get(USER_ID, String.class))));
        securityUser.setUserPrincipal(principal);
        if (claims.get(SESSION_ID, String.class) != null) {
            securityUser.setSessionId(claims.get(SESSION_ID, String.class));
        }
        return securityUser;
    }

    public JwtToken createMfaToken(SecurityUser user, Authority scope, Integer expirationTime) {
        JwtBuilder jwtBuilder = setUpToken(user, Collections.singletonList(scope.name()), expirationTime)
                .claim(TENANT_ID, user.getTenantId().toString());
        if (user.getCustomerId() != null) {
            jwtBuilder.claim(CUSTOMER_ID, user.getCustomerId().toString());
        }
        return new AccessJwtToken(jwtBuilder.compact());
    }

    public void reload() {
        getSecretKey(true);
        getJwtParser(true);
    }

    private JwtBuilder setUpToken(SecurityUser securityUser, List<String> scopes, long expirationTime) {
        if (StringUtils.isBlank(securityUser.getEmail())) {
            throw new IllegalArgumentException("Cannot create JWT Token without username/email");
        }

        UserPrincipal principal = securityUser.getUserPrincipal();

        ClaimsBuilder claimsBuilder = Jwts.claims()
                .subject(principal.getValue())
                .add(USER_ID, securityUser.getId().getId().toString())
                .add(SCOPES, scopes);
        if (securityUser.getSessionId() != null) {
            claimsBuilder.add(SESSION_ID, securityUser.getSessionId());
        }

        ZonedDateTime currentTime = ZonedDateTime.now();

        claimsBuilder.expiration(Date.from(currentTime.plusSeconds(expirationTime).toInstant()));

        return Jwts.builder()
                .claims(claimsBuilder.build())
                .issuer(jwtSettingsService.getJwtSettings().getTokenIssuer())
                .issuedAt(Date.from(currentTime.toInstant()))
                .signWith(getSecretKey(false), Jwts.SIG.HS512);
    }

    public Jws<Claims> parseTokenClaims(String token) {
        try {
            return getJwtParser(false).parseSignedClaims(token);
        } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT Token", ex);
            throw new BadCredentialsException("Invalid JWT token: ", ex);
        } catch (SignatureException | ExpiredJwtException expiredEx) {
            log.debug("JWT Token is expired", expiredEx);
            throw new JwtExpiredTokenException(token, "JWT Token expired", expiredEx);
        }
    }

    public JwtPair createTokenPair(SecurityUser securityUser) {
        securityUser.setSessionId(UUID.randomUUID().toString());
        JwtToken accessToken = createAccessJwtToken(securityUser);
        JwtToken refreshToken = createRefreshToken(securityUser);
        return new JwtPair(accessToken.getToken(), refreshToken.getToken());
    }

    private SecretKey getSecretKey(boolean forceReload) {
        if (secretKey == null || forceReload) {
            synchronized (this) {
                if (secretKey == null || forceReload) {
                    byte[] decodedToken = Base64.getDecoder().decode(jwtSettingsService.getJwtSettings().getTokenSigningKey());
                    secretKey = new SecretKeySpec(decodedToken, "HmacSHA512");
                }
            }
        }
        return secretKey;
    }

    private JwtParser getJwtParser(boolean forceReload) {
        if (jwtParser == null || forceReload) {
            synchronized (this) {
                if (jwtParser == null || forceReload) {
                    jwtParser = Jwts.parser()
                            .verifyWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSettingsService.getJwtSettings().getTokenSigningKey())))
                            .build();
                }
            }
        }
        return jwtParser;
    }
}
