// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.license.client.InstanceRegistry;
import org.thingsboard.license.client.OfflineTbLicenseClient;
import org.thingsboard.license.client.TbLicenseClient;
import org.thingsboard.license.client.TbLicenseCtx;
import org.thingsboard.license.shared.PlanData;
import org.thingsboard.license.shared.PlanDataConstants;
import org.thingsboard.license.shared.SubscriptionPreviewResponse;
import org.thingsboard.license.shared.exception.LicenseException;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.dao.subscription.CommunityGrantPlan;
import org.thingsboard.server.dao.subscription.EntityCapExceededException;
import org.thingsboard.server.dao.subscription.LicenseCapacity;
import org.thingsboard.server.dao.subscription.LicenseCapacity.CappedEntity;
import org.thingsboard.server.dao.subscription.LicenseReleaseDate;
import org.thingsboard.server.dao.subscription.SubscriptionInfoMapper;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Checks, before the upgrade touches any data, that this instance does not hold more devices or assets than
 * the licence it is about to run on covers - for the plans {@link CommunityGrantPlan#COMMUNITY_GRANT_KEY}
 * identifies, and for no others. {@code LicenseCapacityStartupGate} enforces the same condition permanently at
 * startup, but only on an already converted database.
 * <p>
 * The licence key the check runs on is {@code license.secret} ({@code TB_LICENSE_SECRET}) when one is supplied
 * and the key stored in {@code tb_cluster} otherwise ({@link #resolveLicenseSecret}); a supplied key the check
 * does not refuse replaces the stored one ({@link #replaceStoredLicenseSecret}). The plan behind the key is read
 * from the offline licence when it is one ({@link #buildLicenseClient}), and otherwise from the portal's
 * describe-only preview ({@link #previewPlanData}), which activates nothing. Everything except a positively
 * established over-cap lets the upgrade proceed - no key, no cluster identity, a portal that cannot be reached,
 * an unreadable plan, an unlimited quota, a bounded count that cannot be read; the startup gate re-checks on the
 * first boot afterwards. Once the cap is established the refusal stands even if the exact count for the message
 * cannot be read. Edges are untouched here, exactly as in the startup gate: their quota stays a soft, creation-time cap.
 */
@Service
@Profile("install")
@RequiredArgsConstructor
@Slf4j
public class LicenseCapacityUpgradePreflight {

    private final JdbcTemplate jdbcTemplate;

    @Value("${license.secret:}")
    private String licenseSecret;

    // Long enough for a slow but working portal, short enough for an operator watching the upgrade to wait out a stall.
    private static final long PREVIEW_BUDGET_MILLIS = TimeUnit.SECONDS.toMillis(30);

    // Not final: overridden by the unit test so the timeout path does not have to wait out the real budget.
    private long previewBudgetMillis = PREVIEW_BUDGET_MILLIS;

    public void check() {
        String storedSecret = readStoredLicenseSecret();
        verifyCapacity(resolveLicenseSecret(storedSecret));
        replaceStoredLicenseSecret(storedSecret);
    }

    private void verifyCapacity(String secret) {
        if (StringUtils.isEmpty(secret)) {
            log.info("No license secret is available; skipping the pre-upgrade license capacity check.");
            return;
        }
        UUID clusterId = resolveClusterId();
        if (clusterId == null) {
            log.info("No cluster identity found yet; skipping the pre-upgrade license capacity check.");
            return;
        }
        AbstractTbLicenseClient offlineClient = null;
        try {
            offlineClient = buildLicenseClient(secret, clusterId);
            // A plan, not a client, is all the rest of the check needs - which is what lets the online case be
            // answered by a stateless preview instead of by a second client.
            PlanData planData = offlineClient != null
                    ? offlineClient.getPlanData()
                    : previewPlanData(secret, clusterId);
            if (planData == null) {
                log.info("Could not resolve the license plan; skipping the pre-upgrade license capacity check.");
                return;
            }
            if (!SubscriptionInfoMapper.booleanValue(planData, CommunityGrantPlan.COMMUNITY_GRANT_KEY)) {
                return;
            }
            verifyCap(CappedEntity.DEVICE, SubscriptionInfoMapper.longValue(planData, PlanDataConstants.MAX_DEVICES_KEY));
            verifyCap(CappedEntity.ASSET, SubscriptionInfoMapper.longValue(planData, PlanDataConstants.MAX_ASSETS_KEY));
        } catch (EntityCapExceededException e) {
            // Ahead of the broad catch below on purpose: it would otherwise swallow the one outcome this
            // check exists to produce and let the upgrade run.
            throw e;
        } catch (Exception e) {
            // Everything else - a portal that is unreachable, slow, hostile or answering nonsense included -
            // is a check that could not be made, never a refusal.
            log.info("Skipping the pre-upgrade license capacity check: {}", e.getMessage());
        } finally {
            // The offline client starts a background scheduler on init that only stop() shuts down, and it must
            // be stopped on the refusal path too - hence a finally. The preview leaves nothing to stop.
            stopQuietly(offlineClient);
        }
    }

    /**
     * Refuses the upgrade when the instance holds more {@code entity} than {@code limit} allows. {@code limit}
     * is the raw plan value, where 0 is what an absent key reads as and a negative value is the unlimited
     * sentinel; neither is a cap. (The startup gate reads the normalised quota, where unlimited is 0 instead.)
     */
    private void verifyCap(CappedEntity entity, long limit) {
        if (limit <= 0) {
            return;
        }
        String table = entity.getSingular();
        // Bounded on purpose: this runs on the critical path of an upgrade the operator is watching, and only
        // whether the cap is exceeded matters. The limit is a primitive read from the plan, so interpolating
        // it carries no injection risk.
        Long boundedCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM (SELECT 1 FROM " + table + " LIMIT " + (limit + 1) + ") bounded", Long.class);
        if (boundedCount == null) {
            return;
        }
        if (!LicenseCapacity.exceedsCap(boundedCount, limit)) {
            return;
        }
        // Only now is the exact count worth a full scan: the refusal message reports it, and the upgrade is
        // about to abort anyway. Its own try/catch, because the refusal is already decided - a scan that fails
        // must not reach the broad catch above and turn the refusal into a skipped check.
        Long count;
        try {
            count = jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
        } catch (Exception e) {
            log.debug("Failed to read the exact {} count", table, e);
            count = null;
        }
        String message = count != null
                ? LicenseCapacity.capExceededMessage(entity, count, limit)
                : LicenseCapacity.capExceededMessage(entity, limit);
        // Logged on its own so the sentence reaches the operator even when the caller reports the failure
        // differently. The second line stays out of the shared sentence, which is also read by people
        // activating a licence in the UI, to whom an environment variable means nothing.
        log.error(message);
        log.error("To upgrade on a different license, set TB_LICENSE_SECRET to that license key and run the " +
                "upgrade again.");
        throw new EntityCapExceededException(message);
    }

    /**
     * The secret the check runs on: the supplied one wins over the stored one, the inverse of
     * {@code BasicLicenseActivationService.resolveEffectiveSecret} at runtime and deliberately so, because the
     * upgrade is the one moment at which a single node may choose the licence for the whole cluster.
     */
    private String resolveLicenseSecret(String storedSecret) {
        return StringUtils.isNotEmpty(licenseSecret) ? licenseSecret : storedSecret;
    }

    /** The column not existing yet is the expected outcome on a database the conversion has not run on. */
    private String readStoredLicenseSecret() {
        try {
            return jdbcTemplate.queryForObject("SELECT license_secret FROM tb_cluster LIMIT 1", String.class);
        } catch (Exception e) {
            log.debug("Failed to read the stored license secret", e);
            return null;
        }
    }

    /**
     * Publishes the supplied secret to the cluster, reached only once the check has not refused the upgrade -
     * every skip included, since only a positively established over-cap is a verdict. Best-effort: a database
     * without the table or the column has no stored key to replace, and {@code BasicLicenseActivationService}
     * seeds the supplied one into the column on the first boot afterwards.
     */
    private void replaceStoredLicenseSecret(String storedSecret) {
        if (StringUtils.isEmpty(licenseSecret) || licenseSecret.equals(storedSecret)) {
            return;
        }
        try {
            if (jdbcTemplate.update("UPDATE tb_cluster SET license_secret = ?", licenseSecret) > 0) {
                log.info("Stored the license key supplied through TB_LICENSE_SECRET.");
            }
        } catch (Exception e) {
            log.debug("Failed to store the supplied license secret", e);
        }
    }

    private UUID resolveClusterId() {
        try {
            return jdbcTemplate.queryForObject("SELECT cluster_id FROM tb_cluster LIMIT 1", UUID.class);
        } catch (Exception e) {
            log.debug("Failed to read the cluster identity", e);
            return null;
        }
    }

    /**
     * Resolves the plan for {@code secret} from an <b>offline</b> licence, the only way to read a plan that
     * leaves no state behind for anyone to undo. Answers {@code null} for anything else - an online
     * subscription secret, a malformed one, a licence for another cluster - and the caller skips the check.
     * <p>
     * An online client is not the fallback for anything else - {@link #previewPlanData} is. Initialising one
     * deletes {@code instance-license.data} as soon as the secret differs from the one recorded in it, before the
     * portal is contacted at all, and an upgrade must never be what destroys that file. No node is running yet,
     * hence {@code checkInstanceRequired(FALSE)} - which does not suppress {@code OfflineUpdateService}'s
     * scheduler, so the caller has to stop the client in a {@code finally}. Package-visible for the unit test.
     */
    AbstractTbLicenseClient buildLicenseClient(String secret, UUID clusterId) throws Exception {
        long releaseDate = LicenseReleaseDate.resolveReleaseDate();
        AbstractTbLicenseClient offlineClient = null;
        try {
            offlineClient = OfflineTbLicenseClient.builder()
                    .listener(this::logLicenseError)
                    .releaseDate(releaseDate)
                    .encodedLicenseData(secret)
                    .tbLicenseCtx(new PreflightLicenseCtx(clusterId))
                    .checkInstanceRequired(Boolean.FALSE)
                    .build();
            offlineClient.init();
            return offlineClient;
        } catch (Exception e) {
            stopQuietly(offlineClient);
            log.debug("The provided secret is not an offline license; the pre-upgrade capacity check is skipped", e);
            return null;
        }
    }

    /**
     * The plan the portal says {@code secret} carries, for a secret that is not an offline licence. Asked
     * <b>describe-only</b>, so the licence is described rather than a free instance slot looked for: this
     * cluster normally holds the only slot its own licence has, and the slot-seeking preview would answer
     * ACTIVE_INSTANCES_CAPACITY_EXCEEDED to it. Activates nothing, and touches no instance data file - there is
     * no client here to own one.
     * <p>
     * The endpoint is left null because PE configures none: the client resolves the {@code tb.license.server}
     * system property and then the public portal, exactly as every other licence call in the product does.
     * <p>
     * The client's own connect/read timeouts are inactivity timeouts - every byte received resets them - so
     * they bound silence between bytes, not the call as a whole. A connection that stays open but never
     * answers - a half-open hop through a firewall, a load balancer that accepts and never responds - would
     * pass through them unbounded, and this check runs ahead of the migration with nothing else watching the
     * clock. The call is therefore run on a daemon thread under its own wall-clock budget
     * ({@link #previewBudgetMillis}), so a socket read that never returns cannot keep the JVM alive and
     * cannot keep the upgrade waiting past it; a budget that expires is treated exactly like a portal that
     * cannot be reached. Package-visible for the unit test.
     */
    PlanData previewPlanData(String secret, UUID clusterId) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "license-capacity-preflight-preview");
            thread.setDaemon(true);
            return thread;
        });
        Future<SubscriptionPreviewResponse> previewFuture = executor.submit(
                () -> requestSubscriptionPreview(secret, clusterId));
        try {
            SubscriptionPreviewResponse preview = previewFuture.get(previewBudgetMillis, TimeUnit.MILLISECONDS);
            return preview != null ? preview.getPlanData() : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            previewFuture.cancel(true);
            executor.shutdownNow();
        }
    }

    /**
     * The actual portal call, pulled out of {@link #previewPlanData} so the wall-clock budget wrapping it can
     * be exercised in a unit test without a real network round trip. Package-visible for the unit test.
     */
    SubscriptionPreviewResponse requestSubscriptionPreview(String secret, UUID clusterId) throws Exception {
        return TbLicenseClient.previewSubscription(null, secret, LicenseReleaseDate.resolveReleaseDate(), clusterId, true);
    }

    /**
     * The licence client reports problems here instead of throwing them. Only logged: a licence that does not
     * cover this software version reports through this path yet still yields readable plan data and still
     * boots, so skipping it here would let an instance upgrade cleanly and then be refused at startup.
     */
    private void logLicenseError(LicenseException licenseException) {
        log.debug("License error during the pre-upgrade check: {}", licenseException.getMessage());
    }

    private void stopQuietly(AbstractTbLicenseClient client) {
        if (client == null) {
            return;
        }
        try {
            client.stop();
        } catch (Exception e) {
            log.debug("Failed to stop the pre-upgrade license client", e);
        }
    }

    /**
     * The minimum a licence client needs during the upgrade: the cluster identity, and nothing else. The
     * instance registry is never consulted, because the client is built with
     * {@code checkInstanceRequired(FALSE)} - so the registry methods throw rather than pretending to answer.
     */
    private record PreflightLicenseCtx(UUID id) implements TbLicenseCtx {

        @Override
        public InstanceRegistry save(InstanceRegistry instanceRegistry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public InstanceRegistry findByServiceId(String serviceId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<InstanceRegistry> findAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteByServiceId(String serviceId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getServiceId() {
            return "tb-upgrade-preflight";
        }

        @Override
        public UUID getClusterId() {
            return id;
        }

        @Override
        public boolean isServiceAvailable(String serviceId) {
            return false;
        }
    }
}
