// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thingsboard.ai.common.data.usage.ApiUsageInfo;
import org.thingsboard.ai.common.data.usage.Resource;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.setup.LicenseChangeResult;
import org.thingsboard.server.common.data.setup.LicenseClaimInfo;
import org.thingsboard.server.common.data.setup.LicenseClaimMode;
import org.thingsboard.server.common.data.setup.LicenseClaimResult;
import org.thingsboard.server.common.data.setup.LicenseClaimStatus;
import org.thingsboard.server.common.data.setup.SetupInfo;
import org.thingsboard.server.common.data.setup.SystemSetupState;
import org.thingsboard.server.common.data.subscription.SubscriptionErrorCode;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.service.ai.TbAiService;
import org.thingsboard.server.service.license.NonProductionConfirmationService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.setup.SystemSetupService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class SystemSetupControllerUnitTest {

    /**
     * Carries a query string, because the produced URL always does - the service appends the cluster identifier
     * and the claim token to it - and the wizard appends its own parameter with an ampersand. A query-less
     * fixture would document a shape production never emits and a consumer would turn into a broken link.
     */
    private static final String CLAIM_TOKEN = "fd1c0c9d6f2f4f0a9e2f1c7b3a5d8e40";
    private static final String SIGN_UP_URL = "https://license.thingsboard.io/declareLicense"
            + "?clusterId=cbb0ea36-3d5f-4b0e-9f5a-4a3d3b8f5f21&claimToken=" + CLAIM_TOKEN;

    @Mock
    private SystemSetupService systemSetupService;
    @Mock
    private NonProductionConfirmationService nonProductionConfirmationService;
    @Mock
    private TbAiService aiService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        SystemSetupController controller = new SystemSetupController(systemSetupService,
                nonProductionConfirmationService);
        // The error handler that BaseController delegates its @ExceptionHandler methods to is normally
        // autowired. It is set by hand here on purpose: the claim poll's contract with the setup wizard is
        // expressed entirely in HTTP status codes, so a test of that contract has to exercise the same mapping
        // from ThingsboardErrorCode to status that the running application uses.
        ReflectionTestUtils.setField(controller, "errorResponseHandler", new ThingsboardErrorResponseHandler());
        // The collaborator BaseController.enrichSubscriptionInfo reads, set the same way and for the same
        // reason: this standalone setup autowires nothing.
        ReflectionTestUtils.setField(controller, "aiService", aiService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void completeSetupPassesTheRequestBodyToTheServiceWhenAccountRequired() throws Exception {
        when(systemSetupService.getState()).thenReturn(SystemSetupState.ACCOUNT_REQUIRED);

        mockMvc.perform(post("/api/noauth/setup/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"sysadmin@thingsboard.org\",\"password\":\"StrongPass1\",\"loadDemo\":true}"))
                .andExpect(status().isOk());

        verify(systemSetupService).completeSetup("sysadmin@thingsboard.org", "StrongPass1", true);
    }

    @Test
    public void applySetupLicenseDelegatesToServiceAndReturnsNextState() throws Exception {
        when(systemSetupService.getState()).thenReturn(SystemSetupState.LICENSE_REQUIRED);
        when(systemSetupService.getSetupInfo()).thenReturn(new SetupInfo(SystemSetupState.ACCOUNT_REQUIRED));

        mockMvc.perform(post("/api/noauth/setup/license")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"secret\":\"SECRET\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCOUNT_REQUIRED"));

        verify(systemSetupService).applyLicenseKey("SECRET");
    }

    /**
     * The next state is whatever the service reports, READY included - the controller neither computes it nor
     * second-guesses it. The rule that an instance which already has a sysadmin reports READY after its licence
     * is applied is a service-level one, pinned where the state is computed rather than stubbed, in
     * DefaultSystemSetupServiceTest#runtimeReLockReportsLicenseRequiredEvenWhenSysadminAlreadyExists.
     */
    @Test
    public void applySetupLicenseEchoesBackTheNextStateReportedByTheService() throws Exception {
        when(systemSetupService.getState()).thenReturn(SystemSetupState.LICENSE_REQUIRED);
        when(systemSetupService.getSetupInfo()).thenReturn(new SetupInfo(SystemSetupState.READY));

        mockMvc.perform(post("/api/noauth/setup/license")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"secret\":\"SECRET\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));

        verify(systemSetupService).applyLicenseKey("SECRET");
    }

    /**
     * The sysadmin-only key change hands the secret straight to the service and answers with the resulting
     * setup state, so a system administrator recovering a locked instance learns in one round trip whether the
     * key lifted the lock. Pinned here rather than in the integration test, where the bean the test profile
     * supplies accepts any key silently, so a 200 there says nothing about what the controller did with the
     * body. The authority check is not exercised by this standalone setup, and is pinned by
     * SystemSetupControllerTest#changeLicenseKeyIsSysAdminOnly.
     */
    @Test
    public void changeLicenseKeyPassesTheSecretToTheServiceAndReturnsTheResultingState() throws Exception {
        // Stubbed rather than left to answer null: an unstubbed result yields an empty 200 body, which a
        // status assertion would pass over in silence while the caller learned nothing about the lock.
        when(systemSetupService.applyLicenseKeyAndReport("SECRET"))
                .thenReturn(new LicenseChangeResult(SystemSetupState.READY, null));

        mockMvc.perform(post("/api/admin/license/key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"secret\":\"SECRET\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));

        verify(systemSetupService).applyLicenseKeyAndReport("SECRET");
    }

    @Test
    public void changeLicenseKeyEnrichesTheLicenceItLeftInForce() throws Exception {
        // The body carries a licence, so it has to carry the AI credits already spent - the one usage figure
        // only this deployment can answer - or the page shows them blank until reloaded.
        SubscriptionInfo subscriptionInfo = new SubscriptionInfo();
        when(systemSetupService.applyLicenseKeyAndReport("SECRET"))
                .thenReturn(new LicenseChangeResult(SystemSetupState.READY, subscriptionInfo));
        authenticate();

        try {
            mockMvc.perform(post("/api/admin/license/key")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"secret\":\"SECRET\"}"))
                    .andExpect(status().isOk());
        } finally {
            SecurityContextHolder.clearContext();
        }

        // Asserted against a stubbed usage record that differs from a default-constructed SubscriptionInfo,
        // so an enrich that never ran fails here.
        assertThat(subscriptionInfo.getUsedAiCredits()).isEqualTo(1200L);
        // The grant is the licence's own, read off plan data by the mapper. The AI service's limit - which is
        // tenant-scoped, and 0 whenever that remote call degrades - must not overwrite it.
        assertThat(subscriptionInfo.getMaxAiCredits()).isZero();
    }

    @Test
    public void previewLicenseKeyAnswersWithTheLicenceTheKeyWouldInstall() throws Exception {
        // Nothing is applied: the endpoint exists so an administrator can see what they are about to switch
        // to, and the only service call it may make is the preview.
        SubscriptionInfo preview = new SubscriptionInfo();
        preview.setMaxDevices(109000L);
        preview.setMaxInstances(21L);
        preview.setMaxAiCredits(3_000_000L);
        when(systemSetupService.previewLicenseKey("SECRET")).thenReturn(preview);

        mockMvc.perform(post("/api/admin/license/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"secret\":\"SECRET\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxDevices").value(109000))
                .andExpect(jsonPath("$.maxInstances").value(21))
                // The previewed key's own AI grant, read off its plan data and left alone here.
                .andExpect(jsonPath("$.maxAiCredits").value(3000000))
                // Deliberately NOT enriched: credits already spent were spent against the outgoing licence,
                // so they are no part of what this key would install. Re-adding that call fails here.
                .andExpect(jsonPath("$.usedAiCredits").value(0));

        verifyNoInteractions(aiService);
        verify(systemSetupService, never()).applyLicenseKey(anyString());
        verify(systemSetupService, never()).applyLicenseKeyAndReport(anyString());
    }

    @Test
    public void aKeyRefusedDuringSetupIsAnsweredWithABadRequestCarryingTheReason() throws Exception {
        // The setup wizard is the screen most keys are pasted into, and it gets the same seven sentences the
        // admin endpoints do. Delivering them under a 500 would read as a server fault rather than as a key
        // the operator has to replace, and this endpoint's own contract promises only 200 and 400.
        when(systemSetupService.getState()).thenReturn(SystemSetupState.LICENSE_REQUIRED);
        doThrow(new SubscriptionException("That license has no available instances.",
                SubscriptionErrorCode.FEATURE_DISABLED))
                .when(systemSetupService).applyLicenseKey("SECRET");

        mockMvc.perform(post("/api/noauth/setup/license")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"secret\":\"SECRET\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("That license has no available instances."));
    }

    @Test
    public void anAbsentSecretIsAnsweredWithABadRequestOnEveryKeyEndpoint() throws Exception {
        // The licence layer refuses an empty secret with an IllegalArgumentException, which is the caller's
        // mistake and not a server fault. BaseController's Exception handler already maps that to
        // BAD_REQUEST_PARAMS, so none of the three endpoints needs a catch of its own - pinned here because
        // that is easy to lose by narrowing the handler or by declaring one closer to these methods.
        when(systemSetupService.getState()).thenReturn(SystemSetupState.LICENSE_REQUIRED);
        doThrow(new IllegalArgumentException("License secret must not be empty"))
                .when(systemSetupService).applyLicenseKey(null);
        when(systemSetupService.previewLicenseKey(null))
                .thenThrow(new IllegalArgumentException("License secret must not be empty"));
        when(systemSetupService.applyLicenseKeyAndReport(null))
                .thenThrow(new IllegalArgumentException("License secret must not be empty"));

        for (String path : new String[]{"/api/noauth/setup/license", "/api/admin/license/preview", "/api/admin/license/key"}) {
            mockMvc.perform(post(path)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("License secret must not be empty"));
        }
    }

    @Test
    public void aRefusedKeyIsAnsweredWithABadRequestCarryingTheReason() throws Exception {
        // The operator-facing sentence is the whole point of the refusal, so it has to survive the mapping -
        // and it has to arrive as a 400, since the caller is meant to read it and try another key.
        when(systemSetupService.previewLicenseKey("SECRET"))
                .thenThrow(new SubscriptionException("That license has no available instances.",
                        SubscriptionErrorCode.FEATURE_DISABLED));

        mockMvc.perform(post("/api/admin/license/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"secret\":\"SECRET\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("That license has no available instances."));
    }

    /**
     * Puts a signed-in system administrator in the security context, which {@code getCurrentUser()} reads and
     * this standalone MockMvc setup does not otherwise populate.
     */
    private SecurityUser authenticate() {
        SecurityUser user = new SecurityUser();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, "token"));
        when(aiService.getApiUsageInfo(user)).thenReturn(new ApiUsageInfo(
                Map.of(Resource.AI_CREDITS, new ApiUsageInfo.ResourceUsage(1200L, 50000)), 0));
        return user;
    }

    @Test
    public void requestClaimDelegatesToServiceWhileLicenseIsRequired() throws Exception {
        when(systemSetupService.getState()).thenReturn(SystemSetupState.LICENSE_REQUIRED);
        when(systemSetupService.requestClaim()).thenReturn(new LicenseClaimResult(SIGN_UP_URL, LicenseClaimMode.ONLINE, CLAIM_TOKEN));

        // The sign-up URL is the whole point of the response: the operator cannot start the claim without it.
        // The token beside it is what the client names on every poll, so that a second session starting its own
        // activation is told its link is dead rather than left watching the claim that replaced it.
        mockMvc.perform(post("/api/noauth/setup/claim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signUpUrl").value(SIGN_UP_URL))
                .andExpect(jsonPath("$.mode").value("ONLINE"))
                .andExpect(jsonPath("$.claimToken").value(CLAIM_TOKEN));

        verify(systemSetupService).requestClaim();
    }

    @Test
    public void getClaimDelegatesToServiceAndIsNotGuardedBySetupState() throws Exception {
        when(systemSetupService.pollClaim(null)).thenReturn(new LicenseClaimInfo(LicenseClaimStatus.PENDING));

        // Omitting the claim token is what a client written before the parameter existed does, and it has to
        // reach the service as no token at all rather than as an empty one.
        mockMvc.perform(get("/api/noauth/setup/claim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(systemSetupService).pollClaim(null);
        // No checkSetupState: the successful poll is itself what advances the state, so guarding on
        // LICENSE_REQUIRED would make the wizard's own confirming poll fail with a 400.
        verify(systemSetupService, never()).getState();
    }

    @Test
    public void getClaimHandsTheCallersOwnClaimTokenToTheService() throws Exception {
        when(systemSetupService.pollClaim(CLAIM_TOKEN)).thenReturn(new LicenseClaimInfo(LicenseClaimStatus.EXPIRED));

        // Without the parameter reaching the service the poll reports on whatever claim is stored, which is
        // the other session's - and the caller on the superseded link waits on PENDING for good.
        mockMvc.perform(get("/api/noauth/setup/claim").param("claimToken", CLAIM_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));

        verify(systemSetupService).pollClaim(CLAIM_TOKEN);
    }

    /**
     * The three tests below pin what the setup wizard branches on over {@code GET /api/noauth/setup/claim}. The
     * split is not between statuses but between a status and an error: the end of the automatic path is
     * reported as {@link LicenseClaimStatus#EXPIRED} on a 200, and every error status this endpoint emits means the
     * claim token is still stored and the wizard must keep polling with a backoff. So there is deliberately no
     * terminal-4xx branch to pin - see {@code DefaultSystemSetupService#toThingsboardException}, which has no
     * arm that maps onto one.
     */
    @Test
    public void getClaimReportsARefusedClaimAsAStatusRatherThanAnError() throws Exception {
        when(systemSetupService.pollClaim(null)).thenReturn(new LicenseClaimInfo(LicenseClaimStatus.EXPIRED));

        mockMvc.perform(get("/api/noauth/setup/claim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    @Test
    public void getClaimReturnsTooManyRequestsWhenThrottledSoTheWizardKeepsPolling() throws Exception {
        when(systemSetupService.pollClaim(null))
                .thenThrow(new ThingsboardException("Too many attempts", ThingsboardErrorCode.TOO_MANY_REQUESTS));

        mockMvc.perform(get("/api/noauth/setup/claim"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    public void getClaimReturnsInternalServerErrorOnRetryableFailureSoTheWizardKeepsPolling() throws Exception {
        when(systemSetupService.pollClaim(null))
                .thenThrow(new ThingsboardException("Could not reach the license server", ThingsboardErrorCode.GENERAL));

        mockMvc.perform(get("/api/noauth/setup/claim"))
                .andExpect(status().isInternalServerError());
    }

}
