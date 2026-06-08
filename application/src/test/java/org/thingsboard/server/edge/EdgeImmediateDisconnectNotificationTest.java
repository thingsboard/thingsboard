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

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// Covers the disconnect_notification_delay_ms <= 0 branch in EdgeGrpcService.scheduleDisconnectNotification,
// where the "disconnected" notification must be sent immediately instead of being scheduled with a delay.
@DaoSqlTest
@TestPropertySource(properties = {
        "edges.connectivity.disconnect_notification_delay_ms=0"
})
public class EdgeImmediateDisconnectNotificationTest extends AbstractEdgeTest {

    @MockitoSpyBean
    private NotificationRuleProcessor notificationRuleProcessor;

    @Test
    public void givenZeroDelay_whenEdgeDisconnects_thenDisconnectNotificationSentImmediately() throws Exception {
        clearInvocations(notificationRuleProcessor);

        edgeImitator.disconnect();

        // With a zero delay there is no debounce window - the "disconnected" notification fires right away.
        await().atMost(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS).untilAsserted(() ->
                verify(notificationRuleProcessor, times(1)).process(argThat(edgeConnectionTrigger())));
    }

    private ArgumentMatcher<NotificationRuleTrigger> edgeConnectionTrigger() {
        return trigger -> trigger instanceof EdgeConnectionTrigger edgeTrigger
                && edge.getId().equals(edgeTrigger.getEdgeId())
                && !edgeTrigger.isConnected();
    }

}
