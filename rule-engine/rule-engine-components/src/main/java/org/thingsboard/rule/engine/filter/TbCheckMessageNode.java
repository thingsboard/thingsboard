/**
 * Copyright © 2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.filter;

import com.google.gson.Gson;
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

@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "check key",
        relationTypes = {"True", "False"},
        configClazz = TbCheckMessageNodeConfiguration.class,
        nodeDescription = "Checks the existence of the selected key in the message payload.",
        nodeDetails = "If the selected key  exists - send Message via <b>True</b> chain, otherwise <b>False</b> chain is used.",
        uiResources = {"static/rulenode/custom-nodes-config.js"},
        configDirective = "tbFilterNodeCheckKeyConfig")
public class TbCheckMessageNode implements TbNode {

    private static final Gson gson = new Gson();

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
        JsonObject data = new JsonParser().parse(msg.getData()).getAsJsonObject();
        JsonObject metadata = gson.toJsonTree(msg.getMetaData().getData()).getAsJsonObject();
        try {
            ctx.tellNext(msg, processAllMessageData(data) && processAllMessageMetadata(metadata) ? "True" : "False");
        } catch (Exception e) {
            ctx.tellFailure(msg, e);
        }
    }

    @Override
    public void destroy() {
    }

    private boolean processAllMessageData(JsonObject data) {
        if (config.isCheckMessageData()) {
            return process(data, messageNamesList);
        } else {
            return true;
        }
    }

    private boolean processAllMessageMetadata(JsonObject metadata) {
        if (config.isCheckMessageMetadata()) {
            return process(metadata, metadataNamesList);
        } else {
            return true;
        }
    }

    private boolean process(JsonObject object, List<String> fieldsList) {
        for (String field : fieldsList) {
            if (!object.has(field)) {
                return false;
            }
        }
        return true;
    }

}