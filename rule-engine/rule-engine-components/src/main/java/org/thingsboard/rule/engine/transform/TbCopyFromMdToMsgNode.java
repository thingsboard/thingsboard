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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "copy from metadata to msg",
        configClazz = TbCopyFromMdToMsgNodeConfiguration.class,
        nodeDescription = "Copies the msg metadata keys to msg data with specified key names selected in the list",
        nodeDetails = "Will fetch fields values specified in list. If specified field is not part of msg metadata fields it will be ignored." +
                "If the msg is not a JSON object returns the incoming message as outbound message with <code>Failure</code> chain, " +
                "otherwise returns transformed messages via <code>Success</code> chain",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "",
        icon = "functions"
)
public class TbCopyFromMdToMsgNode implements TbNode {

    TbCopyFromMdToMsgNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCopyFromMdToMsgNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        processCopy(ctx, msg);
    }

    @Override
    public void destroy() {
    }

    private void processCopy(TbContext ctx, TbMsg msg) {
        List<String> metadataMsgKeys = config.getMetadataMsgKeys();
        if (CollectionUtils.isEmpty(metadataMsgKeys)) {
            ctx.tellSuccess(msg);
        } else {
            JsonNode dataNode = JacksonUtil.toJsonNode(msg.getData());
            if (dataNode.isObject()) {
                ObjectNode msgData = (ObjectNode) dataNode;
                TbMsgMetaData metaData = msg.getMetaData();
                metadataMsgKeys.forEach(metadataKey -> {
                    String value = metaData.getValue(metadataKey);
                    if (!StringUtils.isEmpty(value)) {
                        msgData.put(metadataKey, value);
                    }
                });
                ctx.tellSuccess(TbMsg.transformMsg(msg, msg.getType(), msg.getOriginator(), metaData, JacksonUtil.toString(msgData)));
            } else {
                ctx.tellFailure(msg, new RuntimeException("Msg data is not a JSON Object!"));
            }
        }
    }
}

