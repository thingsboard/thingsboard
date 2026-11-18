// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.security.auth.jwt;

import com.google.common.net.InetAddresses;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.config.ClientAddressCaptureFilter;
import org.thingsboard.server.service.security.auth.extractor.TokenExtractor;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class JwtTokenAuthenticationProcessingFilterTest {

    @Test
    void clientAddressTakenFromResolvedAttributeNotRemoteAddr() throws Exception {
        JwtTokenAuthenticationProcessingFilter filter = filter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        // what ForwardedHeaderFilter would report: derived from a client-controlled header
        request.setRemoteAddr("5.5.5.5");
        request.setAttribute(ClientAddressCaptureFilter.RESOLVED_CLIENT_ADDRESS_ATTR, InetAddresses.forString("203.0.113.5"));
        SecurityUser user = new SecurityUser(new UserId(UUID.randomUUID()));

        filter.successfulAuthentication(request, new MockHttpServletResponse(), new MockFilterChain(),
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        assertThat(user.getClientAddress()).isEqualTo("203.0.113.5");
    }

    @Test
    void clientAddressFallsBackToRemoteAddrWithoutCapture() throws Exception {
        JwtTokenAuthenticationProcessingFilter filter = filter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("9.9.9.9");
        SecurityUser user = new SecurityUser(new UserId(UUID.randomUUID()));

        filter.successfulAuthentication(request, new MockHttpServletResponse(), new MockFilterChain(),
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        assertThat(user.getClientAddress()).isEqualTo("9.9.9.9");
    }

    private static JwtTokenAuthenticationProcessingFilter filter() {
        return new JwtTokenAuthenticationProcessingFilter(
                mock(AuthenticationFailureHandler.class), mock(TokenExtractor.class), mock(RequestMatcher.class));
    }

}
