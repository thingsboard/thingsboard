// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.license;

import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.dao.subscription.TbClusterStore;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Owns the recurring non-production confirmation: when it is due, and the record that it was made.
 * <p>
 * The record is an audit-log entry rather than a table of its own - a named user asserting something at a point
 * in time is what the audit log exists to capture. The anchor timestamp is kept in tb_cluster because the whole
 * cluster shares one confirmation. A plain {@link Service}, not restricted to a node type, because the
 * confirmation state has to be readable wherever a request can arrive.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NonProductionConfirmationService {

    public static final long CONFIRMATION_PERIOD_MS = TimeUnit.DAYS.toMillis(30);

    private final SubscriptionService subscriptionService;
    private final TbClusterStore tbClusterStore;
    private final AuditLogService auditLogService;

    /**
     * Cached so that {@link #isLapsed()}, which runs on every API request through getState, does not query the
     * database. Once lapsed the value is re-read on each check, so a confirmation made on a peer node unlocks
     * this one on its next request.
     */
    private volatile long confirmedTs;

    /**
     * So an unreadable store is reported once with its stack trace and then stays quiet: the read that sets this
     * runs on every API request.
     */
    private volatile boolean storeReadFailureReported;

    /**
     * Guarded because it runs at context startup: {@code tb_cluster} and this column exist only from the schema
     * upgrade scripts, so a node started against a not-yet-upgraded database would otherwise fail the whole
     * context instead of coming up locked but diagnosable.
     * <p>
     * Safe to swallow because this is pure cache warm-up: {@link #isLapsed()} already re-reads the store
     * whenever it observes {@code confirmedTs == 0}.
     */
    @PostConstruct
    public void init() {
        try {
            confirmedTs = tbClusterStore.getNonProductionConfirmedTs().orElse(0L);
        } catch (Exception e) {
            log.error("Failed to resolve the non-production confirmation timestamp from the database. " +
                    "It will be re-read on the next check instead.", e);
        }
    }

    /**
     * Seeding is lazy rather than done in {@link #init()}, because a licence can be applied at runtime: a node
     * that boots unactivated and is then given a DEV key would otherwise still hold confirmedTs == 0 and lapse
     * on the very next request instead of thirty days later.
     */
    public boolean isLapsed() {
        if (!appliesToThisDeployment()) {
            return false;
        }
        long confirmed = this.confirmedTs;
        if (confirmed == 0L) {
            // Re-read before seeding: a peer node may already have seeded this cluster.
            Long stored = readConfirmedTs();
            if (stored == null) {
                return false;
            }
            confirmed = stored;
            if (confirmed == 0L) {
                seed();
                return false;
            }
            this.confirmedTs = confirmed;
        }
        if (System.currentTimeMillis() - confirmed <= CONFIRMATION_PERIOD_MS) {
            return false;
        }
        // Past the cached deadline: re-read so a confirmation recorded by a peer node unlocks this one on its
        // next check instead of waiting for a restart.
        Long stored = readConfirmedTs();
        if (stored != null && stored != 0L) {
            // Only cache a real timestamp: caching a momentarily empty or unreadable one would send the
            // seeding branch above down the "never confirmed" path and reseed a lapsed deployment.
            this.confirmedTs = stored;
            confirmed = stored;
        }
        return System.currentTimeMillis() - confirmed > CONFIRMATION_PERIOD_MS;
    }

    /**
     * The stored confirmation timestamp, {@code 0L} when the column holds no value and {@code null} when the
     * store could not be read at all - only the first is a statement about this deployment.
     * <p>
     * Guarded for the same reason {@link #init()} is: this runs on every API request, so against the database
     * {@code init()} defends against an unguarded read would make every request answer 500. An unreadable store
     * therefore leaves the instance unlocked - a database problem must not be what locks a deployment out.
     */
    private Long readConfirmedTs() {
        try {
            return tbClusterStore.getNonProductionConfirmedTs().orElse(0L);
        } catch (Exception e) {
            if (storeReadFailureReported) {
                log.debug("Failed to read the non-production confirmation timestamp from the database", e);
            } else {
                storeReadFailureReported = true;
                log.warn("Failed to read the non-production confirmation timestamp from the database. " +
                        "The deployment is treated as not lapsed until the read succeeds.", e);
            }
            return null;
        }
    }

    /**
     * @param user the system administrator making the declaration. Required: a caller that loses track of the
     *             principal must fail loudly rather than record a confirmation nobody made. The automatic
     *             seeding at first activation goes through {@link #seed()} instead.
     * @throws IllegalStateException if the confirmation was not actually persisted (the shared tb_cluster row
     *             is missing); unlike {@link #seed()}, a sysadmin who asked for this must be told it failed.
     * @throws IllegalStateException if this deployment has nothing to confirm - seeding the timestamp anyway
     *             would make a development key applied a period later look as though it had lapsed long ago.
     */
    public void confirm(SecurityUser user) {
        Objects.requireNonNull(user, "user");
        if (!appliesToThisDeployment()) {
            throw new IllegalStateException("This deployment does not run on a development licence, so there is nothing to confirm");
        }
        if (!write()) {
            throw new IllegalStateException("Failed to persist the non-production confirmation: table tb_cluster is empty");
        }
        // Best-effort: the confirmation is already persisted and the instance already unlocked, so failing
        // here would report a failure that did not happen.
        try {
            ListenableFuture<Void> recorded = auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(),
                    user.getId(), user.getName(), user.getId(), user, ActionType.NON_PRODUCTION_CONFIRMED, null);
            if (recorded == null) {
                // Audit logging is off: the tb_cluster timestamp carries no attribution.
                log.info("Non-production confirmation by {} was persisted, but audit logging is disabled on this " +
                        "deployment, so the declaration itself is not recorded anywhere attributable", user.getId());
            }
        } catch (Exception e) {
            log.warn("Non-production confirmation by {} was persisted, but recording the audit log entry failed", user.getId(), e);
        }
    }

    /**
     * Records the confirmation timestamp with nobody attributed, because nobody declared it: this runs only the
     * first time a development licence is observed on a cluster that was never seeded before. Unlike
     * {@link #confirm(SecurityUser)}, a failed write is left to retry on the next check - nobody is waiting on
     * this one, and the request it runs from must not fail because of it.
     */
    private void seed() {
        if (write()) {
            log.debug("Seeded the non-production confirmation timestamp at {}", confirmedTs);
        }
    }

    /**
     * @return whether the timestamp was actually persisted, so {@link #confirm(SecurityUser)} and
     * {@link #seed()} can each decide what a failed write means for their caller.
     */
    private boolean write() {
        long now = System.currentTimeMillis();
        boolean persisted = tbClusterStore.saveNonProductionConfirmedTs(now);
        if (persisted) {
            this.confirmedTs = now;
        }
        return persisted;
    }

    /**
     * The DEV-key path only. Keyless mode also reports development mode, but it is anonymous - nobody to record
     * a declaration against - and is capped by cumulative uptime instead.
     * <p>
     * Activation is part of the question, not an implied precondition: a locked node has no licence client, so
     * isDevelopment() fails open to true while isNonProductionMode() is false, and both remaining conditions
     * hold on a deployment that may never have been on a development key at all.
     * <p>
     * Tested last of the three, because it is the only one that is not free: on a node that needs a stored
     * secret {@code isLicenseActivated()} attempts an activation. A production node short-circuits on the
     * first condition and a keyless one on the second. A node on a development key - the case that actually
     * runs this on every request - does reach it, but there {@code needsStoredSecret()} is false and the call
     * degenerates to a field read. Only a locked node reaches the activation attempt, where the five-second
     * reactivation throttle and the single-flight lock already bound the cost.
     */
    private boolean appliesToThisDeployment() {
        return subscriptionService.isDevelopment(TenantId.SYS_TENANT_ID)
                && !subscriptionService.isNonProductionMode()
                && subscriptionService.isLicenseActivated();
    }
}
