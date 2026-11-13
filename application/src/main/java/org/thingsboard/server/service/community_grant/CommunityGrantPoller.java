// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.community_grant.CommunityGrantFlowState;
import org.thingsboard.server.common.data.community_grant.CommunityGrantMode;
import org.thingsboard.server.common.data.community_grant.CommunityGrantParkReason;
import org.thingsboard.server.common.data.community_grant.CommunityGrantState;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.subscription.TbClusterStore;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;

/**
 * Polls the portal about this deployment's claim and projects the answer onto the local state. Only the node
 * owning the system tenant's TB_CORE partition runs the loop; it supervises for flows other nodes began.
 * On the offline rail it never calls out, and only enforces the sign-up deadline.
 */
@Component
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class CommunityGrantPoller extends TbApplicationEventListener<PartitionChangeEvent> {

    static final long MIN_POLL_INTERVAL_MS = 2000;
    static final int MAX_RETRY_AFTER_SEC = 3600;
    static final long MAX_FAILURE_BACKOFF_MS = TimeUnit.MINUTES.toMillis(10);
    static final int CONSECUTIVE_FAILURE_WARN_THRESHOLD = 5;

    static final int MAX_POLL_FAILURES = 200;

    /** Several of these failures are permanent, and every retry re-downloads the whole checker. */
    static final int MAX_VERIFICATION_FAILURES = 5;

    @Value("${community-grant.enabled:true}")
    private boolean enabled;

    @Value("${community-grant.fast-poll-interval-ms:3000}")
    private long fastPollIntervalMs;

    @Value("${community-grant.fast-poll-window-ms:120000}")
    private long fastPollWindowMs;

    @Value("${community-grant.slow-poll-interval-ms:30000}")
    private long slowPollIntervalMs;

    /** Matched to the portal's claim-token lifetime (24h). */
    @Value("${community-grant.sign-up-deadline-ms:86400000}")
    private long signUpDeadlineMs;

    @Value("${community-grant.supervise-interval-ms:30000}")
    private long superviseIntervalMs;

    private final PartitionService partitionService;
    private final TbClusterStore tbClusterStore;
    private final CommunityGrantFlowStateStore flowStateStore;
    private final CommunityGrantPortalClient portalClient;
    private final CommunityGrantReportRunner reportRunner;

    private final ScheduledExecutorService scheduler =
            ThingsBoardExecutors.newSingleThreadScheduledExecutor("tb-community-grant-poller");
    private final AtomicBoolean armed = new AtomicBoolean(false);
    private final ReentrantLock pollLock = new ReentrantLock();
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicInteger verificationFailures = new AtomicInteger(0);

    /** Stops a poll still running across a disarm and re-arm from rescheduling beside the new chain. */
    private final AtomicLong generation = new AtomicLong();

    private volatile ScheduledFuture<?> pendingPoll;
    private volatile ScheduledFuture<?> supervisor;
    private volatile long pollingSince;
    private volatile long lastPollTs;

    /** The claim token the current budgets and fast window belong to. */
    private volatile String pollingToken;

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void init() {
        if (enabled && isSystemTenantPartitionMine()) {
            startSupervising();
            armIfPollable();
        }
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        if (!enabled) {
            return;
        }
        if (isSystemTenantPartitionMine()) {
            startSupervising();
            armIfPollable();
        } else {
            stopSupervising();
            disarm();
        }
    }

    @Override
    protected boolean filterTbApplicationEvent(PartitionChangeEvent event) {
        return ServiceType.TB_CORE == event.getServiceType();
    }

    /** A no-op off the partition owner, whose supervisor picks the flow up instead. Arms offline flows too. */
    public void arm() {
        if (!enabled) {
            return;
        }
        if (!isSystemTenantPartitionMine()) {
            log.debug("Not arming the community grant poller: this node does not own the system tenant's "
                    + "TB_CORE partition");
            return;
        }
        if (armed.compareAndSet(false, true)) {
            pollingSince = System.currentTimeMillis();
            verificationFailures.set(0);
            consecutiveFailures.set(0);
            cancelPendingPoll();
            schedule(fastPollIntervalMs, generation.incrementAndGet());
        }
    }

    public void disarm() {
        armed.set(false);
        cancelPendingPoll();
    }

    boolean isArmed() {
        return armed.get();
    }

    @PreDestroy
    public void stop() {
        stopSupervising();
        disarm();
        scheduler.shutdownNow();
    }

    private synchronized void startSupervising() {
        if (supervisor != null) {
            return;
        }
        try {
            supervisor = scheduler.scheduleWithFixedDelay(this::supervise,
                    superviseIntervalMs, superviseIntervalMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("Failed to schedule the community grant supervisor", e);
        }
    }

    private synchronized void stopSupervising() {
        ScheduledFuture<?> pending = supervisor;
        if (pending != null) {
            pending.cancel(false);
            supervisor = null;
        }
    }

    void supervise() {
        try {
            if (!armed.get()) {
                armIfPollable();
            }
        } catch (Throwable e) {
            // A fixed-delay task that throws is never run again.
            log.debug("The community grant supervisor tick failed", e);
        }
    }

    private boolean isSystemTenantPartitionMine() {
        try {
            return partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition();
        } catch (Exception e) {
            log.debug("Failed to resolve the system tenant partition", e);
            return false;
        }
    }

    /** Offline, nothing ever answers past the sign-up form, so there is no work left once it is done. */
    private static boolean hasWorkLeft(CommunityGrantFlowState state) {
        return state.getMode() != CommunityGrantMode.OFFLINE
                || state.getState() == CommunityGrantState.AWAITING_SIGNUP;
    }

    private void armIfPollable() {
        boolean pollable;
        try {
            CommunityGrantFlowState stored = flowStateStore.get();
            pollable = stored.getState().isPollable() && hasWorkLeft(stored);
        } catch (Exception e) {
            log.warn("Failed to read the stored community grant state, not arming the community grant poller", e);
            return;
        }
        if (pollable) {
            arm();
        }
    }

    private void schedule(long delayMs, long gen) {
        if (!armed.get() || gen != generation.get()) {
            return;
        }
        try {
            pendingPoll = scheduler.schedule(() -> pollAndReschedule(gen), delayMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // Cleared so arm() can start a new chain.
            armed.set(false);
            log.warn("Failed to schedule the next community grant poll", e);
        }
    }

    private void cancelPendingPoll() {
        ScheduledFuture<?> pending = pendingPoll;
        if (pending != null) {
            pending.cancel(false);
            pendingPoll = null;
        }
    }

    void pollAndReschedule() {
        pollAndReschedule(generation.get());
    }

    private void pollAndReschedule(long gen) {
        if (gen != generation.get()) {
            return;
        }
        long nextDelay;
        try {
            nextDelay = nextDelay(poll());
        } catch (Throwable e) {
            // Throwable, so an Error cannot cancel the scheduled task.
            int failures = consecutiveFailures.incrementAndGet();
            if (failures >= CONSECUTIVE_FAILURE_WARN_THRESHOLD) {
                log.warn("Failed to poll the license portal ({} consecutive failures)", failures, e);
            } else {
                log.debug("Failed to poll the license portal", e);
            }
            onPollFailure(failures);
            nextDelay = failureDelay(failures);
        }
        schedule(nextDelay, gen);
    }

    /** An unreachable portal never answers, so its endings are enforced locally. */
    private void onPollFailure(int failures) {
        try {
            String token = tbClusterStore.getLicenseClaimToken().orElse(null);
            if (StringUtils.isEmpty(token)) {
                return;
            }
            long now = System.currentTimeMillis();
            if (parkIfSignUpAbandoned(token, flowStateStore.get(), now)) {
                return;
            }
            if (failures >= MAX_POLL_FAILURES) {
                log.warn("Parking the community grant flow after {} consecutive poll failures", failures);
                park(token, now, CommunityGrantParkReason.PORTAL_UNREACHABLE);
            }
        } catch (Exception e) {
            log.debug("Failed to park the community grant flow after a poll failure", e);
        }
    }

    /**
     * @return the portal's own pacing instruction, or {@code null} when this tick did not reach it.
     */
    Integer poll() {
        if (!armed.get()) {
            return null;
        }
        String token = tbClusterStore.getLicenseClaimToken().orElse(null);
        if (StringUtils.isEmpty(token)) {
            disarm();
            return null;
        }
        if (!token.equals(pollingToken)) {
            // A new token is a new enrollment. Re-based here: on another node, the owner never sees start().
            if (pollingToken != null) {
                pollingSince = System.currentTimeMillis();
                verificationFailures.set(0);
                consecutiveFailures.set(0);
            }
            pollingToken = token;
        }
        CommunityGrantFlowState current = flowStateStore.get();
        if (current.getMode() == CommunityGrantMode.OFFLINE) {
            if (!hasWorkLeft(current)) {
                disarm();
                return null;
            }
            parkIfSignUpAbandoned(token, current, System.currentTimeMillis());
            return null;
        }
        UUID clusterId = tbClusterStore.getClusterId().orElse(null);
        if (clusterId == null) {
            disarm();
            return null;
        }
        if (System.currentTimeMillis() - lastPollTs < MIN_POLL_INTERVAL_MS || !pollLock.tryLock()) {
            return null;
        }
        try {
            lastPollTs = System.currentTimeMillis();
            CommunityGrantPortalStatus status = portalClient.getStatus(token, clusterId);
            // Reset only once the portal has actually answered.
            consecutiveFailures.set(0);
            if (status == null) {
                return null;
            }
            apply(token, status);
            return status.retryAfterSec();
        } finally {
            pollLock.unlock();
        }
    }

    /** The portal's {@code retryAfterSec}, when positive, wins over the local cadence. */
    long nextDelay(Integer retryAfterSec) {
        if (retryAfterSec != null && retryAfterSec > 0) {
            return TimeUnit.SECONDS.toMillis(Math.min(retryAfterSec, MAX_RETRY_AFTER_SEC));
        }
        return System.currentTimeMillis() - pollingSince < fastPollWindowMs ? fastPollIntervalMs : slowPollIntervalMs;
    }

    long failureDelay(int failures) {
        return Math.min(nextDelay(null) << Math.min(failures - 1, 10), MAX_FAILURE_BACKOFF_MS);
    }

    void apply(String token, CommunityGrantPortalStatus status) {
        long now = System.currentTimeMillis();
        String statusValue = status.status();
        if (statusValue == null) {
            // An empty or partial 200 body.
            log.debug("Ignoring a portal status response with no status value");
            return;
        }
        if (applyIfTerminal(token, statusValue, now)) {
            return;
        }
        switch (statusValue) {
            case "AWAITING_SIGNUP" -> onAwaitingSignup(token, now);
            case "VERIFICATION_REQUIRED" -> onVerificationRequired(token, status, now);
            case "UNDER_REVIEW" -> onUnderReview(token, now);
            case "EXPIRED" -> park(token, now, CommunityGrantParkReason.LINK_EXPIRED);
            default -> log.debug("Ignoring an unrecognized portal status");
        }
    }

    /** @return whether the status was a terminal one, even when the flow could not be ended on this poll. */
    private boolean applyIfTerminal(String token, String status, long now) {
        CommunityGrantState terminalState = terminalStateFor(status);
        if (terminalState == null) {
            return false;
        }
        if (terminalState == CommunityGrantState.REGISTERED) {
            onDone(token, now);
        } else {
            retire(token, terminalState, now, null);
        }
        return true;
    }

    /** The key is stored before the token is cleared, so a failure above it leaves the next poll to retry. */
    private void onDone(String token, long now) {
        String licenseSecret;
        try {
            licenseSecret = portalClient.claimLicense(token);
        } catch (CommunityGrantLicenseClaimRefusedException e) {
            log.warn("The license portal did not hand over the license key for this registration ({}). "
                    + "The registration stands and the key can be entered by hand later", e.getMessage());
            retire(token, CommunityGrantState.REGISTERED, now, null);
            return;
        }
        if (StringUtils.isEmpty(licenseSecret)) {
            return;
        }
        tbClusterStore.saveLicenseSecret(licenseSecret);
        retire(token, CommunityGrantState.REGISTERED, now, null);
    }

    /** @return the state the flow ends in, or {@code null} if the status is not a terminal one. */
    private static CommunityGrantState terminalStateFor(String status) {
        return switch (status) {
            case "DONE" -> CommunityGrantState.REGISTERED;
            case "WRONG_INSTALLATION" -> CommunityGrantState.WRONG_INSTALLATION;
            default -> null;
        };
    }

    /** The portal cannot tell an unfinished sign-up from an abandoned one, so the deadline is enforced here. */
    private void onAwaitingSignup(String token, long now) {
        if (parkIfSignUpAbandoned(token, flowStateStore.get(), now)) {
            return;
        }
        applyIfCurrent(token, state -> {
            state.setState(CommunityGrantState.AWAITING_SIGNUP);
            state.setLastPolledAt(now);
            return state;
        });
    }

    /** @return whether the flow was parked. */
    private boolean parkIfSignUpAbandoned(String token, CommunityGrantFlowState current, long now) {
        Long startedAt = current.getStartedAt();
        if (current.getState() != CommunityGrantState.AWAITING_SIGNUP
                || startedAt == null || now - startedAt <= signUpDeadlineMs) {
            return false;
        }
        park(token, now, CommunityGrantParkReason.SIGNUP_ABANDONED);
        return true;
    }

    private void onVerificationRequired(String token, CommunityGrantPortalStatus status, long now) {
        CommunityGrantFlowState updated = applyIfCurrent(token, state -> {
            // The portal asks for verification until a by-hand report lands; that state must not be overwritten.
            if (state.getState() != CommunityGrantState.VALIDATING
                    && state.getState() != CommunityGrantState.REPORT_HANDED_OVER) {
                state.setState(CommunityGrantState.COLLECTING);
            }
            state.setLastPolledAt(now);
            return state;
        });
        if (updated == null) {
            return;
        }
        if (updated.getMode() == CommunityGrantMode.OFFLINE) {
            return;
        }
        if (updated.getState() == CommunityGrantState.VALIDATING) {
            // A by-hand report is already in flight.
            return;
        }
        Optional<CommunityGrantCheckerInput> checkerInput = CommunityGrantCheckerInput.from(status.checkerInput());
        if (checkerInput.isEmpty()) {
            log.warn("The portal asked for verification without the input needed to produce it");
            return;
        }
        String uploadedStatus;
        try {
            uploadedStatus = reportRunner.runAndUpload(token, checkerInput.get());
        } catch (Exception e) {
            int failures = verificationFailures.incrementAndGet();
            if (failures < MAX_VERIFICATION_FAILURES) {
                log.warn("Failed to produce and upload the instance check", e);
            } else if (updated.getState() == CommunityGrantState.REPORT_HANDED_OVER) {
                // Parking would retire the token the handed-over report is bound to.
                log.warn("The instance check keeps failing ({} attempts), but the report has been handed over "
                        + "by hand, so the flow is left waiting on the portal", failures, e);
            } else {
                log.warn("Parking the community grant flow after {} failed instance checks", failures, e);
                park(token, now, CommunityGrantParkReason.CHECK_FAILED);
            }
            return;
        }
        verificationFailures.set(0);
        if (uploadedStatus == null) {
            return;
        }
        if (applyIfTerminal(token, uploadedStatus, now)) {
            return;
        }
        if ("UNDER_REVIEW".equals(uploadedStatus)) {
            onUnderReview(token, now);
        } else {
            log.debug("Ignoring an unrecognized portal status after the upload");
        }
    }

    /** A wait, not an ending: the token is kept for the poll that brings the reviewer's decision. */
    private void onUnderReview(String token, long now) {
        applyIfCurrent(token, state -> {
            state.setState(CommunityGrantState.UNDER_REVIEW);
            state.setLastPolledAt(now);
            return state;
        });
    }

    /** @return the state as written, or {@code null} if the claim token had been replaced. */
    private CommunityGrantFlowState applyIfCurrent(String token, UnaryOperator<CommunityGrantFlowState> updater) {
        AtomicBoolean applied = new AtomicBoolean();
        CommunityGrantFlowState updated = flowStateStore.update(state -> {
            // Checked under the store's lock, so a start() on this node cannot replace the token mid-write.
            applied.set(token.equals(tbClusterStore.getLicenseClaimToken().orElse(null)));
            return applied.get() ? updater.apply(state) : state;
        });
        if (!applied.get()) {
            log.debug("Ignoring a portal answer for a claim token that has since been replaced");
            return null;
        }
        return updated;
    }

    private void park(String token, long now, CommunityGrantParkReason reason) {
        retire(token, CommunityGrantState.RESUME, now, reason);
    }

    /** The state is written before the token is cleared, so a failed write leaves the next poll to retry. */
    private void retire(String token, CommunityGrantState newState, long now, CommunityGrantParkReason reason) {
        AtomicBoolean applied = new AtomicBoolean();
        flowStateStore.update(state -> {
            // Checked under the store's lock, so a start() on this node cannot replace the token mid-write.
            applied.set(token.equals(tbClusterStore.getLicenseClaimToken().orElse(null)));
            if (!applied.get()) {
                return state;
            }
            // The portal still serves a finished grant's page on this link.
            String registrationLink = newState == CommunityGrantState.REGISTERED ? state.getSignUpUrl() : null;
            state.setState(newState);
            state.setParkReason(reason);
            state.clearClaim();
            state.setSignUpUrl(registrationLink);
            state.setLastPolledAt(now);
            return state;
        });
        if (!applied.get()) {
            log.debug("Ignoring a portal answer for a claim token that has since been replaced");
            return;
        }
        tbClusterStore.clearLicenseClaimToken(token);
        disarm();
    }

}
