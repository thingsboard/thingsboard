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
package org.thingsboard.server.edge;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.notification.rule.trigger.EdgeConnectionTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTrigger;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.controller.AbstractWebTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.edge.imitator.EdgeImitator;
import org.thingsboard.server.service.edge.rpc.EdgeGrpcService;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DaoSqlTest
public class EdgeConnectionNotificationTest extends AbstractEdgeTest {

    private static final long DELAY_MS = 1500L;

    @MockitoSpyBean
    private NotificationRuleProcessor notificationRuleProcessor;

    @Autowired
    private EdgeGrpcService edgeGrpcService;

    private long originalDisconnectNotificationDelayMs;

    // Capture the bean's configured delay before each test and restore it after, so a test method that forgets
    // to call setDisconnectNotificationDelayMs can't silently inherit the previous method's mutated value
    // (the shared context means the mutation would otherwise persist across methods).
    @Before
    public void captureDisconnectNotificationDelay() {
        originalDisconnectNotificationDelayMs = (long) ReflectionTestUtils.getField(edgeGrpcService, "disconnectNotificationDelayMs");
    }

    @After
    public void restoreDisconnectNotificationDelay() {
        setDisconnectNotificationDelayMs(originalDisconnectNotificationDelayMs);
    }

    // The delay is overridden per test (rather than via a per-class @TestPropertySource) so all cases share a
    // single Spring application context instead of booting a separate heavy context per delay value.
    private void setDisconnectNotificationDelayMs(long delayMs) {
        ReflectionTestUtils.setField(edgeGrpcService, "disconnectNotificationDelayMs", delayMs);
    }

    @Test
    public void givenEdgeStaysDisconnected_whenDelayElapses_thenDisconnectNotificationSent() throws Exception {
        // After the configured delay, the "disconnected" notification is sent exactly once.
        assertDisconnectNotificationSentOnce(DELAY_MS);
    }

    @Test
    public void givenZeroDelay_whenEdgeDisconnects_thenDisconnectNotificationSentImmediately() throws Exception {
        // With a zero delay there is no debounce window - the "disconnected" notification fires right away.
        assertDisconnectNotificationSentOnce(0);
    }

    private void assertDisconnectNotificationSentOnce(long delayMs) throws Exception {
        setDisconnectNotificationDelayMs(delayMs);
        clearInvocations(notificationRuleProcessor);

        edgeImitator.disconnect();

        await().atMost(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS).untilAsserted(() ->
                verify(notificationRuleProcessor, times(1)).process(argThat(edgeConnectionTrigger(false))));
    }

    @Test
    public void givenEdgeReconnectsWithinDelay_whenEdgeFlaps_thenDisconnectNotificationSuppressed() throws Exception {
        setDisconnectNotificationDelayMs(DELAY_MS);
        clearInvocations(notificationRuleProcessor);

        // Edge drops...
        edgeImitator.disconnect();
        // Ensure the server processed the disconnect (and scheduled the delayed notification) before reconnecting.
        TimeUnit.SECONDS.sleep(1);

        // ...and reconnects within the delay window, which must cancel the pending "disconnected" notification.
        EdgeImitator reconnected = createEdgeImitator();
        reconnected.connect();
        edgeImitator = reconnected; // let teardown clean up the live session

        // The "connected" notification still fires immediately on reconnect (we suppress the disconnect only).
        await().atMost(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS).untilAsserted(() ->
                verify(notificationRuleProcessor, atLeastOnce()).process(argThat(edgeConnectionTrigger(true))));

        // The "disconnected" notification must never be sent throughout the full delay window.
        await().during(DELAY_MS + 500, TimeUnit.MILLISECONDS)
                .atMost(DELAY_MS + 2000, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> verify(notificationRuleProcessor, never()).process(argThat(edgeConnectionTrigger(false))));
    }

    private ArgumentMatcher<NotificationRuleTrigger> edgeConnectionTrigger(boolean connected) {
        return trigger -> trigger instanceof EdgeConnectionTrigger edgeTrigger
                && edge.getId().equals(edgeTrigger.getEdgeId())
                && edgeTrigger.isConnected() == connected;
    }

}
