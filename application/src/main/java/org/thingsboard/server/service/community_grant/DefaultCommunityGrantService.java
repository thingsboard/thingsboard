// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.community_grant.CommunityGrantFlowState;
import org.thingsboard.server.common.data.community_grant.CommunityGrantMode;
import org.thingsboard.server.common.data.community_grant.CommunityGrantOfflineRun;
import org.thingsboard.server.common.data.community_grant.CommunityGrantOfflineRunStatus;
import org.thingsboard.server.common.data.community_grant.CommunityGrantState;
import org.thingsboard.server.common.data.community_grant.CommunityGrantStateInfo;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.dao.subscription.TbClusterStore;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultCommunityGrantService implements CommunityGrantService {

    static final int CLAIM_TOKEN_BYTES = 32;

    static final long REQUEST_ACCESS_COOLDOWN_MS = TimeUnit.MINUTES.toMillis(5);

    static final String NOT_AVAILABLE_MESSAGE = "This action is not available on this deployment.";
    static final String NO_CLUSTER_ID_MESSAGE =
            "This deployment is not fully installed yet. Restart the platform and try again.";
    private static final String START_FAILED_MESSAGE =
            "Failed to start. Please check that the database is reachable and try again.";
    private static final String ALREADY_FINISHED_MESSAGE = "This deployment has already completed this process.";
    static final String UNDER_REVIEW_MESSAGE =
            "This deployment's registration is with a reviewer. There is nothing to send again while it is.";
    static final String REPORT_HANDED_OVER_MESSAGE =
            "This deployment's check result has already been handed over. Starting again would abandon it.";
    static final String NOTHING_TO_HAND_OVER_MESSAGE =
            "This deployment is not waiting for a check result to be handed over.";
    static final String HANDOFF_FAILED_MESSAGE =
            "Failed to record the hand-over. Please check that the database is reachable and try again.";
    private static final String PORTAL_MISCONFIGURED_MESSAGE =
            "This deployment's connection to the license portal is misconfigured. Contact your administrator.";
    static final String NO_OFFLINE_REPORT_MESSAGE = "No result is available. Run the check first.";
    static final String OFFLINE_RUN_IN_PROGRESS_MESSAGE =
            "A check is already running on this deployment. Wait for it to finish and try again.";
    static final String OFFLINE_STATE_NOT_PERSISTED_MESSAGE =
            "The check completed, but this deployment could not record it. The result is still available for download.";
    static final String OFFLINE_RUN_START_FAILED_MESSAGE =
            "Failed to start the check. Please check that the database is reachable and try again.";
    static final String OFFLINE_RUN_NOT_SCHEDULED_MESSAGE =
            "Failed to start the check: this server is shutting down. Please try again.";
    static final String OFFLINE_RUN_INTERRUPTED_MESSAGE =
            "The check was interrupted before it finished. Please run it again.";
    static final String OFFLINE_RESULT_NOT_RECORDED_MESSAGE =
            "The check completed, but this deployment could not record its result. Please check that the database "
                    + "is reachable and run the check again.";
    static final String OFFLINE_RUN_SUPERSEDED_MESSAGE =
            "The check finished after a newer check or enrollment replaced it, so its result was discarded.";
    private static final String RUN_FAILED_PREFIX = "Failed to run the check. ";

    /** Past the checker's own timeout: room for writing the binary and recording the result. */
    static final long OFFLINE_RUN_GRACE_MS = TimeUnit.MINUTES.toMillis(2);
    static final String NOT_ALREADY_REGISTERED_MESSAGE =
            "This action is only available once this deployment is confirmed as already registered under another account.";
    static final String REQUEST_ACCESS_RATE_LIMITED_MESSAGE =
            "Please wait a few minutes before requesting access again.";
    static final String REQUEST_ACCESS_STORE_FAILED_MESSAGE =
            "Failed to process the request. Please check that the database is reachable and try again.";
    static final String REQUEST_ACCESS_FAILED_MESSAGE =
            "Failed to reach the license portal. Please try again later.";

    @Value("${community-grant.enabled:true}")
    private boolean enabled;

    private final TbClusterStore tbClusterStore;
    private final CommunityGrantFlowStateStore flowStateStore;
    private final CommunityGrantPortalClient portalClient;
    private final CommunityGrantPoller poller;
    private final CommunityGrantReportRunner reportRunner;
    private final CommunityGrantOfflineReportStore offlineReportStore;

    private final SecureRandom secureRandom = new SecureRandom();
    private final ReentrantLock startLock = new ReentrantLock();

    /** A permit, not a lock: the request thread takes it and the executor thread gives it back. */
    private final Semaphore offlineRunPermit = new Semaphore(1);

    private ExecutorService offlineRunExecutor =
            Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tb-community-grant-offline-run"));

    /** A failure to read the report drops it from the answer rather than failing the whole read. */
    @Override
    public CommunityGrantStateInfo getStateInfo() {
        CommunityGrantFlowState flowState = flowStateStore.get();
        String portalUrl = portalClient.getPortalUrl();
        CommunityGrantOfflineRun run = flowState.getOfflineRun();
        boolean runInProgress = isOfflineRunInProgress(run, System.currentTimeMillis(), offlineRunStaleAfterMs());
        String runError = offlineRunError(run, runInProgress);
        String report = null;
        if (CommunityGrantStateInfo.carriesOfflineReport(flowState)) {
            try {
                report = offlineReportStore.get().orElse(null);
            } catch (Exception e) {
                log.warn("Failed to read the stored community grant offline report", e);
            }
        }
        return CommunityGrantStateInfo.of(flowState, portalUrl, report, runInProgress, runError);
    }

    @Override
    public CommunityGrantStateInfo start() throws ThingsboardException {
        if (!enabled) {
            throw new ThingsboardException(NOT_AVAILABLE_MESSAGE, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        startLock.lock();
        try {
            CommunityGrantFlowState currentState = flowStateStore.get();
            CommunityGrantState current = currentState.getState();
            if (current.isTerminal()) {
                throw new ThingsboardException(ALREADY_FINISHED_MESSAGE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            if (current == CommunityGrantState.UNDER_REVIEW) {
                // Re-minting would replace the token the reviewer's decision is delivered against.
                throw new ThingsboardException(UNDER_REVIEW_MESSAGE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            if (current == CommunityGrantState.REPORT_HANDED_OVER) {
                // Re-minting would abandon the submission and clear the report a failed paste comes back for.
                throw new ThingsboardException(REPORT_HANDED_OVER_MESSAGE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            UUID clusterId = tbClusterStore.getClusterId()
                    .orElseThrow(() -> new ThingsboardException(NO_CLUSTER_ID_MESSAGE, ThingsboardErrorCode.GENERAL));

            // Probed even when OFFLINE: the only way back to ONLINE.
            CommunityGrantMode mode;
            try {
                mode = portalClient.isPortalReachable() ? CommunityGrantMode.ONLINE : CommunityGrantMode.OFFLINE;
            } catch (IllegalArgumentException e) {
                // Only a malformed base URL; unreachability is a plain false.
                log.warn("Failed to reach the license portal: the configured base URL is not valid", e);
                throw new ThingsboardException(PORTAL_MISCONFIGURED_MESSAGE, ThingsboardErrorCode.GENERAL);
            }

            // Only from NOT_STARTED: on a flow this deployment began, its own pending registration answers
            // true, and the terminal ALREADY_REGISTERED would dead-end the operator's own enrollment.
            if (mode == CommunityGrantMode.ONLINE && current == CommunityGrantState.NOT_STARTED
                    && isAlreadyRegistered(clusterId)) {
                return enrollAsAlreadyRegistered();
            }

            String token = mintToken();
            // Built before the token is stored, so a bad base URL leaves nothing to retire.
            String signUpUrl;
            try {
                signUpUrl = portalClient.buildSignUpUrl(clusterId, token, mode);
            } catch (Exception e) {
                log.warn("Failed to build the sign-up URL: the configured base URL is not valid", e);
                throw new ThingsboardException(PORTAL_MISCONFIGURED_MESSAGE, ThingsboardErrorCode.GENERAL);
            }
            try {
                tbClusterStore.saveLicenseClaimToken(token);
            } catch (Exception e) {
                log.warn("Failed to store the community grant claim token", e);
                throw new ThingsboardException(START_FAILED_MESSAGE, ThingsboardErrorCode.GENERAL);
            }

            CommunityGrantFlowState updated;
            try {
                long now = System.currentTimeMillis();
                updated = flowStateStore.update(state -> {
                    state.setMode(mode);
                    state.setState(CommunityGrantState.AWAITING_SIGNUP);
                    state.setSignUpUrl(signUpUrl);
                    state.setStartedAt(now);
                    state.setLastPolledAt(null);
                    state.setParkReason(null);
                    state.setLastOfflineCheckAt(null);
                    state.setOfflineRun(null);
                    return state;
                });
            } catch (Exception e) {
                tbClusterStore.clearLicenseClaimToken(token);
                log.warn("Failed to persist the community grant flow state after minting a token", e);
                throw new ThingsboardException(START_FAILED_MESSAGE, ThingsboardErrorCode.GENERAL);
            }
            // After the write, so a failed start keeps the previous report.
            offlineReportStore.clear();
            // After the write: the loop reads the persisted mode.
            poller.arm();
            return CommunityGrantStateInfo.of(updated, portalClient.getPortalUrl());
        } finally {
            startLock.unlock();
        }
    }

    /**
     * Refusals answer the upload request; the run itself goes to {@link #offlineRunExecutor} on this node.
     * One run at a time: per node by {@link #offlineRunPermit}, cluster-wide by the run record on the flow
     * state. Two nodes starting in the same instant can both pass; the run written last keeps its result.
     */
    @Override
    public CommunityGrantStateInfo runOfflineChecker(byte[] checkerData, byte[] signatureData, String fileName,
                                                     CommunityGrantCheckerInput checkerInput,
                                                     Consumer<Exception> completionListener)
            throws ThingsboardException {
        if (!enabled) {
            throw new ThingsboardException(NOT_AVAILABLE_MESSAGE, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        if (!offlineRunPermit.tryAcquire()) {
            throw new ThingsboardException(OFFLINE_RUN_IN_PROGRESS_MESSAGE, ThingsboardErrorCode.TOO_MANY_REQUESTS);
        }
        boolean handedToExecutor = false;
        try {
            try {
                reportRunner.verifyUploadedChecker(checkerData, signatureData, fileName);
            } catch (IllegalArgumentException e) {
                throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            } catch (Exception e) {
                log.warn("Failed to verify the uploaded instance check", e);
                throw new ThingsboardException(RUN_FAILED_PREFIX + e.getMessage(), ThingsboardErrorCode.GENERAL);
            }
            UUID runId = UUID.randomUUID();
            long startedAt = System.currentTimeMillis();
            CommunityGrantStateInfo answer = CommunityGrantStateInfo.of(recordOfflineRunStarted(runId, startedAt),
                    portalClient.getPortalUrl(), null, true, null);
            try {
                offlineRunExecutor.execute(() ->
                        completeOfflineRun(runId, checkerData, signatureData, fileName, checkerInput,
                                completionListener));
            } catch (RejectedExecutionException e) {
                log.warn("Failed to schedule the uploaded instance check", e);
                recordOfflineRunFailed(runId, OFFLINE_RUN_NOT_SCHEDULED_MESSAGE);
                throw new ThingsboardException(OFFLINE_RUN_NOT_SCHEDULED_MESSAGE, ThingsboardErrorCode.GENERAL);
            }
            handedToExecutor = true;
            return answer;
        } finally {
            if (!handedToExecutor) {
                offlineRunPermit.release();
            }
        }
    }

    private CommunityGrantFlowState recordOfflineRunStarted(UUID runId, long startedAt) throws ThingsboardException {
        long staleAfterMs = offlineRunStaleAfterMs();
        try {
            return flowStateStore.update(state -> {
                if (isOfflineRunInProgress(state.getOfflineRun(), startedAt, staleAfterMs)) {
                    throw new UpdateRefusal(OFFLINE_RUN_IN_PROGRESS_MESSAGE, ThingsboardErrorCode.TOO_MANY_REQUESTS);
                }
                state.setOfflineRun(CommunityGrantOfflineRun.running(runId, startedAt));
                return state;
            });
        } catch (UpdateRefusal e) {
            throw new ThingsboardException(e.getMessage(), e.errorCode);
        } catch (Exception e) {
            // No cause: the audit log stores the full stack trace.
            log.warn("Failed to record the start of a community grant offline check", e);
            throw new ThingsboardException(OFFLINE_RUN_START_FAILED_MESSAGE, ThingsboardErrorCode.GENERAL);
        }
    }

    private void completeOfflineRun(UUID runId, byte[] checkerData, byte[] signatureData, String fileName,
                                    CommunityGrantCheckerInput checkerInput, Consumer<Exception> completionListener) {
        try {
            ThingsboardException failure = null;
            try {
                runAndRecordOfflineCheck(runId, checkerData, signatureData, fileName, checkerInput);
            } catch (ThingsboardException e) {
                failure = e;
            } catch (Exception e) {
                log.warn("Failed to complete the uploaded instance check", e);
                failure = new ThingsboardException(RUN_FAILED_PREFIX + e.getMessage(), ThingsboardErrorCode.GENERAL);
            }
            if (failure != null) {
                recordOfflineRunFailed(runId, failure.getMessage());
            }
            try {
                completionListener.accept(failure);
            } catch (Exception e) {
                log.warn("Failed to report the end of a community grant offline check", e);
            }
        } finally {
            offlineRunPermit.release();
        }
    }

    private void runAndRecordOfflineCheck(UUID runId, byte[] checkerData, byte[] signatureData, String fileName,
                                          CommunityGrantCheckerInput checkerInput) throws ThingsboardException {
        String report;
        try {
            report = reportRunner.runUploadedChecker(checkerData, signatureData, fileName, checkerInput);
        } catch (IllegalArgumentException e) {
            throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } catch (Exception e) {
            log.warn("Failed to run the uploaded instance check", e);
            throw new ThingsboardException(RUN_FAILED_PREFIX + e.getMessage(), ThingsboardErrorCode.GENERAL);
        }
        CommunityGrantOfflineRun current;
        try {
            current = flowStateStore.get().getOfflineRun();
        } catch (Exception e) {
            log.warn("Failed to read the community grant flow state after an offline check", e);
            throw new ThingsboardException(OFFLINE_RESULT_NOT_RECORDED_MESSAGE, ThingsboardErrorCode.GENERAL);
        }
        if (current == null || !current.isOwnedBy(runId)) {
            throw supersededOfflineRun();
        }
        // Before and apart from the state write: OFFLINE_STATE_NOT_PERSISTED_MESSAGE promises the download.
        offlineReportStore.put(report);
        long lastOfflineCheckAt = System.currentTimeMillis();
        AtomicBoolean owned = new AtomicBoolean();
        try {
            flowStateStore.update(state -> {
                owned.set(state.getOfflineRun() != null && state.getOfflineRun().isOwnedBy(runId));
                if (!owned.get()) {
                    return state;
                }
                if (state.getMode() != CommunityGrantMode.ONLINE) {
                    state.setMode(CommunityGrantMode.OFFLINE);
                }
                if (!state.getState().isAwaitingOrPastDecision()) {
                    state.setState(CommunityGrantState.VALIDATING);
                    state.setParkReason(null);
                }
                state.setLastOfflineCheckAt(lastOfflineCheckAt);
                state.setOfflineRun(null);
                return state;
            });
        } catch (Exception e) {
            log.warn("Failed to persist the community grant flow state after a successful offline check", e);
            throw new ThingsboardException(OFFLINE_STATE_NOT_PERSISTED_MESSAGE, ThingsboardErrorCode.GENERAL);
        }
        if (!owned.get()) {
            // The report stays: deleting it could delete a newer run's.
            throw supersededOfflineRun();
        }
    }

    private static ThingsboardException supersededOfflineRun() {
        return new ThingsboardException(OFFLINE_RUN_SUPERSEDED_MESSAGE, ThingsboardErrorCode.GENERAL);
    }

    /** Best effort: a run left {@code RUNNING} is reported as interrupted once it goes stale. */
    private void recordOfflineRunFailed(UUID runId, String error) {
        try {
            flowStateStore.update(state -> {
                CommunityGrantOfflineRun run = state.getOfflineRun();
                if (run != null && run.isOwnedBy(runId)) {
                    run.setStatus(CommunityGrantOfflineRunStatus.FAILED);
                    run.setError(error);
                }
                return state;
            });
        } catch (Exception e) {
            log.warn("Failed to record a failed community grant offline check", e);
        }
    }

    private long offlineRunStaleAfterMs() {
        return TimeUnit.SECONDS.toMillis(reportRunner.getCheckerTimeoutSec()) + OFFLINE_RUN_GRACE_MS;
    }

    private static boolean isOfflineRunInProgress(CommunityGrantOfflineRun run, long now, long staleAfterMs) {
        return run != null && run.getStatus() == CommunityGrantOfflineRunStatus.RUNNING
                && run.getStartedAt() != null && now - run.getStartedAt() <= staleAfterMs;
    }

    private static String offlineRunError(CommunityGrantOfflineRun run, boolean inProgress) {
        if (run == null || inProgress) {
            return null;
        }
        return run.getStatus() == CommunityGrantOfflineRunStatus.FAILED
                ? run.getError()
                : OFFLINE_RUN_INTERRUPTED_MESSAGE;
    }

    @PreDestroy
    public void stop() {
        offlineRunExecutor.shutdownNow();
    }

    @Override
    public String getOfflineReport() throws ThingsboardException {
        return offlineReportStore.get()
                .orElseThrow(() -> new ThingsboardException(NO_OFFLINE_REPORT_MESSAGE,
                        ThingsboardErrorCode.ITEM_NOT_FOUND));
    }

    /** Under {@link #startLock}, so a concurrent start() is not relabelled as handed over. */
    @Override
    public CommunityGrantStateInfo confirmOfflineHandoff() throws ThingsboardException {
        if (!enabled) {
            throw new ThingsboardException(NOT_AVAILABLE_MESSAGE, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        startLock.lock();
        try {
            CommunityGrantState current = flowStateStore.get().getState();
            if (current == CommunityGrantState.REPORT_HANDED_OVER) {
                return getStateInfo();
            }
            if (current != CommunityGrantState.VALIDATING) {
                throw new ThingsboardException(NOTHING_TO_HAND_OVER_MESSAGE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            try {
                flowStateStore.update(state -> {
                    // Re-tested: a poll may have retired the flow since the read above.
                    if (state.getState() == CommunityGrantState.VALIDATING) {
                        state.setState(CommunityGrantState.REPORT_HANDED_OVER);
                    }
                    return state;
                });
            } catch (Exception e) {
                log.warn("Failed to persist the community grant offline hand-off", e);
                throw new ThingsboardException(HANDOFF_FAILED_MESSAGE, ThingsboardErrorCode.GENERAL);
            }
            return getStateInfo();
        } finally {
            startLock.unlock();
        }
    }

    @Override
    public void requestAccess() throws ThingsboardException {
        if (!enabled) {
            throw new ThingsboardException(NOT_AVAILABLE_MESSAGE, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        UUID clusterId;
        long now = System.currentTimeMillis();
        try {
            clusterId = tbClusterStore.getClusterId().orElse(null);
            // The cooldown lives on the shared flow state; only the lock is per node.
            flowStateStore.update(state -> {
                // Not WRONG_INSTALLATION: there is no registration on this cluster id to notify.
                if (state.getState() != CommunityGrantState.ALREADY_REGISTERED) {
                    throw new UpdateRefusal(NOT_ALREADY_REGISTERED_MESSAGE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
                Long lastRequestAccessAt = state.getLastRequestAccessAt();
                if (lastRequestAccessAt != null && now - lastRequestAccessAt < REQUEST_ACCESS_COOLDOWN_MS) {
                    throw new UpdateRefusal(REQUEST_ACCESS_RATE_LIMITED_MESSAGE, ThingsboardErrorCode.TOO_MANY_REQUESTS);
                }
                if (clusterId == null) {
                    throw new UpdateRefusal(NO_CLUSTER_ID_MESSAGE, ThingsboardErrorCode.GENERAL);
                }
                // Recorded whatever the portal call below does.
                state.setLastRequestAccessAt(now);
                return state;
            });
        } catch (UpdateRefusal e) {
            throw new ThingsboardException(e.getMessage(), e.errorCode);
        } catch (Exception e) {
            log.warn("Failed to read or persist the community grant request-access state", e);
            throw new ThingsboardException(REQUEST_ACCESS_STORE_FAILED_MESSAGE, ThingsboardErrorCode.GENERAL);
        }
        try {
            portalClient.requestAccess(clusterId);
        } catch (Exception e) {
            log.warn("Failed to ask the license portal to notify the registered owner", e);
            throw new ThingsboardException(REQUEST_ACCESS_FAILED_MESSAGE, ThingsboardErrorCode.GENERAL);
        }
    }

    /** Unchecked so it can leave an updater; translated back to {@link ThingsboardException} by the caller. */
    private static final class UpdateRefusal extends RuntimeException {
        private final ThingsboardErrorCode errorCode;

        private UpdateRefusal(String message, ThingsboardErrorCode errorCode) {
            super(message);
            this.errorCode = errorCode;
        }
    }

    /** The token is retired before the terminal state is written, since nothing retires it afterwards. */
    private CommunityGrantStateInfo enrollAsAlreadyRegistered() throws ThingsboardException {
        poller.disarm();
        tbClusterStore.forceClearLicenseClaimToken();
        try {
            CommunityGrantFlowState updated = flowStateStore.update(state -> {
                state.setMode(CommunityGrantMode.ONLINE);
                state.setState(CommunityGrantState.ALREADY_REGISTERED);
                state.clearClaim();
                state.setLastPolledAt(null);
                state.setParkReason(null);
                state.setOfflineRun(null);
                return state;
            });
            offlineReportStore.clear();
            return CommunityGrantStateInfo.of(updated, portalClient.getPortalUrl());
        } catch (Exception e) {
            log.warn("Failed to persist the community grant flow state for an already-registered deployment", e);
            throw new ThingsboardException(START_FAILED_MESSAGE, ThingsboardErrorCode.GENERAL);
        }
    }

    /** Best effort: a failure here reads as not registered, and the poll reaches the same answer later. */
    private boolean isAlreadyRegistered(UUID clusterId) {
        try {
            return portalClient.isAlreadyRegistered(clusterId);
        } catch (Exception e) {
            log.debug("Failed to read the cluster status from the portal", e);
            return false;
        }
    }

    private String mintToken() {
        byte[] tokenBytes = new byte[CLAIM_TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

}
