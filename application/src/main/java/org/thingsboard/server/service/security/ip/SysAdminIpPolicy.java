// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.security.ip;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.net.InetAddresses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.CidrRange;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.config.ClientAddressCaptureFilter;
import org.thingsboard.server.config.SysAdminAllowlistProperties;
import org.thingsboard.server.service.security.exception.IpNotAllowedException;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.net.InetAddress;
import java.time.Duration;

/**
 * Decides whether a System Administrator-scoped principal may act from a given client address,
 * per the configured sysadmin IP allowlist. Every blocked attempt emits one WARN line in a stable
 * format meant for external ban tooling (the resolved client IP always appears as "client=<ip>"),
 * rate-limited per client IP to prevent log flooding.
 */
@Slf4j
@Service
public class SysAdminIpPolicy {

    // generous enough that external ban tooling can still count repeated attempts towards its retry threshold
    private static final String BLOCKED_LOG_RATE_LIMIT = "10:60";

    private final SysAdminAllowlistProperties properties;
    private final Cache<String, TbRateLimits> blockedLogRateLimits = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    public SysAdminIpPolicy(SysAdminAllowlistProperties properties) {
        this.properties = properties;
    }

    public boolean isEnforced() {
        return properties.isEnforced();
    }

    public boolean isSysAdminScoped(SecurityUser user) {
        return isSysAdminScoped(user.getAuthority(), user.getTenantId());
    }

    public boolean isAllowed(InetAddress address) {
        if (address == null) {
            return false;
        }
        for (CidrRange range : properties.getAllowedRanges()) {
            if (range.contains(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The single decision point: true means the attempt is sysadmin-scoped, outside the allowlist,
     * and has been logged for external ban tooling.
     */
    public boolean isBlocked(Authority authority, TenantId tenantId, UserId userId,
                             InetAddress clientAddress, InetAddress socketAddress, String surface) {
        if (!isEnforced() || !isSysAdminScoped(authority, tenantId)) {
            return false;
        }
        if (isAllowed(clientAddress)) {
            return false;
        }
        logBlocked(clientAddress, socketAddress, surface, userId);
        return true;
    }

    public void enforce(SecurityUser user, HttpServletRequest request, String surface) throws IpNotAllowedException {
        if (isBlocked(user.getAuthority(), user.getTenantId(), user.getId(),
                resolveFromRequest(request), rawSocketAddress(request), surface)) {
            throw new IpNotAllowedException();
        }
    }

    public InetAddress resolveFromRequest(HttpServletRequest request) {
        if (request.getAttribute(ClientAddressCaptureFilter.RESOLVED_CLIENT_ADDRESS_ATTR) instanceof InetAddress resolved) {
            return resolved;
        }
        return parseQuietly(request.getRemoteAddr());
    }

    private static InetAddress rawSocketAddress(HttpServletRequest request) {
        if (request.getAttribute(ClientAddressCaptureFilter.RAW_SOCKET_ADDRESS_ATTR) instanceof InetAddress socket) {
            return socket;
        }
        return parseQuietly(request.getRemoteAddr());
    }

    private static boolean isSysAdminScoped(Authority authority, TenantId tenantId) {
        if (authority == Authority.SYS_ADMIN) {
            return true;
        }
        return (authority == Authority.PRE_VERIFICATION_TOKEN || authority == Authority.MFA_CONFIGURATION_TOKEN)
                && TenantId.SYS_TENANT_ID.equals(tenantId);
    }

    private void logBlocked(InetAddress clientAddress, InetAddress socketAddress, String surface, UserId userId) {
        String client = clientAddress != null ? clientAddress.getHostAddress() : "unknown";
        TbRateLimits rateLimits = blockedLogRateLimits.get(client, key -> new TbRateLimits(BLOCKED_LOG_RATE_LIMIT));
        if (rateLimits.tryConsume()) {
            // stable format consumed by external ban tooling: the client IP always appears as client=<ip>
            log.warn("Sysadmin IP allowlist blocked: client={} socket={} surface={} userId={}",
                    client,
                    socketAddress != null ? socketAddress.getHostAddress() : "unknown",
                    surface,
                    userId != null ? userId.getId() : "unknown");
        }
    }

    private static InetAddress parseQuietly(String literal) {
        if (literal == null || literal.isBlank()) {
            return null;
        }
        try {
            return InetAddresses.forString(literal);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
