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
package org.thingsboard.server.mqtt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.kafka.AsyncCallbackTemplate;
import org.thingsboard.server.kafka.TBKafkaConsumerTemplate;
import org.thingsboard.server.kafka.TBKafkaProducerTemplate;
import org.thingsboard.server.kafka.TbKafkaRequestTemplate;
import org.thingsboard.server.gen.transport.TransportProtos.*;
import org.thingsboard.server.kafka.TbKafkaSettings;
import org.thingsboard.server.transport.mqtt.MqttTransportContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ashvayka on 05.10.18.
 */
@Service
public class MqttTransportService implements TransportService {

    @Value("${kafka.rule_engine.topic}")
    private String ruleEngineTopic;
    @Value("${kafka.transport_api.requests_topic}")
    private String transportApiRequestsTopic;
    @Value("${kafka.transport_api.responses_topic}")
    private String transportApiResponsesTopic;
    @Value("${kafka.transport_api.max_pending_requests}")
    private long maxPendingRequests;
    @Value("${kafka.transport_api.max_requests_timeout}")
    private long maxRequestsTimeout;
    @Value("${kafka.transport_api.response_poll_interval}")
    private int responsePollDuration;
    @Value("${kafka.transport_api.response_auto_commit_interval}")
    private int autoCommitInterval;

    @Autowired
    private TbKafkaSettings kafkaSettings;
    //We use this to get the node id. We should replace this with a component that provides the node id.
    @Autowired
    private MqttTransportContext transportContext;

    private ExecutorService transportCallbackExecutor;

    private TbKafkaRequestTemplate<TransportApiRequestMsg, TransportApiResponseMsg> transportApiTemplate;

    @PostConstruct
    public void init() {
        this.transportCallbackExecutor = Executors.newCachedThreadPool();

        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TransportApiRequestMsg> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.defaultTopic(transportApiRequestsTopic);
        requestBuilder.encoder(new TransportApiRequestEncoder());

        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<TransportApiResponseMsg> responseBuilder = TBKafkaConsumerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.topic(transportApiResponsesTopic + "." + transportContext.getNodeId());
        responseBuilder.clientId(transportContext.getNodeId());
        responseBuilder.groupId("transport-node");
        responseBuilder.autoCommit(true);
        responseBuilder.autoCommitIntervalMs(autoCommitInterval);
        responseBuilder.decoder(new TransportApiResponseDecoder());

        TbKafkaRequestTemplate.TbKafkaRequestTemplateBuilder
                <TransportApiRequestMsg, TransportApiResponseMsg> builder = TbKafkaRequestTemplate.builder();
        builder.requestTemplate(requestBuilder.build());
        builder.responseTemplate(responseBuilder.build());
        builder.maxPendingRequests(maxPendingRequests);
        builder.maxRequestTimeout(maxRequestsTimeout);
        builder.pollInterval(responsePollDuration);
        transportApiTemplate = builder.build();
        transportApiTemplate.init();
    }

    @PreDestroy
    public void destroy() {
        if (transportApiTemplate != null) {
            transportApiTemplate.stop();
        }
        if (transportCallbackExecutor != null) {
            transportCallbackExecutor.shutdownNow();
        }
    }

    @Override
    public void process(ValidateDeviceTokenRequestMsg msg, TransportServiceCallback<ValidateDeviceCredentialsResponseMsg> callback) {
        AsyncCallbackTemplate.withCallback(transportApiTemplate.post(msg.getToken(),
                TransportApiRequestMsg.newBuilder().setValidateTokenRequestMsg(msg).build()),
                response -> callback.onSuccess(response.getValidateTokenResponseMsg()), callback::onError, transportCallbackExecutor);
    }

    @Override
    public void process(ValidateDeviceX509CertRequestMsg msg, TransportServiceCallback<ValidateDeviceCredentialsResponseMsg> callback) {
        AsyncCallbackTemplate.withCallback(transportApiTemplate.post(msg.getHash(),
                TransportApiRequestMsg.newBuilder().setValidateX509CertRequestMsg(msg).build()),
                response -> callback.onSuccess(response.getValidateTokenResponseMsg()), callback::onError, transportCallbackExecutor);
    }

    @Override
    public void process(SessionEventMsg msg, TransportServiceCallback<Void> callback) {

    }

    @Override
    public void process(PostTelemetryMsg msg, TransportServiceCallback<Void> callback) {

    }

    @Override
    public void process(PostAttributeMsg msg, TransportServiceCallback<Void> callback) {

    }
}
