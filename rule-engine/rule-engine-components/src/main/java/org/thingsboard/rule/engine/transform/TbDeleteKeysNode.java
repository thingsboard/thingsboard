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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "delete keys",
        configClazz = TbDeleteKeysNodeConfiguration.class,
        nodeDescription = "Removes keys from the msg data or metadata with the specified key names selected in the list",
        nodeDetails = "Will fetch fields (regex) values specified in list. If specified field (regex) is not part of msg " +
                "or metadata fields it will be ignored. Returns transformed messages via <code>Success</code> chain",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeDeleteKeysConfig",
        icon = "remove_circle"
)
public class TbDeleteKeysNode implements TbNode {

    TbDeleteKeysNodeConfiguration config;
    List<Pattern> patternKeys;
    boolean fromMetadata;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbDeleteKeysNodeConfiguration.class);
        this.fromMetadata = config.isFromMetadata();
        this.patternKeys = new ArrayList<>();
        config.getKeys().forEach(key -> {
            this.patternKeys.add(Pattern.compile(key));
        });
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        TbMsgMetaData metaData = msg.getMetaData();
        String msgData = msg.getData();
        List<String> keysToDelete = new ArrayList<>();
        if (fromMetadata) {
            Map<String, String> metaDataMap = metaData.getData();
            metaDataMap.forEach((keyMetaData, valueMetaData) -> {
                if (checkKey(keyMetaData)) {
                    keysToDelete.add(keyMetaData);
                }
            });
            keysToDelete.forEach(key -> metaDataMap.remove(key));
            metaData = new TbMsgMetaData(metaDataMap);
        } else {
            JsonNode dataNode = JacksonUtil.toJsonNode(msgData);
            if (dataNode.isObject()) {
                ObjectNode msgDataObject = (ObjectNode) dataNode;
                dataNode.fields().forEachRemaining(entry -> {
                    String keyData = entry.getKey();
                    if (checkKey(keyData)) {
                        keysToDelete.add(keyData);
                    }
                });
                msgDataObject.remove(keysToDelete);
                msgData = JacksonUtil.toString(msgDataObject);
            }
        }
        if (keysToDelete.isEmpty()) {
            ctx.tellSuccess(msg);
        } else {
            ctx.tellSuccess(TbMsg.transformMsg(msg, msg.getType(), msg.getOriginator(), metaData, msgData));
        }
    }

    boolean checkKey(String key) {
        return patternKeys.stream().anyMatch(pattern -> pattern.matcher(key).matches());
    }

    @Override
    public void destroy() {

    }
}
