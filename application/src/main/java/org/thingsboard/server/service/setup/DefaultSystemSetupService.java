// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.setup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.license.client.TbLicenseCtx;
import org.thingsboard.license.shared.FreeLicenseClaimResponse;
import org.thingsboard.license.shared.exception.LicenseErrorCode;
import org.thingsboard.license.shared.exception.LicenseException;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.setup.LicenseChangeResult;
import org.thingsboard.server.common.data.setup.LicenseClaimInfo;
import org.thingsboard.server.common.data.setup.LicenseClaimMode;
import org.thingsboard.server.common.data.setup.LicenseClaimResult;
import org.thingsboard.server.common.data.setup.LicenseClaimStatus;
import org.thingsboard.server.common.data.setup.SetupInfo;
import org.thingsboard.server.common.data.setup.SystemSetupState;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.dao.subscription.TbClusterStore;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.service.license.NonProductionConfirmationService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultSystemSetupService implements SystemSetupService {

    /** 256 bits, Base64url-encoded: the claim token is a bearer credential for the subscription secret. */
    private static final int CLAIM_TOKEN_BYTES = 32;

    private static final String MANUAL_FALLBACK_HINT = "You can also enter the license key manually.";
    private static final String RATE_LIMITED_MESSAGE = "Too many attempts. Please try again in a few minutes.";

    // Fixed on purpose: saveLicenseSecret rethrows raw, so a database failure would otherwise put the UPDATE
    // statement and the PostgreSQL error text into an anonymously reachable response body.
    private static final String APPLY_FAILED_MESSAGE = "The license key was retrieved but could not be applied. " +
            "Copy the license key from the ThingsBoard portal and enter it below.";

    private static final String CLAIM_TOKEN_NOT_STORED_MESSAGE = "This instance could not record the activation " +
            "request, so nothing was requested. Please try again.";

    // Both are reported with the claim token already stored, so only the manual key form is offered on. Kept
    // separate because the causes are unrelated: this instance's cluster row, or the configured portal address.
    private static final String CLUSTER_ID_UNAVAILABLE_MESSAGE = "This instance could not read the cluster id " +
            "that the sign-up link has to carry, so no link could be built. " +
            "Copy the license key from the ThingsBoard portal and enter it below.";

    private static final String SIGN_UP_URL_NOT_BUILT_MESSAGE = "No sign-up link could be built for this " +
            "instance, so the automatic activation cannot be started. " +
            "Copy the license key from the ThingsBoard portal and enter it below.";

    private static final String CLAIM_REQUEST_THROTTLED_MESSAGE = "An activation request for this instance " +
            "was just started. Please wait a moment before requesting another one.";

    /**
     * The claim poll endpoint is unauthenticated, so without a throttle every request would become an outbound
     * portal call. At most one portal call per this interval per node; concurrent and in-between callers get
     * the last result. Node-local, so a cluster of N nodes polled on every node emits up to N times this rate.
     */
    private static final long MIN_CLAIM_INTERVAL_MS = 2000;

    /**
     * The same throttle on the endpoint that mints the token. It bounds the RATE, not the outcome: each mint
     * replaces the stored token unconditionally and the poll claims whatever is stored, so an anonymous caller
     * who can reach an unlicensed instance can still supersede an operator's in-flight claim - just not faster
     * than this. Node-local, as above, so a cluster of N nodes allows up to N times this rate.
     */
    private static final long MIN_CLAIM_REQUEST_INTERVAL_MS = 2000;

    /**
     * Bounds how often the terminal answer below reads {@code tb_cluster.license_secret}. An anonymous caller
     * reaches that path on every request, and an instance that is locked and never re-activates would
     * otherwise cost one SELECT per request forever. Sized well under any plausible client poll cadence, so a
     * cached answer can only arise from another request to this same node within the window.
     */
    private static final long LICENSE_SECRET_READ_INTERVAL_MS = 1000;

    private final SubscriptionService subscriptionService;
    private final UserService userService;
    private final SetupDataService setupDataService;
    private final LicensePortalClient licensePortalClient;
    private final TbClusterStore tbClusterStore;
    private final NonProductionConfirmationService nonProductionConfirmationService;
    private final TbLicenseCtx licenseCtx;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * One-way latch: a sysadmin is never deleted once created. While it is false the database is queried again
     * on every check, so a node that has not yet observed the sysadmin created on another node self-corrects
     * on its next request; caching the false would leave peer nodes stale with the setup endpoints still open.
     */
    private volatile boolean sysAdminExists;

    private final ReentrantLock claimAttemptLock = new ReentrantLock();
    private volatile long lastClaimAttemptTs;
    private volatile LicenseClaimStatus lastClaimStatus = LicenseClaimStatus.PENDING;
    /**
     * Bumped whenever a fresh claim token supersedes the one a poll may be out on. A poll that started before
     * the bump must not publish its verdict - terminal for the old token - over the new claim.
     */
    private volatile int claimGeneration;

    private final ReentrantLock claimRequestLock = new ReentrantLock();
    private volatile long lastClaimRequestTs;

    /**
     * Serializes the check-then-act the account step rests on: {@code checkSetupState(ACCOUNT_REQUIRED)} in the
     * controller and the create below are separate steps, and several SYS_ADMIN rows are legal, so without this
     * two concurrent anonymous calls both win. Node-local; a durable guard would be a schema change.
     */
    private final Object sysAdminCreationLock = new Object();

    private final Object licenseSecretLock = new Object();
    private long nextLicenseSecretReadTs;
    private volatile boolean licenseSecretStored;
    private volatile long lastStoredSecretPickUpTs;

    @Override
    public SystemSetupState getState() {
        if (!subscriptionService.isLicenseActivated()) {
            return SystemSetupState.LICENSE_REQUIRED;
        }
        if (!hasSysAdmin()) {
            return SystemSetupState.ACCOUNT_REQUIRED;
        }
        // getState() runs on every API request through SystemSetupFilter, which checks the path against
        // SETUP_ALLOWED_ENTRY_POINTS first, so the device API never reaches this line. Checked last because it
        // is the only one of the three that can reach the database on a healthy, licensed instance.
        if (nonProductionConfirmationService.isLapsed()) {
            return SystemSetupState.NON_PRODUCTION_CONFIRMATION_REQUIRED;
        }
        return SystemSetupState.READY;
    }

    @Override
    public SetupInfo getSetupInfo() {
        return new SetupInfo(getState());
    }

    @Override
    public LicenseClaimInfo pollClaim(String callerClaimToken) throws ThingsboardException {
        if (subscriptionService.isLicenseActivated()) {
            // Answers before reading the token: the claim is idempotent within its window, so a poll still in
            // flight on another node would otherwise be handed the secret again and rewrite license_secret.
            // ACTIVATED rather than NOT_REQUESTED so a caller that missed the claiming poll's ACTIVATED is not
            // sent back to the manual key form of an instance that needs no key. Answered ahead of the token
            // check below as well: whichever claim put the licence in place, every session must learn it.
            return new LicenseClaimInfo(LicenseClaimStatus.ACTIVATED);
        }
        // Taken before the token is read, so the snapshot and the token describe the same claim. Taken later -
        // after the throttle, which requestClaim() resets - it could already be the generation of the token
        // that superseded the one read here, and the verdict on the retired token would pass the check below.
        int generation = claimGeneration;
        String claimToken;
        try {
            claimToken = tbClusterStore.getLicenseClaimToken().orElse(null);
        } catch (Exception e) {
            // The setup wizard is exactly the surface a half-installed deployment is on, so an unreadable
            // tb_cluster row must leave it an answer to act on rather than a 500 it cannot interpret.
            log.error("Failed to read the license claim token; reporting no claim in progress.", e);
            claimToken = null;
        }
        if (StringUtils.isEmpty(claimToken)) {
            // NOT_REQUESTED is terminal - the wizard stops polling on it - so it is derived from the durable
            // cluster fact, not from this node's own activation state, which can be a throttled stale "not
            // yet". A stored secret means the cluster is activated, because every write of it is placed after
            // a successful validation. The secret is cleared only by clearLicense(), which resets this latch
            // with it; a licence revoked without that reset keeps answering ACTIVATED here while
            // GET /noauth/setup/state answers LICENSE_REQUIRED.
            if (!isLicenseSecretStored()) {
                return new LicenseClaimInfo(LicenseClaimStatus.NOT_REQUESTED);
            }
            // Best-effort convergence of this node's stale activation state, so the wizard's next request -
            // GET /noauth/setup/state, served from that state - does not report the licence as still required.
            // Throttled on the same window as the read above: once licenseSecretStored has latched, this
            // branch is reached on every anonymous request and the pick-up is itself a tb_cluster read.
            if (System.currentTimeMillis() - lastStoredSecretPickUpTs >= LICENSE_SECRET_READ_INTERVAL_MS) {
                lastStoredSecretPickUpTs = System.currentTimeMillis();
                subscriptionService.pickUpStoredLicenseSecret();
            }
            return new LicenseClaimInfo(LicenseClaimStatus.ACTIVATED);
        }
        if (isSupersededClaimToken(callerClaimToken, claimToken)) {
            // The caller's own claim is dead - a later request replaced its token - while the stored one is
            // alive and belongs to another session. Decided here, before the throttle and the lock, because
            // it needs no portal call and because the cached status describes that other session's claim. The
            // stored token is deliberately left alone: only its own holder may retire it.
            return new LicenseClaimInfo(LicenseClaimStatus.EXPIRED);
        }
        if (System.currentTimeMillis() - lastClaimAttemptTs < MIN_CLAIM_INTERVAL_MS || !claimAttemptLock.tryLock()) {
            return new LicenseClaimInfo(lastClaimStatus);
        }
        try {
            lastClaimAttemptTs = System.currentTimeMillis();
            LicenseClaimStatus status = claim(claimToken);
            if (generation != claimGeneration) {
                // A fresh token was published while this poll was out: its verdict describes the retired token
                // and would send the wizard away from a claim that is perfectly alive. The throttle timestamp
                // this attempt wrote was reset with it, so neither is republished here.
                return new LicenseClaimInfo(lastClaimStatus);
            }
            lastClaimStatus = status;
            return new LicenseClaimInfo(status);
        } finally {
            claimAttemptLock.unlock();
        }
    }

    /**
     * Whether the caller is polling a claim that a later request has already replaced. A caller that names no
     * token - a client written before the parameter existed - claims nothing of its own and is never
     * superseded. Compared in constant time, since the token is a bearer credential for the subscription
     * secret and this endpoint is unauthenticated.
     */
    private static boolean isSupersededClaimToken(String callerClaimToken, String storedClaimToken) {
        return StringUtils.isNotEmpty(callerClaimToken) && !MessageDigest.isEqual(
                callerClaimToken.getBytes(StandardCharsets.UTF_8), storedClaimToken.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Whether a licence secret is stored for the cluster, read at most once per
     * {@link #LICENSE_SECRET_READ_INTERVAL_MS} per node and answered from the last read in between.
     * <p>
     * Once true it is answered from memory until the licence reset: {@code license_secret} has exactly one
     * writer, which refuses an empty value, and only {@link #clearLicense()} clears the column - resetting
     * this latch with it. While it is false, concurrent callers block on
     * the single read rather than take the early return with the field's default - which on an activated
     * cluster is the terminal wrong answer this gate exists to prevent.
     */
    private boolean isLicenseSecretStored() {
        if (licenseSecretStored) {
            return true;
        }
        synchronized (licenseSecretLock) {
            if (licenseSecretStored) {
                return true;
            }
            if (System.currentTimeMillis() < nextLicenseSecretReadTs) {
                return false;
            }
            try {
                licenseSecretStored = StringUtils.isNotEmpty(tbClusterStore.getLicenseSecret().orElse(null));
            } catch (Exception e) {
                // Answers "not stored" instead of failing the request: the latch stays down, so the next
                // window re-reads and the wizard keeps working once the row is repaired.
                log.error("Failed to read the license secret; reporting no license stored for the cluster.", e);
                licenseSecretStored = false;
            }
            nextLicenseSecretReadTs = System.currentTimeMillis() + LICENSE_SECRET_READ_INTERVAL_MS;
            return licenseSecretStored;
        }
    }

    private LicenseClaimStatus claim(String claimToken) throws ThingsboardException {
        FreeLicenseClaimResponse response;
        try {
            response = licensePortalClient.claimFreeLicense(claimToken);
        } catch (LicenseException e) {
            log.debug("Failed to claim the free license", e);
            if (e.getErrorCode() == LicenseErrorCode.CLAIM_LICENSE_REJECTED) {
                // Terminal and understood: the portal refuses for reasons it deliberately does not
                // distinguish to an unauthenticated caller, and every one of them is terminal, so retire the
                // token and report EXPIRED. Compare-and-clear, because the operator may have started a fresh
                // request while this poll was out.
                tbClusterStore.clearLicenseClaimToken(claimToken);
                return LicenseClaimStatus.EXPIRED;
            }
            // Anything else is a failure we do NOT understand (connection, server error): keep throwing,
            // and leave the token in place so the next poll retries.
            throw toThingsboardException(e);
        }
        if (!response.isActivated()) {
            return LicenseClaimStatus.PENDING;
        }
        try {
            // Validates the secret, writes tb_cluster.license_secret, clears the claim token and activates;
            // other cluster nodes converge on their next request.
            subscriptionService.applyLicenseKey(response.getSecret());
        } catch (Exception e) {
            // The claim token is deliberately left in place: the claim is idempotent within its window, so
            // the next poll retries instead of permanently killing the automatic path.
            log.warn("Failed to apply the license key claimed from the license portal", e);
            throw new ThingsboardException(APPLY_FAILED_MESSAGE, ThingsboardErrorCode.GENERAL);
        }
        return LicenseClaimStatus.ACTIVATED;
    }

    /**
     * Clearing the durable state is not enough: {@link #licenseSecretStored} is a one-way latch, and the last
     * claim status and timestamps would replay the finished claim of the previous licence at the wizard.
     */
    @Override
    public void clearLicense() {
        subscriptionService.clearLicense();
        resetLicenseLatches();
    }

    @Override
    public void onLicenseStateReconciled() {
        resetLicenseLatches();
    }

    /** The node-local state that must not outlive the licence it describes. */
    private void resetLicenseLatches() {
        synchronized (licenseSecretLock) {
            licenseSecretStored = false;
            nextLicenseSecretReadTs = 0;
        }
        lastStoredSecretPickUpTs = 0;
        lastClaimAttemptTs = 0;
        lastClaimStatus = LicenseClaimStatus.PENDING;
        lastClaimRequestTs = 0;
    }

    @Override
    public void applyLicenseKey(String secret) {
        subscriptionService.applyLicenseKey(secret);
    }

    @Override
    public SubscriptionInfo previewLicenseKey(String secret) {
        return subscriptionService.previewLicenseKey(secret);
    }

    @Override
    public LicenseChangeResult applyLicenseKeyAndReport(String secret) {
        subscriptionService.applyLicenseKey(secret);
        return new LicenseChangeResult(getState(), reportLicenseNowInForce());
    }

    /**
     * The licence this node now runs, or null when it cannot report one: a critical error or a peer's clear can
     * unactivate it between the apply and this read. Only the apply above may refuse the key - letting this
     * read's refusal out would tell the operator that a key which took effect did not.
     * <p>
     * Read from this node, which has just activated, so it is the licence the caller asked for rather than
     * whatever a peer mid-reconvergence would answer.
     */
    private SubscriptionInfo reportLicenseNowInForce() {
        try {
            return subscriptionService.getSubscriptionInfo();
        } catch (SubscriptionException e) {
            log.warn("The license key was applied, but this node can no longer report the license in force", e);
            return null;
        }
    }

    @Override
    public LicenseClaimResult requestClaim() throws ThingsboardException {
        if (System.currentTimeMillis() - lastClaimRequestTs < MIN_CLAIM_REQUEST_INTERVAL_MS || !claimRequestLock.tryLock()) {
            throw new ThingsboardException(CLAIM_REQUEST_THROTTLED_MESSAGE, ThingsboardErrorCode.TOO_MANY_REQUESTS);
        }
        try {
            lastClaimRequestTs = System.currentTimeMillis();
            return mintAndPublishClaim();
        } finally {
            claimRequestLock.unlock();
        }
    }

    private LicenseClaimResult mintAndPublishClaim() throws ThingsboardException {
        // Write first, call second: the reverse order risks the operator carrying a token into the sign-up
        // page that this instance cannot recognise when the portal quotes it back.
        String claimToken = mintClaimToken();
        try {
            tbClusterStore.saveLicenseClaimToken(claimToken);
            // The cached poll answer belongs to the superseded token and can be terminal, so a poll arriving
            // inside MIN_CLAIM_INTERVAL_MS would abandon a claim that is perfectly alive. The generation bump
            // covers the poll that is already out on the old token and has yet to write its verdict.
            claimGeneration++;
            lastClaimAttemptTs = 0;
            lastClaimStatus = LicenseClaimStatus.PENDING;
        } catch (Exception e) {
            // Nothing was handed to the operator, so nothing was requested of the portal and retrying is the
            // remedy. Reported in the same shape as the other failures rather than as a raw 500.
            log.warn("Failed to store the license claim token", e);
            throw new ThingsboardException(CLAIM_TOKEN_NOT_STORED_MESSAGE, ThingsboardErrorCode.GENERAL);
        }
        UUID clusterId;
        try {
            clusterId = licenseCtx.getClusterId();
        } catch (Exception e) {
            // getClusterId() mints the row when it is missing and throws only when that self-heal cannot produce
            // an id either. Caught separately from the URL build below, whose message would send such an
            // installation off to verify a license server address that is correct.
            log.warn("Failed to build the portal sign-up URL: this instance could not read its cluster id", e);
            throw new ThingsboardException(CLUSTER_ID_UNAVAILABLE_MESSAGE, ThingsboardErrorCode.GENERAL);
        }
        try {
            LicenseClaimMode mode = licensePortalClient.isPortalReachable() ? LicenseClaimMode.ONLINE : LicenseClaimMode.OFFLINE;
            return new LicenseClaimResult(buildSignUpUrl(claimToken, clusterId, mode), mode, claimToken);
        } catch (Exception e) {
            // The configured license server address is parsed here and nowhere earlier, so a malformed one
            // would otherwise escape as a raw 500 with the claim token already stored. Neither the message nor
            // this log line names a cause; the logged exception carries the real one.
            log.warn("Failed to build the portal sign-up URL", e);
            throw new ThingsboardException(SIGN_UP_URL_NOT_BUILT_MESSAGE, ThingsboardErrorCode.GENERAL);
        }
    }

    /**
     * The URL the operator opens. Carries the cluster id because the portal needs it to generate offline
     * licence data, and the offline marker because only the instance has tested its own route out.
     */
    private String buildSignUpUrl(String claimToken, UUID clusterId, LicenseClaimMode mode) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(licensePortalClient.getPortalBaseUrl())
                .path("/declareLicense")
                .queryParam("clusterId", clusterId)
                .queryParam("claimToken", claimToken);
        if (mode == LicenseClaimMode.OFFLINE) {
            builder.queryParam("offline", true);
        }
        return builder.build().toUriString();
    }

    private String mintClaimToken() {
        byte[] claimTokenBytes = new byte[CLAIM_TOKEN_BYTES];
        secureRandom.nextBytes(claimTokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(claimTokenBytes);
    }

    /**
     * Maps a license portal failure onto the exception the setup UI renders. Every branch has to leave the
     * user with somewhere to go, because the UI keeps them on the screen the failure happened on. A refused
     * claim never arrives here - {@link #claim} answers it with {@link LicenseClaimStatus#EXPIRED} on a 200
     * and returns - so no branch below can produce a 400 that would read as terminal.
     */
    private ThingsboardException toThingsboardException(LicenseException e) {
        ThingsboardErrorCode errorCode = switch (e.getErrorCode()) {
            case RATE_LIMITED -> ThingsboardErrorCode.TOO_MANY_REQUESTS;
            default -> ThingsboardErrorCode.GENERAL;
        };
        // Every branch names the manual fallback: the user stays on this screen, so a message that offers no
        // way forward strands them there. Everything except a rate limit keeps the client's own wording.
        String message = switch (e.getErrorCode()) {
            case RATE_LIMITED -> withManualFallbackHint(RATE_LIMITED_MESSAGE);
            default -> withManualFallbackHint(e.getMessage());
        };
        return new ThingsboardException(message, errorCode);
    }

    /**
     * Appends the manual fallback to a message, ending the message first if it does not end itself: the
     * messages come from the license client and from the portal beyond it, and plenty of them are fragments
     * rather than sentences ("Unable to send mail: Connection refused").
     */
    private static String withManualFallbackHint(String message) {
        String text = message == null ? "" : message.strip();
        if (text.isEmpty()) {
            return MANUAL_FALLBACK_HINT;
        }
        if (!text.endsWith(".") && !text.endsWith("!") && !text.endsWith("?")) {
            text += ".";
        }
        return text + " " + MANUAL_FALLBACK_HINT;
    }

    /**
     * The account step is first-caller-wins. Re-checked here under {@link #sysAdminCreationLock} so a second
     * caller that slipped through the controller's {@code checkSetupState} between the two steps loses on the
     * same terms: a BAD_REQUEST_PARAMS, not a second system administrator.
     */
    @Override
    public void completeSetup(String email, String password, boolean loadDemo) {
        synchronized (sysAdminCreationLock) {
            if (hasSysAdmin()) {
                throw new ThingsboardRuntimeException("This setup step is not applicable in the " + getState() +
                        " state", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            setupDataService.createSysAdmin(email, password);
            sysAdminExists = true;
        }
        if (loadDemo) {
            // Best-effort: the instance is fully functional without the demo data, so a failure here must not
            // fail the request and leave the caller unable to retry the (already completed) setup.
            try {
                setupDataService.loadDemoData();
            } catch (Exception e) {
                log.error("Failed to load the demo data. The system setup itself is complete.", e);
            }
        }
    }

    private boolean hasSysAdmin() {
        if (sysAdminExists) {
            return true;
        }
        boolean exists = userService.existsByAuthority(Authority.SYS_ADMIN);
        if (exists) {
            sysAdminExists = true;
        }
        return exists;
    }

}
