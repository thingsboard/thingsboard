// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.relation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.dao.sql.citus.CitusSettings;

import java.util.Collection;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Serializes writes to the {@code relation} reference table under Citus by acquiring PostgreSQL
 * transaction-scoped advisory locks on the <em>current transaction's connection</em>.
 *
 * <p><b>Why this exists.</b> When {@code relation} is a Citus reference table, every write is replicated
 * to all nodes inside one distributed transaction. Two concurrent transactions that touch overlapping
 * relation rows (e.g. the same {@code from}/{@code to} entity) can acquire the per-shard placement locks
 * in different orders across workers and <b>distributed-deadlock</b>, or — worse, depending on conflict
 * handling — let the replicas <b>silently diverge</b>. Taking a single deterministic advisory lock per
 * involved entity, in a stable sorted order, forces all writers through the same ordering and eliminates
 * both failure modes. On plain PostgreSQL ({@code database.citus.enabled=false}) the divergence/distributed-deadlock
 * risk does not apply, so the advisory locking itself is a no-op there; the active-transaction assertion, however,
 * still fires in both modes (see below) so a relation write path missing {@code @Transactional} fails fast everywhere
 * rather than only under Citus at runtime.
 *
 * <p><b>Connection binding.</b> The lock is issued via the JPA {@link EntityManager} native query so it runs
 * on the persistence-context connection — the lock and the subsequent DML share one connection and one Citus
 * distributed transaction. A {@code pg_advisory_xact_lock} is released automatically at commit/rollback, so it
 * is pooling-safe and cannot leak across borrowed connections.
 *
 * <p><b>Covering locks (callers' contract).</b> {@link #withCoveringLock} locks a single anchor entity and
 * suppresses the per-endpoint {@link #lockEntities} of inner relation writes, so a group of relation mutations
 * keyed off one logical entity serialize behind one lock instead of per-write. This is correct only when every
 * writer that can touch those rows takes the same covering lock and distinct anchors own disjoint row sets. The
 * canonical user is rule-chain graph editing (anchor = rule chain id): the {@code RULE_CHAIN->RULE_NODE} "Contains"
 * relations and node-to-node connections are mutated only through RC-scoped paths — {@code saveRuleChainMetaData},
 * rule-chain deletion ({@code checkRuleNodesAndDelete} / {@code deleteRuleNodes}), and version-control restore (which
 * funnels through {@code saveRuleChainMetaData}) — whereas generic relation writers ({@code TbAbstractRelationActionNode},
 * {@code CleanUpService}) only ever touch disjoint rows (different relation-type groups / non-rule endpoints; rule
 * nodes are never independently deleted). Because all inner relation-service calls on those paths run synchronously on
 * the caller's thread/transaction, the covering marker actually suppresses their endpoint locking.
 *
 * <p>Known exception to the disjoint-ownership argument (accepted residual): the cross-chain USES relation
 * ({@code thisChain -> targetChain}, {@code COMMON} group) is written and deleted inside
 * {@code saveRuleChainMetaData}'s covering block, yet its {@code targetChain} endpoint lies outside the anchor's
 * owned row set — the row is serialized only by {@code advisoryKey(thisChain)}. It can therefore race the target
 * chain's post-commit relation cleanup ({@code CleanUpService -> deleteEntityRelations(targetChain)}, which locks
 * only {@code advisoryKey(targetChain)}) under disjoint advisory keys. Accepted because the window requires a
 * metadata save racing the referenced chain's deletion, and the equivalent stale-USES-row race pre-exists on plain
 * PostgreSQL — see the comment at the USES delete site in {@code BaseRuleChainService.doSaveRuleChainMetaData}.
 *
 * <p><b>Lock-acquisition order.</b> A covering-lock caller that also writes a row of a <em>replicated</em> table
 * (e.g. the {@code rule_chain} reference table) must acquire the covering advisory lock FIRST and write that row
 * INSIDE the block. Otherwise a save (covering lock, then row write) and a delete (row write, then covering lock) of
 * the same anchor cross-lock {@code [advisory <-> replicated reference-table row]} in opposite orders and
 * distributed-deadlock (SQLState {@code 40P01}) under Citus. No-op ordering on plain PostgreSQL.
 *
 * <p><b>Accepted residual (anchor-vs-anchor).</b> The covering/endpoint locks serialize anchor-vs-endpoint writers
 * (a per-entity write always locks that entity; a covering caller locks its anchor), but two writers that each hold
 * only their <em>own</em> anchor lock are NOT serialized against each other. Where two such transactions delete an
 * overlapping set of rows, their per-row lock-acquisition order must match to avoid a {@code 40P01} row-lock deadlock.
 * {@code BaseRelationService.removeRelations} closes this window by deleting its collected rows in a deterministic
 * composite-key order. The bulk inbound/outbound {@code DELETE} statements behind {@code deleteEntityRelations}, by
 * contrast, retain a residual anchor-vs-anchor window: the delete order inside a single set-based statement is the
 * table (scan) order, not caller-controllable, so two such statements over an overlapping row set could still lock
 * shared rows in opposite orders. This residual is accepted — those paths delete rows anchored on distinct entities
 * that overlap only transiently, and a {@code 40P01} there surfaces as a retryable transaction abort, not divergence.
 */
@Component
@RequiredArgsConstructor
public class RelationWriteLock {

    // Deadlock-freedom relies on the locks being taken in ascending key order. lockEntities pre-sorts the key
    // array, and this statement depends on Postgres emitting unnest() rows in array (storage) order so the per-row
    // pg_advisory_xact_lock(k) is evaluated in that sorted order. That row order is real Postgres behavior but not
    // SQL-standard; if it ever needed to be airtight, issue the locks as separate statements in a sorted loop
    // (N round-trips) to drop the assumption entirely.
    static final String ADVISORY_LOCK_SQL = "SELECT pg_advisory_xact_lock(k) FROM unnest(?::bigint[]) AS k";

    /**
     * Advisory-only ordering/perf guard. While set, {@link #lockEntities} short-circuits because the caller
     * has already taken a broader covering lock via {@link #withCoveringLock}. Cleared in {@code finally} so it
     * can never leak to the next unit of work this (pooled) thread handles.
     */
    private final ThreadLocal<Boolean> coveringLockHeld = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final CitusSettings citusSettings;

    @PersistenceContext
    EntityManager entityManager;

    /**
     * Acquires advisory locks for the given entities (e.g. the {@code from}/{@code to} of a relation write) in a
     * single batched statement, ordered by key, so concurrent relation writers serialize deterministically.
     *
     * <p>No-ops the locking when Citus is disabled, when a covering lock is already held on this thread, or when the
     * input is empty/null. Keys are derived, deduped and sorted before locking, so a self-relation ({@code from == to})
     * collapses to a single key. Asserts an active transaction in BOTH modes (before the Citus gate) for any non-empty
     * input, so a write path missing {@code @Transactional} fails fast on plain PostgreSQL too, not only under Citus.
     */
    public void lockEntities(Collection<EntityId> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        assertTransactionActive();
        if (!citusSettings.isEnabled() || Boolean.TRUE.equals(coveringLockHeld.get())) {
            return;
        }
        long[] keys = entities.stream()
                .filter(Objects::nonNull)
                .mapToLong(RelationWriteLock::advisoryKey)
                .distinct()
                .sorted()
                .toArray();
        if (keys.length == 0) {
            return;
        }
        acquire(keys);
    }

    /**
     * Takes a single covering advisory lock for {@code entityId}, marks the thread as covered (so nested
     * {@link #lockEntities} calls within {@code body} no-op), runs {@code body}, and clears the marker in
     * {@code finally}. No-ops the locking on plain PostgreSQL, but always runs {@code body}.
     *
     * <p>Intended for paths (e.g. rule-chain edits) that perform several relation writes under one logical entity
     * and want them all serialized behind one lock rather than per-write. Delegates to the value-returning
     * {@link #withCoveringLock(EntityId, Supplier)} to keep the locking/marker logic in one place.
     */
    public void withCoveringLock(EntityId entityId, Runnable body) {
        withCoveringLock(entityId, () -> {
            body.run();
            return null;
        });
    }

    /**
     * Value-returning variant of {@link #withCoveringLock(EntityId, Runnable)} with identical semantics: takes a single
     * covering advisory lock for {@code entityId} (Citus only), marks the thread as covered so nested
     * {@link #lockEntities} calls within {@code body} no-op, runs {@code body}, returns its value, and clears the marker
     * in {@code finally}. Used by RC-scoped callers (e.g. {@code saveRuleChainMetaData}) that must return a result from
     * within the covering block.
     *
     * <p>Asserts an active transaction in BOTH modes (before the Citus gate): the inner relation writes of the covered
     * block assume a transactional boundary, so a caller missing {@code @Transactional} fails fast on plain PostgreSQL
     * too, not only under Citus.
     */
    public <T> T withCoveringLock(EntityId entityId, Supplier<T> body) {
        assertTransactionActive();
        if (citusSettings.isEnabled()) {
            acquire(new long[]{advisoryKey(entityId)});
        }
        // Save and restore the prior marker (rather than unconditionally clearing to FALSE) so a nested covering
        // lock restores the OUTER covered state on exit instead of prematurely re-enabling per-endpoint locking --
        // e.g. BaseRuleChainService's checkRuleNodesAndDelete -> deleteRuleNodes nests covering locks.
        boolean previous = Boolean.TRUE.equals(coveringLockHeld.get());
        coveringLockHeld.set(Boolean.TRUE);
        try {
            return body.get();
        } finally {
            coveringLockHeld.set(previous);
        }
    }

    /**
     * Issues exactly one {@code pg_advisory_xact_lock} statement over the sorted keys on the transaction's connection.
     * Callers must have asserted an active transaction via {@link #assertTransactionActive()} first.
     */
    private void acquire(long[] sortedKeys) {
        Query query = entityManager.createNativeQuery(ADVISORY_LOCK_SQL);
        query.setParameter(1, toArrayLiteral(sortedKeys));
        query.getResultList();
    }

    /**
     * Asserts an active transaction. An advisory <em>xact</em> lock taken outside a transaction would be released
     * immediately and silently provide no protection, and the subsequent DML would not share the lock's connection.
     * Enforced in BOTH Citus and plain-PostgreSQL modes so a relation write path missing {@code @Transactional} trips
     * loudly here everywhere, rather than passing the entire plain-PG suite and failing only under Citus at runtime.
     */
    private static void assertTransactionActive() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("RelationWriteLock requires an active transaction to hold xact advisory locks; " +
                    "the caller must run inside @Transactional");
        }
    }

    /**
     * Renders a Postgres {@code bigint[]} array literal (e.g. {@code "{12,34,56}"}) from primitive longs. Injection
     * safe by construction — the elements are primitive {@code long}s — and bound as a single parameter typed by the
     * {@code ?::bigint[]} cast in the SQL.
     */
    private static String toArrayLiteral(long[] keys) {
        StringJoiner joiner = new StringJoiner(",", "{", "}");
        for (long key : keys) {
            joiner.add(Long.toString(key));
        }
        return joiner.toString();
    }

    /**
     * Derives a stable 64-bit advisory-lock key from an entity's UUID. ThingsBoard ids are time-structured UUID v1s,
     * so a raw {@code msb ^ lsb} would cluster (and collapse symmetric pairs); instead both halves are run through the
     * SplitMix64 / MurmurHash3 {@code fmix64} finalizer and combined, then finalized once more, giving a well-spread
     * single-{@code bigint} key. Shares the global single-bigint advisory key space with other callers; a ~1/2^64
     * collision would only over-serialize, never be incorrect. Pure function so it is directly unit-testable.
     */
    public static long advisoryKey(EntityId entityId) {
        UUID uuid = entityId.getId();
        long mixed = fmix64(uuid.getMostSignificantBits()) ^ Long.rotateLeft(fmix64(uuid.getLeastSignificantBits()), 32);
        return fmix64(mixed);
    }

    private static long fmix64(long value) {
        value ^= (value >>> 33);
        value *= 0xff51afd7ed558ccdL;
        value ^= (value >>> 33);
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= (value >>> 33);
        return value;
    }

}
