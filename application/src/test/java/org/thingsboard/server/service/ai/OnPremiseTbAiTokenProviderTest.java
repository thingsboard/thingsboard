// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
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
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OnPremiseTbAiTokenProviderTest {

    private final String aiToken = "on-premise-ai-token";
    private final String coreBaseUrl = "https://tb.example.org";
    private final String resolvedBaseUrl = "https://resolved.example.org";
    private final String localhostBaseUrl = "http://localhost:8080";
    private final String tbVersion = "4.3.0-AI-TEST";

    @Mock
    SubscriptionService subscriptionService;
    @Mock
    SystemSecurityService systemSecurityService;
    @Mock
    ProjectInfo projectInfo;

    UUID userId;
    TenantId tenantId;
    SecurityUser user;

    // Default provider: a configured core base URL, so origin resolution does not touch SystemSecurityService.
    OnPremiseTbAiTokenProvider provider;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        user = user(userId, tenantId);
        provider = newProvider(coreBaseUrl);
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
    void shouldBuildHeadersWithConfiguredCoreBaseUrlAsOrigin_whenCoreBaseUrlIsSet() {
        // GIVEN
        given(projectInfo.getProjectVersion()).willReturn(tbVersion);

        // WHEN
        Map<String, String> additionalInfo = provider.getAdditionalInfo(user);

        // THEN
        assertThat(additionalInfo).containsExactlyInAnyOrderEntriesOf(expectedHeaders(coreBaseUrl));
        // Origin came from the configured core base URL, so the security service must not be consulted.
        then(systemSecurityService).shouldHaveNoInteractions();
    }

    @Test
    void shouldResolveOriginViaSecurityService_whenCoreBaseUrlIsBlank() {
        // GIVEN
        OnPremiseTbAiTokenProvider providerWithoutCoreUrl = newProvider("");
        given(projectInfo.getProjectVersion()).willReturn(tbVersion);
        given(systemSecurityService.getBaseUrl(user.getAuthority(), user.getTenantId(), user.getCustomerId(), null))
                .willReturn(resolvedBaseUrl);

        // WHEN
        Map<String, String> additionalInfo = providerWithoutCoreUrl.getAdditionalInfo(user);

        // THEN
        assertThat(additionalInfo).containsExactlyInAnyOrderEntriesOf(expectedHeaders(resolvedBaseUrl));
    }

    @Test
    void shouldAcceptLocalhostOrigin_whenBuildingUserHeaders() {
        // GIVEN — no configured core URL and the security service falls back to a localhost base URL.
        OnPremiseTbAiTokenProvider providerWithoutCoreUrl = newProvider("");
        given(projectInfo.getProjectVersion()).willReturn(tbVersion);
        given(systemSecurityService.getBaseUrl(user.getAuthority(), user.getTenantId(), user.getCustomerId(), null))
                .willReturn(localhostBaseUrl);

        // WHEN
        Map<String, String> additionalInfo = providerWithoutCoreUrl.getAdditionalInfo(user);

        // THEN — calls that TB AI does not answer by calling TB back work without a public base URL.
        assertThat(additionalInfo).containsExactlyInAnyOrderEntriesOf(expectedHeaders(localhostBaseUrl));
    }

    @Test
    void shouldThrowBadRequest_whenValidatingCallbackOriginAndResolvedOriginIsLocalhost() {
        // GIVEN
        OnPremiseTbAiTokenProvider providerWithoutCoreUrl = newProvider("");
        given(systemSecurityService.getBaseUrl(user.getAuthority(), user.getTenantId(), user.getCustomerId(), null))
                .willReturn(localhostBaseUrl);

        // WHEN
        Throwable thrown = catchThrowable(() -> providerWithoutCoreUrl.validateCallbackOrigin(user));

        // THEN
        assertThat(thrown)
                .isInstanceOf(ThingsboardRuntimeException.class)
                .hasMessageContaining("Please configure the base URL");
        assertThat(((ThingsboardRuntimeException) thrown).getErrorCode())
                .isEqualTo(ThingsboardErrorCode.BAD_REQUEST_PARAMS);
    }

    @Test
    void shouldAcceptCallbackOrigin_whenResolvedOriginIsPublic() {
        // GIVEN
        OnPremiseTbAiTokenProvider providerWithoutCoreUrl = newProvider("");
        given(systemSecurityService.getBaseUrl(user.getAuthority(), user.getTenantId(), user.getCustomerId(), null))
                .willReturn(resolvedBaseUrl);

        // WHEN-THEN
        providerWithoutCoreUrl.validateCallbackOrigin(user);
    }

    @Test
    void shouldAcceptCallbackOrigin_whenCoreBaseUrlIsSet() {
        // WHEN-THEN
        provider.validateCallbackOrigin(user);
        then(systemSecurityService).shouldHaveNoInteractions();
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
    void shouldResolveIdBasedOriginFromGeneralSettings_whenCoreBaseUrlIsSet() {
        // GIVEN — the configured core base URL and the white-labeling chain are both ignored for background tasks.
        given(projectInfo.getProjectVersion()).willReturn(tbVersion);
        given(systemSecurityService.getBaseUrl(tenantId, null, null)).willReturn(resolvedBaseUrl);

        // WHEN
        Map<String, String> additionalInfo = provider.getAdditionalInfo(tenantId, new UserId(userId));

        // THEN
        assertThat(additionalInfo).containsExactlyInAnyOrderEntriesOf(expectedHeaders(resolvedBaseUrl));
        then(systemSecurityService).should(never()).getBaseUrl(any(Authority.class), any(), any(), any());
    }

    @Test
    void shouldAcceptLocalhostOrigin_whenBuildingIdBasedHeaders() {
        // GIVEN — unlike the user-facing path, background deletion must not fail on an unconfigured base URL.
        given(projectInfo.getProjectVersion()).willReturn(tbVersion);
        given(systemSecurityService.getBaseUrl(tenantId, null, null)).willReturn(localhostBaseUrl);

        // WHEN
        Map<String, String> additionalInfo = provider.getAdditionalInfo(tenantId, new UserId(userId));

        // THEN
        assertThat(additionalInfo).containsExactlyInAnyOrderEntriesOf(expectedHeaders(localhostBaseUrl));
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private Map<String, String> expectedHeaders(String origin) {
        return Map.of(
                TbAiHeaders.USER_ID, userId.toString(),
                TbAiHeaders.TENANT_ID, tenantId.getId().toString(),
                TbAiHeaders.TB_VERSION, tbVersion,
                HttpHeaders.ORIGIN, origin
        );
    }

    private OnPremiseTbAiTokenProvider newProvider(String coreBaseUrl) {
        return new OnPremiseTbAiTokenProvider(subscriptionService, systemSecurityService, projectInfo, coreBaseUrl);
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
