/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.rule.engine.mail;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TbMsgToEmailNodeTest {

    private TbMsgToEmailNode emailNode;

    @Mock
    private TbContext ctx;

    private EntityId originator = new DeviceId(UUIDs.timeBased());
    private TbMsgMetaData metaData = new TbMsgMetaData();
    private String rawJson = "{\"name\": \"temp\", \"passed\": 5 , \"complex\": {\"val\":12, \"count\":100}}";

    private RuleChainId ruleChainId = new RuleChainId(UUIDs.timeBased());
    private RuleNodeId ruleNodeId = new RuleNodeId(UUIDs.timeBased());

    @Test
    public void msgCanBeConverted() throws IOException {
        initWithScript();
        metaData.putValue("username", "oreo");
        metaData.putValue("userEmail", "user@email.io");
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "USER", originator, metaData, rawJson, ruleChainId, ruleNodeId, 0L);

        emailNode.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellNext(captor.capture());
        TbMsg actualMsg = captor.getValue();

        assertEquals("SEND_EMAIL", actualMsg.getType());
        assertEquals(originator, actualMsg.getOriginator());
        assertEquals("oreo", actualMsg.getMetaData().getValue("username"));
        assertNotSame(metaData, actualMsg.getMetaData());


        EmailPojo actual = new ObjectMapper().readValue(actualMsg.getData().getBytes(), EmailPojo.class);

        EmailPojo expected = new EmailPojo.EmailPojoBuilder()
                .from("test@mail.org")
                .to("user@email.io")
                .subject("Hi oreo there")
                .body("temp is to high. Current 5 and 100")
                .build();
        assertEquals(expected, actual);
    }

    private void initWithScript() {
        try {
            TbMsgToEmailNodeConfiguration config = new TbMsgToEmailNodeConfiguration();
            config.setFromTemplate("test@mail.org");
            config.setToTemplate("$metadata.userEmail");
            config.setSubjectTemplate("Hi $metadata.username there");
            config.setBodyTemplate("$msg.name is to high. Current $msg.passed and $msg.complex.count");
            ObjectMapper mapper = new ObjectMapper();
            TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

            emailNode = new TbMsgToEmailNode();
            emailNode.init(ctx, nodeConfiguration);
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

}