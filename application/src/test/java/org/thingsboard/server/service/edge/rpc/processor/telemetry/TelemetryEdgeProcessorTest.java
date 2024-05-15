/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.telemetry;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessorTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TelemetryEdgeProcessor.class})
@TestPropertySource(properties = {
        "edges.rpc.max_telemetry_message_size=1000"
})
public class TelemetryEdgeProcessorTest extends BaseEdgeProcessorTest {

    @SpyBean
    private TelemetryEdgeProcessor telemetryEdgeProcessor;

    @MockBean
    private NotificationRuleProcessor notificationRuleProcessor;

    @Test
    public void testConvert_maxSizeLimit() {
        Edge edge = new Edge();
        EdgeEvent edgeEvent = new EdgeEvent();
        ObjectNode body = JacksonUtil.newObjectNode();
        body.put("value", StringUtils.randomAlphanumeric(1000));
        edgeEvent.setBody(body);

        DownlinkMsg downlinkMsg = telemetryEdgeProcessor.convertTelemetryEventToDownlink(edge, edgeEvent);
        Assert.assertNull(downlinkMsg);

        verify(notificationRuleProcessor, Mockito.times(1)).process(any());
    }

}
