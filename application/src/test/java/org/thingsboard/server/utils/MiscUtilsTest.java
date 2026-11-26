// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class MiscUtilsTest {

    @Test
    public void testBaseUrlUsesForwardedHttpsScheme() {
        MockHttpServletRequest request = newRequest();
        request.addHeader("x-forwarded-proto", "https");

        assertThat(MiscUtils.constructBaseUrl(request)).isEqualTo("https://localhost:443");
    }

    @Test
    public void testForwardedSchemeIsLowerCased() {
        MockHttpServletRequest request = newRequest();
        request.addHeader("x-forwarded-proto", "HTTPS");

        assertThat(MiscUtils.getScheme(request)).isEqualTo("https");
    }

    @Test
    public void testNonHttpForwardedSchemeFallsBackToHttp() {
        MockHttpServletRequest request = newRequest();
        request.addHeader("x-forwarded-proto", "ftp");

        assertThat(MiscUtils.getScheme(request)).isEqualTo("http");
        URI baseUrl = URI.create(MiscUtils.constructBaseUrl(request));
        assertThat(baseUrl.getScheme()).isEqualTo("http");
        assertThat(baseUrl.getHost()).isEqualTo("localhost");
    }

    @Test
    public void testNonHttpForwardedSchemeOnSecureRequestFallsBackToHttps() {
        MockHttpServletRequest request = newRequest();
        request.addHeader("x-forwarded-proto", "ftp");
        request.setSecure(true);

        assertThat(MiscUtils.getScheme(request)).isEqualTo("https");
        assertThat(URI.create(MiscUtils.constructBaseUrl(request)).getHost()).isEqualTo("localhost");
    }

    @Test
    public void testRequestSchemeIsUsedWithoutForwardedHeader() {
        MockHttpServletRequest request = newRequest();
        request.setScheme("https");

        assertThat(MiscUtils.getScheme(request)).isEqualTo("https");
    }

    @Test
    public void testNonHttpRequestSchemeFallsBackToHttp() {
        MockHttpServletRequest request = newRequest();
        request.setScheme("ftp");

        assertThat(MiscUtils.getScheme(request)).isEqualTo("http");
    }

    private MockHttpServletRequest newRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        return request;
    }

}
