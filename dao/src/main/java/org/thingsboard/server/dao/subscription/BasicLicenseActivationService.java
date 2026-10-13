// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.license.client.NonProductionTbLicenseClient;
import org.thingsboard.license.client.OfflineTbLicenseClient;
import org.thingsboard.license.client.TbLicenseClient;
import org.thingsboard.license.client.TbLicenseClientListener;
import org.thingsboard.license.client.TbLicenseCtx;
import org.thingsboard.license.client.TbLicenseStatisticsService;
import org.thingsboard.license.shared.PlanData;
import org.thingsboard.license.shared.PlanDataConstants;
import org.thingsboard.license.shared.PlanItem;
import org.thingsboard.license.shared.SubscriptionPreviewResponse;
import org.thingsboard.license.shared.exception.LicenseErrorCode;
import org.thingsboard.license.shared.exception.LicenseException;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.subscription.SubscriptionErrorCode;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.subscription.LicenseCapacity.CappedEntity;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@Profile("!install & !test")
public class BasicLicenseActivationService implements LicenseActivationService, TbLicenseClientListener {

    @Value("${license.secret}")
    private String licenseSecret;
    @Value("${license.instance_data_file:instance-license.data}")
    private String instanceDataFilePath;
    @Value("${zk.enabled:false}")
    private boolean zkEnabled;
    @Value("${license.non_production_use:false}")
    private boolean nonProductionUse;

    @Autowired
    private TbLicenseCtx licenseCtx;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetService assetService;

    @Autowired(required = false)
    private Optional<TbLicenseStatisticsService> licenseStatisticsService;

    @Autowired
    private TbClusterStore tbClusterStore;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Resolved on use rather than on injection, for two reasons. It is optional - {@code dao} is used by
     * contexts that have no setup service, and by the installer - and its only implementation
     * constructor-injects this service back, so resolving it while this bean is being created would be a
     * cycle that survives only when this bean happens to be created first.
     */
    @Autowired
    private ObjectProvider<LicenseStateReconciliationListener> licenseStateReconciliationListener;

    /** Volatile: written under this service's monitor, read unsynchronised on every entitlement path. */
    private volatile AbstractTbLicenseClient tbLicenseClient;
    /**
     * Licence version of the client in {@link #tbLicenseClient}, or 0 when there is none. Written before that
     * client and cleared after it is nulled, so a live v2 client is never paired with version 0.
     */
    private volatile int licenseVersion;
    private volatile boolean isOfflineLicense = false;
    /**
     * The secret the client in {@link #tbLicenseClient} was built from, or null when this node runs none. Not
     * the cluster-wide stored secret: that one is seeded as soon as any node activates, so a peer that has not
     * activated yet would read a licence it is not running as already in force.
     */
    private volatile String publishedClientSecret;
    /** Activation is terminal: never reset by the on-demand refresh; cleared by {@link #lock(String)} and {@link #clearLicense()}. */
    private volatile boolean licenseActivated;
    /** The last secret this node tried to activate with; with {@link #nextRepeatedSecretAttemptAtMs} it rate-limits re-validating it. */
    private volatile String lastAttemptedSecret;
    /** Earliest time this node may re-validate {@link #lastAttemptedSecret}; a different secret is never held back. */
    private volatile long nextRepeatedSecretAttemptAtMs;
    /**
     * Throttles {@link #tryActivateFromAvailableSecret(boolean)}'s {@code tb_cluster} read, so a locked or
     * keyless node does not pay one SELECT per {@link #isLicenseActivated()} call. Reset on every attempt
     * whatever the outcome, so a persistently empty secret is throttled too.
     */
    private volatile long nextReactivationAttemptAtMs;
    /** Short enough that a secret another node stored converges within seconds, long enough to bound the SELECT rate. */
    private static final long REACTIVATION_RETRY_INTERVAL_MS = TimeUnit.SECONDS.toMillis(5);
    /** Short enough that a node locked by a transient failure recovers quickly, long enough to cap outbound validations. */
    private static final long REPEATED_SECRET_RETRY_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);
    /** Matched to {@link #REPEATED_SECRET_RETRY_INTERVAL_MS}, but its own constant because {@link Scheduled} needs one. */
    private static final long ACTIVATION_RETRY_INTERVAL_MINUTES = 5;
    /**
     * Single-flights every attempt to change which licence this node runs: losers report "not activated"
     * rather than queueing. Always taken before this service's monitor, never inside it.
     */
    private final ReentrantLock activationAttemptLock = new ReentrantLock();

    @PostConstruct
    public void init() {
        String storedSecret;
        boolean storedSecretUnread = false;
        try {
            storedSecret = tbClusterStore.getLicenseSecret().orElse(null);
        } catch (Exception e) {
            // Degrades to "nothing stored" rather than giving up: boot can only acquire a licence, never drop
            // one, so falling through to the environment secret is what lets a deployment whose tb_cluster row
            // is missing still come up activated instead of sitting locked until the row is repaired.
            log.error("Failed to read the license secret from the database; falling back to TB_LICENSE_SECRET.", e);
            storedSecret = null;
            storedSecretUnread = true;
        }
        String effectiveSecret = resolveEffectiveSecret(storedSecret);
        recordActivationAttempt(effectiveSecret);
        try {
            AbstractTbLicenseClient client = selectLicenseClient(effectiveSecret);
            if (client == null) {
                log.warn("No license secret found (env TB_LICENSE_SECRET empty and tb_cluster.license_secret null). " +
                        "The instance is not activated; complete setup via the activation UI.");
                return;
            }
            if (!activate(client, effectiveSecret)) {
                return;
            }
        } catch (NonProductionAllowanceExhaustedException e) {
            // Failing the context refresh is the point: booting on would serve the data plane with the allowance spent.
            log.error("{} Refusing to start.", e.getMessage());
            throw e;
        } catch (Exception e) {
            logActivationFailure(e);
            return;
        }
        // Only a read that succeeded is evidence the cluster holds nothing. Activating on the environment
        // secret is safe either way, but publishing it cluster-wide off a failed read would let a node with a
        // stale TB_LICENSE_SECRET overwrite the licence every peer is running on.
        if (!storedSecretUnread && StringUtils.isEmpty(storedSecret) && StringUtils.isNotEmpty(effectiveSecret)) {
            seedLicenseSecret(effectiveSecret);
        }
    }

    /**
     * Records that this node is about to validate {@code secret}, so it is not validated again until
     * {@link #REPEATED_SECRET_RETRY_INTERVAL_MS} has passed. A null {@code secret} - the keyless boot - equals
     * no stored secret, so the next check may attempt whatever it finds.
     */
    private void recordActivationAttempt(String secret) {
        lastAttemptedSecret = secret;
        nextRepeatedSecretAttemptAtMs = System.currentTimeMillis() + REPEATED_SECRET_RETRY_INTERVAL_MS;
    }

    /**
     * The stored secret wins whenever both exist, since it is the one the rest of the cluster is already using.
     * {@code LicenseCapacityUpgradePreflight} inverts this deliberately: the upgrade is the one moment at which a
     * single node may choose the licence for the whole cluster.
     */
    private String resolveEffectiveSecret(String storedSecret) {
        if (StringUtils.isNotEmpty(licenseSecret)) {
            if (StringUtils.isEmpty(storedSecret)) {
                return licenseSecret;
            }
            if (!licenseSecret.equals(storedSecret)) {
                log.warn("TB_LICENSE_SECRET env differs from the stored DB secret. Using the DB secret. " +
                        "Remove the env var to silence this warning.");
            }
        }
        return storedSecret;
    }

    /**
     * Publishes the secret this node has just activated with to the shared {@code tb_cluster} row, so peers and
     * later restarts converge on it. Called only after {@link #activate(AbstractTbLicenseClient, String)}
     * succeeded, since the stored secret is durable state that is read as proof the cluster holds a licence.
     */
    private void seedLicenseSecret(String secret) {
        try {
            log.info("Seeding license secret from TB_LICENSE_SECRET env into the database.");
            tbClusterStore.saveLicenseSecret(secret);
        } catch (Exception e) {
            log.warn("Failed to seed the license secret from TB_LICENSE_SECRET env into the database. This node " +
                    "is activated, but other cluster nodes will not pick the secret up until it is stored.", e);
        }
    }

    /**
     * A licence secret always wins over the non-production flag: otherwise a licensed deployment carrying
     * NON_PRODUCTION_USE=true would run watermarked and eventually be locked by the uptime cap.
     */
    AbstractTbLicenseClient selectLicenseClient(String secret) throws Exception {
        if (StringUtils.isNotEmpty(secret)) {
            if (nonProductionUse) {
                log.warn("NON_PRODUCTION_USE is set but a license secret is present. The license takes " +
                        "precedence and the instance runs in production mode. Remove the license secret to " +
                        "run in non-production mode, or unset NON_PRODUCTION_USE to silence this warning.");
            }
            return createLicenseClient(secret);
        }
        if (nonProductionUse) {
            log.warn("Starting without a license key: NON_PRODUCTION_USE is set. This instance runs in " +
                    "non-production mode (unlimited device/asset/edge quotas, no white-labeling); do not use it in production.");
            return createNonProductionClient();
        }
        return null;
    }

    /**
     * Builds and validates a client for {@code secret} without mutating this service, so a rejected secret
     * cannot affect a running client. The caller owns the result and must publish or stop it.
     */
    AbstractTbLicenseClient createLicenseClient(String secret) throws Exception {
        long releaseDate = LicenseReleaseDate.resolveReleaseDate();
        ClientScopedLicenseListener listener = new ClientScopedLicenseListener();
        AbstractTbLicenseClient client = null;
        try {
            client = OfflineTbLicenseClient.builder()
                    .listener(listener)
                    .releaseDate(releaseDate)
                    .encodedLicenseData(secret)
                    .tbLicenseCtx(licenseCtx)
                    .checkInstanceRequired(zkEnabled) //No need to check instance registry if zk disabled
                    .build();
        } catch (Exception e) {
            log.debug("The provided secret is not an offline license, falling back to the online license client", e);
        }
        if (client == null) {
            client = TbLicenseClient.builder()
                    .licenseStatisticsService(licenseStatisticsService)
                    .listener(listener)
                    .licenseSecret(secret)
                    .licenseDataFilePath(this.instanceDataFilePath)
                    .releaseDate(releaseDate)
                    .clusterId(licenseCtx.getClusterId())
                    .build();
        }
        listener.bindTo(client);
        try {
            client.init();
        } catch (Exception e) {
            stopClient(client);
            throw e;
        }
        return client;
    }

    /**
     * Builds a {@link NonProductionTbLicenseClient} for a keyless deployment; the caller owns it and must
     * publish or stop it. Refuses once cumulative uptime reaches {@link TbClusterStore#NON_PRODUCTION_UPTIME_LIMIT_MS}.
     */
    AbstractTbLicenseClient createNonProductionClient() throws Exception {
        long uptimeMs = tbClusterStore.getNonProductionUptimeMs();
        if (uptimeMs >= TbClusterStore.NON_PRODUCTION_UPTIME_LIMIT_MS) {
            throw new NonProductionAllowanceExhaustedException(nonProductionAllowanceExhaustedMessage());
        }
        long releaseDate = LicenseReleaseDate.resolveReleaseDate();
        ClientScopedLicenseListener listener = new ClientScopedLicenseListener();
        AbstractTbLicenseClient client = NonProductionTbLicenseClient.builder()
                .releaseDate(releaseDate)
                .listener(listener)
                .build();
        listener.bindTo(client);
        try {
            client.init();
        } catch (Exception e) {
            stopClient(client);
            throw e;
        }
        return client;
    }

    /** The operator-facing sentence for an exhausted allowance; the day count is derived from the cap. */
    private static String nonProductionAllowanceExhaustedMessage() {
        return "This deployment has run in non-production mode for " +
                TimeUnit.MILLISECONDS.toDays(TbClusterStore.NON_PRODUCTION_UPTIME_LIMIT_MS) +
                " days. Obtain a free license key at " + LicensePortal.URL +
                " and set TB_LICENSE_SECRET to continue.";
    }

    /**
     * Publishes an already validated client as the current one and marks the instance activated; the previous
     * client is stopped only after the new one took its place. The fields describing the client are written
     * before the client itself, and the client before the activation flag, so a request thread reading them
     * without this monitor sees the values that belong to the client it observed, or none at all.
     *
     * @param sourceSecret the secret {@code client} was built from, or null/empty for the keyless client.
     * @return whether the client was published; false means the secret was superseded while it was being
     * validated - see {@link #isSupersededSecret(String)} - and the client has been stopped.
     */
    private synchronized boolean activate(AbstractTbLicenseClient client, String sourceSecret) {
        if (isSupersededSecret(sourceSecret)) {
            log.info("Discarding a license client whose secret was replaced in the database while it was being " +
                    "validated. The instance keeps or picks up the license the cluster now holds.");
            discardUnpublishedClient(client, "the license key that was superseded");
            return false;
        }
        AbstractTbLicenseClient previousClient = this.tbLicenseClient;
        this.isOfflineLicense = client instanceof OfflineTbLicenseClient;
        this.licenseVersion = client.getLicenseVersion();
        this.publishedClientSecret = sourceSecret;
        this.tbLicenseClient = client;
        this.licenseActivated = true;
        if (previousClient != null && previousClient != client) {
            stopClient(previousClient);
        }
        if (StringUtils.isNotEmpty(sourceSecret)) {
            // Retire the claim token: a live bearer credential for the subscription secret should not outlive
            // its purpose. Only on the licensed path - a keyless client is not a licence, and the operator may
            // be mid-way through the very claim this would retire. Best effort.
            tbClusterStore.forceClearLicenseClaimToken();
        }
        log.info("License client initialized; instance ACTIVATED.");
        return true;
    }

    /**
     * Whether the secret a client was built from has already been replaced in {@code tb_cluster} by the time
     * that client is ready to be published: validating is a remote call made outside this monitor, so a slow
     * attempt could otherwise publish a superseded licence last. Answers false for anything it cannot
     * positively call superseded: the keyless client, an empty stored secret, and a failed read.
     */
    private boolean isSupersededSecret(String sourceSecret) {
        if (StringUtils.isEmpty(sourceSecret)) {
            return false;
        }
        String storedSecret;
        try {
            storedSecret = tbClusterStore.getLicenseSecret().orElse(null);
        } catch (Exception e) {
            log.warn("Failed to re-read the stored license secret before publishing the license client; " +
                    "publishing it anyway", e);
            return false;
        }
        return StringUtils.isNotEmpty(storedSecret) && !storedSecret.equals(sourceSecret);
    }

    /**
     * Whether {@code secret} is the one the client this node is currently running was built from. Under this
     * service's monitor because {@link #stopCurrentClient()} clears the client and the secret together: read
     * without it, a locked node could still be seen running the key that was just torn down.
     */
    private synchronized boolean isSecretOfPublishedClient(String secret) {
        return this.tbLicenseClient != null && secret.equals(this.publishedClientSecret);
    }

    private void logActivationFailure(Exception e) {
        LicenseErrorCode code = e instanceof LicenseException ?
                ((LicenseException) e).getErrorCode() : LicenseErrorCode.GENERAL_ERROR;
        log.error("License validation failed ({}). The instance is not activated; enter a valid key via the activation UI.", code, e);
    }

    private void stopClient(AbstractTbLicenseClient client) {
        if (client != null) {
            try {
                client.stop();
            } catch (Exception e) {
                log.warn("Failed to stop the license client", e);
            }
        }
    }

    /**
     * Gives up the slot the outgoing licence still holds for this node, unless the incoming client turns out to
     * hold that very instance - which happens whenever {@link #createLicenseClient(String)} found
     * {@code instance-license.data} describing the same secret and reused it. Releasing then would leave this
     * node running against an instance the portal has released, and it would fail on its next check-in.
     * <p>
     * Taken from the live client rather than from {@code instance-license.data}: activating a different secret
     * deletes that file, and its sequence number would already be behind the heartbeat's.
     */
    private void releaseOutgoingInstance(AbstractTbLicenseClient outgoing, AbstractTbLicenseClient incoming) {
        if (holdsSameInstance(outgoing, incoming)) {
            return;
        }
        releaseInstance(outgoing, "the previous license");
    }

    /** Whether {@code client} holds a portal instance of its own, and therefore wrote {@code instance-license.data}. */
    private static boolean holdsInstance(AbstractTbLicenseClient client) {
        return client != null && client.getInstanceId() != null;
    }

    /**
     * Whether two clients hold one and the same portal instance, which is the only thing that makes releasing
     * it unsafe. The secrets they were built from cannot answer this: every client of this node shares one
     * {@code instance-license.data}, and building a client for a different secret deletes it, so a later client
     * built on the original secret activates an instance of its own. A client holding none - offline, keyless,
     * or online but never activated - shares nothing.
     */
    private static boolean holdsSameInstance(AbstractTbLicenseClient one, AbstractTbLicenseClient other) {
        if (one == null || other == null) {
            return false;
        }
        String instanceId = one.getInstanceId();
        return instanceId != null && instanceId.equals(other.getInstanceId());
    }

    /**
     * Gives up a client that was built but is never going to run, whatever stopped it - refused, unstorable,
     * superseded or cleared: its slot goes back, it is stopped, and {@code instance-license.data} is put back
     * into agreement with the licence this node is still running. Every failure between acquiring a client and
     * publishing it ends here, so none of the three can be forgotten at one site and not another.
     * <p>
     * A discarded client that turns out to hold the instance the live client is running on - which it does
     * whenever it reused {@code instance-license.data} instead of activating one of its own - is only stopped.
     * Its slot is not this node's to give back, and the file is left as the discarded client rewrote it: that
     * client re-checked the instance, so the live client's own credential is the stale one by now and putting
     * it back would be no better.
     */
    private void discardUnpublishedClient(AbstractTbLicenseClient client, String whatItWasHeldFor) {
        if (holdsSameInstance(client, this.tbLicenseClient)) {
            stopClient(client);
            return;
        }
        boolean instanceReleased = releaseInstance(client, whatItWasHeldFor);
        stopClient(client);
        restoreRunningInstanceDataFile(instanceReleased);
    }

    /**
     * Puts {@code instance-license.data} back into agreement with the licence this node is running, after
     * {@link #createLicenseClient(String)} deleted it - which it does as soon as it is given another secret,
     * before it asks the portal for an instance, so the file is gone whether that client went on to run or was
     * refused. Called on every path that ends without it running.
     * <p>
     * The running client rewrites it. Without that, a node restarted before its next check-in activates a
     * second instance, and on a single-instance licence cannot activate at all, because its own live client
     * holds the only slot.
     * <p>
     * When nothing can rewrite it - no running client, or an offline or keyless one, for which
     * {@link AbstractTbLicenseClient#persistInstanceData()} is a no-op - the file is left as it stands, since
     * it may still describe a live instance whose slot deleting it would orphan. Only
     * {@code discardedInstanceReleased} says otherwise: the instance the file describes has just been given
     * back, and a file describing a deactivated instance refuses the same key as a stale activation on every
     * later attempt, across restarts.
     */
    private void restoreRunningInstanceDataFile(boolean discardedInstanceReleased) {
        AbstractTbLicenseClient running = this.tbLicenseClient;
        if (!holdsInstance(running)) {
            if (discardedInstanceReleased) {
                deleteInstanceDataFile();
            }
            return;
        }
        try {
            running.persistInstanceData();
        } catch (Exception e) {
            log.warn("Failed to restore this node's license instance data file. If this node is restarted " +
                    "before its next license check-in, it will activate a new instance.", e);
        }
    }

    /**
     * Gives up the instance {@code client} holds. Best effort and never fatal: by the time this runs the
     * licence change it belongs to has already been decided, and a portal that cannot be reached must not undo
     * it. The cost of a failure is one orphaned instance the customer can deactivate in the portal.
     *
     * @return whether the slot is back in the licence's pool - false both for a client that held none and for
     * a release the portal refused, on which nothing that still records that instance may be destroyed.
     */
    private boolean releaseInstance(AbstractTbLicenseClient client, String whatItWasHeldFor) {
        if (client == null) {
            return false;
        }
        boolean heldInstance = holdsInstance(client);
        try {
            client.releaseInstance();
        } catch (Exception e) {
            log.warn("Failed to release this node's instance on {}. Deactivate it in the ThingsBoard portal " +
                    "to free the slot.", whatItWasHeldFor, e);
            return false;
        }
        return heldInstance;
    }

    private synchronized void stopCurrentClient() {
        stopClient(this.tbLicenseClient);
        this.tbLicenseClient = null;
        this.publishedClientSecret = null;
    }

    @PreDestroy
    public void stop() {
        stopCurrentClient();
    }

    @Override
    public void onError(LicenseException e) {
        log.error("License Error occurred: {}({}) - {}", e.getErrorCode(),
                e.getErrorCode().getErrorCode(), e.getMessage());
        if (e.isCritical()) {
            lock("Critical license error at runtime: the instance is no longer activated (management plane locked). " +
                    "Re-enter a valid key via the activation UI.");
        }
    }

    /**
     * The listener a licence client is given, scoped to that client: a stopped client can still deliver a
     * critical error from a check already in flight, and locking on it would tear down the freshly activated
     * replacement. A callback from a client that is not the published one is dropped.
     */
    private class ClientScopedLicenseListener implements TbLicenseClientListener {

        /** Written right after the client is constructed and before it is initialized, so no check can see it null. */
        private volatile AbstractTbLicenseClient client;

        void bindTo(AbstractTbLicenseClient client) {
            this.client = client;
        }

        @Override
        public void onError(LicenseException licenseError) {
            AbstractTbLicenseClient owner = this.client;
            if (owner == null || owner != tbLicenseClient) {
                log.debug("Ignoring a license error raised by a license client that is no longer the current one: {}",
                        licenseError.getMessage());
                return;
            }
            BasicLicenseActivationService.this.onError(licenseError);
        }

    }

    /**
     * Re-checks the mode under the monitor {@link #activate(AbstractTbLicenseClient, String)} publishes under,
     * so a node that acquired a real licence meanwhile is not locked over an allowance that no longer governs it.
     */
    @Override
    public synchronized boolean revokeNonProductionEntitlement() {
        if (!isNonProductionMode()) {
            // Not a failure: the deployment acquired a licence while the revocation was being decided.
            log.debug("Skipping the non-production revocation: the instance is no longer running keyless.");
            return false;
        }
        lock(nonProductionAllowanceExhaustedMessage() + " The instance is no longer activated (management plane locked).");
        return true;
    }

    /**
     * Locks the instance: clears activation and stops the current client, so it settles into the same
     * unactivated, management-plane-locked state whatever triggered it. Idempotent, and not terminal -
     * {@link #isLicenseActivated()} keeps retrying the stored secret. The fields describing the client are
     * cleared after it is nulled, so no licence is reported without one.
     */
    private synchronized void lock(String reason) {
        if (!licenseActivated) {
            return;
        }
        log.error(reason);
        licenseActivated = false;
        stopCurrentClient();
        licenseVersion = 0;
        isOfflineLicense = false;
    }

    /** Answered from the mirrored field, so this and the quota helpers cannot disagree about the same licence. */
    @Override
    public int getLicenseVersion() {
        return licenseVersion;
    }

    @Override
    public boolean isLicenseActivated() {
        // Activation happens on a single node but is stored in the shared database, so a node that still has
        // something to gain picks the secret up on its next request - no polling, no inter-node messaging.
        if (needsStoredSecret()) {
            tryActivateFromAvailableSecret(false);
        }
        return licenseActivated;
    }

    @Override
    public void pickUpStoredLicenseSecret() {
        if (!needsStoredSecret()) {
            return;
        }
        tryActivateFromAvailableSecret(true);
    }

    @Override
    public SubscriptionInfo previewLicenseKey(String secret) {
        if (StringUtils.isEmpty(secret)) {
            throw new IllegalArgumentException("License secret must not be empty");
        }
        long releaseDate;
        try {
            releaseDate = LicenseReleaseDate.resolveReleaseDate();
        } catch (ParseException e) {
            // The build date is a compile-time constant in the one agreed format, so this is unreachable.
            throw new IllegalStateException("Failed to resolve this release's date", e);
        }
        UUID clusterId = licenseCtx.getClusterId();
        // Read once, before anything is asked of the portal: what is previewed and how it is judged are two
        // decisions that must not be taken on different answers to this.
        boolean keyAlreadyInForce = isSecretOfPublishedClient(secret);
        SubscriptionPreviewResponse response;
        boolean offline = true;
        try {
            // Offline first, as createLicenseClient does, so the two agree on what an offline key looks like.
            response = OfflineTbLicenseClient.preview(secret, clusterId, releaseDate);
        } catch (Exception offlineError) {
            if (looksLikeOfflineBlob(offlineError)) {
                throw rejection(offlineError);
            }
            offline = false;
            if (clusterId == null) {
                // The portal reads the cluster id as a UUID, so sending none is rejected as a malformed
                // request rather than answered with a verdict on the key - and a malformed-request body is
                // not a licence error, so it would reach the operator as the wrong sentence entirely.
                throw rejection(new LicenseException("Cluster Id not found!", LicenseErrorCode.CLUSTER_ID_NOT_FOUND));
            }
            try {
                // The key already in force is described rather than sought a slot for: the slot such a search
                // would look for is the one this very node holds, so a full licence comes back refused - for
                // the key that Replace short-circuits on without building a client at all.
                response = keyAlreadyInForce
                        ? TbLicenseClient.previewSubscription(null, secret, releaseDate, clusterId, true)
                        : TbLicenseClient.previewSubscription(null, secret, releaseDate, clusterId);
            } catch (Exception onlineError) {
                throw rejection(onlineError);
            }
        }
        return buildPreview(response, offline, keyAlreadyInForce);
    }

    /**
     * Whether a blob that failed to preview was an offline licence with a verdict of its own, which must not
     * be retried against the portal: the portal has no opinion on a cluster hash or an updates horizon, and
     * would answer INVALID_LICENSE_SECRET and hide the real reason.
     * <p>
     * A rejected signature is deliberately <b>not</b> in this list, even though it is such a verdict. The
     * client library reports it under {@code INVALID_OFFLINE_LICENSE_DATA_CHECK} - the same code it reports a
     * secret that is simply not base64, and therefore every online key, under - so keying on it here would
     * stop every online key from ever reaching the portal. The cost of letting a tampered blob through is one
     * wasted round trip: that code and the portal's INVALID_LICENSE_SECRET map to the same sentence, so the
     * operator is told the same thing either way.
     */
    private static boolean looksLikeOfflineBlob(Exception e) {
        if (!(e instanceof LicenseException licenseException)) {
            return false;
        }
        return switch (licenseException.getErrorCode()) {
            case INVALID_CLUSTER_ID_CHECK, CLUSTER_ID_NOT_FOUND, UNSUPPORTED_SOFTWARE_VERSION -> true;
            default -> false;
        };
    }

    private SubscriptionException rejection(Exception e) {
        log.warn("License key preview rejected", e);
        return new SubscriptionException(LicenseKeyRejections.messageFor(e), SubscriptionErrorCode.FEATURE_DISABLED);
    }

    /**
     * Describes the previewed key and nothing else. This deployment's own numbers - the entity counts and the
     * AI credits already spent - are deliberately left unset: the caller knows them, and filled in beside the
     * new key's quotas they would read as a promise about it.
     */
    private SubscriptionInfo buildPreview(SubscriptionPreviewResponse response, boolean offline,
                                          boolean keyAlreadyInForce) {
        PlanData planData = response.getPlanData();
        int licenseVersion = previewedLicenseVersion(planData);
        SubscriptionInfo preview = SubscriptionInfoMapper.toSubscriptionInfo(planData,
                response.getSubscriptionData(), licenseVersion, offline);
        if (!keyAlreadyInForce) {
            // The preview answers the same question the apply does, so it has to give the same answer: a
            // dialog that previewed clean and then failed on Replace would be worse than no dialog. Nothing
            // is measured against the key already in force, because applying it changes nothing - a fleet
            // grown past a plan the portal has since downgraded must not be refused here and accepted there.
            verifyFleetFitsLicense(deviceService.countDevices(), assetService.countAssets(), planData, licenseVersion);
        }
        return preview;
    }

    /**
     * Refuses a licence that covers fewer devices or assets than this deployment already holds: block before,
     * never break after. Edges are deliberately excluded - their quota is a soft, creation-time cap under every
     * licence - so their count is never even read here.
     * <p>
     * Shares {@link LicenseCapacity#exceedsCap} and the operator-facing sentence with
     * {@code LicenseCapacityStartupGate} and {@code LicenseCapacityUpgradePreflight}, but not the condition:
     * those two run only on a community-grant plan and treat every non-positive quota as "no cap", while this
     * one runs on every plan and reads a v2 zero as the zero it is. So a v2 licence granting no devices is
     * refused here and still boots and upgrades - which is the asymmetry, not an oversight: this is the only
     * one of the three that can still stop the change from happening.
     * <p>
     * Reads the plan itself rather than the licence it was mapped into, for the reason
     * {@link #verifyCapFits} gives.
     */
    private static void verifyFleetFitsLicense(long devicesCount, long assetsCount, PlanData planData, int licenseVersion) {
        verifyCapFits(CappedEntity.DEVICE, devicesCount,
                SubscriptionInfoMapper.longValue(planData, PlanDataConstants.MAX_DEVICES_KEY), licenseVersion);
        verifyCapFits(CappedEntity.ASSET, assetsCount,
                SubscriptionInfoMapper.longValue(planData, PlanDataConstants.MAX_ASSETS_KEY), licenseVersion);
    }

    /**
     * {@code limit} is the raw plan quota, read under plan-data rules: the quota as the API reports it answers
     * 0 both for an unlimited licence and for a v2 licence granting zero, and those are opposite verdicts here.
     */
    private static void verifyCapFits(CappedEntity entity, long count, long limit, int licenseVersion) {
        if (!SubscriptionInfoMapper.isUnlimited(limit, licenseVersion) && LicenseCapacity.exceedsCap(count, limit)) {
            throw new SubscriptionException(LicenseCapacity.capExceededMessage(entity, count, limit),
                    SubscriptionErrorCode.FEATURE_DISABLED);
        }
    }

    /**
     * The version of the licence being previewed, not of the one this node runs on: v1 and v2 disagree about
     * what a zero quota means, so the incoming plan has to be read by its own rules. Read exactly as
     * {@link AbstractTbLicenseClient#getLicenseVersion()} reads it, absent meaning v1.
     */
    private static int previewedLicenseVersion(PlanData planData) {
        PlanItem item = planData != null ? planData.get(PlanDataConstants.LICENSE_VERSION_KEY) : null;
        return item != null && item.getValue() != null ? item.getValue().asInt() : 1;
    }

    /**
     * Re-attempts convergence on a timer, so an unattended node recovers without a management request
     * arriving. Since {@link #reconcileLicenseState()} exists this is a missed-message reconciler and nothing
     * more: it is what covers a node that was down or partitioned when the broadcast went out, never the path
     * a waiting administrator depends on.
     * <p>
     * The unactivated branch goes through {@link #tryActivateFromAvailableSecret(boolean)} first, because only
     * that path carries the environment-secret fallback that makes a failed boot recoverable without a restart
     * - {@link #init()} seeds the environment secret only after activating with it, so a node whose boot hit
     * something transient has nothing stored to retry. Only when there is no secret to activate from at all
     * does reconvergence run on that branch: it is what settles a NON_PRODUCTION_USE node that came up
     * unactivated onto its keyless client instead of leaving it locked until a restart.
     */
    @Scheduled(fixedRate = ACTIVATION_RETRY_INTERVAL_MINUTES, initialDelay = ACTIVATION_RETRY_INTERVAL_MINUTES,
            timeUnit = TimeUnit.MINUTES)
    public void retryActivation() {
        if (needsStoredSecret() && (tryActivateFromAvailableSecret(false) || hasSecretToActivateFrom())) {
            return;
        }
        reconcileLicenseState();
    }

    /**
     * Brings this node onto whatever licence state the cluster now holds. Handles every way that state can
     * change, which the replaced-secret-only predecessor did not: a cleared secret reached nobody at all,
     * because an empty stored secret returned early here and on the tick alike.
     */
    @Override
    public void reconcileLicenseState() {
        if (!activationAttemptLock.tryLock()) {
            // Another thread is already inside an activation attempt. It may have read tb_cluster before the
            // write this call is reacting to, in which case this signal is lost and the five-minute tick is
            // what recovers it - the accepted cost of never queueing threads behind a remote validation.
            // At info: this is the one path on which an administrator waiting for an activation to take effect
            // waits out the tick instead, and the log is the only place that says why.
            log.info("Skipping license reconvergence: an activation attempt is already in flight.");
            return;
        }
        boolean licenseCleared;
        try {
            licenseCleared = reconcileWhileLocked();
        } finally {
            activationAttemptLock.unlock();
        }
        if (licenseCleared) {
            // Notified even when there was no client to give up: an unactivated node is exactly where the
            // setup latches go stale, since only an unactivated node ever sets them.
            //
            // Called after the unlock and outside this service's monitor: the listener is a bean in another
            // module whose work includes a database read, and every activation path would stall behind it.
            //
            // Best effort, like everything else on this path: a listener that is absent, ambiguous or failing
            // leaves stale setup latches behind, which must not turn into a failed reconvergence.
            try {
                LicenseStateReconciliationListener listener = licenseStateReconciliationListener.getIfUnique();
                if (listener != null) {
                    listener.onLicenseStateReconciled();
                }
            } catch (Exception e) {
                log.warn("Failed to notify the license state reconciliation listener", e);
            }
        }
    }

    /**
     * The half of {@link #reconcileLicenseState()} that runs under {@link #activationAttemptLock}.
     *
     * @return whether the cluster was found to hold no licence secret, so the caller must drop the node-local
     * setup latches too.
     */
    private boolean reconcileWhileLocked() {
        String storedSecret;
        try {
            storedSecret = tbClusterStore.getLicenseSecret().orElse(null);
        } catch (Exception e) {
            // Includes the empty table the store refuses to report as a cleared value: a row that could not be
            // read is not evidence of anything, so this node stays on the licence it already has.
            log.error("Failed to read the license secret from the database", e);
            return false;
        }
        // The stored secret alone. Deliberately NOT resolveEffectiveSecret(): that falls back to the
        // environment secret, which would make this node answer a cleared licence by re-activating on the
        // old key - and, since nothing is stored, re-seeding it for the whole cluster.
        if (StringUtils.isEmpty(storedSecret)) {
            dropLocalLicense();
            return true;
        }
        if (storedSecret.equals(lastAttemptedSecret) && licenseActivated && !isNonProductionMode()) {
            // Already on it. Not a no-op for a locked or keyless node, which is why the state is part of
            // the test and not just the secret.
            return false;
        }
        log.info("Reconverging on the license secret the cluster holds.");
        recordActivationAttempt(storedSecret);
        AbstractTbLicenseClient client;
        try {
            // Validated before anything is published, so a bad secret leaves this node on the licence it
            // has. Nothing is written back on either outcome - see this method's contract.
            client = createLicenseClient(storedSecret);
        } catch (Exception e) {
            logActivationFailure(e);
            // The failure may well have come after the file was deleted: building a client for another secret
            // deletes it before it asks the portal for an instance, and the portal is what refuses.
            restoreRunningInstanceDataFile(false);
            return false;
        }
        if (isLicenseSecretCleared()) {
            // Validating is a remote call, and a peer can clear the licence while it is out.
            // isSupersededSecret() cannot catch that - it answers false for an empty stored secret - so
            // publishing here would leave this node serving a licence the cluster has already given up.
            log.info("Discarding a license client whose secret was cleared in the database while it was being " +
                    "validated.");
            discardUnpublishedClient(client, "the license key that was cleared");
            // Restores the file and then, through releaseLicenseClientState, deletes it: this node is giving
            // the licence up, so the correct final state is no instance and no file.
            dropLocalLicense();
            return true;
        }
        AbstractTbLicenseClient outgoing = this.tbLicenseClient;
        if (activate(client, storedSecret)) {
            // After the new client is published, so a portal call that hangs cannot delay the swap taking
            // effect, and only ever for a client this node has actually stopped running on - a superseded
            // activation leaves this node on the outgoing licence, whose slot it still needs.
            releaseOutgoingInstance(outgoing, client);
        }
        return false;
    }

    /**
     * Whether {@code tb_cluster} now holds no licence secret. A read that fails - the unreadable row included -
     * answers false, so the caller publishes the client it validated rather than giving up a licence on no
     * evidence.
     */
    private boolean isLicenseSecretCleared() {
        try {
            return StringUtils.isEmpty(tbClusterStore.getLicenseSecret().orElse(null));
        } catch (Exception e) {
            log.warn("Failed to re-read the stored license secret before publishing the license client; " +
                    "publishing it anyway", e);
            return false;
        }
    }

    /**
     * Gives up the licence this node holds because the cluster no longer holds one, and settles into whichever
     * unlicensed state this node is configured for - keyless when NON_PRODUCTION_USE is set and this node
     * holds no secret of its own, unactivated otherwise. Writes nothing back: the node that cleared the
     * secret already did that.
     * <p>
     * A node holding nothing is not an early return: settling is exactly what it is missing, since nothing
     * else grants the keyless client to a NON_PRODUCTION_USE node whose boot found no secret.
     */
    private synchronized void dropLocalLicense() {
        if (isNonProductionMode()) {
            // Already on the keyless client this would settle into.
            return;
        }
        if (tbLicenseClient != null) {
            log.warn("The cluster's license secret was cleared; this node is no longer activated.");
        }
        // Clearing a licence cluster-wide requires TB_LICENSE_SECRET to be unset on every node first:
        // otherwise the retry path's environment fallback re-seeds the old key and undoes the clear.
        //
        // Known limitation: a node that activated from the environment secret but failed to seed it flaps
        // here - it is dropped on each tick and re-activated on the next retry - because its state is
        // indistinguishable from a node whose secret the cluster legitimately cleared. Separating the two
        // would need this service to track whether the licence it holds came from the stored secret.
        releaseLicenseClientState();
        if (StringUtils.isNotEmpty(licenseSecret)) {
            // Reached on a clear broadcast, which retryActivation() no longer falls through to: a node that
            // still holds TB_LICENSE_SECRET has a licence to retry, and selectLicenseClient()'s contract is
            // that a secret outranks the non-production flag. Settling it keyless would run it watermarked
            // and accrue non-production uptime until the next retry picks that secret up again.
            return;
        }
        try {
            // selectLicenseClient(null) answers the keyless client when NON_PRODUCTION_USE is set and null
            // otherwise, so this settles into the same state a fresh boot with no secret would reach.
            AbstractTbLicenseClient keylessClient = selectLicenseClient(null);
            if (keylessClient != null) {
                activate(keylessClient, null);
            }
        } catch (Exception e) {
            logActivationFailure(e);
        }
    }

    /**
     * Drops everything that describes the running licence client, and the client with it, so no licence field
     * outlives the client it describes. The activation throttles go too, so a key entered right afterwards is
     * not held back.
     * <p>
     * The instance goes back to the portal and the file describing it is deleted, on both the operator's clear
     * and the peer's drop. These are one act and must not be separable: a node that gave the instance up but
     * kept the file re-checks a deactivated instance when the same key is entered again, which is critical and
     * which nothing on that path clears - so it could never activate again, across restarts.
     */
    private synchronized void releaseLicenseClientState() {
        licenseActivated = false;
        releaseInstance(this.tbLicenseClient, "the license that was cleared");
        stopCurrentClient();
        deleteInstanceDataFile();
        licenseVersion = 0;
        isOfflineLicense = false;
        lastAttemptedSecret = null;
        nextRepeatedSecretAttemptAtMs = 0;
        nextReactivationAttemptAtMs = 0;
    }

    /**
     * Whether a stored licence secret could still change this node's licence state: it is not activated, or it
     * is activated only off the keyless client, whose grant is time-limited - without which a keyless-booted
     * node would never converge onto a real licence a peer stored afterwards.
     */
    private boolean needsStoredSecret() {
        return !licenseActivated || isNonProductionMode();
    }

    /**
     * Whether this node still has a secret it could activate from - the one stored for the cluster, or the
     * environment one. Guards the fall-through from {@link #retryActivation()} into reconvergence, which
     * consults the stored secret alone: a node still working through the environment fallback would be
     * settled onto its keyless client and drift into non-production mode, and one merely held back by the
     * per-secret backoff would have that secret validated a second time on the same tick. A read that fails
     * answers true, since the failure is no evidence there is nothing left to retry.
     */
    private boolean hasSecretToActivateFrom() {
        if (StringUtils.isNotEmpty(licenseSecret)) {
            return true;
        }
        try {
            return StringUtils.isNotEmpty(tbClusterStore.getLicenseSecret().orElse(null));
        } catch (Exception e) {
            log.error("Failed to read the license secret from the database; assuming there is still one to " +
                    "activate from.", e);
            return true;
        }
    }

    @Override
    public boolean isNonProductionMode() {
        return tbLicenseClient instanceof NonProductionTbLicenseClient;
    }

    /**
     * Re-attempts activation from whichever secret this node can obtain: the one stored for the cluster, or -
     * when nothing is stored yet - the environment one. That fallback is what makes a failed boot recoverable
     * without a restart: {@link #init()} seeds the environment secret only after activating with it, so a node
     * whose boot hit something transient has nothing stored to retry. On success the secret is seeded.
     */
    private boolean tryActivateFromAvailableSecret(boolean bypassThrottle) {
        // Cheapest possible fast path: an in-memory comparison, no lock and no database access. Skipped when
        // a caller needs this node to converge right now - see pickUpStoredLicenseSecret().
        if (!bypassThrottle && System.currentTimeMillis() < nextReactivationAttemptAtMs) {
            return false;
        }
        if (!activationAttemptLock.tryLock()) {
            // Another request is already validating; report "not activated" instead of queueing behind it.
            return false;
        }
        try {
            if (!needsStoredSecret()) {
                return true;
            }
            // Reserve the next window before the database read, so a slow or failing read cannot itself be
            // retried in a tight loop. Reserved even on a forced call: it arms the throttle for the next caller.
            nextReactivationAttemptAtMs = System.currentTimeMillis() + REACTIVATION_RETRY_INTERVAL_MS;
            String storedSecret;
            boolean storedSecretUnread = false;
            try {
                storedSecret = tbClusterStore.getLicenseSecret().orElse(null);
            } catch (Exception e) {
                // Degrades to "nothing stored", as boot does: this path only ever acquires a licence, so a
                // read it cannot make must not be what keeps a node with TB_LICENSE_SECRET set unactivated.
                log.error("Failed to read the license secret from the database; falling back to TB_LICENSE_SECRET.", e);
                storedSecret = null;
                storedSecretUnread = true;
            }
            String effectiveSecret = resolveEffectiveSecret(storedSecret);
            if (StringUtils.isEmpty(effectiveSecret)) {
                return false;
            }
            if (effectiveSecret.equals(lastAttemptedSecret) && System.currentTimeMillis() < nextRepeatedSecretAttemptAtMs) {
                // A secret this node already attempted, and the backoff has not elapsed. Rate-limiting rather
                // than refusing outright: the stored secret IS the one a locked node last attempted, so an
                // outright refusal would make a transient lock permanent. The window still caps the
                // unauthenticated pickUpStoredLicenseSecret() path at one outbound validation per interval.
                return false;
            }
            recordActivationAttempt(effectiveSecret);
            try {
                // effectiveSecret is never empty here, so this can never fall through to the keyless client
                // and drift a licensed-but-unactivated instance into non-production mode.
                if (!activate(selectLicenseClient(effectiveSecret), effectiveSecret)) {
                    return false;
                }
            } catch (Exception e) {
                logActivationFailure(e);
                return false;
            }
            if (!storedSecretUnread && StringUtils.isEmpty(storedSecret)) {
                // Nothing was stored, so publish the validated secret for the rest of the cluster, as boot does.
                // A read that failed says nothing about what is stored, and this runs every few minutes on a
                // live cluster, so seeding off it would republish a stale environment secret cluster-wide.
                seedLicenseSecret(effectiveSecret);
            }
            return true;
        } finally {
            activationAttemptLock.unlock();
        }
    }

    /**
     * Under {@link #activationAttemptLock} rather than this service's monitor: a reconvergence that read
     * {@code tb_cluster} before this call holds no lock while it validates, and would otherwise tear the
     * operator's key back down on evidence that is already stale. Taking the lock here and not inside the
     * {@code synchronized} helpers is what keeps the class's one lock order intact.
     */
    @Override
    public void applyLicenseKey(String secret) {
        if (StringUtils.isEmpty(secret)) {
            throw new IllegalArgumentException("License secret must not be empty");
        }
        activationAttemptLock.lock();
        try {
            if (isSecretOfPublishedClient(secret)) {
                // Nothing to change, and building a client to say so would break this node: it would find
                // instance-license.data describing this very instance and re-check it, which rotates the
                // instance's credential on the portal and leaves the running client's heartbeat stale. Every
                // discard path below would then leave that client running until the portal refuses it and the
                // node locks. The key is re-validated by the check-in the running client already makes.
                log.info("The submitted license key is the one this node is already running on; nothing to apply.");
                // Stored all the same: this node's activation may never have reached the cluster row, since the
                // seed after an env-var activation can fail, and re-entering the key is what an administrator
                // does about that. Nothing was changed here, so a failure to store must not be raised as one.
                try {
                    tbClusterStore.saveLicenseSecret(secret);
                } catch (Exception e) {
                    log.warn("Failed to store the license key this node is already running on.", e);
                }
                return;
            }
            recordActivationAttempt(secret);
            AbstractTbLicenseClient client;
            try {
                // Validate before touching the running client, so a wrong key cannot take down an activated instance.
                client = createLicenseClient(secret);
            } catch (Exception e) {
                logActivationFailure(e);
                // The most common way this node ends up without the file describing the licence it runs: the
                // client deletes it before asking the portal for an instance, and the portal refuses the key.
                restoreRunningInstanceDataFile(false);
                // The licence server told us which of seven things went wrong and the code survived the wire;
                // one sentence for all of them would throw that away.
                throw new SubscriptionException(LicenseKeyRejections.messageFor(e),
                        SubscriptionErrorCode.FEATURE_DISABLED);
            }
            try {
                // Before the secret is persisted and before anything is published, so a key that cannot cover
                // this deployment leaves no trace: the cluster keeps the secret it had and this node the
                // licence it was running on.
                verifyFleetFitsLicense(deviceService.countDevices(), assetService.countAssets(),
                        client.getPlanData(), client.getLicenseVersion());
            } catch (SubscriptionException e) {
                // The online client took an instance on the portal during createLicenseClient, and the plan
                // this refusal reads only became available with it. A refused key must cost no slot - and, since
                // createLicenseClient also rewrote the shared data file, must leave no trace of it either.
                discardUnpublishedClient(client, "the license key that was refused");
                throw e;
            } catch (Exception e) {
                // The fleet counts are read here too, and a database that cannot answer them is no verdict on
                // the key - but the client is just as unpublishable, so it costs the same nothing.
                discardUnpublishedClient(client, "the license key that could not be checked");
                throw e;
            }
            try {
                tbClusterStore.saveLicenseSecret(secret); // persist only after a successful validation
            } catch (Exception e) {
                // Same slot and same file the refusal above gives back, for the same reason: nothing was
                // published, the cluster keeps the secret it had, and this client is never going to run.
                discardUnpublishedClient(client, "the license key that could not be stored");
                throw e;
            }
            AbstractTbLicenseClient outgoing = this.tbLicenseClient;
            if (!activate(client, secret)) {
                // The key stored last is the cluster's, so the administrator is told rather than left believing
                // this one took effect.
                throw new SubscriptionException("The license key was superseded by another activation; try again",
                        SubscriptionErrorCode.FEATURE_DISABLED);
            }
            // After the new client is published, so a portal call that hangs cannot delay the swap taking
            // effect, and only ever for a client this node has actually stopped running on.
            releaseOutgoingInstance(outgoing, client);
            publishLicenseStateChanged();
        } finally {
            activationAttemptLock.unlock();
        }
    }

    /**
     * Puts this node back where a freshly installed one starts, without a restart: the client is stopped, the
     * stored secret, claim token and instance data file are dropped, and the activation throttles are reset so
     * a key entered right afterwards is not held back.
     * <p>
     * Under {@link #activationAttemptLock} for the same reason as
     * {@link #applyLicenseKey(String)}: an in-flight activation attempt must not re-publish the licence this
     * is giving up.
     */
    @Override
    public void clearLicense() {
        activationAttemptLock.lock();
        try {
            log.warn("Clearing all license state of the deployment. The instance is no longer activated.");
            releaseLicenseClientState();
            tbClusterStore.clearLicenseSecret();
            tbClusterStore.forceClearLicenseClaimToken();
            publishLicenseStateChanged();
        } finally {
            activationAttemptLock.unlock();
        }
    }

    /**
     * Tells the rest of the cluster to reconverge. Best effort and never fatal: the five-minute tick is the
     * backstop for a node that misses this, and a broadcast failure must not undo a licence change that is
     * already persisted and already in effect on this node. The listener hands the fan-out to its own thread,
     * so this returns without doing I/O even though both call sites hold {@link #activationAttemptLock}.
     */
    private void publishLicenseStateChanged() {
        try {
            eventPublisher.publishEvent(new LicenseStateChangedEvent());
        } catch (Exception e) {
            log.warn("Failed to broadcast the license change to the cluster. Other nodes will pick it up on " +
                    "their next reconciliation tick.", e);
        }
    }

    private void deleteInstanceDataFile() {
        try {
            Files.deleteIfExists(Paths.get(instanceDataFilePath));
        } catch (Exception e) {
            log.warn("Failed to delete the license instance data file {}", instanceDataFilePath, e);
        }
    }

    @Override
    public AbstractTbLicenseClient getClient() {
        return this.tbLicenseClient;
    }

    @Override
    public boolean isOfflineLicense() {
        return this.isOfflineLicense;
    }

}
