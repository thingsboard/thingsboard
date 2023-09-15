/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "delete key-values",
        version = 1,
        configClazz = TbDeleteKeysNodeConfiguration.class,
        nodeDescription = "Removes key-values from message or metadata.",
        nodeDetails = "Removes key-values from message or message metadata based on the keys list specified in the configuration. " +
                "Use regular expression(s) as a key(s) to remove keys by pattern.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeDeleteKeysConfig",
        icon = "remove_circle"
)
public class TbDeleteKeysNode extends TbAbstractTransformNodeWithTbMsgSource {

    private TbDeleteKeysNodeConfiguration config;
    private List<Pattern> patternKeys;
    private TbMsgSource deleteFrom;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbDeleteKeysNodeConfiguration.class);
        this.deleteFrom = config.getDeleteFrom();
        if (deleteFrom == null) {
            throw new TbNodeException("DeleteFrom can't be null! Allowed values: " + Arrays.toString(TbMsgSource.values()));
        }
        this.patternKeys = config.getKeys().stream().map(Pattern::compile).collect(Collectors.toList());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        TbMsgMetaData metaDataCopy = msg.getMetaData().copy();
        String msgData = msg.getData();
        List<String> keysToDelete = new ArrayList<>();
        switch (deleteFrom) {
            case METADATA:
                Map<String, String> metaDataMap = metaDataCopy.getData();
                metaDataMap.forEach((keyMetaData, valueMetaData) -> {
                    if (checkKey(keyMetaData)) {
                        keysToDelete.add(keyMetaData);
                    }
                });
                keysToDelete.forEach(metaDataMap::remove);
                metaDataCopy = new TbMsgMetaData(metaDataMap);
                break;
            case DATA:
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
                break;
            default:
                log.debug("Unexpected DeleteFrom value: {}. Allowed values: {}", deleteFrom, TbMsgSource.values());
                break;
        }
        ctx.tellSuccess(keysToDelete.isEmpty() ? msg : TbMsg.transformMsg(msg, metaDataCopy, msgData));
    }

    @Override
    protected String getKeyToUpgradeFromVersionZero() {
        return "deleteFrom";
    }

    boolean checkKey(String key) {
        return patternKeys.stream().anyMatch(pattern -> pattern.matcher(key).matches());
    }

}
