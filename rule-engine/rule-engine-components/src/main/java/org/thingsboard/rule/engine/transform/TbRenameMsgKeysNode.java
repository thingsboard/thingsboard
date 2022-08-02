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
package org.thingsboard.rule.engine.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "rename keys",
        configClazz = TbRenameMsgKeysNodeConfiguration.class,
        nodeDescription = "Renames the keys in the msg data to the specified key names selected in the key mapping.",
        nodeDetails = "Will fetch fields values specified in mapping. If specified field is not part of msg fields it will be ignored." +
                "If the msg is not a JSON object returns the incoming message as outbound message with <code>Failure</code> chain," +
                " otherwise returns transformed messages via <code>Success</code> chain",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeRenameMsgKeysConfig",
        icon = "functions"
)
public class TbRenameMsgKeysNode implements TbNode {

    TbRenameMsgKeysNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbRenameMsgKeysNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        processRename(ctx, msg);
    }

    @Override
    public void destroy() {
    }

    private void processRename(TbContext ctx, TbMsg msg) {
        Map<String, String> renameKeysMapping = config.getRenameKeysMapping();
        if (CollectionUtils.isEmpty(renameKeysMapping)) {
            ctx.tellSuccess(msg);
        } else {
            JsonNode dataNode = JacksonUtil.toJsonNode(msg.getData());
            if (dataNode.isObject()) {
                ObjectNode msgData = (ObjectNode) dataNode;
                renameKeysMapping.forEach((nameKey, newNameKey) -> {
                    if (msgData.has(nameKey)) {
                        msgData.set(newNameKey, msgData.get(nameKey));
                        msgData.remove(nameKey);
                    }
                });
                ctx.tellSuccess(TbMsg.transformMsg(msg, msg.getType(), msg.getOriginator(), msg.getMetaData(), JacksonUtil.toString(msgData)));
            } else {
                ctx.tellFailure(msg, new RuntimeException("Msg data is not a JSON Object!"));
            }
        }
    }
}

