// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.extension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.dao.relation.RelationWriteLock;

import java.util.function.Supplier;

/**
 * Serializes the quota-consuming extension writes of ONE tenant.
 *
 * <p>The extension quotas (count, aggregate CPU, aggregate memory) are aggregates over the tenant's rows,
 * so enforcing them is a read-modify-write that no single statement can make atomic: between the check and
 * the write a concurrent request — including one handled by another core node — slips in, both pass, and the
 * tenant ends up over quota. Since the quotas are the cost / DoS guard for tenant-supplied containers running
 * on shared infrastructure, that overrun is the thing worth preventing.
 *
 * <p><b>Why an advisory lock.</b> No cheaper mechanism covers it: a unique/exclusion constraint cannot express
 * an aggregate sum against a per-tenant maximum that lives in the tenant profile; {@code SELECT ... FOR UPDATE}
 * over the tenant's rows locks what exists but cannot stop a concurrent {@code INSERT}, which is exactly the
 * count race; and {@code SERIALIZABLE} would need a retry loop and only holds while every quota-consuming path
 * stays serializable — a guarantee a future write path could weaken silently.
 *
 * <p><b>Why try-lock.</b> {@code pg_try_advisory_xact_lock} returns immediately instead of queueing. Waiting
 * would let one tenant's burst pile up requests that each occupy an HTTP thread and a pooled connection —
 * the very abuse the quota exists to bound — so contention is reported as {@code TOO_MANY_REQUESTS} (HTTP 429)
 * and the caller retries. It also makes deadlock impossible by construction rather than by lock-ordering
 * discipline: a lock that never waits cannot take part in a wait cycle.
 *
 * <p><b>Lifetime.</b> The lock is issued through the persistence context, so it runs on the transaction's own
 * connection and the following DML shares it. Being {@code xact}-scoped it is released on commit AND on
 * rollback, and a dying backend aborts the transaction, so it cannot leak into a pooled connection the way a
 * session-scoped {@code pg_advisory_lock} can.
 */
@Slf4j
@Component
public class ExtensionQuotaGuard {

    static final String TRY_LOCK_SQL = "SELECT pg_try_advisory_xact_lock(?)";

    private final TransactionTemplate txTemplate;

    @PersistenceContext
    EntityManager entityManager;

    public ExtensionQuotaGuard(PlatformTransactionManager transactionManager) {
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Runs {@code body} with this tenant's quota writes serialized. The body must perform BOTH the quota check
     * and the write it authorizes — splitting them across the boundary reopens the race the guard closes.
     *
     * @throws ThingsboardRuntimeException with {@link ThingsboardErrorCode#TOO_MANY_REQUESTS} when another
     *         quota-consuming request for the same tenant is already in flight.
     */
    public <T> T run(TenantId tenantId, Supplier<T> body) {
        return txTemplate.execute(status -> {
            if (!tryLockTenant(tenantId)) {
                log.debug("[{}] Rejecting extension request: another quota-consuming request is in flight", tenantId);
                throw new ThingsboardRuntimeException(
                        "Another extension request for this tenant is in progress; please retry.",
                        ThingsboardErrorCode.TOO_MANY_REQUESTS);
            }
            return body.get();
        });
    }

    private boolean tryLockTenant(TenantId tenantId) {
        Query query = entityManager.createNativeQuery(TRY_LOCK_SQL);
        // Shares the global single-bigint advisory key space; a ~1/2^64 collision only over-serializes.
        query.setParameter(1, RelationWriteLock.advisoryKey(tenantId));
        return Boolean.TRUE.equals(query.getSingleResult());
    }

}
