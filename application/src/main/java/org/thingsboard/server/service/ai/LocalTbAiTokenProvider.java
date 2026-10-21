// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai;

import com.auth0.jwt.JWT;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thingsboard.ai.common.client.TbAiTokenFactory;
import org.thingsboard.ai.common.data.usage.RateLimitScope;
import org.thingsboard.ai.common.data.usage.RateLimitedApi;
import org.thingsboard.ai.common.data.user.ProfileConfig;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@TbCoreComponent
@ConditionalOnExpression("'${ai.jwt.signing_key:}' != ''")
class LocalTbAiTokenProvider implements TbAiTokenProvider {

    private final TbAiTokenFactory tokenFactory;
    private final SystemSecurityService systemSecurityService;
    private final TbTenantProfileCache tenantProfileCache;
    private final Cache<UUID, TokenInfo> tokenCache;

    // AI-related settings are read directly from environment variables, not yaml properties: they are
    // managed-service settings, not operator-tunable configuration.
    public LocalTbAiTokenProvider(@Value("${ai.jwt.issuer:local}") String issuer,
                                  @Value("${ai.jwt.signing_key}") String signingKey,
                                  @Value("${ai.jwt.expiration_time_minutes:60}") int expirationTimeMinutes,
                                  @Value("${cache.tbAiAccessTokens.timeToLiveInMinutes:60}") int cacheTtlMinutes,
                                  @Value("${cache.tbAiAccessTokens.maxSize:20000}") int cacheMaxSize,
                                  SystemSecurityService systemSecurityService,
                                  TbTenantProfileCache tenantProfileCache) {
        this.tokenFactory = new TbAiTokenFactory(issuer, signingKey, Duration.ofMinutes(expirationTimeMinutes));
        this.systemSecurityService = systemSecurityService;
        this.tenantProfileCache = tenantProfileCache;
        this.tokenCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheTtlMinutes, TimeUnit.MINUTES)
                .maximumSize(cacheMaxSize)
                .build();
    }

    @Override
    public String getToken(SecurityUser user) {
        return tokenCache.asMap().compute(user.getUuidId(), (id, tokenInfo) -> {
            if (tokenInfo == null || hasExpired(tokenInfo.token()) ||
                !Objects.equals(tokenInfo.clientAddress(), user.getClientAddress())) {
                tokenInfo = createToken(user);
            }
            return tokenInfo;
        }).token();
    }

    @Override
    public String getToken(TenantId tenantId, UserId userId) {
        Map<String, Object> claims = Map.of(
                "tenantId", tenantId.getId(),
                "userId", userId.getId());
        return tokenFactory.createToken(userId.getId().toString(), null, claims);
    }

    @EventListener(ComponentLifecycleMsg.class)
    public void onComponentLifecycleEvent(ComponentLifecycleMsg event) {
        EntityId entityId = event.getEntityId();
        switch (entityId.getEntityType()) {
            case TENANT_PROFILE -> {
                // Any tenant profile change invalidates cached tokens for its tenants:
                // rate-limit configuration is embedded in the signed JWT, so even an
                // unrelated profile edit must force a resign to avoid serving stale limits.
                tokenCache.asMap().values().removeIf(tokenInfo ->
                        tokenInfo.tenantProfileId().equals(entityId));
            }
            case TENANT -> {
                if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
                    tokenCache.asMap().values().removeIf(tokenInfo ->
                            tokenInfo.tenantId().equals(entityId));
                }
            }
            case USER -> {
                if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
                    tokenCache.invalidate(entityId.getId());
                }
            }
        }
    }

    private static boolean hasExpired(String token) {
        return JWT.decode(token).getExpiresAtAsInstant().isBefore(Instant.now());
    }

    private TokenInfo createToken(SecurityUser user) {
        TenantProfile tenantProfile = tenantProfileCache.get(user.getTenantId());
        if (tenantProfile == null) {
            throw new IllegalArgumentException("Tenant profile not found for tenant id " + user.getTenantId());
        }
        DefaultTenantProfileConfiguration tenantProfileConfig = tenantProfile.getDefaultProfileConfiguration();
        ProfileConfig profileConfig = toProfileConfig(tenantProfileConfig);

        String origin = systemSecurityService.getBaseUrl(user.getAuthority(), user.getTenantId(), user.getCustomerId(), null);
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", user.getTenantId().getId());
        claims.put("userId", user.getUuidId());
        claims.put("email", user.getEmail());
        claims.put("origin", origin);
        if (user.getClientAddress() != null) {
            claims.put("clientAddress", user.getClientAddress());
        }
        String token = tokenFactory.createToken(user.getUuidId().toString(), profileConfig, claims);
        return new TokenInfo(user.getTenantId(), tenantProfile.getId(), user.getClientAddress(), token);
    }

    private static ProfileConfig toProfileConfig(DefaultTenantProfileConfiguration tenantProfileConfig) {
        var profileConfig = new ProfileConfig();
        profileConfig.setMaxAiCredits(Integer.MAX_VALUE); // for local the limitation happens through API usage states
        profileConfig.setRateLimits(Map.of(RateLimitedApi.AI_CHAT_REQUESTS, Map.of(
                RateLimitScope.TENANT, Strings.nullToEmpty(tenantProfileConfig.getAiChatRequestsPerTenantRateLimit())
        )));
        return profileConfig;
    }

    private record TokenInfo(TenantId tenantId, TenantProfileId tenantProfileId, String clientAddress, String token) {}

}
