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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "rename keys",
        configClazz = TbRenameKeysNodeConfiguration.class,
        nodeDescription = "Renames msg data or metadata keys to the new key names selected in the key mapping.",
        nodeDetails = "If the key that is selected in the key mapping is missed in the selected msg source(data or metadata), it will be ignored." +
                " Returns transformed messages via <code>Success</code> chain",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeRenameKeysConfig",
        icon = "find_replace"
)
public class TbRenameKeysNode implements TbNode {

    TbRenameKeysNodeConfiguration config;
    Map<String, String> renameKeysMapping;
    boolean fromMetadata;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbRenameKeysNodeConfiguration.class);
        this.renameKeysMapping = config.getRenameKeysMapping();
        this.fromMetadata = config.isFromMetadata();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        TbMsgMetaData metaData = msg.getMetaData();
        String data = msg.getData();
        boolean msgChanged = false;
        if (fromMetadata) {
            Map<String, String> metaDataMap = metaData.getData();
            for (Map.Entry<String, String> entry : renameKeysMapping.entrySet()) {
                String nameKey = entry.getKey();
                if (metaDataMap.containsKey(nameKey)) {
                    msgChanged = true;
                    metaDataMap.put(entry.getValue(), metaDataMap.get(nameKey));
                    metaDataMap.remove(nameKey);
                }
            }
            metaData = new TbMsgMetaData(metaDataMap);
        } else {
            JsonNode dataNode = JacksonUtil.toJsonNode(msg.getData());
            if (dataNode.isObject()) {
                ObjectNode msgData = (ObjectNode) dataNode;
                for (Map.Entry<String, String> entry : renameKeysMapping.entrySet()) {
                    String nameKey = entry.getKey();
                    if (msgData.has(nameKey)) {
                        msgChanged = true;
                        msgData.set(entry.getValue(), msgData.get(nameKey));
                        msgData.remove(nameKey);
                    }
                }
                data = JacksonUtil.toString(msgData);
            }
        }
        if (msgChanged) {
            ctx.tellSuccess(TbMsg.transformMsg(msg, msg.getType(), msg.getOriginator(), metaData, data));
        } else {
            ctx.tellSuccess(msg);
        }
    }
}
