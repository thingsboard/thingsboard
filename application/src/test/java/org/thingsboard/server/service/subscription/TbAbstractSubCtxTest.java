// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbAbstractSubCtxTest {

    private static final String SESSION_ID = "session-1";
    private static final int CMD_ID = 7;

    private final WebSocketService wsService = mock(WebSocketService.class);
    private final TbLocalSubscriptionService localSubscriptionService = mock(TbLocalSubscriptionService.class);
    private final WebSocketSessionRef sessionRef = mock(WebSocketSessionRef.class);
    private final TbSubscription<?> subscription = mock(TbSubscription.class);

    private TbAbstractSubCtx subCtx;

    @BeforeEach
    public void setUp() {
        lenient().when(sessionRef.getSessionId()).thenReturn(SESSION_ID);
        subCtx = new TbAbstractSubCtx("service", wsService, localSubscriptionService, mock(SubscriptionServiceStatistics.class),
                sessionRef, CMD_ID) {
            @Override
            public boolean isDynamic() {
                return false;
            }
        };
    }

    @Test
    public void registersSubscriptionWhenContextActiveAndSessionOpen() {
        when(wsService.hasSession(SESSION_ID)).thenReturn(true);

        subCtx.registerSubscription(subscription);

        verify(localSubscriptionService).addSubscription(subscription, sessionRef);
    }

    @Test
    public void doesNotRegisterSubscriptionAfterStop() {
        subCtx.stop();

        subCtx.registerSubscription(subscription);

        verify(localSubscriptionService, never()).addSubscription(any(), any());
    }

    @Test
    public void doesNotRegisterSubscriptionForSessionUnknownToWebSocketService() {
        when(wsService.hasSession(SESSION_ID)).thenReturn(false);

        subCtx.registerSubscription(subscription);

        verify(localSubscriptionService, never()).addSubscription(any(), any());
    }

    @Test
    public void stopWaitsForInFlightRegistration() throws Exception {
        when(wsService.hasSession(SESSION_ID)).thenReturn(true);
        CountDownLatch registrationEntered = new CountDownLatch(1);
        CountDownLatch releaseRegistration = new CountDownLatch(1);
        willAnswer(invocation -> {
            registrationEntered.countDown();
            releaseRegistration.await(10, TimeUnit.SECONDS);
            return null;
        }).given(localSubscriptionService).addSubscription(any(), any());

        Thread registrar = new Thread(() -> subCtx.registerSubscription(subscription));
        registrar.start();
        assertThat(registrationEntered.await(10, TimeUnit.SECONDS)).isTrue();

        Thread stopper = new Thread(subCtx::stop);
        stopper.start();
        long deadline = System.currentTimeMillis() + 10_000;
        while (!subCtx.lifecycleLock.hasQueuedThreads() && System.currentTimeMillis() < deadline) {
            Thread.onSpinWait();
        }
        assertThat(subCtx.lifecycleLock.hasQueuedThreads()).as("stop() must queue on the lifecycle lock").isTrue();
        assertThat(subCtx.isStopped()).as("stop() must not mark the context stopped while a registration holds the lock").isFalse();

        releaseRegistration.countDown();
        registrar.join(10_000);
        stopper.join(10_000);

        assertThat(subCtx.isStopped()).isTrue();

        subCtx.registerSubscription(subscription);
        verify(localSubscriptionService, times(1)).addSubscription(any(), any());
    }

}
