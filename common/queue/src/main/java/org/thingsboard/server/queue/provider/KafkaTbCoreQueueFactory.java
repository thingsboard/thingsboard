// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.queue.provider;

import com.google.protobuf.util.JsonFormat;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.integration.IntegrationApiRequestMsg;
import org.thingsboard.server.gen.integration.IntegrationApiResponseMsg;
import org.thingsboard.server.gen.integration.ToCoreIntegrationMsg;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorDownlinkMsg;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorNotificationMsg;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AgentBulkOperationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.FromEdqsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.JobStatsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToAgentNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeEventNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToOtaPackageStateServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTbReportNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.queue.TbEdgeQueueAdmin;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.edqs.EdqsConfig;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerStatsService;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaProducerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.kafka.TbKafkaTopicConfigs;
import org.thingsboard.server.queue.settings.TasksQueueConfig;
import org.thingsboard.server.queue.settings.TbQueueCalculatedFieldSettings;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueEdgeSettings;
import org.thingsboard.server.queue.settings.TbQueueIntegrationApiSettings;
import org.thingsboard.server.queue.settings.TbQueueIntegrationExecutorSettings;
import org.thingsboard.server.queue.settings.TbQueueRemoteJsInvokeSettings;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportNotificationSettings;
import org.thingsboard.server.queue.settings.TbQueueVersionControlSettings;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='kafka' && '${service.type:null}'=='tb-core'")
public class KafkaTbCoreQueueFactory implements TbCoreQueueFactory {

    private final TopicService topicService;
    private final TbKafkaSettings kafkaSettings;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueIntegrationApiSettings integrationApiSettings;
    private final TbQueueRemoteJsInvokeSettings jsInvokeSettings;
    private final TbQueueVersionControlSettings vcSettings;
    private final TbKafkaConsumerStatsService consumerStatsService;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    private final TbQueueEdgeSettings edgeSettings;
    private final TbQueueIntegrationExecutorSettings integrationExecutorSettings;
    private final TbQueueCalculatedFieldSettings calculatedFieldSettings;
    private final EdqsConfig edqsConfig;
    private final TasksQueueConfig tasksQueueConfig;

    private final TbQueueAdmin coreAdmin;
    private final TbQueueAdmin ruleEngineAdmin;
    private final TbQueueAdmin jsExecutorRequestAdmin;
    private final TbQueueAdmin jsExecutorResponseAdmin;
    private final TbQueueAdmin transportApiRequestAdmin;
    private final TbQueueAdmin transportApiResponseAdmin;
    private final TbQueueAdmin integrationApiRequestAdmin;
    private final TbQueueAdmin integrationApiResponseAdmin;
    private final TbQueueAdmin notificationAdmin;
    private final TbQueueAdmin fwUpdatesAdmin;
    private final TbQueueAdmin vcAdmin;
    private final TbQueueAdmin housekeeperAdmin;
    private final TbQueueAdmin housekeeperReprocessingAdmin;
    private final TbEdgeQueueAdmin edgeAdmin;
    private final TbQueueAdmin edgeEventAdmin;
    private final TbQueueAdmin cfAdmin;
    private final TbQueueAdmin edqsEventsAdmin;
    private final TbKafkaAdmin edqsRequestsAdmin;
    private final TbQueueAdmin tasksAdmin;
    private final TbQueueAdmin agentBulkOpsAdmin;

    private final AtomicLong integrationConsumerCount = new AtomicLong();
    private final AtomicLong coreConsumerCount = new AtomicLong();
    private final AtomicLong edgeConsumerCount = new AtomicLong();

    public KafkaTbCoreQueueFactory(TopicService topicService,
                                   TbKafkaSettings kafkaSettings,
                                   TbServiceInfoProvider serviceInfoProvider,
                                   TbQueueCoreSettings coreSettings,
                                   TbQueueRuleEngineSettings ruleEngineSettings,
                                   TbQueueTransportApiSettings transportApiSettings,
                                   TbQueueIntegrationApiSettings integrationApiSettings,
                                   TbQueueRemoteJsInvokeSettings jsInvokeSettings,
                                   TbQueueVersionControlSettings vcSettings,
                                   TbQueueEdgeSettings edgeSettings,
                                   TbKafkaConsumerStatsService consumerStatsService,
                                   TbQueueTransportNotificationSettings transportNotificationSettings,
                                   TbQueueIntegrationExecutorSettings integrationExecutorSettings,
                                   TbQueueCalculatedFieldSettings calculatedFieldSettings,
                                   EdqsConfig edqsConfig,
                                   TasksQueueConfig tasksQueueConfig,
                                   TbKafkaTopicConfigs kafkaTopicConfigs) {
        this.topicService = topicService;
        this.kafkaSettings = kafkaSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.coreSettings = coreSettings;
        this.ruleEngineSettings = ruleEngineSettings;
        this.transportApiSettings = transportApiSettings;
        this.integrationApiSettings = integrationApiSettings;
        this.jsInvokeSettings = jsInvokeSettings;
        this.vcSettings = vcSettings;
        this.consumerStatsService = consumerStatsService;
        this.transportNotificationSettings = transportNotificationSettings;
        this.edgeSettings = edgeSettings;
        this.integrationExecutorSettings = integrationExecutorSettings;
        this.calculatedFieldSettings = calculatedFieldSettings;
        this.edqsConfig = edqsConfig;
        this.tasksQueueConfig = tasksQueueConfig;

        this.coreAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getCoreConfigs());
        this.ruleEngineAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getRuleEngineConfigs());
        this.jsExecutorRequestAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getJsExecutorRequestConfigs());
        this.jsExecutorResponseAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getJsExecutorResponseConfigs());
        this.transportApiRequestAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getTransportApiRequestConfigs());
        this.transportApiResponseAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getTransportApiResponseConfigs());
        this.integrationApiRequestAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getIntegrationApiRequestConfigs());
        this.integrationApiResponseAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getIntegrationApiResponseConfigs());
        this.notificationAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getNotificationsConfigs());
        this.fwUpdatesAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getFwUpdatesConfigs());
        this.vcAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getVcConfigs());
        this.housekeeperAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getHousekeeperConfigs());
        this.housekeeperReprocessingAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getHousekeeperReprocessingConfigs());
        this.edgeAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getEdgeConfigs());
        this.edgeEventAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getEdgeEventConfigs());
        this.cfAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getCalculatedFieldConfigs());
        this.edqsEventsAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getEdqsEventsConfigs());
        this.edqsRequestsAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getEdqsRequestsConfigs());
        this.tasksAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getTasksConfigs());
        this.agentBulkOpsAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getAgentBulkOpsConfigs());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToTransportMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-transport-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(transportNotificationSettings.getNotificationsTopic()));
        requestBuilder.admin(notificationAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        return createRuleEngineProducerTemplate("tb-core-rule-engine-");
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createRuleEngineNotificationsMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-rule-engine-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(ruleEngineSettings.getTopic()));
        requestBuilder.admin(notificationAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCoreMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-to-core-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(coreSettings.getTopic()));
        requestBuilder.admin(coreAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCoreNotificationMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-to-core-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.getNotificationsTopic(ServiceType.TB_CORE, serviceInfoProvider.getServiceId()).getFullTopicName());
        requestBuilder.admin(notificationAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> createIntegrationExecutorNotificationsMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-to-ie-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(integrationExecutorSettings.getNotificationsTopic()));
        requestBuilder.admin(notificationAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> createIntegrationExecutorDownlinkMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-to-ie-downlinks-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(integrationExecutorSettings.getDownlinkTopic()));
        requestBuilder.admin(notificationAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> createToCoreMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToCoreMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(topicService.buildTopicName(coreSettings.getTopic()));
        consumerBuilder.clientId("tb-core-consumer-" + serviceInfoProvider.getServiceId() + "-" + coreConsumerCount.incrementAndGet());
        consumerBuilder.groupId(topicService.buildTopicName("tb-core-node"));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(coreAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreNotificationMsg>> createToCoreNotificationsMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToCoreNotificationMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(topicService.getNotificationsTopic(ServiceType.TB_CORE, serviceInfoProvider.getServiceId()).getFullTopicName());
        consumerBuilder.clientId("tb-core-notifications-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId(topicService.buildTopicName("tb-core-notifications-node-" + serviceInfoProvider.getServiceId()));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(notificationAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> createToIntegrationExecutorNotificationsMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(topicService.getNotificationsTopic(ServiceType.TB_INTEGRATION_EXECUTOR, serviceInfoProvider.getServiceId()).getFullTopicName());
        consumerBuilder.clientId("tb-ie-notifications-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId(topicService.buildTopicName("ie-notifications-node-" + serviceInfoProvider.getServiceId()));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToIntegrationExecutorNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(notificationAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> createToIntegrationExecutorDownlinkMsgConsumer(IntegrationType integrationType) {
        String queueName = integrationType.name();
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(topicService.buildTopicName(integrationExecutorSettings.getIntegrationDownlinkTopic(integrationType)));
        consumerBuilder.clientId("ie-" + queueName + "-consumer-" + serviceInfoProvider.getServiceId() + "-" + integrationConsumerCount.incrementAndGet());
        consumerBuilder.groupId(topicService.buildTopicName("ie-" + queueName + "-consumer"));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToIntegrationExecutorDownlinkMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(ruleEngineAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreIntegrationMsg>> createToCoreIntegrationMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToCoreIntegrationMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(topicService.buildTopicName(integrationExecutorSettings.getUplinkTopic()));
        consumerBuilder.clientId("tb-core-integrations-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId(topicService.buildTopicName("tb-core-integrations-node"));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreIntegrationMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(coreAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportApiRequestMsg>> createTransportApiRequestConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<TransportApiRequestMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(topicService.buildTopicName(transportApiSettings.getRequestsTopic()));
        consumerBuilder.clientId("tb-core-transport-api-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId(topicService.buildTopicName("tb-core-transport-api-consumer"));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportApiRequestMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(transportApiRequestAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiResponseProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<TransportApiResponseMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-transport-api-producer-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(transportApiSettings.getResponsesTopic()));
        requestBuilder.admin(transportApiResponseAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<IntegrationApiRequestMsg>> createIntegrationApiRequestConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<IntegrationApiRequestMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(topicService.buildTopicName(integrationApiSettings.getRequestsTopic()));
        consumerBuilder.clientId("tb-core-integration-api-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId(topicService.buildTopicName("tb-core-integration-api-consumer"));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), IntegrationApiRequestMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(integrationApiRequestAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<IntegrationApiResponseMsg>> createIntegrationApiResponseProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<IntegrationApiResponseMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-integration-api-producer-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(integrationApiSettings.getResponsesTopic()));
        requestBuilder.admin(integrationApiResponseAdmin);
        return requestBuilder.build();
    }

    @Override
    @Bean
    public TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> createRemoteJsRequestTemplate() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("producer-js-invoke-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(jsInvokeSettings.getRequestTopic());
        requestBuilder.admin(jsExecutorRequestAdmin);

        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> responseBuilder = TbKafkaConsumerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.topic(jsInvokeSettings.getResponseTopic() + "." + serviceInfoProvider.getServiceId());
        responseBuilder.clientId("js-" + serviceInfoProvider.getServiceId());
        responseBuilder.groupId(topicService.buildTopicName("rule-engine-node-") + serviceInfoProvider.getServiceId());
        responseBuilder.decoder(msg -> {
                    JsInvokeProtos.RemoteJsResponse.Builder builder = JsInvokeProtos.RemoteJsResponse.newBuilder();
                    JsonFormat.parser().ignoringUnknownFields().merge(new String(msg.getData(), StandardCharsets.UTF_8), builder);
                    return new TbProtoQueueMsg<>(msg.getKey(), builder.build(), msg.getHeaders());
                }
        );
        responseBuilder.admin(jsExecutorResponseAdmin);
        responseBuilder.statsService(consumerStatsService);

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> builder = DefaultTbQueueRequestTemplate.builder();
        builder.queueAdmin(jsExecutorResponseAdmin);
        builder.requestTemplate(requestBuilder.build());
        builder.responseTemplate(responseBuilder.build());
        builder.maxPendingRequests(jsInvokeSettings.getMaxPendingRequests());
        builder.maxRequestTimeout(jsInvokeSettings.getMaxRequestsTimeout());
        builder.pollInterval(jsInvokeSettings.getResponsePollInterval());
        return builder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToUsageStatsServiceMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(topicService.buildTopicName(coreSettings.getUsageStatsTopic()));
        consumerBuilder.clientId("tb-core-us-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId(topicService.buildTopicName("tb-core-us-consumer"));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToUsageStatsServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(coreAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(topicService.buildTopicName(coreSettings.getOtaPackageTopic()));
        consumerBuilder.clientId("tb-core-ota-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId(topicService.buildTopicName("tb-core-ota-consumer"));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToOtaPackageStateServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(fwUpdatesAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-ota-producer-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(coreSettings.getOtaPackageTopic()));
        requestBuilder.admin(fwUpdatesAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToUsageStatsServiceMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-us-producer-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(coreSettings.getUsageStatsTopic()));
        requestBuilder.admin(coreAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createIntegrationRuleEngineMsgProducer() {
        return createRuleEngineProducerTemplate("tb-core-integration-rule-engine-");
    }

    private TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineProducerTemplate(String clientIdPrefix) {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId(clientIdPrefix + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(ruleEngineSettings.getTopic()));
        requestBuilder.admin(ruleEngineAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToVersionControlServiceMsg>> createVersionControlMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToVersionControlServiceMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-vc-producer-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(vcSettings.getTopic()));
        requestBuilder.admin(vcAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> createHousekeeperMsgProducer() {
        return TbKafkaProducerTemplate.<TbProtoQueueMsg<ToHousekeeperServiceMsg>>builder()
                .settings(kafkaSettings)
                .clientId("tb-core-housekeeper-producer-" + serviceInfoProvider.getServiceId())
                .defaultTopic(topicService.buildTopicName(coreSettings.getHousekeeperTopic()))
                .admin(housekeeperAdmin)
                .build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> createHousekeeperMsgConsumer() {
        return TbKafkaConsumerTemplate.<TbProtoQueueMsg<ToHousekeeperServiceMsg>>builder()
                .settings(kafkaSettings)
                .topic(topicService.buildTopicName(coreSettings.getHousekeeperTopic()))
                .clientId("tb-core-housekeeper-consumer-" + serviceInfoProvider.getServiceId())
                .groupId(topicService.buildTopicName("tb-core-housekeeper-consumer"))
                .decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToHousekeeperServiceMsg.parseFrom(msg.getData()), msg.getHeaders()))
                .admin(housekeeperAdmin)
                .statsService(consumerStatsService)
                .build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> createHousekeeperReprocessingMsgProducer() {
        return TbKafkaProducerTemplate.<TbProtoQueueMsg<ToHousekeeperServiceMsg>>builder()
                .settings(kafkaSettings)
                .clientId("tb-core-housekeeper-reprocessing-producer-" + serviceInfoProvider.getServiceId())
                .defaultTopic(topicService.buildTopicName(coreSettings.getHousekeeperReprocessingTopic()))
                .admin(housekeeperReprocessingAdmin)
                .build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> createHousekeeperReprocessingMsgConsumer() {
        return TbKafkaConsumerTemplate.<TbProtoQueueMsg<ToHousekeeperServiceMsg>>builder()
                .settings(kafkaSettings)
                .topic(topicService.buildTopicName(coreSettings.getHousekeeperReprocessingTopic()))
                .clientId("tb-core-housekeeper-reprocessing-consumer-" + serviceInfoProvider.getServiceId())
                .groupId(topicService.buildTopicName("tb-core-housekeeper-reprocessing-consumer"))
                .decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToHousekeeperServiceMsg.parseFrom(msg.getData()), msg.getHeaders()))
                .admin(housekeeperReprocessingAdmin)
                .statsService(consumerStatsService)
                .build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToEdgeMsg>> createEdgeMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToEdgeMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(topicService.buildTopicName(edgeSettings.getTopic()));
        consumerBuilder.clientId("tb-core-edge-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId(topicService.buildTopicName("tb-core-edge-consumer"));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToEdgeMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(edgeAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToEdgeMsg>> createEdgeMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToEdgeMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-to-edge-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(edgeSettings.getTopic()));
        requestBuilder.admin(edgeAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToEdgeNotificationMsg>> createToEdgeNotificationsMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToEdgeNotificationMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(topicService.getEdgeNotificationsTopic(serviceInfoProvider.getServiceId()).getFullTopicName());
        consumerBuilder.clientId("tb-edge-notifications-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId(topicService.buildTopicName("tb-edge-notifications-node-" + serviceInfoProvider.getServiceId()));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToEdgeNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(notificationAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToEdgeNotificationMsg>> createEdgeNotificationsMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToEdgeNotificationMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-to-edge-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.getEdgeNotificationsTopic(serviceInfoProvider.getServiceId()).getFullTopicName());
        requestBuilder.admin(notificationAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToAgentNotificationMsg>> createToAgentNotificationsMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToAgentNotificationMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(topicService.getAgentNotificationsTopic(serviceInfoProvider.getServiceId()).getFullTopicName());
        consumerBuilder.clientId("tb-agent-notifications-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId(topicService.buildTopicName("tb-agent-notifications-node-" + serviceInfoProvider.getServiceId()));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToAgentNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(notificationAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToAgentNotificationMsg>> createAgentNotificationsMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToAgentNotificationMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-to-agent-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.getAgentNotificationsTopic(serviceInfoProvider.getServiceId()).getFullTopicName());
        requestBuilder.admin(notificationAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<AgentBulkOperationMsg>> createAgentBulkOpsMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<AgentBulkOperationMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(topicService.getAgentBulkOpsTopic().getFullTopicName());
        consumerBuilder.clientId("tb-core-agent-bulk-ops-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId(topicService.buildTopicName("tb-core-agent-bulk-ops"));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), AgentBulkOperationMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(agentBulkOpsAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<AgentBulkOperationMsg>> createAgentBulkOpsMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<AgentBulkOperationMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-agent-bulk-ops-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.getAgentBulkOpsTopic().getFullTopicName());
        requestBuilder.admin(agentBulkOpsAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> createEdgeEventMsgConsumer(TenantId tenantId, EdgeId edgeId) {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        String topic = topicService.buildEdgeEventNotificationsTopicPartitionInfo(tenantId, edgeId).getTopic();

        edgeAdmin.syncEdgeNotificationsOffsets(topicService.buildTopicName("tb-core-edge-event-consumer"), topic);

        consumerBuilder.topic(topic);
        consumerBuilder.clientId("tb-core-edge-event-consumer-" + serviceInfoProvider.getServiceId() + "-" + edgeConsumerCount.incrementAndGet());
        consumerBuilder.groupId(topic);
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToEdgeEventNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(edgeEventAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> createEdgeEventMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-edge-event-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName("edge-events"));
        requestBuilder.admin(edgeEventAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCalculatedFieldMsg>> createToCalculatedFieldMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCalculatedFieldMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-to-calculated-field-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(calculatedFieldSettings.getEventTopic()));
        requestBuilder.admin(cfAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCalculatedFieldNotificationMsg>> createToCalculatedFieldNotificationMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCalculatedFieldNotificationMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-calculated-field-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(calculatedFieldSettings.getEventTopic()));
        requestBuilder.admin(notificationAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToEdqsMsg>> createEdqsEventsProducer() {
        return TbKafkaProducerTemplate.<TbProtoQueueMsg<ToEdqsMsg>>builder()
                .clientId("edqs-events-producer-" + serviceInfoProvider.getServiceId())
                .defaultTopic(topicService.buildTopicName(edqsConfig.getEventsTopic()))
                .settings(kafkaSettings)
                .admin(edqsEventsAdmin)
                .build();
    }

    @Override
    public TbQueueRequestTemplate<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>> createEdqsRequestTemplate() {
        var requestProducer = TbKafkaProducerTemplate.<TbProtoQueueMsg<ToEdqsMsg>>builder()
                .settings(kafkaSettings)
                .clientId("edqs-request-" + serviceInfoProvider.getServiceId())
                .defaultTopic(topicService.buildTopicName(edqsConfig.getRequestsTopic()))
                .admin(edqsRequestsAdmin);

        var responseConsumer = TbKafkaConsumerTemplate.<TbProtoQueueMsg<FromEdqsMsg>>builder()
                .settings(kafkaSettings)
                .topic(topicService.buildTopicName(edqsConfig.getResponsesTopic() + "." + serviceInfoProvider.getServiceId()))
                .clientId("tb-core-edqs-response-consumer-" + serviceInfoProvider.getServiceId())
                .groupId(topicService.buildTopicName("tb-core-edqs-response-consumer-" + serviceInfoProvider.getServiceId()))
                .decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), FromEdqsMsg.parseFrom(msg.getData()), msg.getHeaders()))
                .admin(edqsRequestsAdmin)
                .statsService(consumerStatsService);

        return DefaultTbQueueRequestTemplate.<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>>builder()
                .queueAdmin(edqsRequestsAdmin)
                .requestTemplate(requestProducer.build())
                .responseTemplate(responseConsumer.build())
                .maxPendingRequests(edqsConfig.getMaxPendingRequests())
                .maxRequestTimeout(edqsConfig.getMaxRequestTimeout())
                .pollInterval(edqsConfig.getPollInterval())
                .build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<JobStatsMsg>> createJobStatsConsumer() {
        return TbKafkaConsumerTemplate.<TbProtoQueueMsg<JobStatsMsg>>builder()
                .settings(kafkaSettings)
                .topic(topicService.buildTopicName(tasksQueueConfig.getStatsTopic()))
                .clientId("job-stats-consumer-" + serviceInfoProvider.getServiceId())
                .groupId(topicService.buildTopicName("job-stats-consumer-group"))
                .decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), JobStatsMsg.parseFrom(msg.getData()), msg.getHeaders()))
                .admin(tasksAdmin)
                .statsService(consumerStatsService)
                .build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTbReportNotificationMsg>> createTbReportNotificationsMsgProducer() {
        return TbKafkaProducerTemplate.<TbProtoQueueMsg<ToTbReportNotificationMsg>>builder()
                .clientId("to-report-notifications-producer-" + serviceInfoProvider.getServiceId())
                .defaultTopic(topicService.getNotificationsTopic(ServiceType.TB_REPORT, serviceInfoProvider.getServiceId()).getFullTopicName())
                .settings(kafkaSettings)
                .admin(notificationAdmin)
                .build();
    }

    @PreDestroy
    private void destroy() {
        if (coreAdmin != null) {
            coreAdmin.destroy();
        }
        if (ruleEngineAdmin != null) {
            ruleEngineAdmin.destroy();
        }
        if (jsExecutorRequestAdmin != null) {
            jsExecutorRequestAdmin.destroy();
        }
        if (jsExecutorResponseAdmin != null) {
            jsExecutorResponseAdmin.destroy();
        }
        if (transportApiRequestAdmin != null) {
            transportApiRequestAdmin.destroy();
        }
        if (transportApiResponseAdmin != null) {
            transportApiResponseAdmin.destroy();
        }
        if (notificationAdmin != null) {
            notificationAdmin.destroy();
        }
        if (fwUpdatesAdmin != null) {
            fwUpdatesAdmin.destroy();
        }
        if (vcAdmin != null) {
            vcAdmin.destroy();
        }
        if (integrationApiRequestAdmin != null) {
            integrationApiRequestAdmin.destroy();
        }
        if (integrationApiResponseAdmin != null) {
            integrationApiResponseAdmin.destroy();
        }
        if (cfAdmin != null) {
            cfAdmin.destroy();
        }
    }

}
