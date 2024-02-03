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
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class TelemetryEdgeProcessorTest {

    @Test
    public void testConvert_maxSizeLimit() throws Exception {
        EdgeEvent edgeEvent = new EdgeEvent();
        ObjectNode body = JacksonUtil.newObjectNode();
        body.put("value", StringUtils.randomAlphanumeric(10000));
        edgeEvent.setBody(body);
        DownlinkMsg downlinkMsg = new TelemetryEdgeProcessor().convertTelemetryEventToDownlink(edgeEvent);
        Assert.assertNull(downlinkMsg);
    }
}
