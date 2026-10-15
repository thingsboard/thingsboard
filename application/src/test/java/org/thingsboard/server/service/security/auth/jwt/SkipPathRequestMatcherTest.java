// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.DEVICE_API_ENTRY_POINT;
import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.DEVICE_CONNECTIVITY_CERTIFICATE_DOWNLOAD_ENTRY_POINT;
import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.FORM_BASED_LOGIN_ENTRY_POINT;

class SkipPathRequestMatcherTest {

    private final SkipPathRequestMatcher matcher = new SkipPathRequestMatcher(
            List.of(FORM_BASED_LOGIN_ENTRY_POINT, DEVICE_API_ENTRY_POINT, "/api/noauth/setup/**",
                    DEVICE_CONNECTIVITY_CERTIFICATE_DOWNLOAD_ENTRY_POINT, "/api/noauth/whiteLabel/loginLogo/*/*"),
            List.of("/api/**", "/api/user/{userId:[0-9a-fA-F-]{36}}"));

    @Test
    void processesProtectedApiPaths() {
        assertThat(matcher.matches(request("/api/device/123"))).isTrue();
        assertThat(matcher.matches(request("/api/user/0a1b2c3d-0000-1111-2222-333344445555"))).isTrue();
    }

    @Test
    void skipsListedPaths() {
        assertThat(matcher.matches(request("/api/auth/login"))).isFalse();
        assertThat(matcher.matches(request("/api/v1/token/telemetry"))).isFalse();
        assertThat(matcher.matches(request("/api/v1"))).isFalse();
        assertThat(matcher.matches(request("/api/noauth/setup/status"))).isFalse();
        assertThat(matcher.matches(request("/api/device-connectivity/mqtt/certificate/download"))).isFalse();
    }

    @Test
    void singleSegmentWildcardsDoNotSpanSegments() {
        assertThat(matcher.matches(request("/api/noauth/whiteLabel/loginLogo/abc/def"))).isFalse();
        assertThat(matcher.matches(request("/api/noauth/whiteLabel/loginLogo/abc/def/ghi"))).isTrue();
    }

    @Test
    void doesNotSkipNearMisses() {
        assertThat(matcher.matches(request("/api/auth/login/"))).isTrue();
        assertThat(matcher.matches(request("/api/auth/loginx"))).isTrue();
        assertThat(matcher.matches(request("/API/auth/login"))).isFalse();
    }

    @Test
    void ignoresPathsOutsideApi() {
        assertThat(matcher.matches(request("/static/index.html"))).isFalse();
    }

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }

}
