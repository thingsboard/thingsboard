/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriBuilder;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.credentials.CredentialsType;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.plugin.ComponentClusteringMode;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "azure functions",
        configClazz = TbAzureFunctionsNodeConfiguration.class,
        clusteringMode = ComponentClusteringMode.SINGLETON,
        nodeDescription = "Pushes message data to the Azure Functions",
        nodeDetails = "Will invoke REST API call GET | POST | PUT | DELETE to Azure Functions. " +
                "Message payload added into Request body. Query parameters can be added to the url. " +
                "Configured attributes can be added into Headers from Message Metadata. " +
                "Outbound message will contain response fields (status, statusCode, statusReason and response headers) in the Message Metadata. " +
                "Response body saved in outbound Message payload. For example statusCode field can be accessed with metadata.statusCode.</b>.",
        uiResources = {""},
        configDirective = ""
)
public class TbAzureFunctionsNode extends TbAbstractExternalNode {

    private TbHttpClient httpClient;
    private TbAzureFunctionsNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        config = TbNodeUtils.convert(configuration, TbAzureFunctionsNodeConfiguration.class);
        httpClient = new TbHttpClient(config, ctx.getSharedEventLoop());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        var tbMsg = ackIfNeeded(ctx, msg);
        tbMsg = TbMsg.transformMsgData(tbMsg, getRequestBody(tbMsg));
        config.setRestEndpointUrlPattern(buildUrl(msg));
        httpClient.processMessage(ctx, tbMsg,
                m -> tellSuccess(ctx, m),
                (m, t) -> tellFailure(ctx, m, t));
    }

    @Override
    public void destroy() {
        if (httpClient != null) {
            httpClient.destroy();
        }
    }

    private String buildUrl(TbMsg msg) {
        StringBuilder urlBuilder = new StringBuilder(config.getRestEndpointUrlPattern());
        if (CredentialsType.ACCESS_KEY == config.getCredentials().getType()
                && !config.getRestEndpointUrlPattern().contains("?code=")) {
            urlBuilder.append("?code=").append(((AzureFunctionsCredentials) config.getCredentials()).getAccessKey());
        }
        Map<String, String> queryParams = processMappings(msg, config.getQueryParams());
        queryParams.forEach((param, value) -> {
            if (urlBuilder.toString().contains("?")) {
                urlBuilder.append("&");
            } else {
                urlBuilder.append("?");
            }
            urlBuilder.append(param).append("=").append(value);
        });
        return urlBuilder.toString();
    }

    private String getRequestBody(TbMsg msg) {
        ObjectNode requestBodyJson = JacksonUtil.newObjectNode();
        Map<String, String> inputKeys = processMappings(msg, config.getInputKeys());
        inputKeys.forEach(requestBodyJson::put);
        return JacksonUtil.toString(requestBodyJson);
    }

    private Map<String, String> processMappings(TbMsg msg, Map<String, String> mappings) {
        Map<String, String> processedMappings = new HashMap<>();
        JsonNode msgData = JacksonUtil.toJsonNode(msg.getData());
        mappings.forEach((funcKey, msgKey) -> {
            String patternProcessedFuncKey = TbNodeUtils.processPattern(funcKey, msg);
            String patternProcessedMsgValue;
            try {
                String patternProcessedMsgKey = TbNodeUtils.processPattern(msgKey, msg);
                patternProcessedMsgValue = msgData.get(patternProcessedMsgKey).asText();
            } catch (Exception e) {
                patternProcessedMsgValue = msgKey;
            }
            processedMappings.put(patternProcessedFuncKey, patternProcessedMsgValue);
        });
        return processedMappings;
    }
}
