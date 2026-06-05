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

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.notification.rule.trigger.EdgeConnectionTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTrigger;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.controller.AbstractWebTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.edge.imitator.EdgeImitator;
import org.thingsboard.server.gen.edge.v1.OAuth2ClientUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OAuth2DomainUpdateMsg;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DaoSqlTest
@TestPropertySource(properties = {
        "edges.connectivity.disconnect_notification_delay_ms=5000"
})
public class EdgeConnectionNotificationTest extends AbstractEdgeTest {

    private static final long DELAY_MS = 5000L;

    @MockitoSpyBean
    private NotificationRuleProcessor notificationRuleProcessor;

    @Test
    public void givenEdgeStaysDisconnected_whenDelayElapses_thenDisconnectNotificationSent() throws Exception {
        clearInvocations(notificationRuleProcessor);

        edgeImitator.disconnect();

        // After the configured delay, the "disconnected" notification is sent exactly once.
        await().atMost(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS).untilAsserted(() ->
                verify(notificationRuleProcessor, times(1)).process(argThat(edgeConnectionTrigger(false))));
    }

    @Test
    public void givenEdgeReconnectsWithinDelay_whenEdgeFlaps_thenDisconnectNotificationSuppressed() throws Exception {
        clearInvocations(notificationRuleProcessor);

        // Edge drops...
        edgeImitator.disconnect();
        // Ensure the server processed the disconnect (and scheduled the delayed notification) before reconnecting.
        TimeUnit.SECONDS.sleep(1);

        // ...and reconnects within the delay window, which must cancel the pending "disconnected" notification.
        EdgeImitator reconnected = new EdgeImitator(EDGE_HOST, EDGE_PORT, edge.getRoutingKey(), edge.getSecret());
        reconnected.ignoreType(OAuth2ClientUpdateMsg.class);
        reconnected.ignoreType(OAuth2DomainUpdateMsg.class);
        reconnected.connect();
        edgeImitator = reconnected; // let teardown clean up the live session

        // The "connected" notification still fires immediately on reconnect (we suppress the disconnect only).
        await().atMost(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS).untilAsserted(() ->
                verify(notificationRuleProcessor, atLeastOnce()).process(argThat(edgeConnectionTrigger(true))));

        // Wait until the original disconnect-notification window has fully elapsed...
        TimeUnit.MILLISECONDS.sleep(DELAY_MS + 1000);

        // ...the "disconnected" notification must have never been sent.
        verify(notificationRuleProcessor, never()).process(argThat(edgeConnectionTrigger(false)));
    }

    private ArgumentMatcher<NotificationRuleTrigger> edgeConnectionTrigger(boolean connected) {
        return trigger -> trigger instanceof EdgeConnectionTrigger edgeTrigger
                && edge.getId().equals(edgeTrigger.getEdgeId())
                && edgeTrigger.isConnected() == connected;
    }

}
