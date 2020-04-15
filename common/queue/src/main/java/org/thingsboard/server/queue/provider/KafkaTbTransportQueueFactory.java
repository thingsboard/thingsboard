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
package org.thingsboard.server.queue.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.kafka.TBKafkaAdmin;
import org.thingsboard.server.queue.kafka.TBKafkaConsumerTemplate;
import org.thingsboard.server.queue.kafka.TBKafkaProducerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.kafka.TbKafkaTopicConfigs;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportNotificationSettings;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='kafka' && ('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-transport')")
@Slf4j
public class KafkaTbTransportQueueFactory implements TbTransportQueueFactory {

    private final TbKafkaSettings kafkaSettings;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;

    private final TbQueueAdmin coreAdmin;
    private final TbQueueAdmin ruleEngineAdmin;
    private final TbQueueAdmin transportApiAdmin;
    private final TbQueueAdmin notificationAdmin;

    public KafkaTbTransportQueueFactory(TbKafkaSettings kafkaSettings,
                                        TbServiceInfoProvider serviceInfoProvider,
                                        TbQueueCoreSettings coreSettings,
                                        TbQueueRuleEngineSettings ruleEngineSettings,
                                        TbQueueTransportApiSettings transportApiSettings,
                                        TbQueueTransportNotificationSettings transportNotificationSettings,
                                        TbKafkaTopicConfigs kafkaTopicConfigs) {
        this.kafkaSettings = kafkaSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.coreSettings = coreSettings;
        this.ruleEngineSettings = ruleEngineSettings;
        this.transportApiSettings = transportApiSettings;
        this.transportNotificationSettings = transportNotificationSettings;

        this.coreAdmin = new TBKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getCoreConfigs());
        this.ruleEngineAdmin = new TBKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getRuleEngineConfigs());
        this.transportApiAdmin = new TBKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getTransportApiConfigs());
        this.notificationAdmin = new TBKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getNotificationsConfigs());
    }

    @Override
    public TbQueueRequestTemplate<TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiRequestTemplate() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<TransportApiRequestMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("transport-api-request-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(transportApiSettings.getRequestsTopic());
        requestBuilder.admin(transportApiAdmin);

        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<TbProtoQueueMsg<TransportApiResponseMsg>> responseBuilder = TBKafkaConsumerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.topic(transportApiSettings.getResponsesTopic() + "." + serviceInfoProvider.getServiceId());
        responseBuilder.clientId("transport-api-response-" + serviceInfoProvider.getServiceId());
        responseBuilder.groupId("transport-node-" + serviceInfoProvider.getServiceId());
        responseBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportApiResponseMsg.parseFrom(msg.getData()), msg.getHeaders()));
        responseBuilder.admin(transportApiAdmin);

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> templateBuilder = DefaultTbQueueRequestTemplate.builder();
        templateBuilder.queueAdmin(transportApiAdmin);
        templateBuilder.requestTemplate(requestBuilder.build());
        templateBuilder.responseTemplate(responseBuilder.build());
        templateBuilder.maxPendingRequests(transportApiSettings.getMaxPendingRequests());
        templateBuilder.maxRequestTimeout(transportApiSettings.getMaxRequestsTimeout());
        templateBuilder.pollInterval(transportApiSettings.getResponsePollInterval());
        return templateBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("transport-node-rule-engine-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(ruleEngineSettings.getTopic());
        requestBuilder.admin(ruleEngineAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCoreMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("transport-node-core-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(coreSettings.getTopic());
        requestBuilder.admin(coreAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsConsumer() {
        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToTransportMsg>> responseBuilder = TBKafkaConsumerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.topic(transportNotificationSettings.getNotificationsTopic() + "." + serviceInfoProvider.getServiceId());
        responseBuilder.clientId("transport-api-notifications-" + serviceInfoProvider.getServiceId());
        responseBuilder.groupId("transport-node-" + serviceInfoProvider.getServiceId());
        responseBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToTransportMsg.parseFrom(msg.getData()), msg.getHeaders()));
        responseBuilder.admin(notificationAdmin);
        return responseBuilder.build();
    }
}
