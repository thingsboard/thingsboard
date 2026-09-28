// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.relation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.dao.sql.citus.CitusSettings;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelationWriteLockTest {

    @Mock
    private EntityManager entityManager;

    @AfterEach
    void tearDown() {
        // The guard test flips the actual-transaction flag; reset so it never leaks to a pooled thread / other tests.
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    private RelationWriteLock newLock(boolean citusEnabled) {
        CitusSettings citusSettings = mock(CitusSettings.class);
        lenient().when(citusSettings.isEnabled()).thenReturn(citusEnabled);
        RelationWriteLock lock = new RelationWriteLock(citusSettings);
        lock.entityManager = entityManager;
        return lock;
    }

    private static EntityId entityId() {
        return new DeviceId(UUID.randomUUID());
    }

    // ---------------------------------------------------------------------------------------------
    // 1. Key derivation
    // ---------------------------------------------------------------------------------------------

    @Test
    void keyDerivationIsDeterministic() {
        EntityId id = entityId();
        long first = RelationWriteLock.advisoryKey(id);
        long second = RelationWriteLock.advisoryKey(id);
        // Same id (and a fresh wrapper around the same UUID) must produce the same key across calls.
        long fromCopy = RelationWriteLock.advisoryKey(new DeviceId(id.getId()));
        assertThat(second).isEqualTo(first);
        assertThat(fromCopy).isEqualTo(first);
    }

    @Test
    void keyDerivationIsDistinctOverLargeSample() {
        int sample = 100_000;
        Set<Long> keys = IntStream.range(0, sample)
                .mapToObj(i -> RelationWriteLock.advisoryKey(entityId()))
                .collect(Collectors.toSet());
        // Over 100k random UUID-based ids we expect effectively zero collisions in a 64-bit space.
        assertThat(keys).hasSize(sample);
    }

    @Test
    void keyDerivationIsNotRawXor() {
        // Two distinct UUIDs whose (msb ^ lsb) collide: a raw XOR would map both to the same key.
        // a: msb=1, lsb=2 -> xor=3 ; b: msb=2, lsb=1 -> xor=3.
        UUID a = new UUID(1L, 2L);
        UUID b = new UUID(2L, 1L);
        assertThat(a.getMostSignificantBits() ^ a.getLeastSignificantBits())
                .isEqualTo(b.getMostSignificantBits() ^ b.getLeastSignificantBits());

        long keyA = RelationWriteLock.advisoryKey(new DeviceId(a));
        long keyB = RelationWriteLock.advisoryKey(new DeviceId(b));
        assertThat(keyA).isNotEqualTo(keyB);
    }

    // ---------------------------------------------------------------------------------------------
    // 2. Suppression marker
    // ---------------------------------------------------------------------------------------------

    @Test
    void lockEntitiesNoOpsWhenCoveringMarkerSet() {
        RelationWriteLock lock = newLock(true);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        EntityId outer = entityId();
        AtomicBoolean ran = new AtomicBoolean(false);
        lock.withCoveringLock(outer, () -> {
            // Inside the covering lock the marker is set: lockEntities must not issue any extra native query.
            lock.lockEntities(List.of(entityId(), entityId()));
            ran.set(true);
        });

        assertThat(ran).isTrue();
        // Exactly one native query: the single covering lock(outer). The nested lockEntities is suppressed.
        verify(entityManager, times(1)).createNativeQuery(anyString());
    }

    @Test
    void supplierOverloadReturnsBodyValueAndSuppressesNestedLockEntities() {
        RelationWriteLock lock = newLock(true);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        EntityId outer = entityId();
        String result = lock.withCoveringLock(outer, () -> {
            // Inside the covering lock the marker is set: nested lockEntities must not issue an extra native query.
            lock.lockEntities(List.of(entityId(), entityId()));
            return "ok";
        });

        // The body's value is propagated to the caller.
        assertThat(result).isEqualTo("ok");
        // Exactly one native query: the single covering lock(outer). The nested lockEntities is suppressed.
        verify(entityManager, times(1)).createNativeQuery(anyString());
    }

    @Test
    void supplierOverloadClearsMarkerAfterReturn() {
        RelationWriteLock lock = newLock(true);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        Integer result = lock.withCoveringLock(entityId(), () -> 42);
        assertThat(result).isEqualTo(42);

        // Marker must be cleared after the supplier returns: a subsequent lockEntities issues its own query again.
        lock.lockEntities(List.of(entityId()));
        verify(entityManager, times(2)).createNativeQuery(anyString());
    }

    @Test
    void supplierOverloadRunsBodyAndDoesNotLockOnCitusDisabled() {
        RelationWriteLock lock = newLock(false);
        TransactionSynchronizationManager.setActualTransactionActive(true);

        // Citus off (but inside a transaction, as every write path is): must run the body and never touch the entity
        // manager (no advisory lock on plain PostgreSQL).
        String result = lock.withCoveringLock(entityId(), () -> "ran");

        assertThat(result).isEqualTo("ran");
        verifyNoInteractions(entityManager);
    }

    @Test
    void coveringMarkerClearedEvenWhenBodyThrows() {
        RelationWriteLock lock = newLock(true);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        assertThatThrownBy(() -> lock.withCoveringLock(entityId(), () -> {
            throw new RuntimeException("boom");
        })).isInstanceOf(RuntimeException.class).hasMessage("boom");

        // Marker must be cleared in finally: a subsequent lockEntities issues its own query again.
        lock.lockEntities(List.of(entityId()));
        verify(entityManager, times(2)).createNativeQuery(anyString());
    }

    @Test
    void coveringMarkerDoesNotLeakAcrossSequentialInvocations() {
        RelationWriteLock lock = newLock(true);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        lock.withCoveringLock(entityId(), () -> {});
        // After the first covering block returns, the marker is cleared, so the next block's own
        // covering lock is issued (and any lockEntities inside it would also fire).
        AtomicInteger inner = new AtomicInteger();
        lock.withCoveringLock(entityId(), () -> {
            lock.lockEntities(List.of(entityId()));
            inner.incrementAndGet();
        });

        assertThat(inner.get()).isEqualTo(1);
        // 1st covering lock + 2nd covering lock. The lockEntities inside the 2nd block is suppressed by the marker.
        verify(entityManager, times(2)).createNativeQuery(anyString());
    }

    @Test
    void nestedCoveringLockRestoresOuterCoveredStateOnInnerExit() {
        RelationWriteLock lock = newLock(true);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        lock.withCoveringLock(entityId(), () -> {
            lock.withCoveringLock(entityId(), () -> {});
            // The inner block's exit must restore the OUTER covered state (not clear the marker to FALSE): the
            // outer covering lock is still held, so this lockEntities stays suppressed. Exactly two native queries
            // so far — the outer and inner covering locks themselves.
            lock.lockEntities(List.of(entityId()));
            verify(entityManager, times(2)).createNativeQuery(anyString());
        });

        // Only after the OUTER block exits is per-endpoint locking re-enabled: this one issues its own query.
        lock.lockEntities(List.of(entityId()));
        verify(entityManager, times(3)).createNativeQuery(anyString());
    }

    // ---------------------------------------------------------------------------------------------
    // 3. Transaction guard
    // ---------------------------------------------------------------------------------------------

    @Test
    void lockEntitiesThrowsWhenCitusEnabledAndNoActiveTransaction() {
        RelationWriteLock lock = newLock(true);
        TransactionSynchronizationManager.setActualTransactionActive(false);

        assertThatThrownBy(() -> lock.lockEntities(List.of(entityId())))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(entityManager);
    }

    @Test
    void withCoveringLockThrowsWhenCitusEnabledAndNoActiveTransaction() {
        RelationWriteLock lock = newLock(true);
        TransactionSynchronizationManager.setActualTransactionActive(false);

        assertThatThrownBy(() -> lock.withCoveringLock(entityId(), () -> {}))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(entityManager);
    }

    @Test
    void guardFiresOnPlainPostgresWhenNoActiveTransaction() {
        RelationWriteLock lock = newLock(false);
        TransactionSynchronizationManager.setActualTransactionActive(false);

        // The transaction assertion is hoisted before the Citus gate, so both entry points fail fast on plain
        // PostgreSQL too (not only under Citus) when a write path forgot to run inside @Transactional.
        assertThatThrownBy(() -> lock.lockEntities(List.of(entityId())))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> lock.withCoveringLock(entityId(), () -> {}))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(entityManager);
    }

    @Test
    void emptyOrNullInputNoOpsEvenWithoutActiveTransaction() {
        RelationWriteLock lock = newLock(true);
        TransactionSynchronizationManager.setActualTransactionActive(false);

        // Empty/null input has nothing to lock, so it short-circuits before the transaction assertion — no throw.
        lock.lockEntities(List.of());
        lock.lockEntities(null);

        verifyNoInteractions(entityManager);
    }

    // ---------------------------------------------------------------------------------------------
    // 4. SQL shape
    // ---------------------------------------------------------------------------------------------

    @Test
    void lockEntitiesEmitsSingleBatchedAdvisoryLockOverSortedKeys() {
        RelationWriteLock lock = newLock(true);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        Query query = mock(Query.class);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(entityManager.createNativeQuery(sqlCaptor.capture())).thenReturn(query);

        EntityId a = entityId();
        EntityId b = entityId();
        EntityId c = entityId();
        lock.lockEntities(List.of(a, b, c));

        // Exactly one statement, and it is the single batched form over unnest of a bigint[].
        verify(entityManager, times(1)).createNativeQuery(anyString());
        verify(query, times(1)).getResultList();
        String sql = sqlCaptor.getValue();
        assertThat(sql).isEqualTo("SELECT pg_advisory_xact_lock(k) FROM unnest(?::bigint[]) AS k");

        // The single bound parameter is the PG array literal of the sorted, deduped keys.
        ArgumentCaptor<Object> paramCaptor = ArgumentCaptor.forClass(Object.class);
        verify(query).setParameter(eq(1), paramCaptor.capture());
        long[] expectedSorted = new long[]{
                RelationWriteLock.advisoryKey(a),
                RelationWriteLock.advisoryKey(b),
                RelationWriteLock.advisoryKey(c)
        };
        Arrays.sort(expectedSorted);
        String expectedLiteral = "{" + expectedSorted[0] + "," + expectedSorted[1] + "," + expectedSorted[2] + "}";
        assertThat(paramCaptor.getValue()).isEqualTo(expectedLiteral);
    }

    @Test
    void lockEntitiesDedupesSelfRelationToSingleKey() {
        RelationWriteLock lock = newLock(true);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        Query query = mock(Query.class);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(entityManager.createNativeQuery(sqlCaptor.capture())).thenReturn(query);

        EntityId self = entityId();
        // from == to: the two ids dedupe to a single key (one-element array literal).
        lock.lockEntities(List.of(self, self));

        ArgumentCaptor<Object> paramCaptor = ArgumentCaptor.forClass(Object.class);
        verify(query).setParameter(eq(1), paramCaptor.capture());
        assertThat(paramCaptor.getValue()).isEqualTo("{" + RelationWriteLock.advisoryKey(self) + "}");
    }

    @Test
    void lockEntitiesNeverCreatesNativeQueryWhenCitusDisabled() {
        RelationWriteLock lock = newLock(false);
        TransactionSynchronizationManager.setActualTransactionActive(true);

        lock.lockEntities(List.of(entityId(), entityId()));

        verifyNoInteractions(entityManager);
    }

    @Test
    void lockEntitiesNoOpsOnEmptyOrNullInput() {
        RelationWriteLock lock = newLock(true);
        TransactionSynchronizationManager.setActualTransactionActive(true);

        lock.lockEntities(List.of());
        lock.lockEntities(null);

        verifyNoInteractions(entityManager);
    }
}
