/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.actors.device;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.LinkedHashMapRemoveEldest;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.RpcPersistResult;
import org.thingsboard.server.common.msg.rpc.RpcPersistResultActorMsg;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionType;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.service.rpc.TbCoreDeviceRpcService;
import org.thingsboard.server.service.rpc.TbRpcService;
import org.thingsboard.server.service.transport.TbCoreToTransportService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class DeviceActorMessageProcessorTest {

    private static final int MAX_CONCURRENT_SESSIONS_PER_DEVICE = 1;
    ActorSystemContext systemContext;
    DeviceService deviceService;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("ae651b45-2a92-4bdf-9d56-faede44eafb8"));
    DeviceId deviceId = DeviceId.fromString("78bf9b26-74ef-4af2-9cfb-ad6cf24ad2ec");

    DeviceActorMessageProcessor processor;
    TbRpcService rpcService;
    TbCoreDeviceRpcService coreRpcService;
    TbCoreToTransportService toTransport;

    @Before
    public void setUp() {
        systemContext = mock(ActorSystemContext.class);
        deviceService = mock(DeviceService.class);
        given(systemContext.getMaxConcurrentSessionsPerDevice()).willReturn(MAX_CONCURRENT_SESSIONS_PER_DEVICE);
        given(systemContext.getDeviceService()).willReturn(deviceService);
        given(systemContext.getRpcSubmitStrategy()).willReturn("BURST");
        processor = new DeviceActorMessageProcessor(systemContext, tenantId, deviceId);
        given(systemContext.getTbCoreToTransportService()).willReturn(mock(TbCoreToTransportService.class));
    }

    @Test
    public void givenSystemContext_whenNewInstance_thenVerifySessionMapMaxSize() {
        assertThat(processor.sessions).isInstanceOf(LinkedHashMapRemoveEldest.class);
        assertThat(processor.sessions.getMaxEntries()).isEqualTo(MAX_CONCURRENT_SESSIONS_PER_DEVICE);
        assertThat(processor.sessions.getRemovalConsumer()).isNotNull();
    }

    @Test
    public void givenFullSessionMap_whenSessionOverflow_thenShouldDeleteAttributeAndRPCSubscriptions() {
        //givenFullSessionMap
        for (int i = 0; i < MAX_CONCURRENT_SESSIONS_PER_DEVICE; i++) {
            UUID sessionID = UUID.randomUUID();
            processor.sessions.put(sessionID, Mockito.mock(SessionInfoMetaData.class, RETURNS_DEEP_STUBS));
            processor.attributeSubscriptions.put(sessionID, Mockito.mock(SessionInfo.class));
            processor.rpcSubscriptions.put(sessionID, Mockito.mock(SessionInfo.class));
        }
        assertThat(processor.sessions.size()).isEqualTo(MAX_CONCURRENT_SESSIONS_PER_DEVICE);
        assertThat(processor.attributeSubscriptions.size()).isEqualTo(MAX_CONCURRENT_SESSIONS_PER_DEVICE);
        assertThat(processor.rpcSubscriptions.size()).isEqualTo(MAX_CONCURRENT_SESSIONS_PER_DEVICE);

        //add one more
        processor.sessions.put(UUID.randomUUID(), Mockito.mock(SessionInfoMetaData.class));

        assertThat(processor.sessions.size()).isEqualTo(MAX_CONCURRENT_SESSIONS_PER_DEVICE);
        assertThat(processor.attributeSubscriptions.size()).isEqualTo(MAX_CONCURRENT_SESSIONS_PER_DEVICE - 1);
        assertThat(processor.rpcSubscriptions.size()).isEqualTo(MAX_CONCURRENT_SESSIONS_PER_DEVICE - 1);

    }

    @Test
    public void nonPersistentRpcIsSentInTheArrivalTurnWithoutTouchingTheDatabase() {
        mockRpcInfra();
        subscribeAsyncSession();

        processor.processRpcRequest(mock(TbActorCtx.class),
                new ToDeviceRpcRequestActorMsg("svc", nonPersistedRequest(UUID.randomUUID())));

        // Straight out in the arrival turn: no create enqueued, no persist result involved.
        assertThat(publishedRequestIds()).containsExactly(0);
        verify(rpcService, never()).createIfAbsent(any(), any(), any());
        verify(rpcService, never()).update(any(), any());
    }

    @Test
    public void nonPersistentOneWayRpcCompletesAsSoonAsItIsSent() {
        mockRpcInfra();
        subscribeAsyncSession();
        UUID rpcId = UUID.randomUUID();

        processor.processRpcRequest(mock(TbActorCtx.class), new ToDeviceRpcRequestActorMsg("svc",
                nonPersistedRequest(rpcId, System.currentTimeMillis() + 60_000, true)));

        assertThat(publishedRequestIds()).containsExactly(0);
        ArgumentCaptor<FromDeviceRpcResponse> captor = ArgumentCaptor.forClass(FromDeviceRpcResponse.class);
        verify(coreRpcService).processRpcResponseFromDeviceActor(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(rpcId);
        assertThat(processor.toDeviceRpcPendingMap).isEmpty();
    }

    @Test
    public void nonPersistentTwoWayRpcStaysPendingAfterItIsSent() {
        mockRpcInfra();
        subscribeAsyncSession();

        processor.processRpcRequest(mock(TbActorCtx.class),
                new ToDeviceRpcRequestActorMsg("svc", nonPersistedRequest(UUID.randomUUID())));

        assertThat(publishedRequestIds()).containsExactly(0);
        assertThat(processor.toDeviceRpcPendingMap).containsKey(0);
        verify(coreRpcService, never()).processRpcResponseFromDeviceActor(any());
    }

    @Test
    public void nonPersistentRpcExpiredOnArrivalIsDroppedSilently() {
        mockRpcInfra();
        subscribeAsyncSession();

        processor.processRpcRequest(mock(TbActorCtx.class), new ToDeviceRpcRequestActorMsg("svc",
                nonPersistedRequest(UUID.randomUUID(), System.currentTimeMillis() - 1, false)));

        // No row is written for a non-persistent RPC, and the caller gets no rpcId to read.
        verify(toTransport, never()).process(any(), any());
        verify(rpcService, never()).createIfAbsent(any(), any(), any());
        verify(coreRpcService, never()).processRpcResponseFromDeviceActor(any());
        assertThat(processor.toDeviceRpcPendingMap).isEmpty();
    }

    @Test
    public void pendingNonPersistentRpcIsStillDeliveredOnSubscribe() {
        mockRpcInfra();

        // No session yet, so it is registered unsent.
        processor.processRpcRequest(mock(TbActorCtx.class),
                new ToDeviceRpcRequestActorMsg("svc", nonPersistedRequest(UUID.randomUUID())));
        verify(toTransport, never()).process(any(), any());

        // The durability filter added for persistent RPCs must not hold a non-persistent one back.
        pushViaAsyncSession();
        assertThat(publishedRequestIds()).containsExactly(0);
    }

    @Test
    public void persistsRequestIdOnCreate() {
        mockRpcInfra();

        TbActorCtx ctx = mock(TbActorCtx.class);
        processor.processRpcRequest(ctx, new ToDeviceRpcRequestActorMsg("svc", persistedRequest(UUID.randomUUID())));

        ArgumentCaptor<Rpc> captor = ArgumentCaptor.forClass(Rpc.class);
        verify(rpcService).createIfAbsent(eq(tenantId), captor.capture(), any());
        assertThat(captor.getValue().getRequestId()).isEqualTo(0); // first rpcSeq
    }

    @Test
    public void reloadedDeliveredRpcMatchesDeviceResponse() {
        mockRpcInfra();
        Rpc row = inFlightRow(RpcStatus.DELIVERED, 7, System.currentTimeMillis());
        stubInFlight(row);

        processor.init(mock(TbActorCtx.class));

        // device replies with the OLD id 7:
        processor.processRpcResponses(sessionInfoProto(), ToDeviceRpcResponseMsg.newBuilder()
                .setRequestId(7).setPayload("{\"ok\":true}").build());

        // matched → row updated to SUCCESSFUL (not "stale"):
        ArgumentCaptor<Rpc> captor = ArgumentCaptor.forClass(Rpc.class);
        verify(rpcService).update(eq(tenantId), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RpcStatus.SUCCESSFUL);
    }

    @Test
    public void oneWayDeliveredRowNotReExpiredOnReload() {
        mockRpcInfra();
        long pastExp = System.currentTimeMillis() - 60_000; // already expired
        Rpc row = expiredRow(RpcStatus.DELIVERED, 9, true /*oneway*/, pastExp);
        stubInFlight(row);

        processor.init(mock(TbActorCtx.class));

        // terminal one-way DELIVERED row, past expiry: must be left untouched, no EXPIRED overwrite
        verify(rpcService, never()).update(any(), any());
    }

    @Test
    public void pastExpiryTwoWaySentRowIsExpiredOnReload() {
        mockRpcInfra();
        long pastExp = System.currentTimeMillis() - 60_000; // already expired
        Rpc row = expiredRow(RpcStatus.SENT, 3, false /*two-way*/, pastExp);
        stubInFlight(row);

        processor.init(mock(TbActorCtx.class));

        // non-terminal (two-way SENT) row past its expiry must be force-updated to EXPIRED on reload:
        ArgumentCaptor<Rpc> captor = ArgumentCaptor.forClass(Rpc.class);
        verify(rpcService).update(eq(tenantId), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RpcStatus.EXPIRED);
    }

    @Test
    public void pastExpiryTimeoutRowIsExpiredOnReload() {
        mockRpcInfra();
        long pastExp = System.currentTimeMillis() - 60_000; // already expired
        Rpc row = expiredRow(RpcStatus.TIMEOUT, 4, false /*two-way*/, pastExp);
        stubInFlight(row);

        processor.init(mock(TbActorCtx.class));

        // a reloaded TIMEOUT row (delivery ack timed out, mid-retry) past its expiry must be force-closed to EXPIRED:
        ArgumentCaptor<Rpc> captor = ArgumentCaptor.forClass(Rpc.class);
        verify(rpcService).update(eq(tenantId), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RpcStatus.EXPIRED);
    }

    @Test
    public void inFlightTimeoutRowIsReTrackedNotExpiredOnReload() {
        mockRpcInfra();
        Rpc row = inFlightRow(RpcStatus.TIMEOUT, 8, System.currentTimeMillis());
        stubInFlight(row);

        processor.init(mock(TbActorCtx.class));

        // a not-yet-expired TIMEOUT row is re-tracked for retry, not force-closed: no terminal update on reload
        verify(rpcService, never()).update(any(), any());
    }

    @Test
    public void seedsCounterPastHighestReloadedId() {
        mockRpcInfra();
        stubInFlight(inFlightRow(RpcStatus.SENT, 5, System.currentTimeMillis()));

        processor.init(mock(TbActorCtx.class));

        // next brand-new persistent RPC must get id 6, not 0:
        processor.processRpcRequest(mock(TbActorCtx.class),
                new ToDeviceRpcRequestActorMsg("svc", persistedRequest(UUID.randomUUID())));

        ArgumentCaptor<Rpc> captor = ArgumentCaptor.forClass(Rpc.class);
        verify(rpcService).createIfAbsent(eq(tenantId), captor.capture(), any());
        assertThat(captor.getValue().getRequestId()).isEqualTo(6);
    }

    @Test
    public void resubscribeReSendsUndeliveredButNotDelivered() {
        mockRpcInfra();
        stubInFlight(inFlightRow(RpcStatus.SENT, 6, 1000L),          // undelivered
                inFlightRow(RpcStatus.DELIVERED, 5, 2000L));
        processor.init(mock(TbActorCtx.class));

        pushViaAsyncSession();

        // undelivered re-sent, delivered not:
        assertThat(publishedRequestIds()).contains(6).doesNotContain(5);
    }

    @Test
    public void oneWaySentRowIsRePublishedOnReload() {
        mockRpcInfra();
        stubInFlight(inFlightRow(RpcStatus.SENT, 6, 1000L, true)); // one-way, QoS-1 publish->PUBACK window
        processor.init(mock(TbActorCtx.class));

        pushViaAsyncSession();

        // one-way SENT is no longer skipped on reload — it must be re-published to the device:
        assertThat(publishedRequestIds()).contains(6);
    }

    @Test
    public void legacyNullRequestIdRowReloadsWithoutNpe() {
        mockRpcInfra();
        stubInFlight(legacyQueuedRow(System.currentTimeMillis())); // legacy pre-migration row: null requestId
        processor.init(mock(TbActorCtx.class)); // must not NPE despite the null persisted requestId

        pushViaAsyncSession();

        // fresh-id fallback (rpcSeq, starting at 0) was assigned and the row was registered/re-published:
        assertThat(publishedRequestIds()).contains(0);
    }

    @Test
    public void legacyNullAndReusedIdDoNotCollideOnReload() {
        mockRpcInfra();
        // a legacy row (null id, older) and a post-migration row that reused id 0 (newer) in the SAME batch:
        stubInFlight(legacyQueuedRow(1000L), inFlightRow(RpcStatus.QUEUED, 0, 2000L));

        processor.init(mock(TbActorCtx.class));
        pushViaAsyncSession();

        // the legacy row's fallback id must be seeded past the reused id 0, so both re-publish under distinct
        // ids (0 stays 0, the legacy row gets 1) — neither clobbers the other in the pending map:
        assertThat(publishedRequestIds()).containsExactlyInAnyOrder(0, 1);
    }

    @Test
    public void nullRequestRowIsSkippedWithoutAbortingReload() {
        mockRpcInfra();
        // corrupt/legacy row whose request JSON deserializes to null — processed FIRST (earlier createdTime):
        Rpc badRow = new Rpc(new RpcId(UUID.randomUUID()));
        badRow.setCreatedTime(999L);
        badRow.setExpirationTime(System.currentTimeMillis() + 60_000);
        badRow.setStatus(RpcStatus.QUEUED);
        badRow.setRequestId(7);
        badRow.setRequest(null); // JacksonUtil.convertValue(null, ...) returns null -> restore must skip, not NPE
        stubInFlight(badRow, inFlightRow(RpcStatus.QUEUED, 9, 1000L));

        processor.init(mock(TbActorCtx.class)); // must not NPE on the null-request row

        pushViaAsyncSession();

        // the bad row is silently skipped; the following good row still restores and re-publishes:
        assertThat(publishedRequestIds()).contains(9).doesNotContain(7);
    }

    @Test
    public void legacyNullSentRowIsClosedNotRePublished() {
        mockRpcInfra();
        Rpc row = row(UUID.randomUUID(), RpcStatus.SENT, System.currentTimeMillis(),
                System.currentTimeMillis() + 60_000, false); // two-way, future expiration
        row.setRequestId(null); // legacy pre-migration row: no persisted requestId
        stubInFlight(row);

        processor.init(mock(TbActorCtx.class));
        pushViaAsyncSession();

        // untrackable legacy row must be closed (not re-armed/re-published):
        ArgumentCaptor<Rpc> captor = ArgumentCaptor.forClass(Rpc.class);
        verify(rpcService).update(eq(tenantId), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RpcStatus.FAILED);
        assertThat(captor.getValue().getResponse()).isNotNull();
        verify(toTransport, never()).process(any(), any());
    }

    @Test
    public void firstPersistedRpcRegistersAndSendsAsBefore() {
        mockRpcInfra();
        UUID rpcId = UUID.randomUUID();
        subscribeAsyncSession();

        processor.processRpcRequest(mock(TbActorCtx.class),
                new ToDeviceRpcRequestActorMsg("svc", persistedRequest(rpcId)));
        deliverPersistResult(rpcId, 0, RpcPersistResult.INSERTED);

        // The non-duplicate path must be untouched by the restructure: the command still reaches the device,
        // and the caller still gets its id back.
        assertThat(publishedRequestIds()).containsExactly(0); // first rpcSeq
        assertRpcIdReplied(rpcId);
    }

    @Test
    public void duplicatePersistedRpcSkipsSendAndPendingRegistration() {
        mockRpcInfra();
        UUID sessionId = subscribeAsyncSession(); // active subscription: a send WOULD happen if not skipped
        UUID rpcId = UUID.randomUUID();

        processor.processRpcRequest(mock(TbActorCtx.class),
                new ToDeviceRpcRequestActorMsg("svc", persistedRequest(rpcId)));
        deliverPersistResult(rpcId, 0, RpcPersistResult.DUPLICATE);

        // Not sent - the existing row owns delivery, re-sending would double-execute.
        verify(toTransport, never()).process(any(), any());
        // ...and nothing was registered as pending, so a later push has nothing to deliver either.
        processor.sendPendingRequests(sessionId, "svc");
        verify(toTransport, never()).process(any(), any());
    }

    @Test
    public void duplicatePersistedRpcStillReturnsRpcIdToCaller() {
        mockRpcInfra();
        UUID rpcId = UUID.randomUUID();

        processor.processRpcRequest(mock(TbActorCtx.class),
                new ToDeviceRpcRequestActorMsg("svc", persistedRequest(rpcId)));
        deliverPersistResult(rpcId, 0, RpcPersistResult.DUPLICATE);

        // The caller still gets its id back: completion is keyed by rpcId and remove-once, so replying for a
        // duplicate is harmless - but NOT replying would hang the REST DeferredResult / rule-node callback.
        assertRpcIdReplied(rpcId);
    }

    @Test
    public void expiredOnArrivalRpcReturnsRpcIdToCaller() {
        mockRpcInfra(); // first delivery of a command that arrived past its expiration
        UUID rpcId = UUID.randomUUID();
        subscribeAsyncSession();

        processor.processRpcRequest(mock(TbActorCtx.class),
                new ToDeviceRpcRequestActorMsg("svc", expiredRequest(rpcId)));
        deliverPersistResult(rpcId, 0, RpcPersistResult.INSERTED);

        // The EXPIRED row is written AND the caller gets its id, so it can read that row instead of waiting
        // out the core's safety net for an opaque TIMEOUT. Never sent to the device - it is already expired.
        ArgumentCaptor<Rpc> rpcCaptor = ArgumentCaptor.forClass(Rpc.class);
        verify(rpcService).createIfAbsent(eq(tenantId), rpcCaptor.capture(), any());
        assertThat(rpcCaptor.getValue().getStatus()).isEqualTo(RpcStatus.EXPIRED);
        verify(toTransport, never()).process(any(), any());

        assertRpcIdReplied(rpcId);
    }

    @Test
    public void duplicateExpiredOnArrivalRpcIsNoOpButStillReplies() {
        mockRpcInfra();
        UUID rpcId = UUID.randomUUID();
        subscribeAsyncSession();

        processor.processRpcRequest(mock(TbActorCtx.class),
                new ToDeviceRpcRequestActorMsg("svc", expiredRequest(rpcId)));
        deliverPersistResult(rpcId, 0, RpcPersistResult.DUPLICATE);

        // Insert-if-absent turns the EXPIRED create into a no-op, so an already-SUCCESSFUL row is not clobbered.
        // The reply is NOT gated on the insert result: it carries only the id, and completion is remove-once.
        verify(rpcService).createIfAbsent(eq(tenantId), any(), any());
        verify(toTransport, never()).process(any(), any());
        assertRpcIdReplied(rpcId);
    }

    @Test
    public void arrivalSendsNothingUntilThePersistResultArrives() {
        mockRpcInfra();
        subscribeAsyncSession();

        processor.processRpcRequest(mock(TbActorCtx.class),
                new ToDeviceRpcRequestActorMsg("svc", persistedRequest(UUID.randomUUID())));

        // Persist-before-send: nothing on the wire, and no rpcId returned, until the row is durable.
        verify(toTransport, never()).process(any(), any());
        verify(coreRpcService, never()).processRpcResponseFromDeviceActor(any());
    }

    @Test
    public void subscribingDoesNotPushAnRpcWhoseRowIsNotDurableYet() {
        mockRpcInfra(); // BURST
        TbActorCtx ctx = mock(TbActorCtx.class);
        UUID rpcId = UUID.randomUUID();

        // Turn 1 only: the entry is registered, its insert has not flushed.
        processor.processRpcRequest(ctx, new ToDeviceRpcRequestActorMsg("svc", persistedRequest(rpcId)));

        // A device subscribing in that window must not be handed the command - persist-before-send - and the
        // command must not then be sent a second time by its own continuation.
        pushViaAsyncSession();
        verify(toTransport, never()).process(any(), any());

        deliverPersistResult(rpcId, 0, RpcPersistResult.INSERTED);
        assertThat(publishedRequestIds()).containsExactly(0);
    }

    @Test
    public void unpersistedHeadBlocksTheSequentialQueueInsteadOfBeingSteppedOver() {
        given(systemContext.getRpcSubmitStrategy()).willReturn("SEQUENTIAL_ON_ACK_FROM_DEVICE");
        processor = new DeviceActorMessageProcessor(systemContext, tenantId, deviceId);
        mockRpcInfra();

        // B is durable, A is not. Sending B would reorder the commands the caller submitted.
        registerEntry(0, false);
        registerEntry(1, true);

        pushViaAsyncSession();

        verify(toTransport, never()).process(any(), any());
    }

    @Test
    public void durableHeadIsSentAheadOfLaterEntries() {
        given(systemContext.getRpcSubmitStrategy()).willReturn("SEQUENTIAL_ON_ACK_FROM_DEVICE");
        processor = new DeviceActorMessageProcessor(systemContext, tenantId, deviceId);
        mockRpcInfra();

        registerEntry(0, true);
        registerEntry(1, true);

        pushViaAsyncSession();

        assertThat(publishedRequestIds()).containsExactly(0);
    }

    @Test
    public void outOfOrderPersistResultsPreserveArrivalOrderOnAckStrategy() {
        given(systemContext.getRpcSubmitStrategy()).willReturn("SEQUENTIAL_ON_ACK_FROM_DEVICE");
        processor = new DeviceActorMessageProcessor(systemContext, tenantId, deviceId);
        mockRpcInfra();
        TbActorCtx ctx = mock(TbActorCtx.class);
        subscribeAsyncSession();

        UUID rpcA = UUID.randomUUID();
        UUID rpcB = UUID.randomUUID();
        processor.processRpcRequest(ctx, new ToDeviceRpcRequestActorMsg("svc", persistedRequest(rpcA)));
        processor.processRpcRequest(ctx, new ToDeviceRpcRequestActorMsg("svc", persistedRequest(rpcB)));

        // B's insert flushes first. B is durable but is NOT at the head, so nothing may go out yet.
        deliverPersistResult(rpcB, 1, RpcPersistResult.INSERTED);
        verify(toTransport, never()).process(any(), any());

        deliverPersistResult(rpcA, 0, RpcPersistResult.INSERTED);
        assertThat(publishedRequestIds()).containsExactly(0);
    }

    @Test
    public void outOfOrderPersistResultsPreserveArrivalOrderOnResponseStrategy() {
        given(systemContext.getRpcSubmitStrategy()).willReturn("SEQUENTIAL_ON_RESPONSE_FROM_DEVICE");
        processor = new DeviceActorMessageProcessor(systemContext, tenantId, deviceId);
        mockRpcInfra();
        TbActorCtx ctx = mock(TbActorCtx.class);
        subscribeAsyncSession();

        UUID rpcA = UUID.randomUUID();
        UUID rpcB = UUID.randomUUID();
        processor.processRpcRequest(ctx, new ToDeviceRpcRequestActorMsg("svc", persistedRequest(rpcA)));
        processor.processRpcRequest(ctx, new ToDeviceRpcRequestActorMsg("svc", persistedRequest(rpcB)));

        deliverPersistResult(rpcB, 1, RpcPersistResult.INSERTED);
        verify(toTransport, never()).process(any(), any());

        deliverPersistResult(rpcA, 0, RpcPersistResult.INSERTED);
        assertThat(publishedRequestIds()).containsExactly(0);
    }

    @Test
    public void burstSendsFromItsOwnContinuationWithoutResendingOtherEntries() {
        mockRpcInfra(); // default strategy is BURST
        TbActorCtx ctx = mock(TbActorCtx.class);
        subscribeAsyncSession();

        UUID rpcA = UUID.randomUUID();
        UUID rpcB = UUID.randomUUID();
        processor.processRpcRequest(ctx, new ToDeviceRpcRequestActorMsg("svc", persistedRequest(rpcA)));
        processor.processRpcRequest(ctx, new ToDeviceRpcRequestActorMsg("svc", persistedRequest(rpcB)));

        deliverPersistResult(rpcA, 0, RpcPersistResult.INSERTED);
        deliverPersistResult(rpcB, 1, RpcPersistResult.INSERTED);

        // Each continuation sends exactly its own command. A must not be re-sent when B becomes durable.
        assertThat(publishedRequestIds()).containsExactly(0, 1);
    }

    @Test
    public void failedPersistSendsNothingRepliesNothingAndLeavesNoPendingEntry() {
        mockRpcInfra();
        TbActorCtx ctx = mock(TbActorCtx.class);
        subscribeAsyncSession();

        UUID rpcId = UUID.randomUUID();
        processor.processRpcRequest(ctx, new ToDeviceRpcRequestActorMsg("svc", persistedRequest(rpcId)));
        deliverPersistResult(rpcId, 0, RpcPersistResult.FAILED);

        verify(toTransport, never()).process(any(), any());
        verify(coreRpcService, never()).processRpcResponseFromDeviceActor(any());
        assertThat(processor.toDeviceRpcPendingMap).isEmpty();
    }

    @Test
    public void failedHeadDoesNotStallTheSequentialQueue() {
        given(systemContext.getRpcSubmitStrategy()).willReturn("SEQUENTIAL_ON_ACK_FROM_DEVICE");
        processor = new DeviceActorMessageProcessor(systemContext, tenantId, deviceId);
        mockRpcInfra();
        TbActorCtx ctx = mock(TbActorCtx.class);
        subscribeAsyncSession();

        UUID rpcA = UUID.randomUUID();
        UUID rpcB = UUID.randomUUID();
        processor.processRpcRequest(ctx, new ToDeviceRpcRequestActorMsg("svc", persistedRequest(rpcA)));
        processor.processRpcRequest(ctx, new ToDeviceRpcRequestActorMsg("svc", persistedRequest(rpcB)));

        // A held the head from arrival. Dropping it must advance the queue, or B waits out its own expiry.
        deliverPersistResult(rpcA, 0, RpcPersistResult.FAILED);
        deliverPersistResult(rpcB, 1, RpcPersistResult.INSERTED);

        assertThat(publishedRequestIds()).containsExactly(1);
    }

    @Test
    public void actorRestartBetweenTurnsStillSendsTheDurableRow() {
        mockRpcInfra();
        UUID rpcId = UUID.randomUUID();

        processor.processRpcRequest(mock(TbActorCtx.class),
                new ToDeviceRpcRequestActorMsg("svc", persistedRequest(rpcId)));
        verify(toTransport, never()).process(any(), any());

        // Evicted before turn 2, so the self-tell is dropped - the reload path must deliver it instead.
        processor = new DeviceActorMessageProcessor(systemContext, tenantId, deviceId);
        stubInFlight(inFlightRow(RpcStatus.QUEUED, 0, System.currentTimeMillis()));
        processor.init(mock(TbActorCtx.class));

        pushViaAsyncSession();

        assertThat(publishedRequestIds()).containsExactly(0);
    }

    // Keyed by requestId so a test can resolve them in any order, as batch flushes complete.
    private final Map<Integer, Consumer<RpcPersistResult>> continuations = new LinkedHashMap<>();

    private void mockRpcInfra() {
        rpcService = mock(TbRpcService.class);
        continuations.clear();
        doAnswer(invocation -> {
            Rpc rpc = invocation.getArgument(1);
            continuations.put(rpc.getRequestId(), invocation.getArgument(2));
            return null;
        }).when(rpcService).createIfAbsent(any(), any(), any());
        given(systemContext.getTbRpcService()).willReturn(rpcService);
        coreRpcService = mock(TbCoreDeviceRpcService.class);
        given(systemContext.getTbCoreDeviceRpcService()).willReturn(coreRpcService);
        given(systemContext.getServiceId()).willReturn("svc");
        toTransport = mock(TbCoreToTransportService.class);
        given(systemContext.getTbCoreToTransportService()).willReturn(toTransport);
    }

    // Delivers turn 2, as the batch-flush callback would. Fails if turn 1 never enqueued a create.
    private void deliverPersistResult(UUID rpcId, int requestId, RpcPersistResult result) {
        continuations.get(requestId).accept(result);
        processor.processRpcPersistResult(new RpcPersistResultActorMsg(rpcId, requestId, result));
    }

    // Registers a pending entry directly; persisted=false models a create still queued for its batch insert.
    private void registerEntry(int requestId, boolean persisted) {
        ToDeviceRpcRequestActorMsg actorMsg =
                new ToDeviceRpcRequestActorMsg("svc", persistedRequest(UUID.randomUUID()));
        ToDeviceRpcRequestMetadata md =
                new ToDeviceRpcRequestMetadata(actorMsg, System.currentTimeMillis());
        md.setPersisted(persisted);
        processor.toDeviceRpcPendingMap.put(requestId, md);
    }

    // The reload issues a single findInFlightForReload query (DB-side filters out one-way DELIVERED and
    // terminal statuses - see JpaRpcDaoTest); the actor sorts the rows itself, so the stub just returns
    // them all in one page regardless of order.
    private void stubInFlight(Rpc... rows) {
        given(rpcService.findInFlightForReload(eq(tenantId), eq(deviceId), any()))
                .willReturn(new PageData<>(List.of(rows), 1, 0, false));
    }

    private void pushViaAsyncSession() {
        processor.sendPendingRequests(subscribeAsyncSession(), "svc");
    }

    private UUID subscribeAsyncSession() {
        UUID sessionId = UUID.randomUUID();
        SessionInfo sessionInfo = new SessionInfo(SessionType.ASYNC, "svc");
        processor.sessions.put(sessionId, new SessionInfoMetaData(sessionInfo));
        processor.rpcSubscriptions.put(sessionId, sessionInfo);
        return sessionId;
    }

    private ToDeviceRpcRequest persistedRequest(UUID rpcId) {
        return persistedRequest(rpcId, System.currentTimeMillis() + 60_000);
    }

    private ToDeviceRpcRequest persistedRequest(UUID rpcId, long expirationTime) {
        return persistedRequest(rpcId, expirationTime, false);
    }

    private ToDeviceRpcRequest persistedRequest(UUID rpcId, long expirationTime, boolean oneway) {
        return new ToDeviceRpcRequest(rpcId, tenantId, deviceId, oneway, expirationTime,
                new ToDeviceRpcRequestBody("m", "{}"), true, null, null); // persisted=true
    }

    private ToDeviceRpcRequest nonPersistedRequest(UUID rpcId) {
        return nonPersistedRequest(rpcId, System.currentTimeMillis() + 60_000, false);
    }

    private ToDeviceRpcRequest nonPersistedRequest(UUID rpcId, long expirationTime, boolean oneway) {
        return new ToDeviceRpcRequest(rpcId, tenantId, deviceId, oneway, expirationTime,
                new ToDeviceRpcRequestBody("m", "{}"), false, null, null); // persisted=false
    }

    private ToDeviceRpcRequest expiredRequest(UUID rpcId) {
        // expirationTime already in the past -> the actor's `timeout <= 0` branch
        return persistedRequest(rpcId, System.currentTimeMillis() - 1);
    }

    private void assertRpcIdReplied(UUID rpcId) {
        ArgumentCaptor<FromDeviceRpcResponse> captor = ArgumentCaptor.forClass(FromDeviceRpcResponse.class);
        verify(coreRpcService).processRpcResponseFromDeviceActor(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(rpcId);
        assertThat(JacksonUtil.toJsonNode(captor.getValue().getResponse().orElseThrow()).get("rpcId").asText())
                .isEqualTo(rpcId.toString());
    }

    private List<Integer> publishedRequestIds() {
        ArgumentCaptor<ToTransportMsg> captor = ArgumentCaptor.forClass(ToTransportMsg.class);
        verify(toTransport, atLeastOnce()).process(any(), captor.capture());
        return captor.getAllValues().stream().map(m -> m.getToDeviceRequest().getRequestId()).toList();
    }

    private Rpc inFlightRow(RpcStatus status, int requestId, long createdTime) {
        return inFlightRow(status, requestId, createdTime, false);
    }

    private Rpc inFlightRow(RpcStatus status, int requestId, long createdTime, boolean oneway) {
        long exp = System.currentTimeMillis() + 60_000;
        Rpc rpc = row(UUID.randomUUID(), status, createdTime, exp, oneway);
        rpc.setRequestId(requestId);
        return rpc;
    }

    private Rpc expiredRow(RpcStatus status, int requestId, boolean oneway, long pastExp) {
        Rpc rpc = row(UUID.randomUUID(), status, System.currentTimeMillis() - 120_000, pastExp, oneway);
        rpc.setRequestId(requestId);
        return rpc;
    }

    private Rpc legacyQueuedRow(long createdTime) {
        Rpc rpc = row(UUID.randomUUID(), RpcStatus.QUEUED, createdTime, System.currentTimeMillis() + 60_000, false);
        rpc.setRequestId(null); // pre-migration row: no persisted requestId
        return rpc;
    }

    private Rpc row(UUID rpcUuid, RpcStatus status, long createdTime, long expirationTime, boolean oneway) {
        ToDeviceRpcRequest req = persistedRequest(rpcUuid, expirationTime, oneway);
        Rpc rpc = new Rpc(new RpcId(rpcUuid));
        rpc.setCreatedTime(createdTime);
        rpc.setExpirationTime(expirationTime);
        rpc.setStatus(status);
        rpc.setRequest(JacksonUtil.valueToTree(req));
        return rpc;
    }

    private SessionInfoProto sessionInfoProto() {
        UUID sid = UUID.randomUUID();
        return SessionInfoProto.newBuilder()
                .setNodeId("svc")
                .setSessionIdMSB(sid.getMostSignificantBits())
                .setSessionIdLSB(sid.getLeastSignificantBits())
                .build();
    }
}
