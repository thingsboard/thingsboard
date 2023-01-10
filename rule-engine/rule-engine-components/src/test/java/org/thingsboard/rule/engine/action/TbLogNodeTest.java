/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.rule.engine.action;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class TbLogNodeTest {

    @Test
    void givenMsg_whenToLog_thenReturnString() {
        TbLogNode node = new TbLogNode();
        String data = "{\"key\": \"value\"}";
        TbMsgMetaData metaData = new TbMsgMetaData(Map.of("mdKey1", "mdValue1", "mdKey2", "23"));
        TbMsg msg = TbMsg.newMsg("POST_TELEMETRY", TenantId.SYS_TENANT_ID, metaData, data);

        String logMessage = node.toLogMessage(msg);
        log.info(logMessage);

        assertThat(logMessage).isEqualTo("\n" +
                "Incoming message:\n" +
                "{\"key\": \"value\"}\n" +
                "Incoming metadata:\n" +
                "{\"mdKey1\":\"mdValue1\",\"mdKey2\":\"23\"}");
    }

    @Test
    void givenEmptyDataMsg_whenToLog_thenReturnString() {
        TbLogNode node = new TbLogNode();
        TbMsgMetaData metaData = new TbMsgMetaData(Collections.emptyMap());
        TbMsg msg = TbMsg.newMsg("POST_TELEMETRY", TenantId.SYS_TENANT_ID, metaData, "");

        String logMessage = node.toLogMessage(msg);
        log.info(logMessage);

        assertThat(logMessage).isEqualTo("\n" +
                "Incoming message:\n" +
                "\n" +
                "Incoming metadata:\n" +
                "{}");
    }
    @Test
    void givenNullDataMsg_whenToLog_thenReturnString() {
        TbLogNode node = new TbLogNode();
        TbMsgMetaData metaData = new TbMsgMetaData(Collections.emptyMap());
        TbMsg msg = TbMsg.newMsg("POST_TELEMETRY", TenantId.SYS_TENANT_ID, metaData, null);

        String logMessage = node.toLogMessage(msg);
        log.info(logMessage);

        assertThat(logMessage).isEqualTo("\n" +
                "Incoming message:\n" +
                "null\n" +
                "Incoming metadata:\n" +
                "{}");
    }

}
