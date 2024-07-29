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
package org.thingsboard.server.queue.provider;

import com.google.protobuf.util.JsonFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToOtaPackageStateServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerStatsService;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaProducerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.kafka.TbKafkaTopicConfigs;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueRemoteJsInvokeSettings;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportNotificationSettings;
import org.thingsboard.server.queue.settings.TbQueueVersionControlSettings;

import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='kafka' && '${service.type:null}'=='tb-core'")
public class KafkaTbCoreQueueFactory implements TbCoreQueueFactory {

    private final TbQueueAdmin ruleEngineAdmin;

    private org.thingsboard.server.queue.provider.KafkaQueueAdministration kafkaQueueAdministration = new org.thingsboard.server.queue.provider.KafkaQueueAdministration(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, new AtomicLong());

    public KafkaTbCoreQueueFactory(TopicService topicService,
                                   TbKafkaSettings kafkaSettings,
                                   TbServiceInfoProvider serviceInfoProvider,
                                   TbQueueCoreSettings coreSettings,
                                   TbQueueRuleEngineSettings ruleEngineSettings,
                                   TbQueueTransportApiSettings transportApiSettings,
                                   TbQueueRemoteJsInvokeSettings jsInvokeSettings,
                                   TbQueueVersionControlSettings vcSettings,
                                   TbKafkaConsumerStatsService consumerStatsService,
                                   TbQueueTransportNotificationSettings transportNotificationSettings,
                                   TbKafkaTopicConfigs kafkaTopicConfigs) {
        this.kafkaQueueAdministration.setTopicService(topicService);
        this.kafkaQueueAdministration.setKafkaSettings(kafkaSettings);
        this.kafkaQueueAdministration.setServiceInfoProvider(serviceInfoProvider);
        this.kafkaQueueAdministration.setCoreSettings(coreSettings);
        this.kafkaQueueAdministration.setRuleEngineSettings(ruleEngineSettings);
        this.kafkaQueueAdministration.setTransportApiSettings(transportApiSettings);
        this.kafkaQueueAdministration.setJsInvokeSettings(jsInvokeSettings);
        this.kafkaQueueAdministration.setVcSettings(vcSettings);
        this.kafkaQueueAdministration.setConsumerStatsService(consumerStatsService);
        this.kafkaQueueAdministration.setTransportNotificationSettings(transportNotificationSettings);

        this.kafkaQueueAdministration.setCoreAdmin(new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getCoreConfigs()));
        this.ruleEngineAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getRuleEngineConfigs());
        this.kafkaQueueAdministration.setJsExecutorRequestAdmin(new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getJsExecutorRequestConfigs()));
        this.kafkaQueueAdministration.setJsExecutorResponseAdmin(new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getJsExecutorResponseConfigs()));
        this.kafkaQueueAdministration.setTransportApiRequestAdmin(new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getTransportApiRequestConfigs()));
        this.kafkaQueueAdministration.setTransportApiResponseAdmin(new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getTransportApiResponseConfigs()));
        this.kafkaQueueAdministration.setNotificationAdmin(new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getNotificationsConfigs()));
        this.kafkaQueueAdministration.setFwUpdatesAdmin(new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getFwUpdatesConfigs()));
        this.kafkaQueueAdministration.setVcAdmin(new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getVcConfigs()));
        this.kafkaQueueAdministration.setHousekeeperAdmin(new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getHousekeeperConfigs()));
        this.kafkaQueueAdministration.setHousekeeperReprocessingAdmin(new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getHousekeeperReprocessingConfigs()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToTransportMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaQueueAdministration.getKafkaSettings());
        requestBuilder.clientId("tb-core-transport-notifications-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId());
        requestBuilder.defaultTopic(kafkaQueueAdministration.getTopicService().buildTopicName(kafkaQueueAdministration.getTransportNotificationSettings().getNotificationsTopic()));
        requestBuilder.admin(kafkaQueueAdministration.getNotificationAdmin());
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaQueueAdministration.getKafkaSettings());
        requestBuilder.clientId("tb-core-rule-engine-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId());
        requestBuilder.defaultTopic(kafkaQueueAdministration.getTopicService().buildTopicName(kafkaQueueAdministration.getCoreSettings().getTopic()));
        requestBuilder.admin(kafkaQueueAdministration.getCoreAdmin());
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createRuleEngineNotificationsMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaQueueAdministration.getKafkaSettings());
        requestBuilder.clientId("tb-core-rule-engine-notifications-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId());
        requestBuilder.defaultTopic(kafkaQueueAdministration.getTopicService().buildTopicName(kafkaQueueAdministration.getRuleEngineSettings().getTopic()));
        requestBuilder.admin(kafkaQueueAdministration.getNotificationAdmin());
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCoreMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaQueueAdministration.getKafkaSettings());
        requestBuilder.clientId("tb-core-to-core-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId());
        requestBuilder.defaultTopic(kafkaQueueAdministration.getTopicService().buildTopicName(kafkaQueueAdministration.getCoreSettings().getTopic()));
        requestBuilder.admin(kafkaQueueAdministration.getCoreAdmin());
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCoreNotificationMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaQueueAdministration.getKafkaSettings());
        requestBuilder.clientId("tb-core-to-core-notifications-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId());
        requestBuilder.defaultTopic(kafkaQueueAdministration.getTopicService().buildTopicName(kafkaQueueAdministration.getCoreSettings().getTopic()));
        requestBuilder.admin(kafkaQueueAdministration.getNotificationAdmin());
        return requestBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> createToCoreMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToCoreMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaQueueAdministration.getKafkaSettings());
        consumerBuilder.topic(kafkaQueueAdministration.getTopicService().buildTopicName(kafkaQueueAdministration.getCoreSettings().getTopic()));
        consumerBuilder.clientId("tb-core-consumer-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId() + "-" + kafkaQueueAdministration.getConsumerCount().incrementAndGet());
        consumerBuilder.groupId(kafkaQueueAdministration.getTopicService().buildTopicName("tb-core-node"));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(kafkaQueueAdministration.getCoreAdmin());
        consumerBuilder.statsService(kafkaQueueAdministration.getConsumerStatsService());
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreNotificationMsg>> createToCoreNotificationsMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToCoreNotificationMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaQueueAdministration.getKafkaSettings());
        consumerBuilder.topic(kafkaQueueAdministration.getTopicService().getNotificationsTopic(ServiceType.TB_CORE, kafkaQueueAdministration.getServiceInfoProvider().getServiceId()).getFullTopicName());
        consumerBuilder.clientId("tb-core-notifications-consumer-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId());
        consumerBuilder.groupId(kafkaQueueAdministration.getTopicService().buildTopicName("tb-core-notifications-node-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId()));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(kafkaQueueAdministration.getNotificationAdmin());
        consumerBuilder.statsService(kafkaQueueAdministration.getConsumerStatsService());
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportApiRequestMsg>> createTransportApiRequestConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<TransportApiRequestMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaQueueAdministration.getKafkaSettings());
        consumerBuilder.topic(kafkaQueueAdministration.getTopicService().buildTopicName(kafkaQueueAdministration.getTransportApiSettings().getRequestsTopic()));
        consumerBuilder.clientId("tb-core-transport-api-consumer-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId());
        consumerBuilder.groupId(kafkaQueueAdministration.getTopicService().buildTopicName("tb-core-transport-api-consumer"));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportApiRequestMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(kafkaQueueAdministration.getTransportApiRequestAdmin());
        consumerBuilder.statsService(kafkaQueueAdministration.getConsumerStatsService());
        return consumerBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiResponseProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<TransportApiResponseMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaQueueAdministration.getKafkaSettings());
        requestBuilder.clientId("tb-core-transport-api-producer-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId());
        requestBuilder.defaultTopic(kafkaQueueAdministration.getTopicService().buildTopicName(kafkaQueueAdministration.getTransportApiSettings().getResponsesTopic()));
        requestBuilder.admin(kafkaQueueAdministration.getTransportApiResponseAdmin());
        return requestBuilder.build();
    }

    @Override
    @Bean
    public TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> createRemoteJsRequestTemplate() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaQueueAdministration.getKafkaSettings());
        requestBuilder.clientId("producer-js-invoke-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId());
        requestBuilder.defaultTopic(kafkaQueueAdministration.getJsInvokeSettings().getRequestTopic());
        requestBuilder.admin(kafkaQueueAdministration.getJsExecutorRequestAdmin());

        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> responseBuilder = TbKafkaConsumerTemplate.builder();
        responseBuilder.settings(kafkaQueueAdministration.getKafkaSettings());
        responseBuilder.topic(kafkaQueueAdministration.getJsInvokeSettings().getResponseTopic() + "." + kafkaQueueAdministration.getServiceInfoProvider().getServiceId());
        responseBuilder.clientId("js-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId());
        responseBuilder.groupId(kafkaQueueAdministration.getTopicService().buildTopicName("rule-engine-node-") + kafkaQueueAdministration.getServiceInfoProvider().getServiceId());
        responseBuilder.decoder(msg -> {
                    JsInvokeProtos.RemoteJsResponse.Builder builder = JsInvokeProtos.RemoteJsResponse.newBuilder();
                    JsonFormat.parser().ignoringUnknownFields().merge(new String(msg.getData(), StandardCharsets.UTF_8), builder);
                    return new TbProtoQueueMsg<>(msg.getKey(), builder.build(), msg.getHeaders());
                }
        );
        responseBuilder.admin(kafkaQueueAdministration.getJsExecutorResponseAdmin());
        responseBuilder.statsService(kafkaQueueAdministration.getConsumerStatsService());

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> builder = DefaultTbQueueRequestTemplate.builder();
        builder.queueAdmin(kafkaQueueAdministration.getJsExecutorResponseAdmin());
        builder.requestTemplate(requestBuilder.build());
        builder.responseTemplate(responseBuilder.build());
        builder.maxPendingRequests(kafkaQueueAdministration.getJsInvokeSettings().getMaxPendingRequests());
        builder.maxRequestTimeout(kafkaQueueAdministration.getJsInvokeSettings().getMaxRequestsTimeout());
        builder.pollInterval(kafkaQueueAdministration.getJsInvokeSettings().getResponsePollInterval());
        return builder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToUsageStatsServiceMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaQueueAdministration.getKafkaSettings());
        consumerBuilder.topic(kafkaQueueAdministration.getTopicService().buildTopicName(kafkaQueueAdministration.getCoreSettings().getUsageStatsTopic()));
        consumerBuilder.clientId("tb-core-us-consumer-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId());
        consumerBuilder.groupId(kafkaQueueAdministration.getTopicService().buildTopicName("tb-core-us-consumer"));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToUsageStatsServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(kafkaQueueAdministration.getCoreAdmin());
        consumerBuilder.statsService(kafkaQueueAdministration.getConsumerStatsService());
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaQueueAdministration.getKafkaSettings());
        consumerBuilder.topic(kafkaQueueAdministration.getTopicService().buildTopicName(kafkaQueueAdministration.getCoreSettings().getOtaPackageTopic()));
        consumerBuilder.clientId("tb-core-ota-consumer-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId());
        consumerBuilder.groupId(kafkaQueueAdministration.getTopicService().buildTopicName("tb-core-ota-consumer"));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToOtaPackageStateServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(kafkaQueueAdministration.getFwUpdatesAdmin());
        consumerBuilder.statsService(kafkaQueueAdministration.getConsumerStatsService());
        return consumerBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaQueueAdministration.getKafkaSettings());
        requestBuilder.clientId("tb-core-ota-producer-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId());
        requestBuilder.defaultTopic(kafkaQueueAdministration.getTopicService().buildTopicName(kafkaQueueAdministration.getCoreSettings().getOtaPackageTopic()));
        requestBuilder.admin(kafkaQueueAdministration.getFwUpdatesAdmin());
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToUsageStatsServiceMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaQueueAdministration.getKafkaSettings());
        requestBuilder.clientId("tb-core-us-producer-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId());
        requestBuilder.defaultTopic(kafkaQueueAdministration.getTopicService().buildTopicName(kafkaQueueAdministration.getCoreSettings().getUsageStatsTopic()));
        requestBuilder.admin(kafkaQueueAdministration.getCoreAdmin());
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToVersionControlServiceMsg>> createVersionControlMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToVersionControlServiceMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaQueueAdministration.getKafkaSettings());
        requestBuilder.clientId("tb-core-vc-producer-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId());
        requestBuilder.defaultTopic(kafkaQueueAdministration.getTopicService().buildTopicName(kafkaQueueAdministration.getVcSettings().getTopic()));
        requestBuilder.admin(kafkaQueueAdministration.getVcAdmin());
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> createHousekeeperMsgProducer() {
        return TbKafkaProducerTemplate.<TbProtoQueueMsg<ToHousekeeperServiceMsg>>builder()
                .settings(kafkaQueueAdministration.getKafkaSettings())
                .clientId("tb-core-housekeeper-producer-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId())
                .defaultTopic(kafkaQueueAdministration.getTopicService().buildTopicName(kafkaQueueAdministration.getCoreSettings().getHousekeeperTopic()))
                .admin(kafkaQueueAdministration.getHousekeeperAdmin())
                .build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> createHousekeeperMsgConsumer() {
        return TbKafkaConsumerTemplate.<TbProtoQueueMsg<ToHousekeeperServiceMsg>>builder()
                .settings(kafkaQueueAdministration.getKafkaSettings())
                .topic(kafkaQueueAdministration.getTopicService().buildTopicName(kafkaQueueAdministration.getCoreSettings().getHousekeeperTopic()))
                .clientId("tb-core-housekeeper-consumer-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId())
                .groupId(kafkaQueueAdministration.getTopicService().buildTopicName("tb-core-housekeeper-consumer"))
                .decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToHousekeeperServiceMsg.parseFrom(msg.getData()), msg.getHeaders()))
                .admin(kafkaQueueAdministration.getHousekeeperAdmin())
                .statsService(kafkaQueueAdministration.getConsumerStatsService())
                .build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> createHousekeeperReprocessingMsgProducer() {
        return TbKafkaProducerTemplate.<TbProtoQueueMsg<ToHousekeeperServiceMsg>>builder()
                .settings(kafkaQueueAdministration.getKafkaSettings())
                .clientId("tb-core-housekeeper-reprocessing-producer-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId())
                .defaultTopic(kafkaQueueAdministration.getTopicService().buildTopicName(kafkaQueueAdministration.getCoreSettings().getHousekeeperReprocessingTopic()))
                .admin(kafkaQueueAdministration.getHousekeeperReprocessingAdmin())
                .build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> createHousekeeperReprocessingMsgConsumer() {
        return TbKafkaConsumerTemplate.<TbProtoQueueMsg<ToHousekeeperServiceMsg>>builder()
                .settings(kafkaQueueAdministration.getKafkaSettings())
                .topic(kafkaQueueAdministration.getTopicService().buildTopicName(kafkaQueueAdministration.getCoreSettings().getHousekeeperReprocessingTopic()))
                .clientId("tb-core-housekeeper-reprocessing-consumer-" + kafkaQueueAdministration.getServiceInfoProvider().getServiceId())
                .groupId(kafkaQueueAdministration.getTopicService().buildTopicName("tb-core-housekeeper-reprocessing-consumer"))
                .decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToHousekeeperServiceMsg.parseFrom(msg.getData()), msg.getHeaders()))
                .admin(kafkaQueueAdministration.getHousekeeperReprocessingAdmin())
                .statsService(kafkaQueueAdministration.getConsumerStatsService())
                .build();
    }


    @PreDestroy
    private void destroy() {
        if (kafkaQueueAdministration.getCoreAdmin() != null) {
            kafkaQueueAdministration.getCoreAdmin().destroy();
        }
        if (ruleEngineAdmin != null) {
            ruleEngineAdmin.destroy();
        }
        if (kafkaQueueAdministration.getJsExecutorRequestAdmin() != null) {
            kafkaQueueAdministration.getJsExecutorRequestAdmin().destroy();
        }
        if (kafkaQueueAdministration.getJsExecutorResponseAdmin() != null) {
            kafkaQueueAdministration.getJsExecutorResponseAdmin().destroy();
        }
        if (kafkaQueueAdministration.getTransportApiRequestAdmin() != null) {
            kafkaQueueAdministration.getTransportApiRequestAdmin().destroy();
        }
        if (kafkaQueueAdministration.getTransportApiResponseAdmin() != null) {
            kafkaQueueAdministration.getTransportApiResponseAdmin().destroy();
        }
        if (kafkaQueueAdministration.getNotificationAdmin() != null) {
            kafkaQueueAdministration.getNotificationAdmin().destroy();
        }
        if (kafkaQueueAdministration.getFwUpdatesAdmin() != null) {
            kafkaQueueAdministration.getFwUpdatesAdmin().destroy();
        }
        if (kafkaQueueAdministration.getVcAdmin() != null) {
            kafkaQueueAdministration.getVcAdmin().destroy();
        }
    }
}
