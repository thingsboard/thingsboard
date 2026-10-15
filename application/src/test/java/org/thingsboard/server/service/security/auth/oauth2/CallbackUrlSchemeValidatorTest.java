// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.oauth2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class CallbackUrlSchemeValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"tbmobile", "tb-mobile.app1", "TbMobile+1", "org.mycompany.myapp.auth", "com.my_company.app.auth"})
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
