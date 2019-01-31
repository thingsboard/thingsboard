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
package org.thingsboard.rule.engine.filter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.Map;

@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "has Hobotok",
        relationTypes = {"True", "False"},
        configClazz = TbCheckMessageNodeConfiguration.class,
        nodeDescription = "Checks the existence of the selected keys from message data and metadata.",
        nodeDetails = "If selected checkbox 'Check that all selected keys are present'\" and all keys in message data and metadata are exist - send Message via <b>True</b> chain, otherwise <b>False</b> chain is used.\n" +
                "Else if the checkbox is not selected, and at least one of the keys from data or metadata of the message exists - send Message via <b>True</b> chain, otherwise, <b>False</b> chain is used. ",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeCheckMessageConfig")
public class TbCheckMessageNode implements TbNode {

    private static final JsonParser parser = new JsonParser();

    private TbCheckMessageNodeConfiguration config;
    private List<String> messageNamesList;
    private List<String> metadataNamesList;

    @Override
    public void init(TbContext tbContext, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCheckMessageNodeConfiguration.class);
        messageNamesList = config.getMessageNames();
        metadataNamesList = config.getMetadataNames();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            if (config.isCheckAllKeys()) {
                ctx.tellNext(msg, allDataKeysExist(msg) && allMetadataKeysExist(msg) ? "True" : "False");
            } else {
                ctx.tellNext(msg, atLeastOneDataKeyExist(msg) || atLeastOneMetadataKeyExist(msg) ? "True" : "False");
            }
        } catch (Exception e) {
            ctx.tellFailure(msg, e);
        }
    }

    private boolean allDataKeysExist(TbMsg msg) {
        if (!messageNamesList.isEmpty()) {
            JsonObject data = parser.parse(msg.getData()).getAsJsonObject();
            for (String field : messageNamesList) {
                if (!data.has(field)) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private boolean atLeastOneDataKeyExist(TbMsg msg) {
        if (!messageNamesList.isEmpty()) {
            JsonObject data = parser.parse(msg.getData()).getAsJsonObject();
            for (String field : messageNamesList) {
                if (data.has(field)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private boolean allMetadataKeysExist(TbMsg msg) {
        if (!metadataNamesList.isEmpty()) {
            Map<String, String> metadata = msg.getMetaData().getData();
            for (String field : metadataNamesList) {
                if (!metadata.containsKey(field)) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private boolean atLeastOneMetadataKeyExist(TbMsg msg) {
        if (!metadataNamesList.isEmpty()) {
            Map<String, String> metadata = msg.getMetaData().getData();
            for (String field : metadataNamesList) {
                if (metadata.containsKey(field)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public void destroy() {

    }

}