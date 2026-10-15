// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import static org.assertj.core.api.Assertions.assertThat;

class CustomOAuth2AuthorizationRequestResolverMatcherTest {

    private final PathPatternRequestMatcher matcher = PathPatternRequestMatcher.withDefaults().matcher("/oauth2/authorization/{registrationId}");

    @Test
    void extractsRegistrationId() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/abc-123");
        assertThat(matcher.matches(request)).isTrue();
        assertThat(matcher.matcher(request).getVariables()).containsEntry("registrationId", "abc-123");
    }

    @Test
    void rejectsDeeperPaths() {
        assertThat(matcher.matches(new MockHttpServletRequest("GET", "/oauth2/authorization/abc/extra"))).isFalse();
    }

}
