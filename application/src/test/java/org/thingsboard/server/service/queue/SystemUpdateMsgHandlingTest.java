// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.queue;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.gen.transport.TransportProtos.SystemUpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SystemUpdateType;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class SystemUpdateMsgHandlingTest {

    /**
     * The handler with its executor replaced by one that runs the submitted work inline, so the test asserts
     * what was handed off rather than thread timing.
     */
    private static SystemUpdateMsgHandler handlerRunningInline(SubscriptionService subscriptionService) {
        return handlerWith(subscriptionService, MoreExecutors.newDirectExecutorService());
    }

    private static SystemUpdateMsgHandler handlerWith(SubscriptionService subscriptionService, ExecutorService executor) {
        SystemUpdateMsgHandler handler = new SystemUpdateMsgHandler(subscriptionService);
        ReflectionTestUtils.setField(handler, "reconvergeExecutor", executor);
        return handler;
    }

    @Test
    public void licenseReconvergesTheNode() {
        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        TbCallback callback = mock(TbCallback.class);

        handlerRunningInline(subscriptionService)
                .handle(SystemUpdateMsg.newBuilder().setType(SystemUpdateType.LICENSE).build(), callback);

        verify(subscriptionService).reconcileLicenseState();
        verify(callback).onSuccess();
    }

    @Test
    public void licenseIsAckedWithoutWaitingForTheReconvergence() {
        // Reconvergence makes a 20s-timeout HTTP call to the licence portal. The consumer thread must not be
        // inside it, and the pack must not wait it out - the ack means "accepted", and reconvergence is
        // idempotent.
        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        TbCallback callback = mock(TbCallback.class);
        ExecutorService neverRuns = mock(ExecutorService.class);

        handlerWith(subscriptionService, neverRuns)
                .handle(SystemUpdateMsg.newBuilder().setType(SystemUpdateType.LICENSE).build(), callback);

        verify(callback).onSuccess();
        verifyNoInteractions(subscriptionService);
    }

    @Test
    public void aFailedReconvergenceDoesNotFailTheMessage() {
        // The five-minute tick is the backstop. Nacking would redeliver a message whose work is already
        // scheduled and whose failure is not the message's fault.
        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        doThrow(new RuntimeException("portal down")).when(subscriptionService).reconcileLicenseState();
        TbCallback callback = mock(TbCallback.class);

        handlerRunningInline(subscriptionService)
                .handle(SystemUpdateMsg.newBuilder().setType(SystemUpdateType.LICENSE).build(), callback);

        verify(callback).onSuccess();
        verify(callback, never()).onFailure(any());
    }

    @Test
    public void anUnrecognisedTypeIsIgnoredButStillAcked() {
        // The case that actually happens on a rolling upgrade is the FORWARD one: a newer node broadcasting a
        // type this build does not have, which protobuf-java answers getType() with UNRECOGNIZED. A node too
        // old to know SystemUpdateMsg at all reaches its consumer's trailing else instead.
        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        TbCallback callback = mock(TbCallback.class);

        handlerRunningInline(subscriptionService)
                .handle(SystemUpdateMsg.newBuilder().setTypeValue(9999).build(), callback);

        verifyNoInteractions(subscriptionService);
        verify(callback).onSuccess();
    }

    @Test
    public void theCoreConsumerRoutesTheLicenseSignalToTheHandler() {
        // The branch has to sit ahead of the trailing else that acks unrecognised notifications, or every
        // licence broadcast is silently dropped with nothing logged above trace.
        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        TbCallback callback = mock(TbCallback.class);
        DefaultTbCoreConsumerService consumer = mock(DefaultTbCoreConsumerService.class);
        ReflectionTestUtils.setField(consumer, "systemUpdateMsgHandler", handlerRunningInline(subscriptionService));

        UUID id = UUID.randomUUID();
        TbProtoQueueMsg<ToCoreNotificationMsg> msg = new TbProtoQueueMsg<>(id, ToCoreNotificationMsg.newBuilder()
                .setSystemUpdateMsg(SystemUpdateMsg.newBuilder().setType(SystemUpdateType.LICENSE))
                .build());
        doCallRealMethod().when(consumer).handleNotification(id, msg, callback);

        consumer.handleNotification(id, msg, callback);

        verify(subscriptionService).reconcileLicenseState();
        verify(callback).onSuccess();
    }

    @Test
    public void theRuleEngineConsumerRoutesTheLicenseSignalToTheHandler() {
        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        TbCallback callback = mock(TbCallback.class);
        DefaultTbRuleEngineConsumerService consumer = mock(DefaultTbRuleEngineConsumerService.class);
        ReflectionTestUtils.setField(consumer, "systemUpdateMsgHandler", handlerRunningInline(subscriptionService));

        UUID id = UUID.randomUUID();
        TbProtoQueueMsg<ToRuleEngineNotificationMsg> msg = new TbProtoQueueMsg<>(id, ToRuleEngineNotificationMsg.newBuilder()
                .setSystemUpdateMsg(SystemUpdateMsg.newBuilder().setType(SystemUpdateType.LICENSE))
                .build());
        doCallRealMethod().when(consumer).handleNotification(id, msg, callback);

        consumer.handleNotification(id, msg, callback);

        verify(subscriptionService).reconcileLicenseState();
        verify(callback).onSuccess();
    }
}
