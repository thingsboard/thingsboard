// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.security.auth.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.API_ENTRY_POINT;
import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.CURRENT_USER_BY_ID_ENTRY_POINT;
import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.DEVICE_API_ENTRY_POINT;
import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.DEVICE_CONNECTIVITY_CERTIFICATE_DOWNLOAD_ENTRY_POINT;
import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.FORM_BASED_LOGIN_ENTRY_POINT;
import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.LOGIN_LOGO_ENTRY_POINT;
import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.SETUP_ENTRY_POINT;

class SkipPathRequestMatcherTest {

    private final SkipPathRequestMatcher matcher = new SkipPathRequestMatcher(
            List.of(FORM_BASED_LOGIN_ENTRY_POINT, DEVICE_API_ENTRY_POINT, SETUP_ENTRY_POINT,
                    DEVICE_CONNECTIVITY_CERTIFICATE_DOWNLOAD_ENTRY_POINT, LOGIN_LOGO_ENTRY_POINT),
            List.of(API_ENTRY_POINT, CURRENT_USER_BY_ID_ENTRY_POINT));

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
