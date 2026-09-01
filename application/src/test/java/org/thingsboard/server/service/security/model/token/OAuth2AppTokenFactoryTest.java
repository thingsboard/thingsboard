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
package org.thingsboard.server.service.security.model.token;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OAuth2AppTokenFactoryTest {

    private static final String APP_PACKAGE = "org.thingsboard.demo.app";
    private static final byte[] KEY_BYTES = "yjNyylzT1TmiVE2jV3YTnUpZzwLLLdPDJKmhLNyXDPnLtVCLcJIjIGmDPKHNoDMK".getBytes();

    private final OAuth2AppTokenFactory tokenFactory = new OAuth2AppTokenFactory();

    @Test
    public void testMobileAppSchemeIsAccepted() {
        assertThat(validate(appToken("tb-mobile.app1", APP_PACKAGE))).isEqualTo("tb-mobile.app1");
    }

    @Test
    public void testInvalidCallbackUrlSchemeIsRejected() {
        assertThatThrownBy(() -> validate(appToken("https://evil.com", APP_PACKAGE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("callbackUrlScheme");
    }

    @Test
    public void testTokenWithoutIssuerIsRejected() {
        assertThatThrownBy(() -> validate(appToken("tbmobile", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("issuer");
    }

    private String appToken(String callbackUrlScheme, String issuer) {
        JwtBuilder builder = Jwts.builder()
                .expiration(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1)))
                .claim("callbackUrlScheme", callbackUrlScheme);
        if (issuer != null) {
            builder.issuer(issuer);
        }
        SecretKey key = Keys.hmacShaKeyFor(KEY_BYTES);
        return builder.signWith(key).compact();
    }

    private String validate(String appToken) {
        return tokenFactory.validateTokenAndGetCallbackUrlScheme(APP_PACKAGE, appToken, Base64.getEncoder().encodeToString(KEY_BYTES));
    }

}
