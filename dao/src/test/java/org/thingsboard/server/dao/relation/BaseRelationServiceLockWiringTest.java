// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.relation;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.dao.sql.relation.JpaRelationQueryExecutorService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.exception.DataValidationException;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseRelationServiceLockWiringTest {

    private static final TenantId TENANT_ID = TenantId.SYS_TENANT_ID;

    @Mock
    private RelationDao relationDao;
    @Mock
    private EntityService entityService;
    @Mock
    private TbTransactionalCache<RelationCacheKey, RelationCacheValue> cache;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private JpaExecutorService executor;
    @Mock
    private JpaRelationQueryExecutorService relationsExecutor;
    @Mock
    private ApiLimitService apiLimitService;
    @Mock
    private RelationWriteLock relationWriteLock;

    private BaseRelationService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new BaseRelationService(relationDao, entityService, cache, eventPublisher,
                executor, relationsExecutor, apiLimitService, relationWriteLock);
        // The async paths re-enter the proxied bean; in a plain unit test there is no proxy, so wire
        // the self-reference straight back to this instance.
        setSelf(service);
        // Run any executor.submit(...) inline so async assertions are deterministic.
        lenient().when(executor.submit(any(Callable.class))).thenAnswer(invocation -> {
            Callable<?> task = invocation.getArgument(0);
            return MoreExecutors.listeningDecorator(MoreExecutors.newDirectExecutorService()).submit(task);
        });
    }

    private void setSelf(BaseRelationService target) throws Exception {
        Field self = BaseRelationService.class.getDeclaredField("self");
        self.setAccessible(true);
        self.set(target, target);
    }

    private static EntityId asset() {
        return new AssetId(UUID.randomUUID());
    }

    private static EntityRelation relation(EntityId from, EntityId to) {
        return new EntityRelation(from, to, EntityRelation.CONTAINS_TYPE);
    }

    private static EntityRelation invalidRelation() {
        // Missing from endpoint: fails the @NotNull field validation with DataValidationException.
        EntityRelation relation = new EntityRelation();
        relation.setTo(asset());
        relation.setType(EntityRelation.CONTAINS_TYPE);
        return relation;
    }

    @SuppressWarnings("unchecked")
    private Collection<EntityId> captureLocked() {
        ArgumentCaptor<Collection<EntityId>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(relationWriteLock).lockEntities(captor.capture());
        return captor.getValue();
    }

    @Test
    void saveRelationLocksFromAndTo() {
        EntityId from = asset();
        EntityId to = asset();
        EntityRelation relation = relation(from, to);
        when(relationDao.saveRelation(TENANT_ID, relation)).thenReturn(relation);

        service.saveRelation(TENANT_ID, relation);

        assertThat(captureLocked()).containsExactlyInAnyOrder(from, to);
        // Lock-then-write: the endpoint locks must be acquired BEFORE the DAO write (a lock taken after the DML
        // serializes nothing — the load-bearing ordering per RelationWriteLock's contract).
        InOrder inOrder = inOrder(relationWriteLock, relationDao);
        inOrder.verify(relationWriteLock).lockEntities(any());
        inOrder.verify(relationDao).saveRelation(TENANT_ID, relation);
    }

    @Test
    void deleteRelationByEntityRelationLocksFromAndTo() {
        EntityId from = asset();
        EntityId to = asset();
        EntityRelation relation = relation(from, to);
        when(relationDao.deleteRelation(TENANT_ID, relation)).thenReturn(relation);

        service.deleteRelation(TENANT_ID, relation);

        assertThat(captureLocked()).containsExactlyInAnyOrder(from, to);
        // Lock-then-write ordering (see saveRelationLocksFromAndTo).
        InOrder inOrder = inOrder(relationWriteLock, relationDao);
        inOrder.verify(relationWriteLock).lockEntities(any());
        inOrder.verify(relationDao).deleteRelation(TENANT_ID, relation);
    }

    @Test
    void deleteRelationByEndpointsLocksFromAndTo() {
        EntityId from = asset();
        EntityId to = new DeviceId(UUID.randomUUID());

        service.deleteRelation(TENANT_ID, from, to, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);

        assertThat(captureLocked()).containsExactlyInAnyOrder(from, to);
        // Lock-then-write ordering (see saveRelationLocksFromAndTo).
        InOrder inOrder = inOrder(relationWriteLock, relationDao);
        inOrder.verify(relationWriteLock).lockEntities(any());
        inOrder.verify(relationDao).deleteRelation(TENANT_ID, from, to, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
    }

    @Test
    void saveRelationsLocksUnionOfAllEndpoints() {
        EntityId a = asset();
        EntityId b = asset();
        EntityId c = asset();
        EntityRelation r1 = relation(a, b);
        EntityRelation r2 = relation(b, c);
        List<EntityRelation> batch = List.of(r1, r2);
        when(relationDao.saveRelations(TENANT_ID, batch)).thenReturn(batch);

        service.saveRelations(TENANT_ID, batch);

        // saveRelations passes the raw from/to of every relation (b appears twice: r1.to and r2.from); deduping the
        // union {a,b,c} is deferred to lockEntities (distinct().sorted()), so the passed collection keeps both b's.
        assertThat(captureLocked()).containsExactlyInAnyOrder(a, b, b, c);
        // Lock-then-write ordering (see saveRelationLocksFromAndTo): the batched lock precedes the batched save.
        InOrder inOrder = inOrder(relationWriteLock, relationDao);
        inOrder.verify(relationWriteLock).lockEntities(any());
        inOrder.verify(relationDao).saveRelations(TENANT_ID, batch);
    }

    @Test
    void deleteEntityRelationsLocksTheAnchor() {
        EntityId anchor = asset();
        when(relationDao.deleteInboundRelations(TENANT_ID, anchor)).thenReturn(List.of());
        when(relationDao.deleteOutboundRelations(TENANT_ID, anchor)).thenReturn(List.of());

        service.deleteEntityRelations(TENANT_ID, anchor);

        assertThat(captureLocked()).containsExactly(anchor);
        // Lock-then-write ordering (see saveRelationLocksFromAndTo): the anchor lock precedes the first bulk delete.
        InOrder inOrder = inOrder(relationWriteLock, relationDao);
        inOrder.verify(relationWriteLock).lockEntities(any());
        inOrder.verify(relationDao).deleteInboundRelations(TENANT_ID, anchor);
    }

    @Test
    void deleteEntityCommonRelationsLocksTheAnchor() {
        EntityId anchor = asset();
        when(relationDao.deleteInboundRelations(TENANT_ID, anchor, RelationTypeGroup.COMMON)).thenReturn(List.of());
        when(relationDao.deleteOutboundRelations(TENANT_ID, anchor, RelationTypeGroup.COMMON)).thenReturn(List.of());

        service.deleteEntityCommonRelations(TENANT_ID, anchor);

        assertThat(captureLocked()).containsExactly(anchor);
        // Lock-then-write ordering (see saveRelationLocksFromAndTo): the anchor lock precedes the first bulk delete.
        InOrder inOrder = inOrder(relationWriteLock, relationDao);
        inOrder.verify(relationWriteLock).lockEntities(any());
        inOrder.verify(relationDao).deleteInboundRelations(TENANT_ID, anchor, RelationTypeGroup.COMMON);
    }

    @Test
    void saveRelationAsyncLocksFromAndToOnExecutorThread() throws Exception {
        EntityId from = asset();
        EntityId to = asset();
        EntityRelation relation = relation(from, to);
        when(relationDao.saveRelation(TENANT_ID, relation)).thenReturn(relation);

        ListenableFuture<Boolean> future = service.saveRelationAsync(TENANT_ID, relation);

        assertThat(future.get()).isTrue();
        // Async path must go through the sync locked method (submitted to the executor).
        verify(executor).submit(any(Callable.class));
        assertThat(captureLocked()).containsExactlyInAnyOrder(from, to);
    }

    @Test
    void deleteRelationAsyncByEndpointsLocksFromAndToOnExecutorThread() throws Exception {
        EntityId from = asset();
        EntityId to = asset();
        when(relationDao.deleteRelation(TENANT_ID, from, to, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON))
                .thenReturn(relation(from, to));

        ListenableFuture<Boolean> future = service.deleteRelationAsync(TENANT_ID, from, to, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);

        assertThat(future.get()).isTrue();
        verify(executor).submit(any(Callable.class));
        assertThat(captureLocked()).containsExactlyInAnyOrder(from, to);
    }

    @Test
    void saveRelationAsyncWithInvalidRelationFailsTheFutureInsteadOfThrowing() {
        EntityRelation invalid = invalidRelation();

        ListenableFuture<Boolean> future = service.saveRelationAsync(TENANT_ID, invalid);

        // Validation runs inside the submitted task, so the invocation itself must not throw: the
        // DataValidationException surfaces as a failed future — the pre-existing async error contract.
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(DataValidationException.class);
        verify(relationDao, never()).saveRelation(any(), any());
    }

    @Test
    void deleteRelationAsyncWithInvalidRelationFailsTheFutureInsteadOfThrowing() {
        EntityRelation invalid = invalidRelation();

        ListenableFuture<Boolean> future = service.deleteRelationAsync(TENANT_ID, invalid);

        // Same failed-future contract as saveRelationAsync: no synchronous throw from the invocation.
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(DataValidationException.class);
        verify(relationDao, never()).deleteRelation(any(), any(EntityRelation.class));
    }

    @Test
    void deleteRelationAsyncByEndpointsWithInvalidArgsThrowsSynchronously() {
        // The 5-arg overload keeps its historical throw-on-call contract: validation runs BEFORE the task is
        // submitted, so a missing endpoint throws DataValidationException synchronously, never as a failed future.
        assertThatThrownBy(() -> service.deleteRelationAsync(TENANT_ID, null, asset(), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON))
                .isInstanceOf(DataValidationException.class);
        verify(executor, never()).submit(any(Callable.class));
    }

    @Test
    void saveRelationsLocksOnceForTheWholeBatch() {
        EntityId a = asset();
        EntityId b = asset();
        EntityRelation r1 = relation(a, b);
        List<EntityRelation> batch = List.of(r1);
        when(relationDao.saveRelations(TENANT_ID, batch)).thenReturn(batch);

        service.saveRelations(TENANT_ID, batch);

        // A single batched lock acquisition for the entire save, not one per relation/chunk.
        verify(relationWriteLock, times(1)).lockEntities(any());
    }

    @Test
    void removeRelationsRunsDeleteLoopUnderCoveringLockOnTheEntity() {
        EntityId entityId = asset();
        EntityRelation outbound = relation(entityId, asset());
        EntityRelation inbound = relation(asset(), entityId);

        // findByFrom/findByTo go through cache.getAndPutInTransaction(key, dbCall, ...); bridge the mocked cache to
        // its dbCall loader so the finders actually return whatever relationDao yields.
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get())
                .when(cache).getAndPutInTransaction(any(), any(Supplier.class), any(), any(), eq(false));
        // Default: no relations for any group; only the COMMON group carries our two seeded relations.
        lenient().when(relationDao.findAllByFrom(eq(TENANT_ID), eq(entityId), any())).thenReturn(List.of());
        lenient().when(relationDao.findAllByTo(eq(TENANT_ID), eq(entityId), any())).thenReturn(List.of());
        when(relationDao.findAllByFrom(TENANT_ID, entityId, RelationTypeGroup.COMMON)).thenReturn(List.of(outbound));
        when(relationDao.findAllByTo(TENANT_ID, entityId, RelationTypeGroup.COMMON)).thenReturn(List.of(inbound));
        when(relationDao.deleteRelation(eq(TENANT_ID), any(EntityRelation.class))).thenReturn(outbound);

        // Make the mocked covering lock actually run the body, mirroring the real helper's contract; the proxy /
        // @Transactional transaction-start behavior is covered by RelationWriteLockTest's transaction-guard tests,
        // not here (this unit test wires self to the raw instance, so there is no proxy to start a transaction).
        doAnswer(invocation -> {
            invocation.getArgument(1, Runnable.class).run();
            return null;
        }).when(relationWriteLock).withCoveringLock(eq(entityId), any(Runnable.class));

        service.removeRelations(TENANT_ID, entityId);

        // (a) the whole removal ran inside exactly one covering lock anchored on entityId, and
        verify(relationWriteLock, times(1)).withCoveringLock(eq(entityId), any(Runnable.class));
        // (b) the inner deletes ran for each found relation (proving the loop executes inside the covering lock).
        verify(relationDao).deleteRelation(TENANT_ID, outbound);
        verify(relationDao).deleteRelation(TENANT_ID, inbound);
    }

    @Test
    void removeRelationsDeletesInDeterministicCompositeKeyOrder() {
        // Fixed UUIDs pin the composite-key sort order: inboundFrom(10) < anchor(100) < toB(200) < toC(300).
        EntityId anchor = new AssetId(new UUID(0, 100));
        EntityRelation inbound = relation(new AssetId(new UUID(0, 10)), anchor);
        EntityRelation outboundToB = relation(anchor, new AssetId(new UUID(0, 200)));
        EntityRelation outboundToC = relation(anchor, new AssetId(new UUID(0, 300)));

        // Same cache/DAO bridging and covering-lock pass-through as the covering-lock test above.
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get())
                .when(cache).getAndPutInTransaction(any(), any(Supplier.class), any(), any(), eq(false));
        lenient().when(relationDao.findAllByFrom(eq(TENANT_ID), eq(anchor), any())).thenReturn(List.of());
        lenient().when(relationDao.findAllByTo(eq(TENANT_ID), eq(anchor), any())).thenReturn(List.of());
        // Scrambled: the collected order is the findByFrom results first ([toC, toB]) then findByTo ([inbound]) —
        // the exact reverse of the sorted composite-key order [inbound(from=10), toB(to=200), toC(to=300)].
        when(relationDao.findAllByFrom(TENANT_ID, anchor, RelationTypeGroup.COMMON)).thenReturn(List.of(outboundToC, outboundToB));
        when(relationDao.findAllByTo(TENANT_ID, anchor, RelationTypeGroup.COMMON)).thenReturn(List.of(inbound));
        when(relationDao.deleteRelation(eq(TENANT_ID), any(EntityRelation.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        doAnswer(invocation -> {
            invocation.getArgument(1, Runnable.class).run();
            return null;
        }).when(relationWriteLock).withCoveringLock(eq(anchor), any(Runnable.class));

        service.removeRelations(TENANT_ID, anchor);

        // The deterministic (from, to, type, typeGroup) sort is load-bearing: it makes two concurrent
        // removeRelations invocations delete any shared rows in the same order, closing the anchor-vs-anchor
        // 40P01 row-lock deadlock window opened by the @Transactional boundary. Dropping the sort fails this.
        InOrder inOrder = inOrder(relationDao);
        inOrder.verify(relationDao).deleteRelation(TENANT_ID, inbound);
        inOrder.verify(relationDao).deleteRelation(TENANT_ID, outboundToB);
        inOrder.verify(relationDao).deleteRelation(TENANT_ID, outboundToC);
    }
}
