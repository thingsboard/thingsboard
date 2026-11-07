// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.security.ip;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.net.InetAddresses;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.config.ClientAddressCaptureFilter;
import org.thingsboard.server.config.SysAdminAllowlistProperties;
import org.thingsboard.server.service.security.exception.IpNotAllowedException;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SysAdminIpPolicyTest {

    private ListAppender<ILoggingEvent> logAppender;
    private Logger policyLogger;

    @BeforeEach
    void setupLogCapture() {
        policyLogger = (Logger) LoggerFactory.getLogger(SysAdminIpPolicy.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        policyLogger.addAppender(logAppender);
    }

    @AfterEach
    void teardownLogCapture() {
        policyLogger.detachAppender(logAppender);
    }

    @Test
    void notEnforcedWhenListEmpty() {
        SysAdminIpPolicy policy = policy(List.of());
        assertThat(policy.isEnforced()).isFalse();
        assertThat(policy.isBlocked(Authority.SYS_ADMIN, TenantId.SYS_TENANT_ID, userId(),
                addr("9.9.9.9"), addr("9.9.9.9"), "rest")).isFalse();
    }

    @Test
    void sysAdminBlockedOutsideList() {
        SysAdminIpPolicy policy = policy(List.of("203.0.113.0/24"));
        assertThat(policy.isBlocked(Authority.SYS_ADMIN, TenantId.SYS_TENANT_ID, userId(),
                addr("9.9.9.9"), addr("9.9.9.9"), "rest")).isTrue();
    }

    @Test
    void sysAdminAllowedInsideList() {
        SysAdminIpPolicy policy = policy(List.of("203.0.113.0/24"));
        assertThat(policy.isBlocked(Authority.SYS_ADMIN, TenantId.SYS_TENANT_ID, userId(),
                addr("203.0.113.5"), addr("203.0.113.5"), "rest")).isFalse();
    }

    @Test
    void tenantAdminNeverBlocked() {
        SysAdminIpPolicy policy = policy(List.of("203.0.113.0/24"));
        assertThat(policy.isBlocked(Authority.TENANT_ADMIN, TenantId.fromUUID(UUID.randomUUID()), userId(),
                addr("9.9.9.9"), addr("9.9.9.9"), "rest")).isFalse();
    }

    @Test
    void mfaPreVerificationTokenOfSysTenantIsScoped() {
        SysAdminIpPolicy policy = policy(List.of("203.0.113.0/24"));
        assertThat(policy.isBlocked(Authority.PRE_VERIFICATION_TOKEN, TenantId.SYS_TENANT_ID, userId(),
                addr("9.9.9.9"), addr("9.9.9.9"), "rest")).isTrue();
        assertThat(policy.isBlocked(Authority.MFA_CONFIGURATION_TOKEN, TenantId.SYS_TENANT_ID, userId(),
                addr("9.9.9.9"), addr("9.9.9.9"), "rest")).isTrue();
        // tenant user at the MFA stage is not sysadmin-scoped
        assertThat(policy.isBlocked(Authority.PRE_VERIFICATION_TOKEN, TenantId.fromUUID(UUID.randomUUID()), userId(),
                addr("9.9.9.9"), addr("9.9.9.9"), "rest")).isFalse();
    }

    @Test
    void nullResolvedAddressFailsClosed() {
        SysAdminIpPolicy policy = policy(List.of("203.0.113.0/24"));
        assertThat(policy.isBlocked(Authority.SYS_ADMIN, TenantId.SYS_TENANT_ID, userId(),
                null, null, "rest")).isTrue();
    }

    @Test
    void resolveFromRequestPrefersAttributeAndFallsBackToRemoteAddr() {
        SysAdminIpPolicy policy = policy(List.of("203.0.113.0/24"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("9.9.9.9");
        assertThat(policy.resolveFromRequest(request)).isEqualTo(addr("9.9.9.9"));
        request.setAttribute(ClientAddressCaptureFilter.RESOLVED_CLIENT_ADDRESS_ATTR, addr("203.0.113.5"));
        assertThat(policy.resolveFromRequest(request)).isEqualTo(addr("203.0.113.5"));
    }

    @Test
    void sysAdminOwnedCredentialScopedEvenWhenImpersonatingTenantUser() {
        SysAdminIpPolicy policy = policy(List.of("203.0.113.0/24"));
        SecurityUser impersonated = new SecurityUser(userId());
        impersonated.setAuthority(Authority.TENANT_ADMIN);
        impersonated.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        assertThat(policy.isBlocked(impersonated, addr("9.9.9.9"), addr("9.9.9.9"), "rest")).isFalse();
        impersonated.setSysAdminOwnedCredential(true);
        assertThat(policy.isBlocked(impersonated, addr("9.9.9.9"), addr("9.9.9.9"), "rest")).isTrue();
        assertThat(policy.isBlocked(impersonated, addr("203.0.113.5"), addr("203.0.113.5"), "rest")).isFalse();
    }

    @Test
    void enforceThrowsForBlockedSysAdmin() {
        SysAdminIpPolicy policy = policy(List.of("203.0.113.0/24"));
        SecurityUser user = sysAdmin();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("9.9.9.9");
        assertThatThrownBy(() -> policy.enforce(user, request, "rest"))
                .isInstanceOf(IpNotAllowedException.class);
        request.setRemoteAddr("203.0.113.5");
        policy.enforce(user, request, "rest"); // no exception
    }

    @Test
    void blockedLogLineMatchesContract() {
        SysAdminIpPolicy policy = policy(List.of("203.0.113.0/24"));
        policy.isBlocked(Authority.SYS_ADMIN, TenantId.SYS_TENANT_ID, userId(),
                addr("9.9.9.9"), addr("10.0.0.2"), "rest");
        assertThat(logAppender.list).anySatisfy(event ->
                assertThat(event.getFormattedMessage())
                        .matches("^Sysadmin IP allowlist blocked: client=9\\.9\\.9\\.9 socket=10\\.0\\.0\\.2 surface=rest userId=\\S+$"));
    }

    @Test
    void logRateLimitedPerIpButNotAcrossIps() {
        SysAdminIpPolicy policy = policy(List.of("203.0.113.0/24"));
        for (int i = 0; i < 50; i++) {
            policy.isBlocked(Authority.SYS_ADMIN, TenantId.SYS_TENANT_ID, userId(),
                    addr("9.9.9.9"), addr("9.9.9.9"), "rest");
        }
        long linesIpOne = countBlockedLines("client=9.9.9.9 ");
        assertThat(linesIpOne).isBetween(1L, 20L); // rate-limited, but fail2ban can still count retries
        policy.isBlocked(Authority.SYS_ADMIN, TenantId.SYS_TENANT_ID, userId(),
                addr("9.9.9.10"), addr("9.9.9.10"), "rest");
        assertThat(countBlockedLines("client=9.9.9.10 ")).isEqualTo(1); // other IPs not starved
    }

    private long countBlockedLines(String fragment) {
        return logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(line -> line.startsWith("Sysadmin IP allowlist blocked: ") && line.contains(fragment))
                .count();
    }

    private static SysAdminIpPolicy policy(List<String> allowed) {
        SysAdminAllowlistProperties props = new SysAdminAllowlistProperties(allowed, List.of());
        props.init();
        return new SysAdminIpPolicy(props);
    }

    private static SecurityUser sysAdmin() {
        SecurityUser user = new SecurityUser(userId());
        user.setAuthority(Authority.SYS_ADMIN);
        user.setTenantId(TenantId.SYS_TENANT_ID);
        return user;
    }

    private static UserId userId() {
        return new UserId(UUID.randomUUID());
    }

    private static InetAddress addr(String literal) {
        return InetAddresses.forString(literal);
    }

}
