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
package org.thingsboard.server.controller.plugin;

import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.SendHandler;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.NativeWebSocketSession;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.ws.WebSocketSessionRef;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
class TbWebSocketHandlerTest {

    TbWebSocketHandler wsHandler;
    NativeWebSocketSession session;
    Session nativeSession;
    RemoteEndpoint.Async asyncRemote;
    WebSocketSessionRef sessionRef;
    int maxMsgQueuePerSession;
    TbWebSocketHandler.SessionMetaData sendHandler;
    ExecutorService executor;

    @BeforeEach
    void setUp() throws IOException {
        maxMsgQueuePerSession = 100;
        executor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName(getClass().getSimpleName()));
        wsHandler = spy(new TbWebSocketHandler());
        willDoNothing().given(wsHandler).close(any(), any());
        session = mock(NativeWebSocketSession.class);
        nativeSession = mock(Session.class);
        willReturn(nativeSession).given(session).getNativeSession(Session.class);
        asyncRemote = mock(RemoteEndpoint.Async.class);
        willReturn(asyncRemote).given(nativeSession).getAsyncRemote();
        sessionRef = mock(WebSocketSessionRef.class, Mockito.RETURNS_DEEP_STUBS); //prevent NPE on logs
        TbWebSocketHandler.SessionMetaData sessionMd = wsHandler.new SessionMetaData(session, sessionRef);
        sessionMd.setMaxMsgQueueSize(maxMsgQueuePerSession);
        sendHandler = spy(sessionMd);
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void sendHandler_sendMsg_parallel_no_race() throws InterruptedException {
        CountDownLatch finishLatch = new CountDownLatch(maxMsgQueuePerSession * 2);
        AtomicInteger sendersCount = new AtomicInteger();
        willAnswer(invocation -> {
            assertThat(sendersCount.incrementAndGet()).as("no race").isEqualTo(1);
            String text = invocation.getArgument(0);
            SendHandler onResultHandler = invocation.getArgument(1);
            SendResult sendResult = new SendResult();
            executor.submit(() -> {
                sendersCount.decrementAndGet();
                onResultHandler.onResult(sendResult);
                finishLatch.countDown();
            });
            return null;
        }).given(asyncRemote).sendText(anyString(), any());

        assertThat(sendHandler.isSending.get()).as("sendHandler not is in sending state").isFalse();
        //first batch
        IntStream.range(0, maxMsgQueuePerSession).parallel().forEach(i -> sendHandler.sendMsg("hello " + i));
        Awaitility.await("first batch processed").atMost(30, TimeUnit.SECONDS).until(() -> finishLatch.getCount() == maxMsgQueuePerSession);
        assertThat(sendHandler.isSending.get()).as("sendHandler not is in sending state").isFalse();
        //second batch - to test pause between big msg batches
        IntStream.range(100, 100 + maxMsgQueuePerSession).parallel().forEach(i -> sendHandler.sendMsg("hello " + i));
        assertThat(finishLatch.await(30, TimeUnit.SECONDS)).as("all callbacks fired").isTrue();

        verify(sendHandler, never()).closeSession(any());
        verify(sendHandler, times(maxMsgQueuePerSession * 2)).onResult(any());
        assertThat(sendHandler.isSending.get()).as("sendHandler not is in sending state").isFalse();
    }

    @Test
    void sendHandler_sendMsg_message_order() throws InterruptedException {
        CountDownLatch finishLatch = new CountDownLatch(maxMsgQueuePerSession);
        Collection<String> outputs = new ConcurrentLinkedQueue<>();
        willAnswer(invocation -> {
            String text = invocation.getArgument(0);
            outputs.add(text);
            SendHandler onResultHandler = invocation.getArgument(1);
            SendResult sendResult = new SendResult();
            executor.submit(() -> {
                onResultHandler.onResult(sendResult);
                finishLatch.countDown();
            });
            return null;
        }).given(asyncRemote).sendText(anyString(), any());

        List<String> inputs = IntStream.range(0, maxMsgQueuePerSession).mapToObj(i -> "msg " + i).collect(Collectors.toList());
        inputs.forEach(s -> sendHandler.sendMsg(s));

        assertThat(finishLatch.await(30, TimeUnit.SECONDS)).as("all callbacks fired").isTrue();
        assertThat(outputs).as("inputs exactly the same as outputs").containsExactlyElementsOf(inputs);

        verify(sendHandler, never()).closeSession(any());
        verify(sendHandler, times(maxMsgQueuePerSession)).onResult(any());
    }

    @Test
    void sendHandler_sendMsg_queue_size_exceed() {
        willDoNothing().given(asyncRemote).sendText(anyString(), any()); // send text will never call back, so queue will grow each sendMsg
        sendHandler.sendMsg("first message to stay in-flight all the time during this test");
        IntStream.range(0, maxMsgQueuePerSession).parallel().forEach(i -> sendHandler.sendMsg("hello " + i));
        verify(sendHandler, never()).closeSession(any());
        sendHandler.sendMsg("excessive message");
        verify(sendHandler, times(1)).closeSession(eq(new CloseStatus(1008, "Max pending updates limit reached!")));
        verify(asyncRemote, times(1)).sendText(anyString(), any());
    }

    @Test
    void sendHandler_onMsg_allProcessed() throws Exception {
        Deque<String> msgs = new ConcurrentLinkedDeque<>();
        doAnswer(inv -> msgs.add(inv.getArgument(1))).when(wsHandler).processMsg(any(), any());
        for (int i = 0; i < 100; i++) {
            String msg = String.valueOf(i);
            executor.submit(() -> {
                try {
                    Thread.sleep(new Random().nextInt(50));
                    sendHandler.onMsg(msg);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(msgs).map(Integer::parseInt).doesNotHaveDuplicates().hasSize(100);
    }

    // Regression test for the bug where publicUserSessionsMap was keyed by UserId(NULL_UUID),
    // making maxWsSessionsPerPublicUser a global limit shared across all tenants.
    // The limit is now scoped per-tenant.
    @Test
    void checkLimits_publicUserSessions_limitIsPerTenantNotGlobal() throws Exception {
        TbTenantProfileCache tenantProfileCache = mock(TbTenantProfileCache.class);
        ReflectionTestUtils.setField(wsHandler, "tenantProfileCache", tenantProfileCache);

        int maxPublicSessions = 2;

        TenantId tenant1 = TenantId.fromUUID(UUID.randomUUID());
        TenantProfile profile1 = new TenantProfile();
        profile1.createDefaultTenantProfileData();
        profile1.getDefaultProfileConfiguration().setMaxWsSessionsPerPublicUser(maxPublicSessions);
        willReturn(profile1).given(tenantProfileCache).get(tenant1);

        TenantId tenant2 = TenantId.fromUUID(UUID.randomUUID());
        TenantProfile profile2 = new TenantProfile();
        profile2.createDefaultTenantProfileData();
        profile2.getDefaultProfileConfiguration().setMaxWsSessionsPerPublicUser(maxPublicSessions);
        willReturn(profile2).given(tenantProfileCache).get(tenant2);

        Method checkLimits = TbWebSocketHandler.class.getDeclaredMethod(
                "checkLimits", WebSocketSession.class, WebSocketSessionRef.class);
        checkLimits.setAccessible(true);

        // tenant1 fills up its limit
        for (int i = 0; i < maxPublicSessions; i++) {
            assertThat((boolean) checkLimits.invoke(wsHandler, mockWsSession("t1-" + i), mockPublicSessionRef(tenant1))).isTrue();
        }

        // tenant2 must get its own independent quota — this was the bug: with NULL_UUID as key
        // all tenants shared one global counter, so tenant2 would be blocked here
        for (int i = 0; i < maxPublicSessions; i++) {
            assertThat((boolean) checkLimits.invoke(wsHandler, mockWsSession("t2-" + i), mockPublicSessionRef(tenant2)))
                    .as("tenant2 session %d should not be affected by tenant1's sessions", i + 1)
                    .isTrue();
        }

        // tenant1's (maxPublicSessions + 1)-th session must be rejected
        NativeWebSocketSession overLimit = mockWsSession("t1-over");
        assertThat((boolean) checkLimits.invoke(wsHandler, overLimit, mockPublicSessionRef(tenant1))).isFalse();
        verify(overLimit).close(CloseStatus.POLICY_VIOLATION.withReason("Max public user sessions limit reached"));
    }

    private NativeWebSocketSession mockWsSession(String id) {
        NativeWebSocketSession s = mock(NativeWebSocketSession.class);
        willReturn(id).given(s).getId();
        return s;
    }

    private WebSocketSessionRef mockPublicSessionRef(TenantId tenantId) {
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        SecurityUser securityUser = mock(SecurityUser.class);
        willReturn(tenantId).given(securityUser).getTenantId();
        willReturn(customerId).given(securityUser).getCustomerId();
        willReturn(new UserId(EntityId.NULL_UUID)).given(securityUser).getId();
        willReturn(true).given(securityUser).isCustomerUser();
        willReturn(new UserPrincipal(UserPrincipal.Type.PUBLIC_ID, customerId.toString())).given(securityUser).getUserPrincipal();

        WebSocketSessionRef ref = mock(WebSocketSessionRef.class);
        willReturn(securityUser).given(ref).getSecurityCtx();
        willReturn(UUID.randomUUID().toString()).given(ref).getSessionId();
        return ref;
    }

}
