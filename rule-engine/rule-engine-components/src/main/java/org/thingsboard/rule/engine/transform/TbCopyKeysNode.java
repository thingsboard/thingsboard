/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "copy key-value pairs",
        version = 2,
        configClazz = TbCopyKeysNodeConfiguration.class,
        nodeDescription = "Copies key-value pairs from message to message metadata or vice-versa.",
        nodeDetails = "Copies key-value pairs from the message to message metadata, or vice-versa, according to the configured direction and keys. " +
                "Regular expressions can be used to define which keys-value pairs to copy. Any configured key not found in the source will be ignored.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        configDirective = "tbTransformationNodeCopyKeysConfig",
        icon = "content_copy",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/transformation/copy-key-value-pairs/"
)
public class TbCopyKeysNode extends TbAbstractTransformNodeWithTbMsgSource {

    private TbMsgSource copyFrom;
    private List<Pattern> compiledKeyPatterns;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbCopyKeysNodeConfiguration.class);
        copyFrom = config.getCopyFrom();
        if (copyFrom == null) {
            throw new TbNodeException("CopyFrom can't be null! Allowed values: " + Arrays.toString(TbMsgSource.values()));
        }
        compiledKeyPatterns = config.getKeys().stream().map(Pattern::compile).collect(Collectors.toList());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        var metaDataCopy = msg.getMetaData().copy();
        String msgData = msg.getData();
        boolean msgChanged = false;
        JsonNode dataNode = JacksonUtil.toJsonNode(msgData);
        if (dataNode.isObject()) {
            switch (copyFrom) {
                case METADATA:
                    ObjectNode msgDataNode = (ObjectNode) dataNode;
                    Map<String, String> metaDataMap = metaDataCopy.getData();
                    for (Map.Entry<String, String> entry : metaDataMap.entrySet()) {
                        String mdKey = entry.getKey();
                        String mdValue = entry.getValue();
                        if (matches(mdKey)) {
                            msgChanged = true;
                            msgDataNode.put(mdKey, mdValue);
                        }
                    }
                    msgData = JacksonUtil.toString(msgDataNode);
                    break;
                case DATA:
                    Iterator<Map.Entry<String, JsonNode>> iteratorNode = dataNode.fields();
                    while (iteratorNode.hasNext()) {
                        Map.Entry<String, JsonNode> entry = iteratorNode.next();
                        String msgKey = entry.getKey();
                        JsonNode msgValue = entry.getValue();
                        if (matches(msgKey)) {
                            msgChanged = true;
                            String value = msgValue.isTextual() ?
                                    msgValue.asText() : JacksonUtil.toString(msgValue);
                            metaDataCopy.putValue(msgKey, value);
                        }
                    }
                    break;
                default:
                    log.debug("Unexpected CopyFrom value: {}. Allowed values: {}", copyFrom, TbMsgSource.values());
            }
        }
        ctx.tellSuccess(msgChanged ? msg.transform()
                .metaData(metaDataCopy)
                .data(msgData)
                .build() : msg);
    }

    @Override
    protected String getNewKeyForUpgradeFromVersionZero() {
        return "copyFrom";
    }

    @Override
    protected String getKeyToUpgradeFromVersionOne() {
        return FROM_METADATA_PROPERTY;
    }

    boolean matches(String key) {
        return compiledKeyPatterns.stream().anyMatch(pattern -> pattern.matcher(key).matches());
    }

}
