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
package org.thingsboard.rule.engine.aws.lambda;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.AWSLambdaAsyncClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "aws lambda",
        configClazz = TbLambdaNodeConfiguration.class,
        nodeDescription = "Publish message to the AWS Lambda",
        nodeDetails = "Will publish message payload to the AWS Lambda function. ",
        iconUrl = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgd2lkdGg9IjQ4IiBoZWlnaHQ9IjQ4Ij48cGF0aCBkPSJNMTMuMjMgMTAuNTZWMTBjLTEuOTQgMC0zLjk5LjM5LTMuOTkgMi42NyAwIDEuMTYuNjEgMS45NSAxLjYzIDEuOTUuNzYgMCAxLjQzLS40NyAxLjg2LTEuMjIuNTItLjkzLjUtMS44LjUtMi44NG0yLjcgNi41M2MtLjE4LjE2LS40My4xNy0uNjMuMDYtLjg5LS43NC0xLjA1LTEuMDgtMS41NC0xLjc5LTEuNDcgMS41LTIuNTEgMS45NS00LjQyIDEuOTUtMi4yNSAwLTQuMDEtMS4zOS00LjAxLTQuMTcgMC0yLjE4IDEuMTctMy42NCAyLjg2LTQuMzggMS40Ni0uNjQgMy40OS0uNzYgNS4wNC0uOTNWNy41YzAtLjY2LjA1LTEuNDEtLjMzLTEuOTYtLjMyLS40OS0uOTUtLjctMS41LS43LTEuMDIgMC0xLjkzLjUzLTIuMTUgMS42MS0uMDUuMjQtLjI1LjQ4LS40Ny40OWwtMi42LS4yOGMtLjIyLS4wNS0uNDYtLjIyLS40LS41Ni42LTMuMTUgMy40NS00LjEgNi00LjEgMS4zIDAgMyAuMzUgNC4wMyAxLjMzQzE3LjExIDQuNTUgMTcgNi4xOCAxNyA3Ljk1djQuMTdjMCAxLjI1LjUgMS44MSAxIDIuNDguMTcuMjUuMjEuNTQgMCAuNzFsLTIuMDYgMS43OGgtLjAxIj48L3BhdGg+PHBhdGggZD0iTTIwLjE2IDE5LjU0QzE4IDIxLjE0IDE0LjgyIDIyIDEyLjEgMjJjLTMuODEgMC03LjI1LTEuNDEtOS44NS0zLjc2LS4yLS4xOC0uMDItLjQzLjI1LS4yOSAyLjc4IDEuNjMgNi4yNSAyLjYxIDkuODMgMi42MSAyLjQxIDAgNS4wNy0uNSA3LjUxLTEuNTMuMzctLjE2LjY2LjI0LjMyLjUxIj48L3BhdGg+PHBhdGggZD0iTTIxLjA3IDE4LjVjLS4yOC0uMzYtMS44NS0uMTctMi41Ny0uMDgtLjE5LjAyLS4yMi0uMTYtLjAzLS4zIDEuMjQtLjg4IDMuMjktLjYyIDMuNTMtLjMzLjI0LjMtLjA3IDIuMzUtMS4yNCAzLjMyLS4xOC4xNi0uMzUuMDctLjI2LS4xMS4yNi0uNjcuODUtMi4xNC41Ny0yLjV6Ij48L3BhdGg+PC9zdmc+"
)
public class TbLambdaNode extends TbAbstractExternalNode {

    private TbLambdaNodeConfiguration config;
    private AWSLambdaAsync client;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbLambdaNodeConfiguration.class);
        checkIfInputKeysAreNotEmptyOrElseThrow(this.config.getInputKeys());
        AWSCredentials awsCredentials = new BasicAWSCredentials(this.config.getAccessKey(), this.config.getSecretKey());
        try {
            this.client = AWSLambdaAsyncClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                    .withRegion(this.config.getRegion())
                    .withClientConfiguration(new ClientConfiguration()
                            .withConnectionTimeout(10000)
                            .withRequestTimeout(5000))
                    .build();
        } catch (Exception e) {
            throw new TbNodeException(e, true);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        var tbMsg = ackIfNeeded(ctx, msg);
        ObjectNode requestBody = getRequestBody(tbMsg);
        InvokeRequest request = toRequest(requestBody, this.config.getFunctionName());
        this.client.invokeAsync(request, new AsyncHandler<>() {
            @Override
            public void onError(Exception e) {
                tellFailure(ctx, tbMsg, e);
            }
            @Override
            public void onSuccess(InvokeRequest request, InvokeResult invokeResult) {
                try {
                    if (invokeResult.getFunctionError() != null) {
                        String errorMessage = invokeResult.getPayload() != null ?
                                JacksonUtil.toString(getPayload(invokeResult)) : invokeResult.getFunctionError();
                        throw new RuntimeException(errorMessage);
                    }
                    tellSuccess(ctx, getResponseMsg(tbMsg, invokeResult));
                } catch (Exception e) {
                    tellFailure(ctx, processException(tbMsg, e), e);
                }
            }
        });
    }

    private void checkIfInputKeysAreNotEmptyOrElseThrow(Map<String, String> inputKeys) throws TbNodeException {
        if (inputKeys == null || inputKeys.isEmpty()) {
            throw new TbNodeException("At least one input key should be specified!", true);
        }
    }

    private ObjectNode getRequestBody(TbMsg msg) {
        ObjectNode requestBodyJson = JacksonUtil.newObjectNode();
        Map<String, String> mappings = processInputKeys(msg);
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            requestBodyJson.put(entry.getKey(), entry.getValue());
        }
        return requestBodyJson;
    }

    private Map<String, String> processInputKeys(TbMsg msg) {
        JsonNode msgData = JacksonUtil.toJsonNode(msg.getData());
        var mappings = new HashMap<String, String>();
        this.config.getInputKeys().forEach((funcKey, msgKey) -> {
            String patternProcessedFuncKey = TbNodeUtils.processPattern(funcKey, msg);
            String patternProcessedMsgValue;
            try {
                String patternProcessedMsgKey = TbNodeUtils.processPattern(msgKey, msg);
                patternProcessedMsgValue = msgData.get(patternProcessedMsgKey).asText();
            } catch (Exception e) {
                patternProcessedMsgValue = msgKey;
            }
            mappings.put(patternProcessedFuncKey, patternProcessedMsgValue);
        });
        return mappings;
    }

    private <T> InvokeRequest toRequest(T requestBody, String functionName) {
        InvokeRequest request = new InvokeRequest()
                .withFunctionName(functionName)
                .withPayload(JacksonUtil.toString(requestBody));
        if (!ObjectUtils.isEmpty(this.config.getInvocationType())) {
            request.setInvocationType(this.config.getInvocationType());
        }
        if (!ObjectUtils.isEmpty(this.config.getQualifier())) {
            request.withQualifier(this.config.getQualifier());
        }
        return request;
    }

    private JsonNode getPayload(InvokeResult invokeResult) {
        if (invokeResult.getPayload() == null || !invokeResult.getPayload().hasRemaining()) {
            return null;
        }
        ByteBuffer buf = invokeResult.getPayload();
        byte[] responseData = new byte[buf.remaining()];
        buf.get(responseData);
        return JacksonUtil.fromBytes(responseData);
    }

    private TbMsg getResponseMsg(TbMsg originalMsg, InvokeResult invokeResult) {
        TbMsgMetaData metaData = originalMsg.getMetaData().copy();
        metaData.putValue("requestId", invokeResult.getSdkResponseMetadata().getRequestId());
        String data = JacksonUtil.toString(getPayload(invokeResult));
        return TbMsg.transformMsg(originalMsg, metaData, data);
    }

    private TbMsg processException(TbMsg origMsg, Throwable t) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue("error", t.getClass() + ": " + t.getMessage());
        return TbMsg.transformMsgMetadata(origMsg, metaData);
    }

    @Override
    public void destroy() {
        if (this.client != null) {
            try {
                this.client.shutdown();
            } catch (Exception e) {
                log.error("Failed to shutdown Lambda client during destroy()", e);
            }
        }
    }
}
