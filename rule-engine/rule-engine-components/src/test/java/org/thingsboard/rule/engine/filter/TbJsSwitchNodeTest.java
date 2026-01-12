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
package org.thingsboard.rule.engine.filter;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbJsSwitchNodeTest {

    private TbJsSwitchNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private ScriptEngine scriptEngine;

    private final RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    private final RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

    @Test
    public void multipleRoutesAreAllowed() throws TbNodeException {
        initWithScript();
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temp", "10");
        metaData.putValue("humidity", "99");
        String rawJson = "{\"name\": \"Vit\", \"passed\": 5}";

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(null)
                .copyMetaData(metaData)
                .dataType(TbMsgDataType.JSON)
                .data(rawJson)
                .ruleChainId(ruleChainId)
                .ruleNodeId(ruleNodeId)
                .build();
        when(scriptEngine.executeSwitchAsync(msg)).thenReturn(Futures.immediateFuture(Sets.newHashSet("one", "three")));

        node.onMsg(ctx, msg);
        verify(ctx).tellNext(msg, Sets.newHashSet("one", "three"));
    }

    private void initWithScript() throws TbNodeException {
        TbJsSwitchNodeConfiguration config = new TbJsSwitchNodeConfiguration();
        config.setScriptLang(ScriptLanguage.JS);
        config.setJsScript("scr");
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctx.createScriptEngine(ScriptLanguage.JS, "scr")).thenReturn(scriptEngine);

        node = new TbJsSwitchNode();
        node.init(ctx, nodeConfiguration);
    }
}
