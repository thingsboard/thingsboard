// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.community_grant.CommunityGrantFlowState;
import org.thingsboard.server.common.data.community_grant.CommunityGrantMode;
import org.thingsboard.server.common.data.community_grant.CommunityGrantOfflineRun;
import org.thingsboard.server.common.data.community_grant.CommunityGrantState;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.community_grant.CommunityGrantBundleFixtures;
import org.thingsboard.server.service.community_grant.CommunityGrantCheckerInput;
import org.thingsboard.server.service.community_grant.CommunityGrantFlowStateStore;
import org.thingsboard.server.service.community_grant.CommunityGrantOfflineReportStore;
import org.thingsboard.server.service.community_grant.CommunityGrantPlatform;
import org.thingsboard.server.service.community_grant.CommunityGrantPoller;
import org.thingsboard.server.service.community_grant.CommunityGrantPortalClient;
import org.thingsboard.server.service.community_grant.CommunityGrantPortalStatus;
import org.thingsboard.server.service.community_grant.CommunityGrantReportRunner;
import org.thingsboard.server.service.community_grant.CommunityGrantService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class CommunityGrantControllerTest extends AbstractControllerTest {

    private static final List<String> ALLOWED_FIELDS =
            List.of("mode", "state", "signUpUrl", "lastPolledAt", "parkReason", "portalUrl", "offlineReport",
                    "offlineRunInProgress", "offlineRunError");
    private static final String REPORT = "-----BEGIN TB INSTANCE CHECK-----\nbody";
    private static final String BUNDLE_FILE_NAME = "tb-instance-check-bundle_"
            + CommunityGrantPlatform.current().os() + "_" + CommunityGrantPlatform.current().arch() + ".tbicb";
    private static final long EXPIRED_ISSUED_AT = 1609459200000L;
    private static final long EXPIRED_EXPIRES_AT = 1609545600000L;
    // Mirror package-private constants of DefaultCommunityGrantService.
    private static final String NOT_ALREADY_REGISTERED_MESSAGE =
            "This action is only available once this deployment is confirmed as already registered under another account.";
    private static final String NOTHING_TO_HAND_OVER_MESSAGE =
            "This deployment is not waiting for a check result to be handed over.";

    @Autowired
    private AuditLogService auditLogService;
    @Autowired
    private CommunityGrantFlowStateStore flowStateStore;
    @Autowired
    private CommunityGrantPoller poller;
    @Autowired
    private CommunityGrantService communityGrantService;
    @Autowired
    private CommunityGrantOfflineReportStore offlineReportStore;
    @MockitoSpyBean
    private CommunityGrantPortalClient portalClient;
    @MockitoSpyBean
    private CommunityGrantReportRunner reportRunner;

    @Test
    public void testStateIsNotStartedBeforeAnyAction() throws Exception {
        loginSysAdmin();

        JsonNode state = doGetTyped("/api/communityGrant/state", new TypeReference<JsonNode>() {});

        assertThat(state.get("state").asText()).isEqualTo(CommunityGrantState.NOT_STARTED.name());
        assertThat(state.get("mode").asText()).isEqualTo("ONLINE");
        assertThat(state.get("signUpUrl").isNull()).isTrue();
        assertThat(state.get("lastPolledAt").isNull()).isTrue();
    }

    @Test
    public void testStateExposesNothingBeyondTheContract() throws Exception {
        loginSysAdmin();

        ResultActions result = doGet("/api/communityGrant/state").andExpect(status().isOk());
        String body = result.andReturn().getResponse().getContentAsString();
        JsonNode state = JacksonUtil.toJsonNode(body);

        state.fieldNames().forEachRemaining(field -> assertThat(field).isIn(ALLOWED_FIELDS));
        assertThat(state.get("portalUrl").asText()).isNotBlank();
    }

    @Test
    public void testStateRequiresAuthentication() throws Exception {
        loginSysAdmin();
        TimeUnit.SECONDS.sleep(1); //We need to make sure that event for invalidating token was successfully processed
        logout();
        doGet("/api/communityGrant/state").andExpect(status().isUnauthorized());
    }

    @Test
    public void testStateIsForbiddenForATenantAdmin() throws Exception {
        loginTenantAdmin();
        doGet("/api/communityGrant/state").andExpect(status().isForbidden());
    }

    @Test
    public void testStartIsForbiddenForATenantAdmin() throws Exception {
        loginTenantAdmin();
        doPost("/api/communityGrant/start").andExpect(status().isForbidden());
    }

    @Test
    public void testOfflineReportIsForbiddenForATenantAdmin() throws Exception {
        loginTenantAdmin();
        doGet("/api/communityGrant/offline/report").andExpect(status().isForbidden());
    }

    @Test
    public void testOfflineReportIsDownloadedAsAPlainTextAttachment() throws Exception {
        loginSysAdmin();
        offlineReportStore.put(REPORT);
        try {
            doGet("/api/communityGrant/offline/report")
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"tb-instance-check-report.txt\""))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string(REPORT));
        } finally {
            // The stored report outlives the test, and other tests expect none.
            offlineReportStore.clear();
        }
    }

    @Test
    public void testOfflineReportIsServedFromTheDatabaseRow() throws Exception {
        loginSysAdmin();
        offlineReportStore.put(REPORT);
        try {
            String served = doGet("/api/communityGrant/offline/report").andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(served).isEqualTo(REPORT);
            Mockito.verifyNoInteractions(reportRunner);
            // This node also holds a local copy, so the row itself is checked.
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT json_value::json ->> 'report' FROM admin_settings WHERE key = 'communityGrantReport'",
                    String.class)).isEqualTo(REPORT);
        } finally {
            offlineReportStore.clear();
        }
    }

    @Test
    public void testStateCarriesTheReportOnTheStepThatShowsIt() throws Exception {
        loginSysAdmin();
        offlineReportStore.put(REPORT);
        try {
            flowStateStore.update(state -> {
                state.setState(CommunityGrantState.VALIDATING);
                state.setLastOfflineCheckAt(System.currentTimeMillis());
                return state;
            });

            JsonNode state = doGetTyped("/api/communityGrant/state", new TypeReference<JsonNode>() {});

            assertThat(state.get("offlineReport").asText()).isEqualTo(REPORT);
        } finally {
            offlineReportStore.clear();
            flowStateStore.update(state -> CommunityGrantFlowState.initial());
        }
    }

    @Test
    public void testStateCarriesNoReportBeforeAnyCheckHasRun() throws Exception {
        loginSysAdmin();

        JsonNode state = doGetTyped("/api/communityGrant/state", new TypeReference<JsonNode>() {});

        assertThat(state.get("offlineReport").isNull()).isTrue();
    }

    @Test
    public void testOfflineReportIsNotFoundBeforeAnyCheckHasRun() throws Exception {
        loginSysAdmin();

        doGet("/api/communityGrant/offline/report").andExpect(status().isNotFound());
    }

    @Test
    public void testOfflineHandoffIsForbiddenForATenantAdmin() throws Exception {
        loginTenantAdmin();
        doPost("/api/communityGrant/offline/handoff").andExpect(status().isForbidden());
    }

    @Test
    public void testOfflineHandoffClaimsTheReportAndIsAudited() throws Exception {
        loginSysAdmin();
        User currentUser = doGet("/api/auth/user", User.class);
        offlineReportStore.put(REPORT);
        try {
            flowStateStore.update(state -> {
                state.setMode(CommunityGrantMode.OFFLINE);
                state.setState(CommunityGrantState.VALIDATING);
                state.setLastOfflineCheckAt(System.currentTimeMillis());
                return state;
            });

            JsonNode state = JacksonUtil.toJsonNode(doPost("/api/communityGrant/offline/handoff")
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

            assertThat(state.get("state").asText()).isEqualTo(CommunityGrantState.REPORT_HANDED_OVER.name());
            assertThat(state.get("offlineReport").asText()).isEqualTo(REPORT);
            assertThat(flowStateStore.get().getState()).isEqualTo(CommunityGrantState.REPORT_HANDED_OVER);
            assertSingleAuditLog(currentUser, ActionType.COMMUNITY_GRANT_ENROLLMENT, ActionStatus.SUCCESS);
        } finally {
            offlineReportStore.clear();
            flowStateStore.update(state -> CommunityGrantFlowState.initial());
        }
    }

    @Test
    public void testOfflineHandoffRefusalIsAudited() throws Exception {
        loginSysAdmin();
        User currentUser = doGet("/api/auth/user", User.class);

        doPost("/api/communityGrant/offline/handoff").andExpect(status().isBadRequest());

        assertThat(assertSingleAuditLog(currentUser, ActionType.COMMUNITY_GRANT_ENROLLMENT, ActionStatus.FAILURE)
                .getActionFailureDetails()).contains(NOTHING_TO_HAND_OVER_MESSAGE);
    }

    @Test
    public void testRequestAccessIsForbiddenForATenantAdmin() throws Exception {
        loginTenantAdmin();
        doPost("/api/communityGrant/requestAccess").andExpect(status().isForbidden());
    }

    // A well-formed bundle, so the 403 comes from @PreAuthorize.
    @Test
    public void testOfflineRunIsForbiddenForATenantAdmin() throws Exception {
        loginTenantAdmin();

        mockMvc.perform(offlineRunRequest(bundleForThisHost(CommunityGrantBundleFixtures.relsig())))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testRequestAccessRefusalIsAuditedWhenNotAlreadyRegistered() throws Exception {
        loginSysAdmin();
        User currentUser = doGet("/api/auth/user", User.class);

        doPost("/api/communityGrant/requestAccess").andExpect(status().isBadRequest());

        String failureDetails = assertSingleAuditLog(currentUser, ActionType.COMMUNITY_GRANT_REQUEST_ACCESS,
                ActionStatus.FAILURE).getActionFailureDetails();
        // The column carries a whole stack trace, so the refusal message is asserted rather than absent substrings.
        assertThat(failureDetails).contains(NOT_ALREADY_REGISTERED_MESSAGE);
    }

    // Neither tb_cluster nor the admin_settings row is reset between tests, and DaoSqlTest does not seed tb_cluster.
    @Test
    public void testRequestAccessSuccessIsAudited() throws Exception {
        loginSysAdmin();
        User currentUser = doGet("/api/auth/user", User.class);
        boolean seededClusterId = Boolean.TRUE.equals(
                jdbcTemplate.queryForObject("SELECT NOT EXISTS (SELECT 1 FROM tb_cluster)", Boolean.class));
        try {
            if (seededClusterId) {
                jdbcTemplate.update("INSERT INTO tb_cluster (cluster_id) VALUES (?::uuid)", UUID.randomUUID().toString());
            }
            flowStateStore.update(state -> {
                state.setMode(CommunityGrantMode.ONLINE);
                state.setState(CommunityGrantState.ALREADY_REGISTERED);
                return state;
            });
            Mockito.doNothing().when(portalClient).requestAccess(any());

            doPost("/api/communityGrant/requestAccess").andExpect(status().isOk());

            assertSingleAuditLog(currentUser, ActionType.COMMUNITY_GRANT_REQUEST_ACCESS, ActionStatus.SUCCESS);
            Mockito.verify(portalClient).requestAccess(any());
        } finally {
            flowStateStore.update(state -> CommunityGrantFlowState.initial());
            if (seededClusterId) {
                jdbcTemplate.update("DELETE FROM tb_cluster");
            }
        }
    }

    @Test
    public void testStartIsDrivenToATerminalStateByTheRealPoller() throws Exception {
        loginSysAdmin();
        boolean seededClusterId = Boolean.TRUE.equals(
                jdbcTemplate.queryForObject("SELECT NOT EXISTS (SELECT 1 FROM tb_cluster)", Boolean.class));
        try {
            if (seededClusterId) {
                jdbcTemplate.update("INSERT INTO tb_cluster (cluster_id) VALUES (?::uuid)", UUID.randomUUID().toString());
            }
            Mockito.doReturn(true).when(portalClient).isPortalReachable();
            Mockito.doReturn(false).when(portalClient).isAlreadyRegistered(any());
            Mockito.doReturn(new CommunityGrantPortalStatus("AWAITING_SIGNUP", null, null),
                            new CommunityGrantPortalStatus("DONE", null, null))
                    .when(portalClient).getStatus(any(), any());

            doPost("/api/communityGrant/start").andExpect(status().isOk());

            await("the poller reaching a terminal state").atMost(30, TimeUnit.SECONDS)
                    .until(() -> flowStateStore.get().getState() == CommunityGrantState.REGISTERED);
            assertThat(jdbcTemplate.queryForObject("SELECT license_claim_token FROM tb_cluster", String.class)).isNull();
        } finally {
            poller.disarm();
            flowStateStore.update(state -> CommunityGrantFlowState.initial());
            if (seededClusterId) {
                jdbcTemplate.update("DELETE FROM tb_cluster");
            }
        }
    }

    @Test
    public void testStartReturnsASignUpUrlToASysAdmin() throws Exception {
        loginSysAdmin();
        boolean seededClusterId = Boolean.TRUE.equals(
                jdbcTemplate.queryForObject("SELECT NOT EXISTS (SELECT 1 FROM tb_cluster)", Boolean.class));
        try {
            if (seededClusterId) {
                jdbcTemplate.update("INSERT INTO tb_cluster (cluster_id) VALUES (?::uuid)", UUID.randomUUID().toString());
            }
            Mockito.doReturn(true).when(portalClient).isPortalReachable();
            Mockito.doReturn(false).when(portalClient).isAlreadyRegistered(any());
            Mockito.doReturn(new CommunityGrantPortalStatus("AWAITING_SIGNUP", null, null))
                    .when(portalClient).getStatus(any(), any());

            JsonNode state = JacksonUtil.toJsonNode(doPost("/api/communityGrant/start")
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

            assertThat(state.get("state").asText()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP.name());
            assertThat(state.get("mode").asText()).isEqualTo(CommunityGrantMode.ONLINE.name());
            assertThat(state.get("signUpUrl").asText()).startsWith("http");
        } finally {
            poller.disarm();
            flowStateStore.update(state -> CommunityGrantFlowState.initial());
            if (seededClusterId) {
                jdbcTemplate.update("DELETE FROM tb_cluster");
            }
        }
    }

    // Zero bytes in place of the release signature: the parser passes them on and the verifier refuses them.
    @Test
    public void testOfflineRunRefusalIsAudited() throws Exception {
        loginSysAdmin();
        User currentUser = doGet("/api/auth/user", User.class);
        String previousClusterId = currentClusterId();
        try {
            setClusterId(CommunityGrantBundleFixtures.CLUSTER_ID);

            mockMvc.perform(offlineRunRequest(bundleForThisHost(new byte[74]))).andExpect(status().isBadRequest());
        } finally {
            setClusterId(previousClusterId);
        }

        String failureDetails = assertSingleAuditLog(currentUser, ActionType.COMMUNITY_GRANT_OFFLINE_CHECKER_RUN,
                ActionStatus.FAILURE).getActionFailureDetails();
        assertThat(failureDetails).contains("not a ThingsBoard release signature");
        assertThat(failureDetails.toLowerCase()).doesNotContain("secret");
        assertThat(failureDetails).doesNotContain("postgres://");
        assertThat(failureDetails.toLowerCase()).doesNotContain("password");
    }

    @Test
    public void testOfflineRunWithAFileThatIsNotABundleIsRefusedAndAudited() throws Exception {
        loginSysAdmin();
        User currentUser = doGet("/api/auth/user", User.class);

        mockMvc.perform(offlineRunRequest("not a bundle at all".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isBadRequest());

        assertThat(assertSingleAuditLog(currentUser, ActionType.COMMUNITY_GRANT_OFFLINE_CHECKER_RUN,
                ActionStatus.FAILURE).getActionFailureDetails()).contains("not a ThingsBoard enrollment bundle");
        Mockito.verify(reportRunner, never()).runUploadedChecker(any(), any(), any(), any());
    }

    @Test
    public void testOfflineRunOnAnArchitectureWithNoCheckerBuildIsRefusedAndAudited() throws Exception {
        loginSysAdmin();
        User currentUser = doGet("/api/auth/user", User.class);
        // Built before os.arch is swapped, so the manifest names the real host.
        byte[] bundle = bundleForThisHost(CommunityGrantBundleFixtures.relsig());
        String previousArch = System.getProperty("os.arch");
        try {
            System.setProperty("os.arch", "s390x");

            mockMvc.perform(offlineRunRequest(bundle))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(Matchers.containsString("no instance checker build")));
        } finally {
            System.setProperty("os.arch", previousArch);
        }

        assertSingleAuditLog(currentUser, ActionType.COMMUNITY_GRANT_OFFLINE_CHECKER_RUN, ActionStatus.FAILURE);
        Mockito.verify(reportRunner, never()).runUploadedChecker(any(), any(), any(), any());
    }

    @Test
    public void testOfflineRunWithABundleForAnotherInstallationIsRefused() throws Exception {
        loginSysAdmin();
        String previousClusterId = currentClusterId();
        try {
            setClusterId(UUID.randomUUID().toString());

            mockMvc.perform(offlineRunRequest(bundleForThisHost(CommunityGrantBundleFixtures.relsig())))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(Matchers.containsString("different installation")));

            Mockito.verify(reportRunner, never()).runUploadedChecker(any(), any(), any(), any());
        } finally {
            setClusterId(previousClusterId);
        }
    }

    @Test
    public void testOfflineRunWithAWrongPlatformBundleNamesBothPlatforms() throws Exception {
        loginSysAdmin();
        CommunityGrantPlatform host = CommunityGrantPlatform.current();
        String otherOs = "linux".equals(host.os()) ? "darwin" : "linux";
        ObjectNode manifest = CommunityGrantBundleFixtures.validManifest();
        manifest.put("os", otherOs);
        manifest.put("arch", host.arch());

        mockMvc.perform(offlineRunRequest(CommunityGrantBundleFixtures.bundle(manifest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(Matchers.containsString(otherOs)))
                .andExpect(content().string(Matchers.containsString(host.os())));

        Mockito.verify(reportRunner, never()).runUploadedChecker(any(), any(), any(), any());
    }

    @Test
    public void testTheRunnerIsGivenTheCheckersOwnNameAndTheManifestsInput() throws Exception {
        loginSysAdmin();
        String previousClusterId = currentClusterId();
        try {
            setClusterId(CommunityGrantBundleFixtures.CLUSTER_ID);
            givenTheRunnerAcceptsAndReturns(REPORT);

            mockMvc.perform(offlineRunRequest(bundleForThisHost(CommunityGrantBundleFixtures.relsig())))
                    .andExpect(status().isAccepted());
            awaitOfflineRunFinished();

            ArgumentCaptor<String> verifiedName = ArgumentCaptor.forClass(String.class);
            Mockito.verify(reportRunner).verifyUploadedChecker(any(), any(), verifiedName.capture());
            assertThat(verifiedName.getValue()).isEqualTo(CHECKER_FILE_NAME_FOR_THIS_HOST);
            ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<CommunityGrantCheckerInput> checkerInput =
                    ArgumentCaptor.forClass(CommunityGrantCheckerInput.class);
            Mockito.verify(reportRunner)
                    .runUploadedChecker(any(), any(), fileName.capture(), checkerInput.capture());
            assertThat(fileName.getValue()).isEqualTo(CHECKER_FILE_NAME_FOR_THIS_HOST);
            assertThat(checkerInput.getValue().json())
                    .isEqualTo(JacksonUtil.toString(CommunityGrantBundleFixtures.validCheckerInput()));
        } finally {
            setClusterId(previousClusterId);
            offlineReportStore.clear();
            flowStateStore.update(state -> CommunityGrantFlowState.initial());
        }
    }

    @Test
    public void testOfflineRunAnswers202AndTheReportArrivesThroughTheState() throws Exception {
        loginSysAdmin();
        User currentUser = doGet("/api/auth/user", User.class);
        String previousClusterId = currentClusterId();
        try {
            setClusterId(CommunityGrantBundleFixtures.CLUSTER_ID);
            givenAnOfflineEnrollment();
            givenTheRunnerAcceptsAndReturns(REPORT);

            byte[] bundle = bundleForThisHost(CommunityGrantBundleFixtures.relsig());
            String body = mockMvc.perform(offlineRunRequest(bundle))
                    .andExpect(status().isAccepted())
                    .andReturn().getResponse().getContentAsString();
            assertThat(JacksonUtil.toJsonNode(body).get("offlineRunInProgress").asBoolean()).isTrue();
            awaitOfflineRunFinished();

            JsonNode state = doGetTyped("/api/communityGrant/state", new TypeReference<JsonNode>() {});
            assertThat(state.get("offlineRunInProgress").asBoolean()).isFalse();
            assertThat(state.get("offlineRunError").isNull()).isTrue();
            assertThat(state.get("state").asText()).isEqualTo(CommunityGrantState.VALIDATING.name());
            assertThat(state.get("offlineReport").asText()).isEqualTo(REPORT);
            assertSingleAuditLog(currentUser, ActionType.COMMUNITY_GRANT_OFFLINE_CHECKER_RUN, ActionStatus.SUCCESS);
        } finally {
            setClusterId(previousClusterId);
            offlineReportStore.clear();
            flowStateStore.update(state -> CommunityGrantFlowState.initial());
        }
    }

    @Test
    public void testAFailedOfflineRunIsReportedThroughTheState() throws Exception {
        loginSysAdmin();
        User currentUser = doGet("/api/auth/user", User.class);
        String previousClusterId = currentClusterId();
        try {
            setClusterId(CommunityGrantBundleFixtures.CLUSTER_ID);
            givenAnOfflineEnrollment();
            Mockito.doNothing().when(reportRunner).verifyUploadedChecker(any(), any(), any());
            Mockito.doThrow(new IllegalStateException("The instance check failed with exit code 3"))
                    .when(reportRunner).runUploadedChecker(any(), any(), any(), any());

            mockMvc.perform(offlineRunRequest(bundleForThisHost(CommunityGrantBundleFixtures.relsig())))
                    .andExpect(status().isAccepted());
            awaitOfflineRunFinished();

            JsonNode state = doGetTyped("/api/communityGrant/state", new TypeReference<JsonNode>() {});
            assertThat(state.get("offlineRunInProgress").asBoolean()).isFalse();
            assertThat(state.get("offlineRunError").asText())
                    .isEqualTo("Failed to run the check. The instance check failed with exit code 3");
            assertThat(state.get("state").asText()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP.name());
            assertThat(assertSingleAuditLog(currentUser, ActionType.COMMUNITY_GRANT_OFFLINE_CHECKER_RUN,
                    ActionStatus.FAILURE).getActionFailureDetails()).contains("exit code 3");
        } finally {
            setClusterId(previousClusterId);
            offlineReportStore.clear();
            flowStateStore.update(state -> CommunityGrantFlowState.initial());
        }
    }

    @Test
    public void testOfflineRunIsRefusedWhileAnotherRunIsInProgress() throws Exception {
        loginSysAdmin();
        User currentUser = doGet("/api/auth/user", User.class);
        String previousClusterId = currentClusterId();
        try {
            setClusterId(CommunityGrantBundleFixtures.CLUSTER_ID);
            Mockito.doNothing().when(reportRunner).verifyUploadedChecker(any(), any(), any());
            flowStateStore.update(state -> {
                state.setOfflineRun(CommunityGrantOfflineRun.running(UUID.randomUUID(), System.currentTimeMillis()));
                return state;
            });

            mockMvc.perform(offlineRunRequest(bundleForThisHost(CommunityGrantBundleFixtures.relsig())))
                    .andExpect(status().isTooManyRequests());

            Mockito.verify(reportRunner, never()).runUploadedChecker(any(), any(), any(), any());
            JsonNode state = doGetTyped("/api/communityGrant/state", new TypeReference<JsonNode>() {});
            assertThat(state.get("offlineRunInProgress").asBoolean()).isTrue();
            assertThat(assertSingleAuditLog(currentUser, ActionType.COMMUNITY_GRANT_OFFLINE_CHECKER_RUN,
                    ActionStatus.FAILURE).getActionFailureDetails()).contains("already running");
        } finally {
            setClusterId(previousClusterId);
            flowStateStore.update(state -> CommunityGrantFlowState.initial());
        }
    }

    /** The stand-in checker is not signed by a trusted key, so both runner steps are stubbed. */
    private void givenTheRunnerAcceptsAndReturns(String report) {
        Mockito.doNothing().when(reportRunner).verifyUploadedChecker(any(), any(), any());
        Mockito.doReturn(report).when(reportRunner).runUploadedChecker(any(), any(), any(), any());
    }

    private void givenAnOfflineEnrollment() {
        flowStateStore.update(state -> {
            state.setMode(CommunityGrantMode.OFFLINE);
            state.setState(CommunityGrantState.AWAITING_SIGNUP);
            return state;
        });
    }

    /** The permit is released last, after the run's state write and audit call. */
    private void awaitOfflineRunFinished() {
        Object service = AopTestUtils.getUltimateTargetObject(communityGrantService);
        Semaphore permit = (Semaphore) ReflectionTestUtils.getField(service, "offlineRunPermit");
        await("the background offline run").atMost(10, TimeUnit.SECONDS)
                .until(() -> permit.availablePermits() == 1);
    }

    @Test
    public void testAnExpiredChallengeWarnsAndStillReachesTheRunner() throws Exception {
        loginSysAdmin();
        String previousClusterId = currentClusterId();
        try {
            setClusterId(CommunityGrantBundleFixtures.CLUSTER_ID);
            givenTheRunnerAcceptsAndReturns(REPORT);
            ObjectNode manifest = CommunityGrantBundleFixtures.validManifest();
            manifest.put("os", CommunityGrantPlatform.current().os());
            manifest.put("arch", CommunityGrantPlatform.current().arch());
            manifest.put("issuedAt", EXPIRED_ISSUED_AT);
            manifest.put("challengeExpiresAt", EXPIRED_EXPIRES_AT);

            mockMvc.perform(offlineRunRequest(CommunityGrantBundleFixtures.bundle(manifest)))
                    .andExpect(status().isAccepted());
            awaitOfflineRunFinished();

            Mockito.verify(reportRunner).runUploadedChecker(any(), any(), any(), any());
        } finally {
            setClusterId(previousClusterId);
            offlineReportStore.clear();
            flowStateStore.update(state -> CommunityGrantFlowState.initial());
        }
    }

    private String currentClusterId() {
        return jdbcTemplate.query("SELECT cluster_id FROM tb_cluster LIMIT 1",
                rs -> rs.next() ? rs.getString(1) : null);
    }

    private void setClusterId(String clusterId) {
        jdbcTemplate.update("DELETE FROM tb_cluster");
        if (clusterId != null) {
            jdbcTemplate.update("INSERT INTO tb_cluster (cluster_id) VALUES (?::uuid)", clusterId);
        }
    }

    private static final String CHECKER_FILE_NAME_FOR_THIS_HOST = "tb-instance-check_"
            + CommunityGrantPlatform.current().os() + "_" + CommunityGrantPlatform.current().arch();

    private static byte[] bundleForThisHost(byte[] relsig) {
        ObjectNode manifest = CommunityGrantBundleFixtures.validManifest();
        manifest.put("os", CommunityGrantPlatform.current().os());
        manifest.put("arch", CommunityGrantPlatform.current().arch());
        manifest.put("checkerFileName", CHECKER_FILE_NAME_FOR_THIS_HOST);
        return CommunityGrantBundleFixtures.bundle(relsig,
                JacksonUtil.toString(manifest).getBytes(StandardCharsets.UTF_8),
                CommunityGrantBundleFixtures.standInChecker());
    }

    private MockMultipartHttpServletRequestBuilder offlineRunRequest(byte[] bundle) {
        MockMultipartHttpServletRequestBuilder postRequest =
                MockMvcRequestBuilders.multipart("/api/communityGrant/offline/run");
        postRequest.file(new MockMultipartFile("bundle", BUNDLE_FILE_NAME, "application/octet-stream", bundle));
        setJwtToken(postRequest);
        return postRequest;
    }

    /** Audit logs are written asynchronously. */
    private AuditLog assertSingleAuditLog(User user, ActionType actionType, ActionStatus expectedStatus) {
        await("async audit log saving").atMost(5, TimeUnit.SECONDS)
                .until(() -> !findAuditLogs(user, actionType).isEmpty());
        List<AuditLog> auditLogs = findAuditLogs(user, actionType);

        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.get(0).getActionStatus()).isEqualTo(expectedStatus);
        return auditLogs.get(0);
    }

    private List<AuditLog> findAuditLogs(User user, ActionType actionType) {
        return auditLogService.findAuditLogsByTenantIdAndUserId(
                user.getTenantId(), user.getId(), List.of(actionType),
                new TimePageLink(new PageLink(10, 0, null, new SortOrder("createdTime", SortOrder.Direction.DESC)),
                        0L, System.currentTimeMillis())).getData();
    }

}
