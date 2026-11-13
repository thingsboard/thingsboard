// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.community_grant.CommunityGrantFlowState;
import org.thingsboard.server.common.data.community_grant.CommunityGrantMode;
import org.thingsboard.server.common.data.community_grant.CommunityGrantParkReason;
import org.thingsboard.server.common.data.community_grant.CommunityGrantState;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.subscription.TbClusterStore;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityGrantPollerTest {

    private static final String TOKEN = "token-one";
    private static final String LICENSE_SECRET = "granted-license-key";
    private static final String SIGN_UP_URL =
            "https://portal.example/communityGrant?clusterId=c1&claimToken=t1";
    private static final UUID CLUSTER_ID = UUID.fromString("2f0a5b1e-1111-4a2b-9c3d-4e5f60718293");
    private static final String CHECKER_INPUT_JSON = "{\"clusterId\":\"" + CLUSTER_ID + "\",\"keyId\":7,"
            + "\"publicKey\":\"cHVi\",\"challenge\":\"Y2hh\",\"referenceMillis\":1787788800000}";
    private static final CommunityGrantCheckerInput CHECKER_INPUT = new CommunityGrantCheckerInput(CHECKER_INPUT_JSON);

    @Mock
    private PartitionService partitionService;
    @Mock
    private TbClusterStore tbClusterStore;
    @Mock
    private CommunityGrantFlowStateStore flowStateStore;
    @Mock
    private CommunityGrantPortalClient portalClient;
    @Mock
    private CommunityGrantReportRunner reportRunner;

    /** Keeps real background ticks away from the mocks, and lets the reschedule be asserted. */
    @Mock
    private ScheduledExecutorService scheduler;
    @Mock
    private ScheduledFuture<Object> scheduledPoll;
    @Mock
    private ScheduledFuture<Object> supervisorFuture;

    @InjectMocks
    private CommunityGrantPoller poller;

    private CommunityGrantFlowState stored;

    @BeforeEach
    void setUp() {
        // Without enabled, init() returns before arming.
        ReflectionTestUtils.setField(poller, "enabled", true);
        ReflectionTestUtils.setField(poller, "fastPollIntervalMs", 3000L);
        ReflectionTestUtils.setField(poller, "fastPollWindowMs", 120000L);
        ReflectionTestUtils.setField(poller, "slowPollIntervalMs", 30000L);
        ReflectionTestUtils.setField(poller, "signUpDeadlineMs", 86400000L);
        ReflectionTestUtils.setField(poller, "superviseIntervalMs", 30000L);
        ReflectionTestUtils.setField(poller, "scheduler", scheduler);
        stored = CommunityGrantFlowState.initial();
        stored.setMode(CommunityGrantMode.ONLINE);
        stored.setState(CommunityGrantState.AWAITING_SIGNUP);
        stored.setStartedAt(System.currentTimeMillis());
        lenient().when(flowStateStore.get()).thenAnswer(invocation -> stored);
        lenient().when(flowStateStore.update(any())).thenAnswer(invocation -> {
            UnaryOperator<CommunityGrantFlowState> updater = invocation.getArgument(0);
            stored = updater.apply(stored);
            return stored;
        });
        lenient().when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of(TOKEN));
        lenient().when(portalClient.claimLicense(TOKEN)).thenReturn(LICENSE_SECRET);
        lenient().when(tbClusterStore.getClusterId()).thenReturn(Optional.of(CLUSTER_ID));
        lenient().doReturn(scheduledPoll).when(scheduler).schedule(any(Runnable.class), anyLong(), any());
        // Non-null, so the supervisor's already-running guard can trip.
        lenient().doReturn(supervisorFuture).when(scheduler)
                .scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());
        ownsSystemTenantPartition(true);
        poller.arm();
    }

    @AfterEach
    void tearDown() {
        poller.stop();
    }

    @Test
    void testAwaitingSignupIsProjected() {
        poller.apply(TOKEN, status("AWAITING_SIGNUP", null));
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
    }

    @Test
    void testAwaitingSignupPastTheDeadlineParksTheFlow() {
        stored.setStartedAt(System.currentTimeMillis() - 86400000L - 1000L);

        poller.apply(TOKEN, status("AWAITING_SIGNUP", null));

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.RESUME);
        assertThat(stored.getSignUpUrl()).isNull();
        assertThat(stored.getParkReason()).isEqualTo(CommunityGrantParkReason.SIGNUP_ABANDONED);
        assertThat(poller.isArmed()).isFalse();
        verify(tbClusterStore).clearLicenseClaimToken(TOKEN);
    }

    @Test
    void testVerificationRequiredRunsTheReportWithThePortalsInput() {
        when(reportRunner.runAndUpload(TOKEN, CHECKER_INPUT)).thenReturn("UNDER_REVIEW");

        poller.apply(TOKEN, verificationRequired());

        verify(reportRunner).runAndUpload(TOKEN, CHECKER_INPUT);
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.UNDER_REVIEW);
    }

    @Test
    void testVerificationRequiredUploadThatIsAcceptedOutrightIsTerminal() {
        when(reportRunner.runAndUpload(TOKEN, CHECKER_INPUT)).thenReturn("DONE");

        poller.apply(TOKEN, verificationRequired());

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.REGISTERED);
        verify(tbClusterStore).clearLicenseClaimToken(TOKEN);
    }

    @Test
    void testVerificationRequiredUploadUnderReviewKeepsTheTokenAndKeepsWatching() {
        when(reportRunner.runAndUpload(TOKEN, CHECKER_INPUT)).thenReturn("UNDER_REVIEW");

        poller.apply(TOKEN, verificationRequired());

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.UNDER_REVIEW);
        assertThat(poller.isArmed()).isTrue();
        verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
    }

    @Test
    void testVerificationRequiredUploadWrongInstallationIsTerminal() {
        when(reportRunner.runAndUpload(TOKEN, CHECKER_INPUT)).thenReturn("WRONG_INSTALLATION");

        poller.apply(TOKEN, verificationRequired());

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.WRONG_INSTALLATION);
        verify(tbClusterStore).clearLicenseClaimToken(TOKEN);
    }

    @Test
    void testVerificationRequiredUploadWithAnUnknownStatusLeavesTheFlowCollecting() {
        when(reportRunner.runAndUpload(TOKEN, CHECKER_INPUT)).thenReturn("SOMETHING_NEW");

        poller.apply(TOKEN, verificationRequired());

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.COLLECTING);
        verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
    }

    @Test
    void testVerificationRequiredUploadReturningNullLeavesTheFlowCollecting() {
        when(reportRunner.runAndUpload(TOKEN, CHECKER_INPUT)).thenReturn(null);

        poller.apply(TOKEN, verificationRequired());

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.COLLECTING);
        verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
    }

    // Refused before the runner, so a status that was never runnable does not spend the verification budget.
    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "[]", "\"text\"", "7"})
    void testVerificationRequiredWithoutAUsableInputNeverRunsTheChecker(String checkerInput) {
        AtomicInteger verificationFailures =
                (AtomicInteger) ReflectionTestUtils.getField(poller, "verificationFailures");

        poller.apply(TOKEN, new CommunityGrantPortalStatus("VERIFICATION_REQUIRED",
                checkerInput.isEmpty() ? null : JacksonUtil.toJsonNode(checkerInput), null));

        verifyNoInteractions(reportRunner);
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.COLLECTING);
        assertThat(verificationFailures).hasValue(0);
    }

    @Test
    void testVerificationRequiredInOfflineModeNeverRunsTheChecker() {
        stored.setMode(CommunityGrantMode.OFFLINE);

        poller.apply(TOKEN, verificationRequired());

        verifyNoInteractions(reportRunner);
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.COLLECTING);
    }

    @Test
    void testAFailedReportRunLeavesTheFlowCollecting() {
        when(reportRunner.runAndUpload(TOKEN, CHECKER_INPUT)).thenThrow(new IllegalStateException("boom"));

        poller.apply(TOKEN, verificationRequired());

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.COLLECTING);
        verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
    }

    @Test
    void testRepeatedReportFailuresParkTheFlow() {
        when(reportRunner.runAndUpload(TOKEN, CHECKER_INPUT)).thenThrow(new IllegalStateException("boom"));

        for (int i = 0; i < CommunityGrantPoller.MAX_VERIFICATION_FAILURES; i++) {
            poller.apply(TOKEN, verificationRequired());
        }

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.RESUME);
        assertThat(stored.getParkReason()).isEqualTo(CommunityGrantParkReason.CHECK_FAILED);
        assertThat(poller.isArmed()).isFalse();
        verify(tbClusterStore).clearLicenseClaimToken(TOKEN);
    }

    // Only the state write is skipped; lastPolledAt still advances so the flow does not look stalled.
    @Test
    void testVerificationRequiredLeavesAHandedOverFlowClaimedButKeepsItsPollFresh() {
        stored.setState(CommunityGrantState.REPORT_HANDED_OVER);
        stored.setLastPolledAt(1L);

        poller.apply(TOKEN, verificationRequired());

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.REPORT_HANDED_OVER);
        assertThat(stored.getLastPolledAt()).isGreaterThan(1L);
        verify(reportRunner).runAndUpload(TOKEN, CHECKER_INPUT);
    }

    @Test
    void testRepeatedReportFailuresDoNotParkAHandedOverFlow() {
        stored.setState(CommunityGrantState.REPORT_HANDED_OVER);
        when(reportRunner.runAndUpload(TOKEN, CHECKER_INPUT)).thenThrow(new IllegalStateException("boom"));

        for (int i = 0; i < CommunityGrantPoller.MAX_VERIFICATION_FAILURES; i++) {
            poller.apply(TOKEN, verificationRequired());
        }

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.REPORT_HANDED_OVER);
        assertThat(stored.getParkReason()).isNull();
        assertThat(poller.isArmed()).isTrue();
        verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
    }

    @Test
    void testVerificationRequiredLeavesAValidatingFlowClaimedButKeepsItsPollFresh() {
        stored.setState(CommunityGrantState.VALIDATING);
        stored.setLastPolledAt(1L);

        poller.apply(TOKEN, verificationRequired());

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.VALIDATING);
        assertThat(stored.getLastPolledAt()).isGreaterThan(1L);
    }

    @Test
    void testVerificationRequiredNeverRunsTheCheckerForAValidatingFlow() {
        stored.setState(CommunityGrantState.VALIDATING);

        for (int i = 0; i < CommunityGrantPoller.MAX_VERIFICATION_FAILURES; i++) {
            poller.apply(TOKEN, verificationRequired());
        }

        verifyNoInteractions(reportRunner);
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.VALIDATING);
        assertThat(stored.getParkReason()).isNull();
        assertThat(poller.isArmed()).isTrue();
        verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
    }

    @Test
    void testAValidatingAnswerIsTreatedAsUnrecognizedAndChangesNothing() {
        poller.apply(TOKEN, status("VALIDATING", null));

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
    }

    @Test
    void testDoneIsTerminalAndRetiresTheToken() {
        poller.apply(TOKEN, status("DONE", null));
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.REGISTERED);
        verify(tbClusterStore).clearLicenseClaimToken(TOKEN);
    }

    @Test
    void testDoneStoresTheGrantedLicenseKeyBeforeClearingTheToken() {
        poller.apply(TOKEN, status("DONE", null));

        InOrder inOrder = inOrder(tbClusterStore);
        inOrder.verify(tbClusterStore).saveLicenseSecret(LICENSE_SECRET);
        inOrder.verify(tbClusterStore).clearLicenseClaimToken(TOKEN);
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.REGISTERED);
    }

    @Test
    void testDoneWithNoKeyToHandOverYetLeavesTheFlowForTheNextPoll() {
        when(portalClient.claimLicense(TOKEN)).thenReturn(null);

        poller.apply(TOKEN, status("DONE", null));

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        assertThat(poller.isArmed()).isTrue();
        verify(tbClusterStore, never()).saveLicenseSecret(anyString());
        verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
    }

    @Test
    void testDoneWithARefusedClaimRegistersWithoutAKey() {
        when(portalClient.claimLicense(TOKEN)).thenThrow(new CommunityGrantLicenseClaimRefusedException(
                new HttpClientErrorException(HttpStatus.BAD_REQUEST)));

        poller.apply(TOKEN, status("DONE", null));

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.REGISTERED);
        assertThat(poller.isArmed()).isFalse();
        verify(tbClusterStore, never()).saveLicenseSecret(anyString());
        verify(tbClusterStore).clearLicenseClaimToken(TOKEN);
    }

    @Test
    void testDoneWithAnUnreachablePortalIsLeftToTheFailureBackoff() {
        when(portalClient.getStatus(TOKEN, CLUSTER_ID)).thenReturn(status("DONE", null));
        when(portalClient.claimLicense(TOKEN)).thenThrow(new ResourceAccessException("connection refused"));
        clearInvocations(scheduler);

        poller.pollAndReschedule();

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        assertThat(poller.isArmed()).isTrue();
        verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
        verify(scheduler).schedule(any(Runnable.class), eq(3000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void testDoneKeepsTheRegistrationLinkWhileRetiringEverythingElseAboutTheClaim() {
        stored.setSignUpUrl(SIGN_UP_URL);

        poller.apply(TOKEN, status("DONE", null));

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.REGISTERED);
        assertThat(stored.getSignUpUrl()).isEqualTo(SIGN_UP_URL);
        assertThat(stored.getStartedAt()).isNull();
        verify(tbClusterStore).clearLicenseClaimToken(TOKEN);
    }

    @Test
    void testAWrongInstallationAnswerDropsTheRegistrationLink() {
        stored.setSignUpUrl(SIGN_UP_URL);

        poller.apply(TOKEN, status("WRONG_INSTALLATION", null));

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.WRONG_INSTALLATION);
        assertThat(stored.getSignUpUrl()).isNull();
    }

    @Test
    void testAParkDropsTheRegistrationLink() {
        stored.setSignUpUrl(SIGN_UP_URL);

        poller.apply(TOKEN, status("EXPIRED", null));

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.RESUME);
        assertThat(stored.getSignUpUrl()).isNull();
    }

    @Test
    void testUnderReviewKeepsTheTokenAndKeepsWatching() {
        poller.apply(TOKEN, status("UNDER_REVIEW", 30));

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.UNDER_REVIEW);
        assertThat(stored.getLastPolledAt()).isNotNull();
        assertThat(poller.isArmed()).isTrue();
        verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
    }

    @Test
    void testUnderReviewFollowedByDoneEndsRegistered() {
        poller.apply(TOKEN, status("UNDER_REVIEW", 30));
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.UNDER_REVIEW);

        poller.apply(TOKEN, status("DONE", null));

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.REGISTERED);
        assertThat(poller.isArmed()).isFalse();
        verify(tbClusterStore).clearLicenseClaimToken(TOKEN);
    }

    @Test
    void testUnderReviewIsPacedByThePortalsRetryAfter() {
        when(portalClient.getStatus(TOKEN, CLUSTER_ID)).thenReturn(status("UNDER_REVIEW", 300));

        assertThat(poller.nextDelay(poller.poll())).isEqualTo(TimeUnit.SECONDS.toMillis(300));
    }

    @Test
    void testAnUnderReviewAnswerForAClearedTokenIsNotWritten() {
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.empty());

        poller.apply(TOKEN, status("UNDER_REVIEW", 30));

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
    }

    @Test
    void testAwaitingSignupAndVerificationRequiredAreAlsoSkippedForAReplacedToken() {
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("a-newer-token"));
        stored.setState(CommunityGrantState.NOT_STARTED);

        poller.apply(TOKEN, status("AWAITING_SIGNUP", null));
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.NOT_STARTED);

        poller.apply(TOKEN, verificationRequired());
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.NOT_STARTED);
        assertThat(stored.getLastPolledAt()).isNull();
        verifyNoInteractions(reportRunner);
    }

    @Test
    void testWrongInstallationIsTerminalAndRetiresTheToken() {
        poller.apply(TOKEN, status("WRONG_INSTALLATION", null));
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.WRONG_INSTALLATION);
        assertThat(stored.getParkReason()).isNull();
        verify(tbClusterStore).clearLicenseClaimToken(TOKEN);
    }

    @Test
    void testExpiredParksTheFlowForARestart() {
        poller.apply(TOKEN, status("EXPIRED", null));
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.RESUME);
        assertThat(stored.getSignUpUrl()).isNull();
        assertThat(stored.getParkReason()).isEqualTo(CommunityGrantParkReason.LINK_EXPIRED);
        verify(tbClusterStore).clearLicenseClaimToken(TOKEN);
    }

    @Test
    void testAnAnswerForASupersededTokenLeavesTheStateAndTheLoopAlone() {
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("token-two"));

        poller.apply(TOKEN, status("DONE", null));

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        assertThat(poller.isArmed()).isTrue();
        verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
    }

    @Test
    void testAnUnknownStatusLeavesTheStateAlone() {
        poller.apply(TOKEN, status("SOMETHING_NEW", null));
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
    }

    @Test
    void testApplyWithANullStatusValueLeavesStateAloneAndDoesNotThrow() {
        poller.apply(TOKEN, status(null, null));
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
        verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
    }

    @Test
    void testTerminalStateAlsoClearsStartedAt() {
        poller.apply(TOKEN, status("DONE", null));
        assertThat(stored.getStartedAt()).isNull();
    }

    @Test
    void testTerminalStateStopsAllFurtherOutboundCalls() {
        poller.apply(TOKEN, status("DONE", null));

        assertThat(poller.poll()).isNull();

        verify(portalClient, never()).getStatus(anyString(), any());
    }

    @Test
    void testPollWithoutATokenDisarms() {
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.empty());

        poller.poll();

        verify(portalClient, never()).getStatus(anyString(), any());
        assertThat(poller.isArmed()).isFalse();
    }

    @Test
    void testPollWithoutAClusterIdDisarms() {
        when(tbClusterStore.getClusterId()).thenReturn(Optional.empty());

        poller.poll();

        verify(portalClient, never()).getStatus(anyString(), any());
        assertThat(poller.isArmed()).isFalse();
    }

    // The loop stays armed because its tick enforces the sign-up deadline.
    @Test
    void testPollSkipsThePortalButKeepsTheLoopWhenTheModeHasSinceBecomeOffline() {
        assertThat(poller.isArmed()).isTrue();
        stored.setMode(CommunityGrantMode.OFFLINE);

        assertThat(poller.poll()).isNull();

        verify(portalClient, never()).getStatus(anyString(), any());
        assertThat(poller.isArmed()).isTrue();
    }

    @Test
    void testAnOfflineEnrollmentIsParkedAtTheSignUpDeadlineWithoutEverCallingThePortal() {
        stored.setMode(CommunityGrantMode.OFFLINE);
        stored.setStartedAt(System.currentTimeMillis() - 86400000L - 1000L);

        assertThat(poller.poll()).isNull();

        verify(portalClient, never()).getStatus(anyString(), any());
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.RESUME);
        assertThat(stored.getParkReason()).isEqualTo(CommunityGrantParkReason.SIGNUP_ABANDONED);
        verify(tbClusterStore).clearLicenseClaimToken(TOKEN);
        assertThat(poller.isArmed()).isFalse();
    }

    @Test
    void testAnOfflineTickInsideTheDeadlineTouchesNothing() {
        stored.setMode(CommunityGrantMode.OFFLINE);

        assertThat(poller.poll()).isNull();

        verify(portalClient, never()).getStatus(anyString(), any());
        verify(flowStateStore, never()).update(any());
        assertThat(stored.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
    }

    @Test
    void testAnOfflineFlowPastTheSignUpFormStopsTheLoop() {
        assertThat(poller.isArmed()).isTrue();
        stored.setMode(CommunityGrantMode.OFFLINE);
        stored.setState(CommunityGrantState.VALIDATING);

        assertThat(poller.poll()).isNull();

        verify(portalClient, never()).getStatus(anyString(), any());
        verify(flowStateStore, never()).update(any());
        assertThat(poller.isArmed()).isFalse();
    }

    @Test
    void testPollRecordsTheTimestampAndThrottlesTheNextCall() {
        when(portalClient.getStatus(TOKEN, CLUSTER_ID))
                .thenReturn(status("AWAITING_SIGNUP", null));

        poller.poll();
        assertThat(stored.getLastPolledAt()).isNotNull();

        poller.poll();
        verify(portalClient).getStatus(TOKEN, CLUSTER_ID);
    }

    @Test
    void testPollWithANullPortalResponseLeavesStateUnchanged() {
        when(portalClient.getStatus(TOKEN, CLUSTER_ID)).thenReturn(null);

        assertThat(poller.poll()).isNull();

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.AWAITING_SIGNUP);
    }

    // The armed flag is untouched on this path, so the reschedule is what is asserted.
    @Test
    void testPortalThrowingDuringAPollDoesNotKillTheScheduledLoop() {
        when(portalClient.getStatus(TOKEN, CLUSTER_ID)).thenThrow(new RuntimeException("portal unreachable"));
        clearInvocations(scheduler);

        poller.pollAndReschedule();

        assertThat(poller.isArmed()).isTrue();
        verify(scheduler).schedule(any(Runnable.class), eq(3000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void testFlowStateStoreThrowingDuringAPollDoesNotKillTheScheduledLoop() {
        when(portalClient.getStatus(TOKEN, CLUSTER_ID))
                .thenReturn(status("AWAITING_SIGNUP", null));
        doThrow(new IllegalStateException("unparseable stored document")).when(flowStateStore).update(any());
        clearInvocations(scheduler);

        poller.pollAndReschedule();

        assertThat(poller.isArmed()).isTrue();
        verify(scheduler).schedule(any(Runnable.class), eq(3000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void testTheFailureCountIsClearedOnlyByAPollThatReachedThePortal() {
        AtomicInteger consecutiveFailures =
                (AtomicInteger) ReflectionTestUtils.getField(poller, "consecutiveFailures");
        consecutiveFailures.set(4);

        // No claim token, so poll() returns before the portal.
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.empty());
        poller.pollAndReschedule();

        assertThat(consecutiveFailures).hasValue(4);
        verify(portalClient, never()).getStatus(anyString(), any());

        // arm() clears the count too, so it is planted again after.
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of(TOKEN));
        when(portalClient.getStatus(TOKEN, CLUSTER_ID))
                .thenReturn(status("AWAITING_SIGNUP", null));
        poller.arm();
        consecutiveFailures.set(4);
        ReflectionTestUtils.setField(poller, "lastPollTs", 0L);
        poller.pollAndReschedule();

        assertThat(consecutiveFailures).hasValue(0);
    }

    // Re-based in poll(), not arm(): the partition owner is not necessarily the node that served start().
    @Test
    void testANewClaimTokenReBasesTheFailureBudgets() {
        AtomicInteger consecutiveFailures =
                (AtomicInteger) ReflectionTestUtils.getField(poller, "consecutiveFailures");
        AtomicInteger verificationFailures =
                (AtomicInteger) ReflectionTestUtils.getField(poller, "verificationFailures");
        // Binds the poller to TOKEN and closes the throttle, so the next poll never reaches the portal.
        poller.poll();
        consecutiveFailures.set(CommunityGrantPoller.MAX_POLL_FAILURES - 1);
        verificationFailures.set(CommunityGrantPoller.MAX_VERIFICATION_FAILURES - 1);
        when(tbClusterStore.getLicenseClaimToken()).thenReturn(Optional.of("token-two"));

        poller.poll();

        assertThat(consecutiveFailures).hasValue(0);
        assertThat(verificationFailures).hasValue(0);
    }

    @Test
    void testTheFirstClaimTokenANodeMeetsLeavesTheBudgetsAlone() {
        AtomicInteger consecutiveFailures =
                (AtomicInteger) ReflectionTestUtils.getField(poller, "consecutiveFailures");
        AtomicInteger verificationFailures =
                (AtomicInteger) ReflectionTestUtils.getField(poller, "verificationFailures");
        consecutiveFailures.set(CommunityGrantPoller.MAX_POLL_FAILURES - 1);
        verificationFailures.set(CommunityGrantPoller.MAX_VERIFICATION_FAILURES - 1);
        // Closes the throttle, so the poll stops after the re-base.
        ReflectionTestUtils.setField(poller, "lastPollTs", System.currentTimeMillis());

        poller.poll();

        assertThat(consecutiveFailures).hasValue(CommunityGrantPoller.MAX_POLL_FAILURES - 1);
        assertThat(verificationFailures).hasValue(CommunityGrantPoller.MAX_VERIFICATION_FAILURES - 1);
    }

    @Test
    void testCadenceSwitchesFromFastToSlow() {
        assertThat(poller.nextDelay(null)).isEqualTo(3000L);
        ReflectionTestUtils.setField(poller, "pollingSince", System.currentTimeMillis() - 130000L);
        assertThat(poller.nextDelay(null)).isEqualTo(30000L);
    }

    @Test
    void testRetryAfterSecOverridesBothIntervals() {
        assertThat(poller.nextDelay(45)).isEqualTo(45000L);
        ReflectionTestUtils.setField(poller, "pollingSince", System.currentTimeMillis() - 130000L);
        assertThat(poller.nextDelay(1)).isEqualTo(1000L);
        assertThat(poller.nextDelay(0)).isEqualTo(30000L);
    }

    @Test
    void testNegativeRetryAfterSecFallsBackToTheLocalCadence() {
        assertThat(poller.nextDelay(-5)).isEqualTo(3000L);
    }

    @Test
    void testRetryAfterSecIsClampedToAnUpperBound() {
        assertThat(poller.nextDelay(Integer.MAX_VALUE)).isEqualTo(CommunityGrantPoller.MAX_RETRY_AFTER_SEC * 1000L);
    }

    @Test
    void testAnUnreachablePortalStillEndsAnAbandonedSignUpAtTheDeadline() {
        when(portalClient.getStatus(TOKEN, CLUSTER_ID)).thenThrow(new RuntimeException("portal unreachable"));
        stored.setStartedAt(System.currentTimeMillis() - 86400000L - 1000L);

        poller.pollAndReschedule();

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.RESUME);
        assertThat(stored.getParkReason()).isEqualTo(CommunityGrantParkReason.SIGNUP_ABANDONED);
        verify(tbClusterStore).clearLicenseClaimToken(TOKEN);
    }

    @Test
    void testAnUnreachablePortalParksTheFlowOnceTheFailureBudgetIsSpent() {
        when(portalClient.getStatus(TOKEN, CLUSTER_ID)).thenThrow(new RuntimeException("portal unreachable"));
        stored.setState(CommunityGrantState.UNDER_REVIEW);
        AtomicInteger consecutiveFailures =
                (AtomicInteger) ReflectionTestUtils.getField(poller, "consecutiveFailures");
        consecutiveFailures.set(CommunityGrantPoller.MAX_POLL_FAILURES - 1);
        ReflectionTestUtils.setField(poller, "lastPollTs", 0L);

        poller.pollAndReschedule();

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.RESUME);
        assertThat(stored.getParkReason()).isEqualTo(CommunityGrantParkReason.PORTAL_UNREACHABLE);
        verify(tbClusterStore).clearLicenseClaimToken(TOKEN);
        assertThat(poller.isArmed()).isFalse();
    }

    @Test
    void testAFailureBelowTheBudgetLeavesTheFlowAlone() {
        when(portalClient.getStatus(TOKEN, CLUSTER_ID)).thenThrow(new RuntimeException("portal unreachable"));
        stored.setState(CommunityGrantState.UNDER_REVIEW);

        poller.pollAndReschedule();

        assertThat(stored.getState()).isEqualTo(CommunityGrantState.UNDER_REVIEW);
        verify(tbClusterStore, never()).clearLicenseClaimToken(anyString());
    }

    @Test
    void testTheDelayGrowsWithConsecutiveFailuresUpToACap() {
        assertThat(poller.failureDelay(1)).isEqualTo(3000L);
        assertThat(poller.failureDelay(3)).isEqualTo(12000L);
        assertThat(poller.failureDelay(30)).isEqualTo(CommunityGrantPoller.MAX_FAILURE_BACKOFF_MS);
    }

    @Test
    void testInitArmsOnlyFromAPollableStoredState() {
        poller.disarm();
        stored.setState(CommunityGrantState.NOT_STARTED);
        poller.init();
        assertThat(poller.isArmed()).isFalse();

        stored.setState(CommunityGrantState.VALIDATING);
        poller.init();
        assertThat(poller.isArmed()).isTrue();
    }

    @Test
    void testInitDoesNotArmFromATerminalState() {
        poller.disarm();
        stored.setState(CommunityGrantState.REGISTERED);
        poller.init();
        assertThat(poller.isArmed()).isFalse();
        verify(reportRunner, never()).runAndUpload(anyString(), any());
        verify(portalClient, never()).getStatus(eq(TOKEN), any());
    }

    @Test
    void testInitDoesNotResumeOnANodeThatDoesNotOwnTheSystemTenantPartition() {
        poller.disarm();
        stored.setState(CommunityGrantState.VALIDATING);
        ownsSystemTenantPartition(false);

        poller.init();

        assertThat(poller.isArmed()).isFalse();
    }

    @Test
    void testTakingOverTheSystemTenantPartitionResumesAPollableFlow() {
        poller.disarm();
        stored.setState(CommunityGrantState.VALIDATING);

        poller.onTbApplicationEvent(corePartitionChangeEvent());

        assertThat(poller.isArmed()).isTrue();
    }

    @Test
    void testLosingTheSystemTenantPartitionStopsTheLoop() {
        ownsSystemTenantPartition(false);

        poller.onTbApplicationEvent(corePartitionChangeEvent());

        assertThat(poller.isArmed()).isFalse();
    }

    @Test
    void testArmDoesNothingWhenDisabled() {
        poller.disarm();
        ReflectionTestUtils.setField(poller, "enabled", false);

        poller.arm();

        assertThat(poller.isArmed()).isFalse();
    }

    @Test
    void testAnOfflineDeploymentArmsSoItsDeadlineIsStillEnforced() {
        poller.disarm();
        stored.setMode(CommunityGrantMode.OFFLINE);

        poller.arm();

        assertThat(poller.isArmed()).isTrue();
    }

    @Test
    void testInitArmsAnOfflineDeploymentStillWaitingOnTheSignUpForm() {
        poller.disarm();
        stored.setMode(CommunityGrantMode.OFFLINE);
        stored.setState(CommunityGrantState.AWAITING_SIGNUP);

        poller.init();

        assertThat(poller.isArmed()).isTrue();
    }

    @Test
    void testInitLeavesAnOfflineDeploymentPastTheSignUpFormDisarmed() {
        poller.disarm();
        stored.setMode(CommunityGrantMode.OFFLINE);
        stored.setState(CommunityGrantState.VALIDATING);

        poller.init();

        assertThat(poller.isArmed()).isFalse();
    }

    @Test
    void testInitArmsAnOnlineDeploymentAndItsPollReachesThePortal() {
        poller.disarm();
        stored.setMode(CommunityGrantMode.ONLINE);
        when(portalClient.getStatus(TOKEN, CLUSTER_ID))
                .thenReturn(status("AWAITING_SIGNUP", null));

        poller.init();
        assertThat(poller.isArmed()).isTrue();

        poller.poll();
        verify(portalClient).getStatus(TOKEN, CLUSTER_ID);
    }

    @Test
    void testArmDoesNothingOnANodeThatDoesNotOwnTheSystemTenantPartition() {
        poller.disarm();
        clearInvocations(scheduler); // setUp() armed this node while it did own the partition
        ownsSystemTenantPartition(false);

        poller.arm();

        assertThat(poller.isArmed()).isFalse();
        verify(scheduler, never()).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    void testTheOwnersSupervisorPicksUpAFlowBegunOnAnotherNode() {
        poller.disarm();
        stored.setState(CommunityGrantState.AWAITING_SIGNUP);

        poller.supervise();

        assertThat(poller.isArmed()).isTrue();
    }

    @Test
    void testTheSupervisorDoesNotArmATerminalFlow() {
        poller.disarm();
        stored.setState(CommunityGrantState.REGISTERED);

        poller.supervise();

        assertThat(poller.isArmed()).isFalse();
    }

    @Test
    void testAHandedOverFlowIsWatchedOnlineAndLeftAloneOffline() {
        poller.disarm();
        stored.setState(CommunityGrantState.REPORT_HANDED_OVER);
        stored.setMode(CommunityGrantMode.OFFLINE);

        poller.supervise();
        assertThat(poller.isArmed()).isFalse();

        stored.setMode(CommunityGrantMode.ONLINE);
        poller.supervise();
        assertThat(poller.isArmed()).isTrue();
    }

    @Test
    void testInitStartsTheSupervisor() {
        poller.init();

        verify(scheduler).scheduleWithFixedDelay(any(), eq(30000L), eq(30000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void testGainingTheSystemTenantPartitionStartsTheSupervisor() {
        poller.onTbApplicationEvent(corePartitionChangeEvent());

        verify(scheduler).scheduleWithFixedDelay(any(), eq(30000L), eq(30000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void testTheSupervisorIsStartedOnlyOnce() {
        poller.init();
        poller.init();

        verify(scheduler, times(1)).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());
    }

    @Test
    void testLosingTheSystemTenantPartitionStopsTheSupervisor() {
        poller.init();
        ownsSystemTenantPartition(false);

        poller.onTbApplicationEvent(corePartitionChangeEvent());

        verify(supervisorFuture).cancel(false);
    }

    @Test
    void testArmingAgainCancelsThePendingPollRatherThanAddingASecondChain() {
        clearInvocations(scheduler, scheduledPoll); // setUp() already armed, so one chain is queued

        poller.disarm();
        poller.arm();

        verify(scheduledPoll).cancel(false);
        verify(scheduler, times(1)).schedule(any(Runnable.class), eq(3000L), eq(TimeUnit.MILLISECONDS));
    }

    // Run through the captured Runnable: pollAndReschedule() passes the current generation and cannot mismatch.
    @Test
    void testAPollLeftOverFromAReplacedChainNeitherReschedulesItselfNorDisarms() {
        ArgumentCaptor<Runnable> queuedPoll = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(queuedPoll.capture(), anyLong(), any()); // setUp()'s arm() queued it

        poller.disarm();
        poller.arm();
        clearInvocations(scheduler); // the re-arm's own chain is the one allowed to be queued
        queuedPoll.getValue().run();

        verify(scheduler, never()).schedule(any(Runnable.class), anyLong(), any());
        verifyNoInteractions(portalClient);
        assertThat(poller.isArmed()).isTrue();
    }

    @Test
    void testARejectedScheduleLeavesTheLoopDisarmed() {
        poller.disarm();
        doThrow(new RejectedExecutionException("shutting down"))
                .when(scheduler).schedule(any(Runnable.class), anyLong(), any());

        poller.arm();

        assertThat(poller.isArmed()).isFalse();
    }

    private void ownsSystemTenantPartition(boolean mine) {
        lenient().when(partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID))
                .thenReturn(new TopicPartitionInfo("tb_core", TenantId.SYS_TENANT_ID, 0, mine));
    }

    private PartitionChangeEvent corePartitionChangeEvent() {
        return new PartitionChangeEvent(this, ServiceType.TB_CORE, Map.of(), Map.of());
    }

    private CommunityGrantPortalStatus status(String status, Integer retryAfterSec) {
        return new CommunityGrantPortalStatus(status, null, retryAfterSec);
    }

    private static CommunityGrantPortalStatus verificationRequired() {
        return new CommunityGrantPortalStatus("VERIFICATION_REQUIRED", JacksonUtil.toJsonNode(CHECKER_INPUT_JSON), null);
    }

}
