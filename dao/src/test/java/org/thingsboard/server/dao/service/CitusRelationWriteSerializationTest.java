// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.secret.SecretConfigurationService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Decisive integration test for the Citus relation-write serialization (the advisory locks added in
 * {@link org.thingsboard.server.dao.relation.RelationWriteLock} and wired into {@code BaseRelationService} /
 * {@code BaseRuleChainService}). It runs the REAL ThingsBoard service stack against a REAL Citus cluster
 * (coordinator + worker) booted by {@link org.thingsboard.server.dao.sql.citus.CitusTestCluster} via
 * {@link CitusDaoSqlTest}, with {@code relation} a Citus reference table replicated to every node.
 *
 * <p><b>What it proves.</b> Under {@code citus.all_modifications_commutative=on}, two concurrent transactions
 * that touch overlapping {@code relation} rows could otherwise (1) distributed-deadlock — surfacing to the
 * caller as PostgreSQL SQLState {@code 40P01} — or (2) silently diverge, with the row landing on some replica
 * placements but not others so reads from different workers disagree. The advisory locks serialize colliding
 * writers to eliminate both. Each scenario hammers a genuine race (insert-vs-delete of the SAME relation row;
 * save-vs-delete of the SAME rule chain graph) for many iterations and asserts:
 * <ul>
 *     <li>no thread ever sees a {@code 40P01} distributed deadlock (nor an unhandled {@code 40001}
 *         serialization failure), and</li>
 *     <li>after each race the row's existence is IDENTICAL across ALL replica placements of the {@code relation}
 *         reference table — checked per-placement via {@code run_command_on_placements}, not via a single
 *         coordinator-local read (which by construction can never reveal a replica divergence).</li>
 * </ul>
 *
 * <p>This test extends {@link AbstractServiceTest} (not {@code RelationServiceTest}) so it inherits the Spring
 * context and tenant bootstrap without inheriting any {@code @Test} methods: under {@code @CitusDaoSqlTest} those
 * inherited cases would otherwise re-run against the heavy real cluster purely as a side effect. It autowires only
 * the services it needs; the new {@code @Test}s here are the point.
 */
@CitusDaoSqlTest
public class CitusRelationWriteSerializationTest extends AbstractServiceTest {

    /** Number of insert-vs-delete (and save-vs-delete) races per scenario; high enough to hit the window. */
    private static final int ITERATIONS = 120;
    /** Postgres SQLState for a (distributed) deadlock. The serialization must make this unreachable for callers. */
    private static final String DEADLOCK_SQLSTATE = "40P01";
    /** Postgres SQLState for a serialization failure; an unhandled one would also mean the guard leaked. */
    private static final String SERIALIZATION_FAILURE_SQLSTATE = "40001";

    @Autowired
    private RelationService relationService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * The DAO test context has no {@code SecretConfigurationService} implementation (its impl lives in the
     * application module); the rule-chain wiring tolerates a mock here, mirroring {@code RuleChainServiceTest}.
     */
    @MockitoBean
    SecretConfigurationService secretConfigurationService;

    /**
     * Scenario A — concurrent {@code saveRelation(A->B)} vs {@code deleteEntityRelations(A)} on the SAME relation
     * row, repeated under a {@link CyclicBarrier} so both transactions are in flight simultaneously. After each
     * race: no caller sees a {@code 40P01}, and every replica placement of {@code relation} agrees on whether the
     * A->B edge exists (all 0 or all 1) — i.e. no silent divergence.
     */
    @Test
    public void edgeWriteVsEntityScopedDeleteNeverDeadlocksAndStaysReplicaConsistent() {
        AssetId a = new AssetId(Uuids.timeBased());
        AssetId b = new AssetId(Uuids.timeBased());
        EntityRelation edge = new EntityRelation(a, b, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < ITERATIONS; i++) {
                // Start each iteration from a clean baseline so it genuinely races an INSERT against a DELETE of
                // the very same row (rather than degenerating into delete-of-absent / insert-of-present).
                relationService.deleteRelation(SYSTEM_TENANT_ID, edge);

                List<Throwable> failures = runConcurrently(pool,
                        () -> relationService.saveRelation(SYSTEM_TENANT_ID, edge),
                        () -> relationService.deleteEntityRelations(SYSTEM_TENANT_ID, a));

                assertNoDistributedDeadlock(failures, "edge save vs entity-scoped delete, iteration " + i);
                assertEdgePlacementsAgree(a.getId(), b.getId(),
                        "after edge save vs entity-scoped delete, iteration " + i);
            }
        } finally {
            relationService.deleteRelation(SYSTEM_TENANT_ID, edge);
            shutdown(pool);
        }
    }

    /**
     * Scenario B — concurrent {@code saveRuleChainMetaData} (re-saving the graph, the Supplier covering-lock path)
     * vs {@code deleteRuleChainById} (the Runnable covering-lock path) on the SAME rule chain, repeated under a
     * barrier. This is inherently racy (save vs delete of the same chain): the assertion is about the GUARANTEES —
     * no {@code 40P01}, and the chain's rule-node relations stay replica-consistent across all placements (every
     * placement reports the same count for the chain's outbound graph relations) — not about which op "wins".
     */
    @Test
    public void ruleChainSaveVsDeleteNeverDeadlocksAndStaysReplicaConsistent() {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < ITERATIONS; i++) {
                RuleChainId ruleChainId = createRuleChainWithGraph("Citus serialization RC " + i);

                RuleChainMetaData reSave = ruleChainService.loadRuleChainMetaData(tenantId, ruleChainId);

                List<Throwable> failures = runConcurrently(pool,
                        () -> ruleChainService.saveRuleChainMetaData(tenantId, reSave, Function.identity()),
                        () -> ruleChainService.deleteRuleChainById(tenantId, ruleChainId));

                assertNoDistributedDeadlock(failures, "rule-chain save vs delete, iteration " + i);
                // The rule chain id anchors the graph relations (RULE_CHAIN -CONTAINS-> first RULE_NODE, etc.);
                // assert every placement agrees on how many outbound relations that anchor currently has.
                assertOutboundRelationCountAgrees(ruleChainId.getId(),
                        "after rule-chain save vs delete, iteration " + i);

                // Best-effort cleanup if the save won the race and the chain still exists.
                if (ruleChainService.findRuleChainById(tenantId, ruleChainId) != null) {
                    ruleChainService.deleteRuleChainById(tenantId, ruleChainId);
                }
            }
        } finally {
            shutdown(pool);
        }
    }

    /**
     * Scenario C — deterministic proof that {@code citus.all_modifications_commutative=on} (set by
     * {@code JpaDaoConfig} on the Hikari connection-init SQL) is actually in effect, asserted on LOCK STATE rather
     * than on wall-clock timing.
     *
     * <p><b>Mechanism.</b> A {@code relation} reference-table DELETE is non-commutative. By default Citus guards the
     * fully-replicated copies by taking a cluster-wide table-level {@code ExclusiveLock} on {@code relation} for such
     * a write, so a second writer of an UNRELATED row blocks behind the first transaction until it commits. The flag
     * downgrades that to {@code RowExclusiveLock}, so different-row writes never conflict. The flag's entire effect is
     * this lock-mode downgrade — so we observe it directly in {@code pg_locks}:
     * <ol>
     *     <li>a holder transaction DELETEs one relation row and stays open; we assert its lock on {@code relation} is
     *         {@code RowExclusiveLock} and that it holds NO {@code ExclusiveLock}/{@code AccessExclusiveLock} on that
     *         table (which is exactly what the flag suppresses), then</li>
     *     <li>a second transaction DELETEs a DIFFERENT relation row; we assert it never acquires an UNGRANTED
     *         (awaited) lock on {@code relation} and runs to completion while the holder is still open — i.e. it is
     *         not serializing behind the holder.</li>
     * </ol>
     * This replaces an earlier wall-clock probe ("the second DELETE returns within N seconds") that could flake on a
     * loaded CI agent: here a slow-but-unblocked probe cannot produce a false failure, because the verdict is the
     * granted/awaited state of a lock, not elapsed time.
     */
    @Test
    public void concurrentDeleteOfUnrelatedRelationsTakesRowLevelLockAndDoesNotBlock() throws Exception {
        AssetId heldParent = new AssetId(Uuids.timeBased());
        AssetId heldChild = new AssetId(Uuids.timeBased());
        AssetId probeParent = new AssetId(Uuids.timeBased());
        AssetId probeChild = new AssetId(Uuids.timeBased());

        EntityRelation held = new EntityRelation(heldParent, heldChild, EntityRelation.CONTAINS_TYPE);
        EntityRelation probe = new EntityRelation(probeParent, probeChild, EntityRelation.CONTAINS_TYPE);
        relationService.saveRelation(SYSTEM_TENANT_ID, held);
        relationService.saveRelation(SYSTEM_TENANT_ID, probe);

        CountDownLatch holderReady = new CountDownLatch(1);
        CountDownLatch releaseHolder = new CountDownLatch(1);
        long[] holderPid = new long[1];
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // Holder: DELETE the "held" row and keep the transaction (and its relation lock) open until released.
            Future<?> holder = pool.submit(() -> transactionTemplate.execute(status -> {
                relationService.deleteRelation(SYSTEM_TENANT_ID, held);
                // pg_backend_pid() runs on the transaction-bound connection (same datasource), so it identifies the
                // holder's backend whose locks we then inspect from a separate connection below.
                holderPid[0] = jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Long.class);
                holderReady.countDown();
                try {
                    releaseHolder.await(60, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }));

            Assert.assertTrue("Holder transaction did not open in time", holderReady.await(30, TimeUnit.SECONDS));

            // (1) Direct flag signal: the holder's DELETE on the relation reference table must hold RowExclusiveLock
            // and NOT a table-level ExclusiveLock/AccessExclusiveLock. The table-level lock is precisely what
            // all_modifications_commutative=on suppresses; seeing it would mean the flag is not in effect.
            List<String> holderRelationModes = relationTableLockModes(holderPid[0]);
            assertThat(holderRelationModes)
                    .as("Holder DELETE must hold RowExclusiveLock on the relation reference table (got %s)",
                            holderRelationModes)
                    .contains("RowExclusiveLock");
            assertThat(holderRelationModes)
                    .as("Holder DELETE must NOT hold a table-level Exclusive lock on relation — "
                        + "citus.all_modifications_commutative=on is not in effect (got %s)", holderRelationModes)
                    .doesNotContain("ExclusiveLock", "AccessExclusiveLock");

            // (2) Probe: DELETE a DIFFERENT relation row in its own transaction. Capture its backend pid so we can
            // assert from this thread that it never waits on a relation lock, then confirm it completes.
            long[] probePid = new long[1];
            CountDownLatch probePidReady = new CountDownLatch(1);
            Future<?> probeFuture = pool.submit(() -> transactionTemplate.execute(status -> {
                probePid[0] = jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Long.class);
                probePidReady.countDown();
                relationService.deleteRelation(SYSTEM_TENANT_ID, probe);
                return null;
            }));

            Assert.assertTrue("Probe transaction did not start in time", probePidReady.await(30, TimeUnit.SECONDS));

            // While the probe runs (and the holder is still open), it must never be parked on an UNGRANTED relation
            // lock. Poll until the probe finishes; if it ever shows a not-granted relation lock, it is blocking.
            while (!probeFuture.isDone()) {
                assertThat(hasUngrantedRelationLock(probePid[0]))
                        .as("Probe DELETE of an unrelated relation is waiting on a relation lock — "
                            + "reference-table writes are serializing (commutative flag not in effect)")
                        .isFalse();
                TimeUnit.MILLISECONDS.sleep(20);
            }
            // Surfaces any failure from the probe transaction itself; must have completed while the holder is open.
            probeFuture.get(30, TimeUnit.SECONDS);

            releaseHolder.countDown();
            holder.get(60, TimeUnit.SECONDS);
        } finally {
            releaseHolder.countDown();
            shutdown(pool);
        }

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, heldParent, heldChild,
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));
        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, probeParent, probeChild,
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));
    }

    /**
     * Returns the lock modes the given backend currently holds (granted) on the local {@code relation} table on the
     * coordinator. Inspected from a separate connection — {@code pg_locks} is instance-global — so it reflects the
     * holder transaction's locks while that transaction is still open.
     */
    private List<String> relationTableLockModes(long pid) {
        return jdbcTemplate.queryForList(
                "SELECT mode FROM pg_locks l JOIN pg_class c ON c.oid = l.relation "
                + "WHERE l.pid = ? AND l.granted AND c.relname = 'relation'",
                String.class, pid);
    }

    /**
     * True if the given backend is currently WAITING on (holds an ungranted) lock on the local {@code relation} table
     * — the deterministic signal that the probe DELETE is serializing behind another writer rather than proceeding.
     */
    private boolean hasUngrantedRelationLock(long pid) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_locks l JOIN pg_class c ON c.oid = l.relation "
                + "WHERE l.pid = ? AND NOT l.granted AND c.relname = 'relation'",
                Integer.class, pid);
        return count != null && count > 0;
    }

    /**
     * Runs the two operations on separate threads, releasing both at once via a {@link CyclicBarrier} so their
     * transactions overlap, and returns every Throwable raised by either op (empty if both completed). Each op runs
     * through the real {@code @Transactional} service method, so it executes in its own transaction/connection —
     * exactly the concurrent-distributed-transaction shape the advisory locks must serialize.
     *
     * <p>Harness failures are NOT tolerated: a barrier timeout/breakage or a hung operation means the race never
     * happened, so they fail the test immediately instead of landing in the returned list (where
     * {@link #assertNoDistributedDeadlock} ignores non-SQL throwables and all iterations would pass vacuously).
     * Only exceptions thrown by {@code op.run()} itself are collected — the race outcome is free by design.
     */
    private List<Throwable> runConcurrently(ExecutorService pool, Runnable op1, Runnable op2) {
        CyclicBarrier barrier = new CyclicBarrier(2);
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>(2);
        for (Runnable op : List.of(op1, op2)) {
            futures.add(pool.submit(() -> {
                try {
                    barrier.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                    // Rethrow so the future.get below trips the test loudly rather than counting a vacuous pass.
                    throw new IllegalStateException("Race harness failed to release both operations", e);
                }
                try {
                    op.run();
                } catch (Throwable t) {
                    failures.add(t);
                }
            }));
        }
        for (Future<?> future : futures) {
            try {
                future.get(60, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                // Nothing operation-level reaches here (op.run() failures are captured above), so any exception is
                // harness-level — barrier breakage or a hung operation — and means the iteration raced nothing.
                throw new AssertionError("Race harness failure — the operations never actually raced", e);
            }
        }
        return failures;
    }

    /**
     * Fails if any captured Throwable's cause chain carries a PostgreSQL distributed-deadlock ({@code 40P01}) or an
     * unhandled serialization-failure ({@code 40001}) SQLState. A residual {@code 40P01} reaching the caller means a
     * real lock-coverage gap, per the design — so we trip loudly rather than tolerate it. Other failures (e.g. a
     * delete racing a not-yet-committed insert) are not deadlocks and are allowed: the race outcome itself is free.
     */
    private void assertNoDistributedDeadlock(List<Throwable> failures, String context) {
        for (Throwable failure : failures) {
            for (Throwable t = failure; t != null; t = t.getCause()) {
                if (t instanceof SQLException sqlException) {
                    String state = sqlException.getSQLState();
                    Assert.assertNotEquals(
                            "Caller saw a Citus distributed deadlock (SQLState 40P01) — relation-write "
                            + "serialization gap: " + context,
                            DEADLOCK_SQLSTATE, state);
                    Assert.assertNotEquals(
                            "Caller saw an unhandled serialization failure (SQLState 40001): " + context,
                            SERIALIZATION_FAILURE_SQLSTATE, state);
                }
                if (t == t.getCause()) {
                    break;
                }
            }
        }
    }

    /**
     * Asserts the A->B edge's existence is identical across ALL replica placements of the {@code relation} reference
     * table. {@code run_command_on_placements('relation', ...)} runs the count on every placement (coordinator group
     * 0 + each worker) and returns one row per placement; a divergence (some placements 1, others 0) would mean the
     * replicated write landed on a subset of nodes. We assert all placements return the same count (all 0 or all 1).
     */
    private void assertEdgePlacementsAgree(UUID fromId, UUID toId, String context) {
        String command = String.format(
                "SELECT count(*) FROM %%s WHERE from_id = ''%s'' AND to_id = ''%s'' "
                + "AND relation_type = ''%s'' AND relation_type_group = ''%s''",
                fromId, toId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON.name());
        assertPlacementCountsAgree(command, context);
    }

    /**
     * Asserts every placement of {@code relation} agrees on the number of outbound rows for the given anchor id
     * (the rule chain id, which is the {@code from_id} of the chain's graph relations). Same per-placement check as
     * {@link #assertEdgePlacementsAgree}, but predicated on {@code from_id} only.
     */
    private void assertOutboundRelationCountAgrees(UUID fromId, String context) {
        String command = String.format("SELECT count(*) FROM %%s WHERE from_id = ''%s''", fromId);
        assertPlacementCountsAgree(command, context);
    }

    /**
     * Runs the given per-placement count command (a {@code SELECT count(*) FROM %%s WHERE ...} template, with single
     * quotes already doubled for the {@code run_command_on_placements} literal) across all {@code relation}
     * placements and asserts every placement reports the same value.
     */
    private void assertPlacementCountsAgree(String command, String context) {
        List<String> perPlacement = jdbcTemplate.queryForList(
                "SELECT result FROM run_command_on_placements('relation', '" + command + "')",
                String.class);
        assertThat(perPlacement)
                .as("run_command_on_placements must return at least one placement for the relation reference table")
                .isNotEmpty();
        assertThat(perPlacement)
                .as("Relation reference-table placements diverged (%s) — a replicated write landed on a subset of "
                    + "placements: %s", context, perPlacement)
                .containsOnly(perPlacement.get(0));
    }

    /**
     * Creates a rule chain plus a small node graph (3 nodes, 3 connections — same shape as the existing rule-chain
     * tests) so the chain owns real graph relations in the {@code relation} reference table, then returns its id.
     */
    private RuleChainId createRuleChainWithGraph(String name) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(name);
        ruleChain.setTenantId(tenantId);
        RuleChain savedRuleChain = ruleChainService.saveRuleChain(ruleChain);

        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(savedRuleChain.getId());

        List<RuleNode> nodes = new ArrayList<>();
        for (int n = 1; n <= 3; n++) {
            RuleNode node = new RuleNode();
            node.setName("name" + n);
            node.setType("type" + n);
            node.setConfiguration(JacksonUtil.toJsonNode("\"key" + n + "\": \"val" + n + "\""));
            nodes.add(node);
        }
        metaData.setFirstNodeIndex(0);
        metaData.setNodes(nodes);
        metaData.addConnectionInfo(0, 1, "success");
        metaData.addConnectionInfo(0, 2, "fail");
        metaData.addConnectionInfo(1, 2, "success");

        Assert.assertTrue(ruleChainService.saveRuleChainMetaData(tenantId, metaData, Function.identity()).isSuccess());
        return savedRuleChain.getId();
    }

    private void shutdown(ExecutorService pool) {
        pool.shutdownNow();
        try {
            pool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
