/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.service.script;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.kafka.TBKafkaConsumerTemplate;
import org.thingsboard.server.kafka.TBKafkaProducerTemplate;
import org.thingsboard.server.kafka.TbKafkaRequestTemplate;
import org.thingsboard.server.kafka.TbKafkaSettings;
import org.thingsboard.server.kafka.TbNodeIdProvider;
import org.thingsboard.server.service.cluster.discovery.DiscoveryService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ConditionalOnProperty(prefix = "js", value = "evaluator", havingValue = "remote", matchIfMissing = true)
@Service
public class RemoteJsInvokeService extends AbstractJsInvokeService {

    @Autowired
    private TbNodeIdProvider nodeIdProvider;

    @Autowired
    private TbKafkaSettings kafkaSettings;

    @Value("${js.remote.request_topic}")
    private String requestTopic;

    @Value("${js.remote.response_topic_prefix}")
    private String responseTopicPrefix;

    @Value("${js.remote.max_pending_requests}")
    private long maxPendingRequests;

    @Value("${js.remote.max_requests_timeout}")
    private long maxRequestsTimeout;

    @Value("${js.remote.response_poll_interval}")
    private int responsePollDuration;

    @Value("${js.remote.response_auto_commit_interval}")
    private int autoCommitInterval;

    @Getter
    @Value("${js.remote.max_errors}")
    private int maxErrors;

    private TbKafkaRequestTemplate<JsInvokeProtos.RemoteJsRequest, JsInvokeProtos.RemoteJsResponse> kafkaTemplate;
    private Map<UUID, String> scriptIdToBodysMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<JsInvokeProtos.RemoteJsRequest> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("producer-js-invoke-" + nodeIdProvider.getNodeId());
        requestBuilder.defaultTopic(requestTopic);
        requestBuilder.encoder(new RemoteJsRequestEncoder());
        requestBuilder.enricher((request, responseTopic, requestId) -> {
            JsInvokeProtos.RemoteJsRequest.Builder remoteRequest = JsInvokeProtos.RemoteJsRequest.newBuilder();
            if (request.hasCompileRequest()) {
                remoteRequest.setCompileRequest(request.getCompileRequest());
            }
            if (request.hasInvokeRequest()) {
                remoteRequest.setInvokeRequest(request.getInvokeRequest());
            }
            if (request.hasReleaseRequest()) {
                remoteRequest.setReleaseRequest(request.getReleaseRequest());
            }
            remoteRequest.setResponseTopic(responseTopic);
            remoteRequest.setRequestIdMSB(requestId.getMostSignificantBits());
            remoteRequest.setRequestIdLSB(requestId.getLeastSignificantBits());
            return remoteRequest.build();
        });

        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<JsInvokeProtos.RemoteJsResponse> responseBuilder = TBKafkaConsumerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.topic(responseTopicPrefix + "." + nodeIdProvider.getNodeId());
        responseBuilder.clientId("js-" + nodeIdProvider.getNodeId());
        responseBuilder.groupId("rule-engine-node-" + nodeIdProvider.getNodeId());
        responseBuilder.autoCommit(true);
        responseBuilder.autoCommitIntervalMs(autoCommitInterval);
        responseBuilder.decoder(new RemoteJsResponseDecoder());
        responseBuilder.requestIdExtractor((response) -> new UUID(response.getRequestIdMSB(), response.getRequestIdLSB()));

        TbKafkaRequestTemplate.TbKafkaRequestTemplateBuilder
                <JsInvokeProtos.RemoteJsRequest, JsInvokeProtos.RemoteJsResponse> builder = TbKafkaRequestTemplate.builder();
        builder.requestTemplate(requestBuilder.build());
        builder.responseTemplate(responseBuilder.build());
        builder.maxPendingRequests(maxPendingRequests);
        builder.maxRequestTimeout(maxRequestsTimeout);
        builder.pollInterval(responsePollDuration);
        kafkaTemplate = builder.build();
        kafkaTemplate.init();
    }

    @PreDestroy
    public void destroy() {
        if (kafkaTemplate != null) {
            kafkaTemplate.stop();
        }
    }

    @Override
    protected ListenableFuture<UUID> doEval(UUID scriptId, String functionName, String scriptBody) {
        JsInvokeProtos.JsCompileRequest jsRequest = JsInvokeProtos.JsCompileRequest.newBuilder()
                .setScriptIdMSB(scriptId.getMostSignificantBits())
                .setScriptIdLSB(scriptId.getLeastSignificantBits())
                .setFunctionName(functionName)
                .setScriptBody(scriptBody).build();

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = JsInvokeProtos.RemoteJsRequest.newBuilder()
                .setCompileRequest(jsRequest)
                .build();

        log.trace("Post compile request for scriptId [{}]", scriptId);
        ListenableFuture<JsInvokeProtos.RemoteJsResponse> future = kafkaTemplate.post(scriptId.toString(), jsRequestWrapper);
        return Futures.transform(future, response -> {
            JsInvokeProtos.JsCompileResponse compilationResult = response.getCompileResponse();
            UUID compiledScriptId = new UUID(compilationResult.getScriptIdMSB(), compilationResult.getScriptIdLSB());
            if (compilationResult.getSuccess()) {
                scriptIdToNameMap.put(scriptId, functionName);
                scriptIdToBodysMap.put(scriptId, scriptBody);
                return compiledScriptId;
            } else {
                log.debug("[{}] Failed to compile script due to [{}]: {}", compiledScriptId, compilationResult.getErrorCode().name(), compilationResult.getErrorDetails());
                throw new RuntimeException(compilationResult.getErrorDetails());
            }
        });
    }

    @Override
    protected ListenableFuture<Object> doInvokeFunction(UUID scriptId, String functionName, Object[] args) {
        String scriptBody = scriptIdToBodysMap.get(scriptId);
        if (scriptBody == null) {
            return Futures.immediateFailedFuture(new RuntimeException("No script body found for scriptId: [" + scriptId + "]!"));
        }
        JsInvokeProtos.JsInvokeRequest.Builder jsRequestBuilder = JsInvokeProtos.JsInvokeRequest.newBuilder()
                .setScriptIdMSB(scriptId.getMostSignificantBits())
                .setScriptIdLSB(scriptId.getLeastSignificantBits())
                .setFunctionName(functionName)
                .setTimeout((int) maxRequestsTimeout)
                .setScriptBody(scriptIdToBodysMap.get(scriptId));

        for (int i = 0; i < args.length; i++) {
            jsRequestBuilder.addArgs(args[i].toString());
        }

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = JsInvokeProtos.RemoteJsRequest.newBuilder()
                .setInvokeRequest(jsRequestBuilder.build())
                .build();

        ListenableFuture<JsInvokeProtos.RemoteJsResponse> future = kafkaTemplate.post(scriptId.toString(), jsRequestWrapper);
        return Futures.transform(future, response -> {
            JsInvokeProtos.JsInvokeResponse invokeResult = response.getInvokeResponse();
            if (invokeResult.getSuccess()) {
                return invokeResult.getResult();
            } else {
                log.debug("[{}] Failed to compile script due to [{}]: {}", scriptId, invokeResult.getErrorCode().name(), invokeResult.getErrorDetails());
                throw new RuntimeException(invokeResult.getErrorDetails());
            }
        });
    }

    @Override
    protected void doRelease(UUID scriptId, String functionName) throws Exception {
        JsInvokeProtos.JsReleaseRequest jsRequest = JsInvokeProtos.JsReleaseRequest.newBuilder()
                .setScriptIdMSB(scriptId.getMostSignificantBits())
                .setScriptIdLSB(scriptId.getLeastSignificantBits())
                .setFunctionName(functionName).build();

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = JsInvokeProtos.RemoteJsRequest.newBuilder()
                .setReleaseRequest(jsRequest)
                .build();

        ListenableFuture<JsInvokeProtos.RemoteJsResponse> future = kafkaTemplate.post(scriptId.toString(), jsRequestWrapper);
        JsInvokeProtos.RemoteJsResponse response = future.get();

        JsInvokeProtos.JsReleaseResponse compilationResult = response.getReleaseResponse();
        UUID compiledScriptId = new UUID(compilationResult.getScriptIdMSB(), compilationResult.getScriptIdLSB());
        if (compilationResult.getSuccess()) {
            scriptIdToBodysMap.remove(scriptId);
        } else {
            log.debug("[{}] Failed to release script due", compiledScriptId);
        }
    }

}
