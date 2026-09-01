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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class CallbackUrlSchemeValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"tbmobile", "tb-mobile.app1", "TbMobile+1"})
    public void testMobileAppSchemeIsValid(String callbackUrlScheme) {
        assertThat(CallbackUrlSchemeValidator.isValid(callbackUrlScheme)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "https://evil.com",
            "http://evil.com",
            "https",
            "HTTPS",
            "javascript",
            "data",
            "file",
            "vbscript",
            "//evil.com",
            "tbmobile/evil.com",
            "tbmobile:evil.com",
            "tbmobile evil",
            "1tbmobile",
            "tbmobile@evil.com"
    })
    public void testInvalidSchemeIsRejected(String callbackUrlScheme) {
        assertThat(CallbackUrlSchemeValidator.isValid(callbackUrlScheme)).isFalse();
    }

    @Test
    public void testValidSchemeIsTakenFromAuthorizationRequest() {
        assertThat(CallbackUrlSchemeValidator.getCallbackUrlScheme(givenAuthorizationRequest("tbmobile"))).isEqualTo("tbmobile");
    }

    @Test
    public void testForgedSchemeFromAuthorizationRequestIsIgnored() {
        assertThat(CallbackUrlSchemeValidator.getCallbackUrlScheme(givenAuthorizationRequest("https://evil.com"))).isNull();
    }

    @Test
    public void testAuthorizationRequestWithoutScheme() {
        assertThat(CallbackUrlSchemeValidator.getCallbackUrlScheme(givenAuthorizationRequest(null))).isNull();
        assertThat(CallbackUrlSchemeValidator.getCallbackUrlScheme(null)).isNull();
    }

    private OAuth2AuthorizationRequest givenAuthorizationRequest(String callbackUrlScheme) {
        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("testUri").clientId("testId");
        if (callbackUrlScheme != null) {
            builder.attributes(attributes -> attributes.put(TbOAuth2ParameterNames.CALLBACK_URL_SCHEME, callbackUrlScheme));
        }
        return builder.build();
    }

}
