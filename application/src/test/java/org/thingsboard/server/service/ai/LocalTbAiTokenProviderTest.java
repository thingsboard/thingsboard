// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.common.data.usage.RateLimitScope;
import org.thingsboard.ai.common.data.usage.RateLimitedApi;
import org.thingsboard.ai.common.data.user.ProfileConfig;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class LocalTbAiTokenProviderTest {

    private final String issuer = "tb-ai-test-issuer";
    // HS512 requires a >= 512-bit key; the factory Base64-decodes this before use.
    private final String signingKey = Base64.getEncoder()
            .encodeToString("tb-ai-unit-test-signing-key-".repeat(3).getBytes(StandardCharsets.UTF_8));
    private final String baseUrl = "https://tb.example.test";
    private final int defaultExpirationMinutes = 60;
    private final int cacheTtlMinutes = 60;
    private final int cacheMaxSize = 1000;

    @Mock
    SystemSecurityService systemSecurityService;
    @Mock
    TbTenantProfileCache tenantProfileCache;

    TenantId tenantId;
    TenantProfileId tenantProfileId;
    UUID userId;
    TenantProfile tenantProfile;
    SecurityUser user;

    LocalTbAiTokenProvider provider;

    @BeforeEach
    void setUp() {
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        tenantProfileId = new TenantProfileId(UUID.randomUUID());
        userId = UUID.randomUUID();
        tenantProfile = tenantProfile(tenantProfileId, "100:60");
        user = user(userId, tenantId, "tenant-admin@example.org", "198.51.100.10");
        // Lenient: this default stub is unused by tests that fail before origin resolution.
        lenient().when(systemSecurityService.getBaseUrl(any(), any(), any(), isNull())).thenReturn(baseUrl);
        provider = newProvider(defaultExpirationMinutes);
    }

    // ---------------------------------------------------------------------------------------------
    // Token content
    // ---------------------------------------------------------------------------------------------

    @Test
    void shouldMintSignedTokenWithExpectedClaims_whenCalledForUser() {
        // GIVEN
        given(tenantProfileCache.get(tenantId)).willReturn(tenantProfile);

        // WHEN
        String token = provider.getToken(user);

        // THEN
        DecodedJWT decoded = JWT.decode(token);
        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getIssuer()).isEqualTo(issuer);
        assertThat(decoded.getClaim("tenantId").asString()).isEqualTo(tenantId.getId().toString());
        assertThat(decoded.getClaim("userId").asString()).isEqualTo(userId.toString());
        assertThat(decoded.getClaim("email").asString()).isEqualTo("tenant-admin@example.org");
        assertThat(decoded.getClaim("origin").asString()).isEqualTo(baseUrl);
        assertThat(decoded.getClaim("clientAddress").asString()).isEqualTo("198.51.100.10");

        // Expiration is exactly the configured offset from issuance and lies in the future.
        assertThat(Duration.between(decoded.getIssuedAtAsInstant(), decoded.getExpiresAtAsInstant()))
                .isEqualTo(Duration.ofMinutes(defaultExpirationMinutes));
        assertThat(decoded.getExpiresAtAsInstant()).isAfter(Instant.now());

        // The token is genuinely signed with the configured key and issuer (verified, not just decoded).
        assertThatTokenIsSignedWithConfiguredKey(token);
    }

    @Test
    void shouldEmbedProfileConfigWithUnlimitedCreditsAndTenantRateLimit_whenCalled() {
        // GIVEN
        given(tenantProfileCache.get(tenantId)).willReturn(tenantProfile);

        // WHEN
        String token = provider.getToken(user);

        // THEN
        ProfileConfig profileConfig = profileConfigClaim(token);
        // Local deployments do not cap AI credits in the JWT; the cap is enforced via API usage states.
        assertThat(profileConfig.getMaxAiCredits()).isEqualTo(Integer.MAX_VALUE);
        assertThat(profileConfig.getRateLimits(RateLimitedApi.AI_CHAT_REQUESTS))
                .containsEntry(RateLimitScope.TENANT, "100:60");
    }

    @Test
    void shouldEmbedEmptyTenantRateLimit_whenTenantProfileRateLimitIsNull() {
        // GIVEN
        given(tenantProfileCache.get(tenantId)).willReturn(tenantProfile(tenantProfileId, null));

        // WHEN
        String token = provider.getToken(user);

        // THEN
        ProfileConfig profileConfig = profileConfigClaim(token);
        assertThat(profileConfig.getRateLimits(RateLimitedApi.AI_CHAT_REQUESTS))
                .containsEntry(RateLimitScope.TENANT, "");
    }

    @Test
    void shouldOmitClientAddressClaim_whenUserHasNoClientAddress() {
        // GIVEN
        user.setClientAddress(null);
        given(tenantProfileCache.get(tenantId)).willReturn(tenantProfile);

        // WHEN
        String token = provider.getToken(user);

        // THEN
        assertThat(JWT.decode(token).getClaims()).doesNotContainKey("clientAddress");
    }

    // ---------------------------------------------------------------------------------------------
    // Id-based tokens (post-deletion identity)
    // ---------------------------------------------------------------------------------------------

    @Test
    void shouldMintMinimalSignedToken_whenCalledWithRawIds() {
        // WHEN
        String token = provider.getToken(tenantId, new UserId(userId));

        // THEN — identity claims only: no email, origin, client address, or profile config. Issuer, expiration,
        // and signing come from the same factory call as user-scoped tokens and are pinned by the tests above.
        DecodedJWT decoded = JWT.decode(token);
        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getClaim("tenantId").asString()).isEqualTo(tenantId.getId().toString());
        assertThat(decoded.getClaim("userId").asString()).isEqualTo(userId.toString());
        assertThat(decoded.getClaims()).doesNotContainKeys("email", "origin", "clientAddress");
        assertThat(profileConfigClaim(token)).isNull();

        // The mint must not depend on anything that is gone after deletion.
        then(tenantProfileCache).shouldHaveNoInteractions();
        then(systemSecurityService).shouldHaveNoInteractions();
    }

    @Test
    void shouldNotTouchUserTokenCache_whenMintingIdBasedToken() {
        // GIVEN — a cached user-scoped token for the same user id.
        given(tenantProfileCache.get(tenantId)).willReturn(tenantProfile);
        String cached = provider.getToken(user);

        // WHEN
        String idBased = provider.getToken(tenantId, new UserId(userId));

        // THEN — the id-based mint bypasses the cache and leaves the cached user token in place.
        assertThat(idBased).isNotEqualTo(cached);
        assertThat(provider.getToken(user)).isEqualTo(cached);
        then(tenantProfileCache).should(times(1)).get(tenantId);
    }

    // ---------------------------------------------------------------------------------------------
    // Origin resolution
    // ---------------------------------------------------------------------------------------------

    @Test
    void shouldResolveOriginViaSecurityService_whenMintingToken() {
        // GIVEN the security service resolves the user-facing base URL (white-labeling aware, no request context);
        // the exact-argument stub and verification pin that the provider passes the user's authority, tenant, and
        // customer, and no HTTP request.
        given(tenantProfileCache.get(tenantId)).willReturn(tenantProfile);
        given(systemSecurityService.getBaseUrl(user.getAuthority(), user.getTenantId(), user.getCustomerId(), null))
                .willReturn("https://resolved.example.test");

        // WHEN
        String token = provider.getToken(user);

        // THEN
        assertThat(JWT.decode(token).getClaim("origin").asString()).isEqualTo("https://resolved.example.test");
        then(systemSecurityService).should()
                .getBaseUrl(user.getAuthority(), user.getTenantId(), user.getCustomerId(), null);
    }

    // ---------------------------------------------------------------------------------------------
    // Caching
    // ---------------------------------------------------------------------------------------------

    @Test
    void shouldReturnCachedToken_onSecondCallForSameUser() {
        // GIVEN
        given(tenantProfileCache.get(tenantId)).willReturn(tenantProfile);

        // WHEN
        String first = provider.getToken(user);
        String second = provider.getToken(user);

        // THEN
        assertThat(second).isEqualTo(first);
        // A single mint: the tenant profile is read only once across both calls.
        then(tenantProfileCache).should(times(1)).get(tenantId);
    }

    @Test
    void shouldMintNewToken_whenCachedTokenHasExpired() {
        // GIVEN — negative expiration means every minted token is already expired.
        LocalTbAiTokenProvider providerWithExpiredTokens = newProvider(-1);
        given(tenantProfileCache.get(tenantId)).willReturn(tenantProfile);

        // WHEN
        providerWithExpiredTokens.getToken(user);
        providerWithExpiredTokens.getToken(user);

        // THEN — the cached (expired) token is discarded and a fresh one is minted.
        then(tenantProfileCache).should(times(2)).get(tenantId);
    }

    @Test
    void shouldMintNewToken_whenClientAddressChanged() {
        // GIVEN
        given(tenantProfileCache.get(tenantId)).willReturn(tenantProfile);
        String first = provider.getToken(user);

        // WHEN — same user (same cache key) connects from a different address.
        user.setClientAddress("203.0.113.20");
        String second = provider.getToken(user);

        // THEN — the cached token is invalidated and re-minted with the new address.
        then(tenantProfileCache).should(times(2)).get(tenantId);
        assertThat(JWT.decode(first).getClaim("clientAddress").asString()).isEqualTo("198.51.100.10");
        assertThat(JWT.decode(second).getClaim("clientAddress").asString()).isEqualTo("203.0.113.20");
    }

    @Test
    void shouldCacheTokensPerUser_whenDifferentUsersRequestTokens() {
        // GIVEN
        SecurityUser otherUser = user(UUID.randomUUID(), tenantId, "other-admin@example.org", "198.51.100.11");
        given(tenantProfileCache.get(tenantId)).willReturn(tenantProfile);

        // WHEN
        String token = provider.getToken(user);
        String otherToken = provider.getToken(otherUser);

        // THEN — distinct cache entries, each minted independently.
        then(tenantProfileCache).should(times(2)).get(tenantId);
        assertThat(JWT.decode(token).getSubject()).isEqualTo(user.getUuidId().toString());
        assertThat(JWT.decode(otherToken).getSubject()).isEqualTo(otherUser.getUuidId().toString());
        assertThat(token).isNotEqualTo(otherToken);
    }

    // ---------------------------------------------------------------------------------------------
    // Error handling
    // ---------------------------------------------------------------------------------------------

    @Test
    void shouldThrowIllegalArgument_whenTenantProfileNotFound() {
        // GIVEN — tenantProfileCache returns null (mock default) for the user's tenant.

        // WHEN
        Throwable thrown = catchThrowable(() -> provider.getToken(user));

        // THEN
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenant profile not found for tenant id");
    }

    // ---------------------------------------------------------------------------------------------
    // Cache invalidation on lifecycle events
    // ---------------------------------------------------------------------------------------------

    @Test
    void shouldEvictUserTokens_whenTheirTenantProfileChanges() {
        // GIVEN
        given(tenantProfileCache.get(tenantId)).willReturn(tenantProfile);
        provider.getToken(user);

        // WHEN — the user's tenant profile is updated (rate limits live inside the signed token).
        provider.onComponentLifecycleEvent(
                new ComponentLifecycleMsg(tenantId, tenantProfileId, ComponentLifecycleEvent.UPDATED));

        // THEN — the cached token is evicted and the next request re-mints it.
        provider.getToken(user);
        then(tenantProfileCache).should(times(2)).get(tenantId);
    }

    @Test
    void shouldNotEvictTokens_whenAnUnrelatedTenantProfileChanges() {
        // GIVEN
        given(tenantProfileCache.get(tenantId)).willReturn(tenantProfile);
        provider.getToken(user);

        // WHEN — a different tenant profile changes.
        provider.onComponentLifecycleEvent(new ComponentLifecycleMsg(
                tenantId, new TenantProfileId(UUID.randomUUID()), ComponentLifecycleEvent.UPDATED));

        // THEN — this user's token stays cached.
        provider.getToken(user);
        then(tenantProfileCache).should(times(1)).get(tenantId);
    }

    @Test
    void shouldEvictUserTokens_whenTheirTenantIsDeleted() {
        // GIVEN
        given(tenantProfileCache.get(tenantId)).willReturn(tenantProfile);
        provider.getToken(user);

        // WHEN
        provider.onComponentLifecycleEvent(
                new ComponentLifecycleMsg(tenantId, tenantId, ComponentLifecycleEvent.DELETED));

        // THEN
        provider.getToken(user);
        then(tenantProfileCache).should(times(2)).get(tenantId);
    }

    @Test
    void shouldNotEvictTokens_whenTenantChangedButNotDeleted() {
        // GIVEN
        given(tenantProfileCache.get(tenantId)).willReturn(tenantProfile);
        provider.getToken(user);

        // WHEN — a non-DELETED tenant event must not invalidate cached tokens.
        provider.onComponentLifecycleEvent(
                new ComponentLifecycleMsg(tenantId, tenantId, ComponentLifecycleEvent.UPDATED));

        // THEN
        provider.getToken(user);
        then(tenantProfileCache).should(times(1)).get(tenantId);
    }

    @Test
    void shouldEvictUserToken_whenUserIsDeleted() {
        // GIVEN
        given(tenantProfileCache.get(tenantId)).willReturn(tenantProfile);
        provider.getToken(user);

        // WHEN
        provider.onComponentLifecycleEvent(
                new ComponentLifecycleMsg(tenantId, new UserId(userId), ComponentLifecycleEvent.DELETED));

        // THEN — the deleted user's cached token is evicted and the next request re-mints it.
        provider.getToken(user);
        then(tenantProfileCache).should(times(2)).get(tenantId);
    }

    @Test
    void shouldNotEvictUserToken_whenUserChangedButNotDeleted() {
        // GIVEN
        given(tenantProfileCache.get(tenantId)).willReturn(tenantProfile);
        provider.getToken(user);

        // WHEN
        provider.onComponentLifecycleEvent(
                new ComponentLifecycleMsg(tenantId, new UserId(userId), ComponentLifecycleEvent.UPDATED));

        // THEN
        provider.getToken(user);
        then(tenantProfileCache).should(times(1)).get(tenantId);
    }

    @Test
    void shouldIgnoreLifecycleEvent_forUnrelatedEntityType() {
        // GIVEN
        given(tenantProfileCache.get(tenantId)).willReturn(tenantProfile);
        provider.getToken(user);

        // WHEN — an entity type the provider does not care about.
        provider.onComponentLifecycleEvent(new ComponentLifecycleMsg(
                tenantId, new DeviceId(UUID.randomUUID()), ComponentLifecycleEvent.UPDATED));

        // THEN — nothing is evicted and the cached token is reused.
        provider.getToken(user);
        then(tenantProfileCache).should(times(1)).get(tenantId);
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private LocalTbAiTokenProvider newProvider(int expirationMinutes) {
        return new LocalTbAiTokenProvider(issuer, signingKey, expirationMinutes,
                cacheTtlMinutes, cacheMaxSize,
                systemSecurityService, tenantProfileCache);
    }

    private static SecurityUser user(UUID userId, TenantId tenantId, String email, String clientAddress) {
        SecurityUser user = new SecurityUser(new UserId(userId));
        user.setTenantId(tenantId);
        user.setCustomerId(new CustomerId(EntityId.NULL_UUID));
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setEmail(email);
        user.setClientAddress(clientAddress);
        return user;
    }

    private static TenantProfile tenantProfile(TenantProfileId tenantProfileId, String aiChatRequestsPerTenantRateLimit) {
        TenantProfile tenantProfile = new TenantProfile(tenantProfileId);
        DefaultTenantProfileConfiguration configuration =
                (DefaultTenantProfileConfiguration) tenantProfile.createDefaultTenantProfileData().getConfiguration();
        configuration.setAiChatRequestsPerTenantRateLimit(aiChatRequestsPerTenantRateLimit);
        return tenantProfile;
    }

    private static ProfileConfig profileConfigClaim(String token) {
        // The factory stores the profile config as a JSON string claim.
        String profileConfigJson = JWT.decode(token).getClaim("profileConfig").asString();
        return JacksonUtil.fromString(profileConfigJson, ProfileConfig.class);
    }

    private void assertThatTokenIsSignedWithConfiguredKey(String token) {
        JWT.require(Algorithm.HMAC512(Base64.getDecoder().decode(signingKey)))
                .withIssuer(issuer)
                .build()
                .verify(token);
    }

}
