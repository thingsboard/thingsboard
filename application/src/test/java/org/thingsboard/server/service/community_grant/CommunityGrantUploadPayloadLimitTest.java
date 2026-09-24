// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.transport.http.config.PayloadSizeFilter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The offline upload carries a whole checker, so it needs its own {@code server.http.max_payload_size} entry.
 * Not a MockMvc test: a mock multipart request's content length does not reflect the parts attached to it.
 */
class CommunityGrantUploadPayloadLimitTest {

    private static final String OFFLINE_RUN_URI = "/api/communityGrant/offline/run";

    private static final long API_DEFAULT_LIMIT = 16777216L;

    @Test
    void testTheCheckerCapPlusTheBundlesOwnOverheadFitsUnderTheEndpointsEntry() {
        assertThat(maxCheckerSizeFromYaml() + CommunityGrantOfflineBundle.MAX_OVERHEAD_BYTES)
                .as("community-grant.max-checker-size-bytes plus the bundle header and manifest must leave "
                        + "room under the %s entry in server.http.max_payload_size, which is compared "
                        + "against the whole request body", OFFLINE_RUN_URI)
                .isLessThan(limitFor(OFFLINE_RUN_URI));
    }

    @Test
    void testABundleAtTheDocumentedMaximumReachesTheControllerRatherThanA413() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        // With a writer, a 413 fails the assertions below instead of throwing an NPE inside the filter.
        HttpServletResponse response = mockResponseWithWriter();

        filter().doFilterInternal(requestOf(OFFLINE_RUN_URI,
                maxCheckerSizeFromYaml() + CommunityGrantOfflineBundle.MAX_OVERHEAD_BYTES + 65536), response, chain);

        verify(chain).doFilter(any(), any());
        verify(response, never()).setStatus(413);
    }

    @Test
    void testABodyPastTheEndpointsOwnEntryIsStillRefused() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        HttpServletResponse response = mockResponseWithWriter();

        filter().doFilterInternal(requestOf(OFFLINE_RUN_URI, 64L * 1024 * 1024), response, chain);

        verify(chain, never()).doFilter(any(), any());
        verify(response).setStatus(413);
    }

    @Test
    void testEveryOtherApiPathKeepsTheStockSixteenMegabyteCap() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        HttpServletResponse response = mockResponseWithWriter();

        filter().doFilterInternal(requestOf("/api/plugins/telemetry/DEVICE/x/values/timeseries",
                API_DEFAULT_LIMIT + 1), response, chain);

        verify(chain, never()).doFilter(any(), any());
        verify(response).setStatus(413);
    }

    @Test
    void testTheEndpointsEntryPrecedesTheCatchAllItWouldOtherwiseFallTo() {
        String limits = payloadLimitsFromYaml();

        int endpointEntry = limits.indexOf(OFFLINE_RUN_URI + "=");
        int catchAllEntry = limits.indexOf("/api/**=");

        assertThat(endpointEntry).as("the offline upload needs its own entry in server.http.max_payload_size")
                .isNotNegative();
        assertThat(endpointEntry).as("it is ignored unless it precedes /api/**, which matches the same URI")
                .isLessThan(catchAllEntry);
    }

    private static PayloadSizeFilter filter() {
        return new PayloadSizeFilter(payloadLimitsFromYaml());
    }

    private static HttpServletRequest requestOf(String uri, long contentLength) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getContentLength()).thenReturn(Math.toIntExact(contentLength));
        return request;
    }

    private static HttpServletResponse mockResponseWithWriter() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        return response;
    }

    private static long limitFor(String pattern) {
        for (String entry : payloadLimitsFromYaml().split(";")) {
            if (entry.startsWith(pattern + "=")) {
                return Long.parseLong(entry.substring(pattern.length() + 1));
            }
        }
        throw new IllegalStateException("server.http.max_payload_size has no entry for " + pattern);
    }

    private static long maxCheckerSizeFromYaml() {
        return Long.parseLong(matchInYaml(
                "max-checker-size-bytes:\\s*\"\\$\\{COMMUNITY_GRANT_MAX_CHECKER_SIZE_BYTES:(\\d+)}\"",
                "community-grant.max-checker-size-bytes"));
    }

    private static String payloadLimitsFromYaml() {
        return matchInYaml("max_payload_size:\\s*\"\\$\\{HTTP_MAX_PAYLOAD_SIZE_LIMIT_CONFIGURATION:([^}]*)}\"",
                "server.http.max_payload_size");
    }

    private static String matchInYaml(String regex, String property) {
        String yaml;
        try (var in = CommunityGrantUploadPayloadLimitTest.class.getResourceAsStream("/thingsboard.yml")) {
            yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("thingsboard.yml is not on the test classpath", e);
        }
        Matcher matcher = Pattern.compile(regex).matcher(yaml);
        if (!matcher.find()) {
            throw new IllegalStateException(property + " is not declared the way this test reads it");
        }
        return matcher.group(1);
    }

}
