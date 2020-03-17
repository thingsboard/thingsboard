/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.TbQueueConsumer;
import org.thingsboard.server.TbQueueCoreSettings;
import org.thingsboard.server.TbQueueProducer;
import org.thingsboard.server.TbQueueRequestTemplate;
import org.thingsboard.server.TbQueueRuleEngineSettings;
import org.thingsboard.server.TbQueueTransportApiSettings;
import org.thingsboard.server.TbQueueTransportNotificationSettings;
import org.thingsboard.server.common.DefaultTbQueueRequestTemplate;
import org.thingsboard.server.common.TbProtoQueueMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.kafka.TBKafkaAdmin;
import org.thingsboard.server.kafka.TBKafkaConsumerTemplate;
import org.thingsboard.server.kafka.TBKafkaProducerTemplate;
import org.thingsboard.server.kafka.TbKafkaSettings;
import org.thingsboard.server.kafka.TbNodeIdProvider;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='kafka' && ('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-transport')")
@Slf4j
public class KafkaTransportQueueProvider implements TransportQueueProvider {

    private final TbKafkaSettings kafkaSettings;
    private final TbNodeIdProvider nodeIdProvider;
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;

    public KafkaTransportQueueProvider(TbKafkaSettings kafkaSettings,
                                       TbNodeIdProvider nodeIdProvider,
                                       TbQueueCoreSettings coreSettings,
                                       TbQueueRuleEngineSettings ruleEngineSettings,
                                       TbQueueTransportApiSettings transportApiSettings,
                                       TbQueueTransportNotificationSettings transportNotificationSettings) {
        this.kafkaSettings = kafkaSettings;
        this.nodeIdProvider = nodeIdProvider;
        this.coreSettings = coreSettings;
        this.ruleEngineSettings = ruleEngineSettings;
        this.transportApiSettings = transportApiSettings;
        this.transportNotificationSettings = transportNotificationSettings;
    }

    @Override
    public TbQueueRequestTemplate<TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> getTransportApiRequestTemplate() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<TransportApiRequestMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("producer-transport-api-" + nodeIdProvider.getNodeId());
        requestBuilder.defaultTopic(transportApiSettings.getRequestsTopic());

        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<TbProtoQueueMsg<TransportApiResponseMsg>> responseBuilder = TBKafkaConsumerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.topic(transportApiSettings.getResponsesTopic() + "." + nodeIdProvider.getNodeId());
        responseBuilder.clientId("consumer-transport-api-" + nodeIdProvider.getNodeId());
        responseBuilder.groupId("transport-node-" + nodeIdProvider.getNodeId());
        responseBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportApiResponseMsg.parseFrom(msg.getData()), msg.getHeaders()));

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> templateBuilder = DefaultTbQueueRequestTemplate.builder();
        templateBuilder.queueAdmin(new TBKafkaAdmin(kafkaSettings));
        templateBuilder.requestTemplate(requestBuilder.build());
        templateBuilder.responseTemplate(responseBuilder.build());
        templateBuilder.maxPendingRequests(transportApiSettings.getMaxPendingRequests());
        templateBuilder.maxRequestTimeout(transportApiSettings.getMaxRequestsTimeout());
        templateBuilder.pollInterval(transportApiSettings.getResponsePollInterval());
        return templateBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> getRuleEngineMsgProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("producer-rule-engine-" + nodeIdProvider.getNodeId());
        requestBuilder.defaultTopic(transportApiSettings.getRequestsTopic());
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> getTbCoreMsgProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCoreMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("producer-tb-core-" + nodeIdProvider.getNodeId());
        requestBuilder.defaultTopic(transportApiSettings.getRequestsTopic());
        return requestBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToTransportMsg>> getTransportNotificationsConsumer() {
        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToTransportMsg>> responseBuilder = TBKafkaConsumerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.topic(transportNotificationSettings.getNotificationsTopic() + "." + nodeIdProvider.getNodeId());
        responseBuilder.clientId("consumer-transport-" + nodeIdProvider.getNodeId());
        responseBuilder.groupId("rule-engine-node-" + nodeIdProvider.getNodeId());
        responseBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToTransportMsg.parseFrom(msg.getData()), msg.getHeaders()));
        return responseBuilder.build();
    }
}
