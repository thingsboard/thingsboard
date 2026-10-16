// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.common.client.TbAiHeaders;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.service.install.ProjectInfo;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OnPremiseTbAiTokenProviderTest {

    private final String aiToken = "on-premise-ai-token";
    private final String tbVersion = "4.3.0-AI-TEST";

    @Mock
    SubscriptionService subscriptionService;
    @Mock
    ProjectInfo projectInfo;

    UUID userId;
    TenantId tenantId;
    SecurityUser user;

    OnPremiseTbAiTokenProvider provider;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        user = user(userId, tenantId);
        provider = new OnPremiseTbAiTokenProvider(subscriptionService, projectInfo);
    }

    // ---------------------------------------------------------------------------------------------
    // Token retrieval
    // ---------------------------------------------------------------------------------------------

    @Test
    void shouldReturnSubscriptionToken_whenSubscriptionProvidesToken() {
        // GIVEN
        given(subscriptionService.getAiToken()).willReturn(aiToken);

        // WHEN
        String token = provider.getToken(user);

        // THEN — the token is taken verbatim from the subscription, not minted locally.
        assertThat(token).isEqualTo(aiToken);
    }

    @Test
    void shouldThrowPermissionDenied_whenSubscriptionHasNoToken() {
        // GIVEN — the subscription plan does not grant an AI token.
        given(subscriptionService.getAiToken()).willReturn(null);

        // WHEN
        Throwable thrown = catchThrowable(() -> provider.getToken(user));

        // THEN
        assertThat(thrown)
                .isInstanceOf(ThingsboardRuntimeException.class)
                .hasMessageContaining("AI is not available for the current subscription plan");
        assertThat(((ThingsboardRuntimeException) thrown).getErrorCode())
                .isEqualTo(ThingsboardErrorCode.PERMISSION_DENIED);
    }

    // ---------------------------------------------------------------------------------------------
    // Additional info (request headers)
    // ---------------------------------------------------------------------------------------------

    @Test
    void shouldBuildHeadersWithoutOrigin_whenCalledForUser() {
        // GIVEN
        given(projectInfo.getProjectVersion()).willReturn(tbVersion);

        // WHEN
        Map<String, String> additionalInfo = provider.getAdditionalInfo(user);

        // THEN — TB AI takes the origin from the channel's hello and start frames, never from the handshake.
        assertThat(additionalInfo).containsExactlyInAnyOrderEntriesOf(expectedHeaders());
    }

    // ---------------------------------------------------------------------------------------------
    // Id-based access (background housekeeper deletion tasks)
    // ---------------------------------------------------------------------------------------------

    @Test
    void shouldReportTokenAvailable_whenSubscriptionProvidesToken() {
        // GIVEN
        given(subscriptionService.getAiToken()).willReturn(aiToken);

        // WHEN & THEN
        assertThat(provider.isTokenAvailable()).isTrue();
    }

    @Test
    void shouldReportTokenUnavailable_whenSubscriptionHasNoToken() {
        // GIVEN
        given(subscriptionService.getAiToken()).willReturn(null);

        // WHEN & THEN
        assertThat(provider.isTokenAvailable()).isFalse();
    }

    @Test
    void shouldReturnSubscriptionToken_whenCalledWithRawIds() {
        // GIVEN
        given(subscriptionService.getAiToken()).willReturn(aiToken);

        // WHEN
        String token = provider.getToken(tenantId, new UserId(userId));

        // THEN
        assertThat(token).isEqualTo(aiToken);
    }

    @Test
    void shouldThrowIllegalState_whenCalledWithRawIdsAndSubscriptionHasNoToken() {
        // GIVEN
        given(subscriptionService.getAiToken()).willReturn(null);

        // WHEN
        Throwable thrown = catchThrowable(() -> provider.getToken(tenantId, new UserId(userId)));

        // THEN — unlike the user-facing path, this is a background-only race guard, not a permission error.
        assertThat(thrown)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AI token is not available");
    }

    @Test
    void shouldBuildHeadersWithoutOrigin_whenCalledWithRawIds() {
        // GIVEN
        given(projectInfo.getProjectVersion()).willReturn(tbVersion);

        // WHEN
        Map<String, String> additionalInfo = provider.getAdditionalInfo(tenantId, new UserId(userId));

        // THEN
        assertThat(additionalInfo).containsExactlyInAnyOrderEntriesOf(expectedHeaders());
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private Map<String, String> expectedHeaders() {
        return Map.of(
                TbAiHeaders.USER_ID, userId.toString(),
                TbAiHeaders.TENANT_ID, tenantId.getId().toString(),
                TbAiHeaders.TB_VERSION, tbVersion
        );
    }

    private static SecurityUser user(UUID userId, TenantId tenantId) {
        SecurityUser user = new SecurityUser(new UserId(userId));
        user.setTenantId(tenantId);
        user.setCustomerId(new CustomerId(EntityId.NULL_UUID));
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setEmail("tenant-admin@example.org");
        return user;
    }

}
