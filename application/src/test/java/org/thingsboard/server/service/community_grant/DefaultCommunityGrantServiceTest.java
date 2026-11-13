// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.community_grant.CommunityGrantFlowState;
import org.thingsboard.server.common.data.community_grant.CommunityGrantMode;
import org.thingsboard.server.common.data.community_grant.CommunityGrantOfflineRun;
import org.thingsboard.server.common.data.community_grant.CommunityGrantOfflineRunStatus;
import org.thingsboard.server.common.data.community_grant.CommunityGrantParkReason;
import org.thingsboard.server.common.data.community_grant.CommunityGrantState;
import org.thingsboard.server.common.data.community_grant.CommunityGrantStateInfo;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.dao.subscription.TbClusterStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultCommunityGrantServiceTest {

    private static final UUID CLUSTER_ID = UUID.randomUUID();
    private static final CommunityGrantCheckerInput CHECKER_INPUT = new CommunityGrantCheckerInput("{\"keyId\":7}");
    private static final int CHECKER_TIMEOUT_SEC = 600;
    /** A run still marked running after this long has lost its node. */
    private static final long STALE_AFTER_MS =
            TimeUnit.SECONDS.toMillis(CHECKER_TIMEOUT_SEC) + DefaultCommunityGrantService.OFFLINE_RUN_GRACE_MS;

    @Mock
    private TbClusterStore tbClusterStore;
    @Mock
    private CommunityGrantFlowStateStore flowStateStore;
    @Mock
    private CommunityGrantPortalClient portalClient;
    @Mock
    private CommunityGrantPoller poller;
    @Mock
    private CommunityGrantReportRunner reportRunner;
    @Mock
    private CommunityGrantOfflineReportStore offlineReportStore;

    @InjectMocks
    private DefaultCommunityGrantService service;

    private CommunityGrantFlowState stored;
    /** What each background run reported on completion: {@code null} for a success. */
    private final List<Exception> completion = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "offlineRunExecutor", MoreExecutors.newDirectExecutorService());
        when(reportRunner.getCheckerTimeoutSec()).thenReturn(CHECKER_TIMEOUT_SEC);
        stored = CommunityGrantFlowState.initial();
        when(flowStateStore.get()).thenAnswer(invocation -> stored);
        when(flowStateStore.update(any())).thenAnswer(invocation -> {
            UnaryOperator<CommunityGrantFlowState> updater = invocation.getArgument(0);
            stored = updater.apply(stored);
            return stored;
        });
        when(tbClusterStore.getClusterId()).thenReturn(Optional.of(CLUSTER_ID));
    }

    private void givenReachablePortal() {
        when(portalClient.isPortalReachable()).thenReturn(true);
        when(portalClient.isAlreadyRegistered(CLUSTER_ID)).thenReturn(false);
        givenSignUpUrl();
    }

    private void givenSignUpUrl() {
        when(portalClient.buildSignUpUrl(any(), anyString(), any())).thenAnswer(invocation ->
                signUpUrl(invocation.getArgument(1), invocation.getArgument(2)));
    }

    private static String signUpUrl(String token, CommunityGrantMode mode) {
        return "https://portal.example/signup?clusterId=" + CLUSTER_ID + "&claimToken=" + token
                + (mode == CommunityGrantMode.OFFLINE ? "&offline=true" : "");
    }

    /** With the direct executor, the background run has finished by the time this returns. */
    private CommunityGrantStateInfo runChecker() throws ThingsboardException {
        return service.runOfflineChecker(new byte[]{1}, new byte[]{2}, "checker", CHECKER_INPUT, completion::add);
    }

    @Test
    void testStateCarriesTheReportOnceAByHandRunHasBeenRecorded() {
        stored.setState(CommunityGrantState.VALIDATING);
        stored.setMode(CommunityGrantMode.OFFLINE);
        stored.setLastOfflineCheckAt(System.currentTimeMillis());
        when(offlineReportStore.get()).thenReturn(Optional.of("report-body"));

        assertThat(service.getStateInfo().offlineReport()).isEqualTo("report-body");
    }

    @Test
    void testStateCarriesNoReportBeforeAnyRun() {
        stored.setState(CommunityGrantState.AWAITING_SIGNUP);

        assertThat(service.getStateInfo().offlineReport()).isNull();
        verifyNoInteractions(offlineReportStore);
    }

    @Test
    void testTheOnlineWaitCarriesNoReportAndNeverReadsTheStore() {
        stored.setState(CommunityGrantState.VALIDATING);
        stored.setMode(CommunityGrantMode.ONLINE);

        assertThat(service.getStateInfo().offlineReport()).isNull();
        verifyNoInteractions(offlineReportStore);
    }

    @Test
    void testStartClearsTheByHandRunTimestampSoNoReportLeaksIntoTheNextEnrollment() throws ThingsboardException {
        stored.setState(CommunityGrantState.RESUME);
        stored.setLastOfflineCheckAt(System.currentTimeMillis());
        givenReachablePortal();

        service.start();

        assertThat(stored.getLastOfflineCheckAt()).isNull();
        stored.setState(CommunityGrantState.VALIDATING);
        assertThat(service.getStateInfo().offlineReport()).isNull();
        verify(offlineReportStore, never()).get();
    }

    @Test
    void testTheStateCarriesThePortalThisDeploymentIsConfiguredAgainst() {
        when(portalClient.getPortalUrl()).thenReturn("https://portal.staging.example");

        assertThat(service.getStateInfo().portalUrl()).isEqualTo("https://portal.staging.example");
    }

    @Test
    void testAnUnreadableReportStoreStillAnswersWithTheState() {
        stored.setState(CommunityGrantState.VALIDATING);
        stored.setLastOfflineCheckAt(System.currentTimeMillis());
        when(offlineReportStore.get()).thenThrow(new IllegalStateException("boom"));

        CommunityGrantStateInfo info = service.getStateInfo();

        assertThat(info.state()).isEqualTo(CommunityGrantState.VALIDATING);
        assertThat(info.offlineReport()).isNull();
    }

    @Test
    void testStartAsksAboutTheInstallationBeforeMintingAToken() throws ThingsboardException {
        givenReachablePortal();

        CommunityGrantStateInfo info = service.start();

        InOrder inOrder = inOrder(tbClusterStore, portalClient);
        inOrder.verify(portalClient).isAlreadyRegistered(CLUSTER_ID);
        inOrder.verify(tbClusterStore).saveLicenseClaimToken(anyString());
        assertThat(info.state()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        assertThat(info.mode()).isEqualTo(CommunityGrantMode.ONLINE);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(tbClusterStore).saveLicenseClaimToken(tokenCaptor.capture());
        verify(portalClient).buildSignUpUrl(CLUSTER_ID, tokenCaptor.getValue(), CommunityGrantMode.ONLINE);
        assertThat(info.signUpUrl()).isEqualTo(signUpUrl(tokenCaptor.getValue(), CommunityGrantMode.ONLINE));
        verify(poller).arm();
    }

    @Test
    void testMintedTokenIsUrlSafeAndLongEnough() throws ThingsboardException {
        givenReachablePortal();

        service.start();

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(tbClusterStore).saveLicenseClaimToken(tokenCaptor.capture());
        String token = tokenCaptor.getValue();
        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void testStartFallsBackToOfflineWhenThePortalIsUnreachable() throws ThingsboardException {
        when(portalClient.isPortalReachable()).thenReturn(false);
        givenSignUpUrl();

        CommunityGrantStateInfo info = service.start();

        assertThat(info.mode()).isEqualTo(CommunityGrantMode.OFFLINE);
        assertThat(info.state()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(tbClusterStore).saveLicenseClaimToken(tokenCaptor.capture());
        verify(portalClient).buildSignUpUrl(CLUSTER_ID, tokenCaptor.getValue(), CommunityGrantMode.OFFLINE);
        assertThat(info.signUpUrl()).isEqualTo(signUpUrl(tokenCaptor.getValue(), CommunityGrantMode.OFFLINE));
        verify(portalClient, never()).isAlreadyRegistered(any());
        // Armed offline too: the tick enforces the sign-up deadline.
        verify(poller).arm();
        verify(poller, never()).disarm();
    }

    // Asserts the mode arm() reads when it is called, not just the order of the calls.
    @Test
    void testRecoveringFromOfflineToOnlineWritesTheNewModeBeforeArming() throws ThingsboardException {
        stored.setMode(CommunityGrantMode.OFFLINE);
        stored.setState(CommunityGrantState.AWAITING_SIGNUP);
        givenReachablePortal();
        AtomicReference<CommunityGrantMode> modeVisibleToArm = new AtomicReference<>();
        doAnswer(invocation -> {
            modeVisibleToArm.set(flowStateStore.get().getMode());
            return null;
        }).when(poller).arm();

        CommunityGrantStateInfo info = service.start();

        assertThat(info.mode()).isEqualTo(CommunityGrantMode.ONLINE);
        verify(poller).arm();
        assertThat(modeVisibleToArm).hasValue(CommunityGrantMode.ONLINE);
    }

    @Test
    void testAStoreFailureOnTheAlreadyRegisteredPathIsReportedCleanly() {
        when(portalClient.isPortalReachable()).thenReturn(true);
        when(portalClient.isAlreadyRegistered(CLUSTER_ID)).thenReturn(true);
        doThrow(new IllegalStateException("db down")).when(flowStateStore).update(any());

        assertThatThrownBy(() -> service.start())
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("Failed to start")
                .hasNoCause();
    }

    @Test
    void testStartStopsAtAlreadyRegisteredWithoutEverMintingAToken() throws ThingsboardException {
        when(portalClient.isPortalReachable()).thenReturn(true);
        when(portalClient.isAlreadyRegistered(CLUSTER_ID)).thenReturn(true);

        CommunityGrantStateInfo info = service.start();

        assertThat(info.state()).isEqualTo(CommunityGrantState.ALREADY_REGISTERED);
        assertThat(info.signUpUrl()).isNull();
        verify(tbClusterStore, never()).saveLicenseClaimToken(anyString());
        verify(tbClusterStore).forceClearLicenseClaimToken();
        verify(poller, never()).arm();
        verify(poller).disarm();
    }

    @Test
    void testStartContinuesWhenTheClusterStatusCallFails() throws ThingsboardException {
        when(portalClient.isPortalReachable()).thenReturn(true);
        givenSignUpUrl();
        when(portalClient.isAlreadyRegistered(CLUSTER_ID))
                .thenThrow(new RuntimeException("portal hiccup"));

        CommunityGrantStateInfo info = service.start();

        assertThat(info.state()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        verify(poller).arm();
    }

    @Test
    void testStartFailsWithoutAdvancingWhenTheTokenCannotBeStored() {
        givenReachablePortal();
        doThrow(new IllegalStateException("empty table")).when(tbClusterStore).saveLicenseClaimToken(anyString());

        assertThatThrownBy(() -> service.start())
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("Failed to start")
                .hasNoCause();

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.NOT_STARTED);
        verify(poller, never()).arm();
    }

    @Test
    void testStartClearsTheExactTokenWhenTheFlowStateCannotBePersisted() {
        givenReachablePortal();
        // doThrow(): re-stubbing with when() would run setUp()'s answer with a null updater.
        doThrow(new IllegalStateException("stored document is unreadable")).when(flowStateStore).update(any());

        assertThatThrownBy(() -> service.start()).isInstanceOf(ThingsboardException.class).hasNoCause();

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(tbClusterStore).saveLicenseClaimToken(tokenCaptor.capture());
        verify(tbClusterStore).clearLicenseClaimToken(tokenCaptor.getValue());
        verify(poller, never()).arm();
    }

    @Test
    void testStartFailsCleanlyWhenThePortalBaseUrlIsMalformed() {
        when(portalClient.isPortalReachable()).thenReturn(true);
        when(portalClient.isAlreadyRegistered(CLUSTER_ID)).thenReturn(false);
        when(portalClient.buildSignUpUrl(any(), anyString(), any()))
                .thenThrow(new IllegalArgumentException("URI is not absolute"));

        assertThatThrownBy(() -> service.start())
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("misconfigured")
                .hasNoCause();

        verify(tbClusterStore, never()).saveLicenseClaimToken(anyString());
        verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
        verify(poller, never()).arm();
    }

    @Test
    void testStartFailsWhenTheDeploymentHasNoClusterId() {
        when(tbClusterStore.getClusterId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.start()).isInstanceOf(ThingsboardException.class);
        verify(tbClusterStore, never()).saveLicenseClaimToken(anyString());
    }

    @Test
    void testStartFromResumeMintsAFreshToken() throws ThingsboardException {
        stored.setState(CommunityGrantState.RESUME);
        givenReachablePortal();

        CommunityGrantStateInfo info = service.start();

        assertThat(info.state()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        assertThat(info.signUpUrl()).contains("claimToken=");
    }

    @Test
    void testStartingAgainClearsTheParkReason() throws ThingsboardException {
        givenReachablePortal();
        stored.setState(CommunityGrantState.RESUME);
        stored.setParkReason(CommunityGrantParkReason.CHECK_FAILED);

        service.start();

        assertThat(stored.getParkReason()).isNull();
    }

    // The pre-flight cannot say whose registration it found; on a flow begun here it is most likely this one's.
    @Test
    void testStartAgainOutOfAParkReMintsInsteadOfDeadEndingOnThePreFlight() throws ThingsboardException {
        when(portalClient.isPortalReachable()).thenReturn(true);
        givenSignUpUrl();
        stored.setState(CommunityGrantState.RESUME);
        stored.setParkReason(CommunityGrantParkReason.LINK_EXPIRED);

        CommunityGrantStateInfo info = service.start();

        assertThat(info.state()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        assertThat(info.parkReason()).isNull();
        verify(portalClient, never()).isAlreadyRegistered(any());
        verify(tbClusterStore).saveLicenseClaimToken(anyString());
    }

    @Test
    void testThePreFlightStillRefusesADeploymentThatHasNeverEnrolledFromHere() throws ThingsboardException {
        when(portalClient.isPortalReachable()).thenReturn(true);
        when(portalClient.isAlreadyRegistered(CLUSTER_ID)).thenReturn(true);

        CommunityGrantStateInfo info = service.start();

        assertThat(info.state()).isEqualTo(CommunityGrantState.ALREADY_REGISTERED);
        verify(tbClusterStore, never()).saveLicenseClaimToken(anyString());
    }

    @Test
    void testStartIsRefusedOnATerminalState() {
        stored.setState(CommunityGrantState.REGISTERED);

        assertThatThrownBy(() -> service.start()).isInstanceOf(ThingsboardException.class);
        verify(tbClusterStore, never()).saveLicenseClaimToken(anyString());
    }

    @Test
    void testStartIsRefusedWhileTheRegistrationIsUnderReview() {
        stored.setState(CommunityGrantState.UNDER_REVIEW);

        assertThatThrownBy(() -> service.start())
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.UNDER_REVIEW_MESSAGE);
        verify(tbClusterStore, never()).saveLicenseClaimToken(anyString());
        verify(poller, never()).disarm();
    }

    @Test
    void testStartIsRefusedWhenTheFeatureIsDisabled() {
        ReflectionTestUtils.setField(service, "enabled", false);

        assertThatThrownBy(() -> service.start()).isInstanceOf(ThingsboardException.class);
        verify(portalClient, never()).isPortalReachable();
    }

    @Test
    void testStartingRetiresThePreviousRunsReport() throws ThingsboardException {
        givenReachablePortal();

        service.start();

        verify(offlineReportStore).clear();
    }

    @Test
    void testStartingAsAlreadyRegisteredRetiresThePreviousRunsReport() throws ThingsboardException {
        when(portalClient.isPortalReachable()).thenReturn(true);
        when(portalClient.isAlreadyRegistered(CLUSTER_ID)).thenReturn(true);

        CommunityGrantStateInfo info = service.start();

        assertThat(info.state()).isEqualTo(CommunityGrantState.ALREADY_REGISTERED);
        verify(offlineReportStore).clear();
    }

    @Test
    void testAFailedStartLeavesTheReportAlone() {
        givenReachablePortal();
        doThrow(new IllegalStateException("connection refused")).when(flowStateStore).update(any());

        assertThatThrownBy(() -> service.start()).isInstanceOf(ThingsboardException.class);

        verify(offlineReportStore, never()).clear();
    }

    @Test
    void testRunOfflineCheckerMovesToValidatingOnSuccess() throws ThingsboardException {
        stored.setMode(CommunityGrantMode.OFFLINE);
        when(reportRunner.runUploadedChecker(any(), any(), any(), any()))
                .thenReturn("report-body");

        runChecker();

        verify(reportRunner).runUploadedChecker(any(), any(), any(), eq(CHECKER_INPUT));
        verify(offlineReportStore).put("report-body");
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.VALIDATING);
        assertThat(stored.getMode()).isEqualTo(CommunityGrantMode.OFFLINE);
        assertThat(stored.getLastOfflineCheckAt()).isNotNull();
    }

    @Test
    void testTheUploadAnswersWithARunInProgressAndTheReportArrivesThroughTheState() throws ThingsboardException {
        stored.setMode(CommunityGrantMode.OFFLINE);
        stored.setState(CommunityGrantState.AWAITING_SIGNUP);
        ReflectionTestUtils.setField(service, "offlineRunExecutor", new QueuedExecutorService());
        when(reportRunner.runUploadedChecker(any(), any(), any(), any())).thenReturn("report-body");
        when(offlineReportStore.get()).thenReturn(Optional.of("report-body"));

        CommunityGrantStateInfo answer = runChecker();

        assertThat(answer.offlineRunInProgress()).isTrue();
        assertThat(answer.offlineRunError()).isNull();
        verify(reportRunner).verifyUploadedChecker(any(), any(), any());
        verify(reportRunner, never()).runUploadedChecker(any(), any(), any(), any());
        assertThat(service.getStateInfo().offlineRunInProgress()).isTrue();

        runQueuedTasks();

        CommunityGrantStateInfo state = service.getStateInfo();
        assertThat(state.offlineRunInProgress()).isFalse();
        assertThat(state.offlineRunError()).isNull();
        assertThat(state.state()).isEqualTo(CommunityGrantState.VALIDATING);
        assertThat(state.offlineReport()).isEqualTo("report-body");
        assertThat(stored.getOfflineRun()).isNull();
        assertThat(completion).containsExactly((Exception) null);
    }

    @Test
    void testAFailedRunIsReportedThroughTheState() throws ThingsboardException {
        stored.setMode(CommunityGrantMode.OFFLINE);
        stored.setState(CommunityGrantState.AWAITING_SIGNUP);
        when(reportRunner.runUploadedChecker(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("The instance check failed with exit code 3"));

        CommunityGrantStateInfo answer = runChecker();

        assertThat(answer.offlineRunInProgress()).isTrue();
        CommunityGrantStateInfo state = service.getStateInfo();
        assertThat(state.offlineRunInProgress()).isFalse();
        assertThat(state.offlineRunError())
                .isEqualTo("Failed to run the check. The instance check failed with exit code 3");
        assertThat(state.state()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        assertThat(stored.getOfflineRun().getStatus()).isEqualTo(CommunityGrantOfflineRunStatus.FAILED);
        assertThat(completion).hasSize(1);
        assertThat(completion.get(0)).isInstanceOf(ThingsboardException.class)
                .hasMessage(state.offlineRunError());
        verify(offlineReportStore, never()).put(any());
    }

    @Test
    void testARunningRecordPastItsTimeIsReportedAsInterrupted() {
        long now = System.currentTimeMillis();
        stored.setOfflineRun(CommunityGrantOfflineRun.running(UUID.randomUUID(), now - STALE_AFTER_MS - 1000));

        CommunityGrantStateInfo state = service.getStateInfo();

        assertThat(state.offlineRunInProgress()).isFalse();
        assertThat(state.offlineRunError()).isEqualTo(DefaultCommunityGrantService.OFFLINE_RUN_INTERRUPTED_MESSAGE);
    }

    @Test
    void testARunningRecordWithinItsTimeIsReportedAsInProgress() {
        long now = System.currentTimeMillis();
        stored.setOfflineRun(CommunityGrantOfflineRun.running(UUID.randomUUID(), now - STALE_AFTER_MS + 60_000));

        CommunityGrantStateInfo state = service.getStateInfo();

        assertThat(state.offlineRunInProgress()).isTrue();
        assertThat(state.offlineRunError()).isNull();
    }

    @Test
    void testASecondStartIsRefusedWhileARunIsInProgress() {
        UUID otherRun = UUID.randomUUID();
        stored.setOfflineRun(CommunityGrantOfflineRun.running(otherRun, System.currentTimeMillis()));

        assertThatThrownBy(this::runChecker)
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.OFFLINE_RUN_IN_PROGRESS_MESSAGE)
                .satisfies(e -> assertThat(((ThingsboardException) e).getErrorCode())
                        .isEqualTo(ThingsboardErrorCode.TOO_MANY_REQUESTS));

        assertThat(stored.getOfflineRun().getRunId()).isEqualTo(otherRun);
        assertThat(stored.getOfflineRun().getStatus()).isEqualTo(CommunityGrantOfflineRunStatus.RUNNING);
        verify(reportRunner, never()).runUploadedChecker(any(), any(), any(), any());
        assertThat(completion).isEmpty();
    }

    @Test
    void testAStaleRunDoesNotRefuseTheNextStart() throws ThingsboardException {
        stored.setOfflineRun(CommunityGrantOfflineRun.running(UUID.randomUUID(),
                System.currentTimeMillis() - STALE_AFTER_MS - 1000));
        when(reportRunner.runUploadedChecker(any(), any(), any(), any())).thenReturn("report-body");

        runChecker();

        verify(reportRunner).runUploadedChecker(any(), any(), any(), any());
        assertThat(stored.getOfflineRun()).isNull();
    }

    @Test
    void testASecondStartOnTheSameNodeIsRefusedWhileItsRunIsQueued() throws ThingsboardException {
        ReflectionTestUtils.setField(service, "offlineRunExecutor", new QueuedExecutorService());
        runChecker();
        // Stands in for another node's clear, so only the permit is left to refuse.
        stored.setOfflineRun(null);

        assertThatThrownBy(this::runChecker)
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.OFFLINE_RUN_IN_PROGRESS_MESSAGE);
        verify(reportRunner, times(1)).verifyUploadedChecker(any(), any(), any());
    }

    // Two starts on two nodes can both pass the per-node lock; the later record wins.
    @Test
    void testACompletionLeavesAnotherRunsRecordAlone() throws ThingsboardException {
        ReflectionTestUtils.setField(service, "offlineRunExecutor", new QueuedExecutorService());
        when(reportRunner.runUploadedChecker(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("The instance check failed with exit code 3"));
        runChecker();
        UUID newerRun = UUID.randomUUID();
        stored.setOfflineRun(CommunityGrantOfflineRun.running(newerRun, System.currentTimeMillis()));

        runQueuedTasks();

        assertThat(stored.getOfflineRun().getRunId()).isEqualTo(newerRun);
        assertThat(stored.getOfflineRun().getStatus()).isEqualTo(CommunityGrantOfflineRunStatus.RUNNING);
        assertThat(completion).hasSize(1);
    }

    @Test
    void testASupersededSuccessfulRunDiscardsItsResult() throws ThingsboardException {
        stored.setMode(CommunityGrantMode.OFFLINE);
        stored.setState(CommunityGrantState.AWAITING_SIGNUP);
        ReflectionTestUtils.setField(service, "offlineRunExecutor", new QueuedExecutorService());
        when(reportRunner.runUploadedChecker(any(), any(), any(), any())).thenReturn("report-body");
        runChecker();
        UUID newerRun = UUID.randomUUID();
        stored.setOfflineRun(CommunityGrantOfflineRun.running(newerRun, System.currentTimeMillis()));

        runQueuedTasks();

        assertThat(stored.getOfflineRun().getRunId()).isEqualTo(newerRun);
        assertThat(stored.getOfflineRun().getStatus()).isEqualTo(CommunityGrantOfflineRunStatus.RUNNING);
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        assertThat(stored.getLastOfflineCheckAt()).isNull();
        verify(offlineReportStore, never()).put(any());
        assertThat(completion).hasSize(1);
        assertThat(completion.get(0)).hasMessage(DefaultCommunityGrantService.OFFLINE_RUN_SUPERSEDED_MESSAGE);
    }

    @Test
    void testARunThatAStartReplacedDiscardsItsResult() throws ThingsboardException {
        givenReachablePortal();
        stored.setMode(CommunityGrantMode.OFFLINE);
        stored.setState(CommunityGrantState.AWAITING_SIGNUP);
        ReflectionTestUtils.setField(service, "offlineRunExecutor", new QueuedExecutorService());
        when(reportRunner.runUploadedChecker(any(), any(), any(), any())).thenReturn("report-body");
        runChecker();

        service.start();
        runQueuedTasks();

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        assertThat(stored.getOfflineRun()).isNull();
        assertThat(stored.getLastOfflineCheckAt()).isNull();
        verify(offlineReportStore, never()).put(any());
        assertThat(completion).hasSize(1);
        assertThat(completion.get(0)).hasMessage(DefaultCommunityGrantService.OFFLINE_RUN_SUPERSEDED_MESSAGE);
    }

    @Test
    void testAStartLandingBetweenTheOwnershipCheckAndTheWriteLeavesTheStateAlone() throws ThingsboardException {
        stored.setMode(CommunityGrantMode.OFFLINE);
        stored.setState(CommunityGrantState.AWAITING_SIGNUP);
        when(reportRunner.runUploadedChecker(any(), any(), any(), any())).thenReturn("report-body");
        // Stands in for a start() clearing the record right after the report was put.
        doAnswer(invocation -> {
            stored.setOfflineRun(null);
            return null;
        }).when(offlineReportStore).put(any());

        runChecker();

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        assertThat(stored.getLastOfflineCheckAt()).isNull();
        assertThat(completion.get(0)).hasMessage(DefaultCommunityGrantService.OFFLINE_RUN_SUPERSEDED_MESSAGE);
    }

    @Test
    void testARunThatCannotBeScheduledIsRecordedAsFailedAndReleasesThePermit() throws ThingsboardException {
        ExecutorService rejecting = MoreExecutors.newDirectExecutorService();
        rejecting.shutdown();
        ReflectionTestUtils.setField(service, "offlineRunExecutor", rejecting);

        assertThatThrownBy(this::runChecker)
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.OFFLINE_RUN_NOT_SCHEDULED_MESSAGE);

        assertThat(stored.getOfflineRun().getStatus()).isEqualTo(CommunityGrantOfflineRunStatus.FAILED);
        assertThat(stored.getOfflineRun().getError())
                .isEqualTo(DefaultCommunityGrantService.OFFLINE_RUN_NOT_SCHEDULED_MESSAGE);
        assertThat(service.getStateInfo().offlineRunError())
                .isEqualTo(DefaultCommunityGrantService.OFFLINE_RUN_NOT_SCHEDULED_MESSAGE);

        ReflectionTestUtils.setField(service, "offlineRunExecutor", MoreExecutors.newDirectExecutorService());
        when(reportRunner.runUploadedChecker(any(), any(), any(), any())).thenReturn("report-body");
        runChecker();

        verify(reportRunner).runUploadedChecker(any(), any(), any(), any());
    }

    @Test
    void testStartClearsTheOfflineRun() throws ThingsboardException {
        givenReachablePortal();
        stored.setState(CommunityGrantState.AWAITING_SIGNUP);
        stored.setOfflineRun(CommunityGrantOfflineRun.running(UUID.randomUUID(), System.currentTimeMillis()));

        service.start();

        assertThat(stored.getOfflineRun()).isNull();
    }

    @Test
    void testARefusedBundleRecordsNoRun() {
        doThrow(new IllegalArgumentException("The uploaded checker does not match its release signature"))
                .when(reportRunner).verifyUploadedChecker(any(), any(), any());

        assertThatThrownBy(this::runChecker)
                .isInstanceOf(ThingsboardException.class)
                .hasMessage("The uploaded checker does not match its release signature")
                .satisfies(e -> assertThat(((ThingsboardException) e).getErrorCode())
                        .isEqualTo(ThingsboardErrorCode.BAD_REQUEST_PARAMS));

        assertThat(stored.getOfflineRun()).isNull();
        verify(reportRunner, never()).runUploadedChecker(any(), any(), any(), any());
        assertThat(completion).isEmpty();
    }

    @Test
    void testARefusedBundleReleasesThePermit() throws ThingsboardException {
        doThrow(new IllegalArgumentException("refused")).doNothing()
                .when(reportRunner).verifyUploadedChecker(any(), any(), any());
        when(reportRunner.runUploadedChecker(any(), any(), any(), any())).thenReturn("report-body");

        assertThatThrownBy(this::runChecker).isInstanceOf(ThingsboardException.class);
        runChecker();

        verify(reportRunner).runUploadedChecker(any(), any(), any(), any());
    }

    private void runQueuedTasks() {
        ExecutorService executor = (ExecutorService) ReflectionTestUtils.getField(service, "offlineRunExecutor");
        ((QueuedExecutorService) executor).runAll();
    }

    @Test
    void testRunOfflineCheckerIsRefusedWhenTheFeatureIsDisabled() {
        ReflectionTestUtils.setField(service, "enabled", false);

        assertThatThrownBy(this::runChecker)
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("not available on this deployment");
        verifyNoInteractions(reportRunner);
    }

    @Test
    void testRunOfflineCheckerPreservesAnAlreadyOnlineMode() throws ThingsboardException {
        stored.setMode(CommunityGrantMode.ONLINE);
        stored.setState(CommunityGrantState.COLLECTING);
        when(reportRunner.runUploadedChecker(any(), any(), any(), any()))
                .thenReturn("report-body");

        runChecker();

        assertThat(stored.getMode()).isEqualTo(CommunityGrantMode.ONLINE);
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.VALIDATING);
    }

    // The audit log renders the whole cause chain, so no cause may be attached.
    @Test
    void testOfflineStateWriteFailureIsReportedCleanlyWithoutACause() throws ThingsboardException {
        stored.setMode(CommunityGrantMode.OFFLINE);
        when(reportRunner.runUploadedChecker(any(), any(), any(), any()))
                .thenReturn("report-body");
        failUpdatesAfterTheFirst(new IllegalStateException(
                "connection refused: jdbc:postgresql://db.internal:5432/tb?user=tb&password=hunter2"));

        runChecker();

        assertThat(completion).hasSize(1);
        assertThat(completion.get(0))
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.OFFLINE_STATE_NOT_PERSISTED_MESSAGE)
                .hasNoCause()
                .satisfies(e -> {
                    assertThat(e.getMessage()).doesNotContain("hunter2");
                    assertThat(e.getMessage()).doesNotContain("jdbc:postgresql");
                });
    }

    @Test
    void testTheReportIsStoredEvenWhenTheStateWriteFails() throws ThingsboardException {
        stored.setMode(CommunityGrantMode.OFFLINE);
        when(reportRunner.runUploadedChecker(any(), any(), any(), any())).thenReturn("report-body");
        failUpdatesAfterTheFirst(new IllegalStateException("connection refused"));

        runChecker();

        assertThat(completion.get(0)).isInstanceOf(ThingsboardException.class);
        verify(offlineReportStore).put("report-body");
    }

    @Test
    void testAFailedReadAfterTheRunStoresNothingAndPromisesNoDownload() throws ThingsboardException {
        stored.setMode(CommunityGrantMode.OFFLINE);
        stored.setState(CommunityGrantState.AWAITING_SIGNUP);
        when(reportRunner.runUploadedChecker(any(), any(), any(), any())).thenAnswer(invocation -> {
            // The checker has run; the database goes away before its result is recorded.
            doThrow(new IllegalStateException("connection refused: jdbc:postgresql://db.internal:5432/tb"))
                    .when(flowStateStore).get();
            return "report-body";
        });

        runChecker();

        verify(offlineReportStore, never()).put(any());
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        assertThat(stored.getOfflineRun().getError())
                .isEqualTo(DefaultCommunityGrantService.OFFLINE_RESULT_NOT_RECORDED_MESSAGE)
                .doesNotContain("download");
        assertThat(completion.get(0))
                .hasMessage(DefaultCommunityGrantService.OFFLINE_RESULT_NOT_RECORDED_MESSAGE)
                .hasNoCause();
    }

    @Test
    void testAStartThatCannotBeRecordedIsRefusedWithoutACause() {
        doThrow(new IllegalStateException("connection refused: jdbc:postgresql://db.internal:5432/tb"))
                .when(flowStateStore).update(any());

        assertThatThrownBy(this::runChecker)
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.OFFLINE_RUN_START_FAILED_MESSAGE)
                .hasNoCause();

        verify(reportRunner, never()).runUploadedChecker(any(), any(), any(), any());
        assertThat(completion).isEmpty();
    }

    @Test
    void testRunOfflineCheckerDoesNotAdvanceTheStateWhenTheRunFails() throws ThingsboardException {
        stored.setState(CommunityGrantState.AWAITING_SIGNUP);
        stored.setMode(CommunityGrantMode.OFFLINE);
        when(reportRunner.runUploadedChecker(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("the uploaded checker does not match its release signature"));

        runChecker();

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        assertThat(stored.getLastOfflineCheckAt()).isNull();
        assertThat(stored.getOfflineRun().getError())
                .isEqualTo("the uploaded checker does not match its release signature");
        verify(offlineReportStore, never()).put(any());
    }

    /** The first write - the one recording the start - lands; every later one throws {@code failure}. */
    private void failUpdatesAfterTheFirst(RuntimeException failure) {
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() > 1) {
                throw failure;
            }
            UnaryOperator<CommunityGrantFlowState> updater = invocation.getArgument(0);
            stored = updater.apply(stored);
            return stored;
        }).when(flowStateStore).update(any());
    }

    @Test
    void testRunOfflineCheckerCanBeRunAgainFromValidating() throws ThingsboardException {
        stored.setState(CommunityGrantState.VALIDATING);
        stored.setMode(CommunityGrantMode.OFFLINE);
        when(reportRunner.runUploadedChecker(any(), any(), any(), any()))
                .thenReturn("second-report-body");

        runChecker();

        verify(offlineReportStore).put("second-report-body");
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.VALIDATING);
    }

    @Test
    void testRunOfflineCheckerFromAParkedResumeClearsTheParkReason() throws ThingsboardException {
        stored.setState(CommunityGrantState.RESUME);
        stored.setParkReason(CommunityGrantParkReason.LINK_EXPIRED);
        stored.setMode(CommunityGrantMode.OFFLINE);
        when(reportRunner.runUploadedChecker(any(), any(), any(), any()))
                .thenReturn("report-body");

        runChecker();

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.VALIDATING);
        assertThat(stored.getParkReason()).isNull();
    }

    @Test
    void testRunOfflineCheckerLeavesATerminalStateAlone() throws ThingsboardException {
        stored.setState(CommunityGrantState.ALREADY_REGISTERED);
        stored.setMode(CommunityGrantMode.OFFLINE);
        when(reportRunner.runUploadedChecker(any(), any(), any(), any()))
                .thenReturn("report-body");

        runChecker();

        verify(offlineReportStore).put("report-body");
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.ALREADY_REGISTERED);
        assertThat(stored.getLastOfflineCheckAt()).isNotNull();
    }

    @Test
    void testRunOfflineCheckerLeavesAnUnderReviewFlowAlone() throws ThingsboardException {
        stored.setState(CommunityGrantState.UNDER_REVIEW);
        stored.setMode(CommunityGrantMode.OFFLINE);
        when(reportRunner.runUploadedChecker(any(), any(), any(), any()))
                .thenReturn("report-body");

        runChecker();

        verify(offlineReportStore).put("report-body");
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.UNDER_REVIEW);
    }

    @Test
    void testRequestAccessIsRefusedOnTheWrongInstallationRefusal() {
        stored.setState(CommunityGrantState.WRONG_INSTALLATION);

        assertThatThrownBy(() -> service.requestAccess())
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.NOT_ALREADY_REGISTERED_MESSAGE);
        verifyNoInteractions(portalClient);
    }

    @Test
    void testRequestAccessCallsThePortalWithTheClusterId() throws ThingsboardException {
        stored.setState(CommunityGrantState.ALREADY_REGISTERED);

        service.requestAccess();

        verify(portalClient).requestAccess(CLUSTER_ID);
    }

    @Test
    void testRequestAccessFailsCleanlyWhenThePortalCallFails() {
        stored.setState(CommunityGrantState.ALREADY_REGISTERED);
        doThrow(new RuntimeException("connect timed out")).when(portalClient).requestAccess(CLUSTER_ID);

        assertThatThrownBy(() -> service.requestAccess())
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.REQUEST_ACCESS_FAILED_MESSAGE);
    }

    @Test
    void testRequestAccessIsRefusedWhenNotAlreadyRegistered() {
        stored.setState(CommunityGrantState.AWAITING_SIGNUP);

        assertThatThrownBy(() -> service.requestAccess())
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.NOT_ALREADY_REGISTERED_MESSAGE);
        verify(portalClient, never()).requestAccess(any());
    }

    @Test
    void testRequestAccessIsRefusedWhenTheFeatureIsDisabled() {
        ReflectionTestUtils.setField(service, "enabled", false);
        stored.setState(CommunityGrantState.ALREADY_REGISTERED);

        assertThatThrownBy(() -> service.requestAccess()).isInstanceOf(ThingsboardException.class);
        verify(portalClient, never()).requestAccess(any());
    }

    @Test
    void testRequestAccessIsRateLimitedWithinCooldownWindow() {
        stored.setState(CommunityGrantState.ALREADY_REGISTERED);
        stored.setLastRequestAccessAt(System.currentTimeMillis() - 1000);

        assertThatThrownBy(() -> service.requestAccess())
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.REQUEST_ACCESS_RATE_LIMITED_MESSAGE);
        verify(portalClient, never()).requestAccess(any());
    }

    @Test
    void testRequestAccessIsAllowedAfterTheCooldownWindowElapses() throws ThingsboardException {
        stored.setState(CommunityGrantState.ALREADY_REGISTERED);
        stored.setLastRequestAccessAt(System.currentTimeMillis()
                - DefaultCommunityGrantService.REQUEST_ACCESS_COOLDOWN_MS - 1000);

        service.requestAccess();

        verify(portalClient).requestAccess(CLUSTER_ID);
    }

    @Test
    void testRequestAccessRecordsTheTimestampEvenWhenThePortalCallFails() {
        stored.setState(CommunityGrantState.ALREADY_REGISTERED);
        doThrow(new RuntimeException("connect timed out")).when(portalClient).requestAccess(CLUSTER_ID);

        assertThatThrownBy(() -> service.requestAccess()).isInstanceOf(ThingsboardException.class);

        assertThat(stored.getLastRequestAccessAt()).isNotNull();
    }

    @Test
    void testRequestAccessFailsWhenTheDeploymentHasNoClusterId() {
        stored.setState(CommunityGrantState.ALREADY_REGISTERED);
        when(tbClusterStore.getClusterId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestAccess())
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.NO_CLUSTER_ID_MESSAGE);
        verify(portalClient, never()).requestAccess(any());
    }

    @Test
    void testRequestAccessFailsCleanlyWhenTheFlowStateCannotBePersisted() {
        stored.setState(CommunityGrantState.ALREADY_REGISTERED);
        doThrow(new IllegalStateException("stored document is unreadable")).when(flowStateStore).update(any());

        assertThatThrownBy(() -> service.requestAccess())
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.REQUEST_ACCESS_STORE_FAILED_MESSAGE)
                .hasNoCause();
        verify(portalClient, never()).requestAccess(any());
    }

    @Test
    void testRequestAccessFailsCleanlyWhenTheClusterIdLookupFails() {
        stored.setState(CommunityGrantState.ALREADY_REGISTERED);
        when(tbClusterStore.getClusterId()).thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> service.requestAccess())
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.REQUEST_ACCESS_STORE_FAILED_MESSAGE);
        verify(portalClient, never()).requestAccess(any());
        verify(flowStateStore, never()).update(any());
    }

    @Test
    void testGetOfflineReportReturnsTheStoredReportVerbatim() throws ThingsboardException {
        when(offlineReportStore.get()).thenReturn(Optional.of("-----BEGIN TB INSTANCE CHECK-----\nbody"));

        assertThat(service.getOfflineReport()).isEqualTo("-----BEGIN TB INSTANCE CHECK-----\nbody");
    }

    @Test
    void testGetOfflineReportRefusesWhenNoCheckHasRun() {
        when(offlineReportStore.get()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOfflineReport())
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.NO_OFFLINE_REPORT_MESSAGE)
                .extracting(e -> ((ThingsboardException) e).getErrorCode())
                .isEqualTo(ThingsboardErrorCode.ITEM_NOT_FOUND);
    }

    @Test
    void testConfirmOfflineHandoffRecordsTheClaimAndAnswersWithTheReport() throws ThingsboardException {
        stored.setState(CommunityGrantState.VALIDATING);
        stored.setMode(CommunityGrantMode.OFFLINE);
        stored.setLastOfflineCheckAt(System.currentTimeMillis());
        when(offlineReportStore.get()).thenReturn(Optional.of("report-body"));

        CommunityGrantStateInfo info = service.confirmOfflineHandoff();

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.REPORT_HANDED_OVER);
        assertThat(info.state()).isEqualTo(CommunityGrantState.REPORT_HANDED_OVER);
        assertThat(info.offlineReport()).isEqualTo("report-body");
        // Online the poller is already armed; offline it stays disarmed.
        verifyNoInteractions(poller);
    }

    @Test
    void testConfirmOfflineHandoffIsIdempotent() throws ThingsboardException {
        stored.setState(CommunityGrantState.REPORT_HANDED_OVER);
        stored.setLastOfflineCheckAt(System.currentTimeMillis());
        when(offlineReportStore.get()).thenReturn(Optional.of("report-body"));

        CommunityGrantStateInfo info = service.confirmOfflineHandoff();

        assertThat(info.state()).isEqualTo(CommunityGrantState.REPORT_HANDED_OVER);
        assertThat(info.offlineReport()).isEqualTo("report-body");
        verify(flowStateStore, never()).update(any());
    }

    @ParameterizedTest
    @EnumSource(value = CommunityGrantState.class, mode = EnumSource.Mode.EXCLUDE,
            names = {"VALIDATING", "REPORT_HANDED_OVER"})
    void testConfirmOfflineHandoffIsRefusedFromEveryOtherState(CommunityGrantState state) {
        stored.setState(state);

        assertThatThrownBy(() -> service.confirmOfflineHandoff())
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.NOTHING_TO_HAND_OVER_MESSAGE);
        verify(flowStateStore, never()).update(any());
    }

    @Test
    void testConfirmOfflineHandoffIsRefusedWhenTheFeatureIsDisabled() {
        ReflectionTestUtils.setField(service, "enabled", false);
        stored.setState(CommunityGrantState.VALIDATING);

        assertThatThrownBy(() -> service.confirmOfflineHandoff())
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.NOT_AVAILABLE_MESSAGE)
                .extracting(e -> ((ThingsboardException) e).getErrorCode())
                .isEqualTo(ThingsboardErrorCode.ITEM_NOT_FOUND);
        verify(flowStateStore, never()).update(any());
    }

    // A poll can land between this call's read and write; the updater re-tests under the store's lock.
    @Test
    void testConfirmOfflineHandoffLosesToADecisionThatLandsMidCall() throws ThingsboardException {
        stored.setState(CommunityGrantState.VALIDATING);
        doAnswer(invocation -> {
            // The poller's retire() landing between the read and the write.
            stored.setState(CommunityGrantState.REGISTERED);
            UnaryOperator<CommunityGrantFlowState> updater = invocation.getArgument(0);
            stored = updater.apply(stored);
            return stored;
        }).when(flowStateStore).update(any());

        CommunityGrantStateInfo info = service.confirmOfflineHandoff();

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.REGISTERED);
        assertThat(info.state()).isEqualTo(CommunityGrantState.REGISTERED);
    }

    @Test
    void testConfirmOfflineHandoffFailsCleanlyWhenTheFlowStateCannotBePersisted() {
        stored.setState(CommunityGrantState.VALIDATING);
        doThrow(new IllegalStateException("connection refused")).when(flowStateStore).update(any());

        assertThatThrownBy(() -> service.confirmOfflineHandoff())
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.HANDOFF_FAILED_MESSAGE)
                .hasNoCause();
    }

    @Test
    void testStartIsRefusedAfterTheReportHasBeenHandedOverAndLeavesTheReportInPlace() {
        stored.setState(CommunityGrantState.REPORT_HANDED_OVER);
        stored.setMode(CommunityGrantMode.OFFLINE);
        stored.setLastOfflineCheckAt(System.currentTimeMillis());
        givenReachablePortal();
        when(offlineReportStore.get()).thenReturn(Optional.of("report-body"));

        assertThatThrownBy(() -> service.start())
                .isInstanceOf(ThingsboardException.class)
                .hasMessage(DefaultCommunityGrantService.REPORT_HANDED_OVER_MESSAGE);

        verify(offlineReportStore, never()).clear();
        verify(tbClusterStore, never()).saveLicenseClaimToken(anyString());
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.REPORT_HANDED_OVER);
        assertThat(service.getStateInfo().offlineReport()).isEqualTo("report-body");
    }

    /** Holds submitted tasks until the test runs them, so a test can act between a start and its completion. */
    private static final class QueuedExecutorService extends AbstractExecutorService {
        private final List<Runnable> tasks = new ArrayList<>();

        void runAll() {
            List<Runnable> queued = new ArrayList<>(tasks);
            tasks.clear();
            queued.forEach(Runnable::run);
        }

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }
    }

}
