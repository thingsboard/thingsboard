// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.http;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class HttpTlsTerminationTest {

    @Test
    void plainRequestIsNotSecure() {
        assertThat(HttpTlsTermination.resolve(new MockHttpServletRequest())).isEqualTo(HttpTlsTermination.NONE);
    }

    @Test
    void ownTlsIsTerminatedOnTransport() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        request.setAttribute(HttpTlsTermination.CIPHER_SUITE_ATTRIBUTE, "TLS_AES_128_GCM_SHA256");
        request.addHeader("X-Forwarded-Proto", "http");
        assertThat(HttpTlsTermination.resolve(request)).isEqualTo(HttpTlsTermination.TRANSPORT);
    }

    @Test
    void secureSchemeResolvedByForwardedHeadersStrategyIsProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        assertThat(HttpTlsTermination.resolve(request)).isEqualTo(HttpTlsTermination.PROXY);
    }

    @Test
    void xForwardedProtoUsesClosestToClientValue() {
        assertThat(resolveWithHeader("X-Forwarded-Proto", "https")).isEqualTo(HttpTlsTermination.PROXY);
        assertThat(resolveWithHeader("X-Forwarded-Proto", "HTTPS, http")).isEqualTo(HttpTlsTermination.PROXY);
        assertThat(resolveWithHeader("X-Forwarded-Proto", "http, https")).isEqualTo(HttpTlsTermination.NONE);
    }

    @Test
    void forwardedHeaderProto() {
        assertThat(resolveWithHeader("Forwarded", "for=192.0.2.60;proto=https;by=203.0.113.43")).isEqualTo(HttpTlsTermination.PROXY);
        assertThat(resolveWithHeader("Forwarded", "for=\"[2001:db8::1]\"; proto=\"https\", for=10.0.0.1;proto=http")).isEqualTo(HttpTlsTermination.PROXY);
        assertThat(resolveWithHeader("Forwarded", "for=192.0.2.60;proto=http")).isEqualTo(HttpTlsTermination.NONE);
    }

    @Test
    void xForwardedSsl() {
        assertThat(resolveWithHeader("X-Forwarded-Ssl", "on")).isEqualTo(HttpTlsTermination.PROXY);
        assertThat(resolveWithHeader("X-Forwarded-Ssl", "off")).isEqualTo(HttpTlsTermination.NONE);
    }

    private static HttpTlsTermination resolveWithHeader(String name, String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(name, value);
        return HttpTlsTermination.resolve(request);
    }

}
