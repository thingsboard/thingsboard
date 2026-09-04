// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
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
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.memory.InMemoryStorage;
import org.thingsboard.server.queue.memory.InMemoryTbQueueConsumer;
import org.thingsboard.server.queue.memory.InMemoryTbQueueProducer;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportNotificationSettings;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='in-memory' && (('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true') || '${service.type:null}'=='tb-transport')")
@Slf4j
public class InMemoryTbTransportQueueFactory implements TbTransportQueueFactory {
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueCoreSettings coreSettings;
    private final InMemoryStorage storage;
    private final TopicService topicService;

    public InMemoryTbTransportQueueFactory(TbQueueTransportApiSettings transportApiSettings,
                                           TbQueueTransportNotificationSettings transportNotificationSettings,
                                           TbServiceInfoProvider serviceInfoProvider,
                                           TbQueueCoreSettings coreSettings,
                                           InMemoryStorage storage,
                                           TopicService topicService) {
        this.transportApiSettings = transportApiSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.coreSettings = coreSettings;
        this.storage = storage;
        this.topicService = topicService;
    }

    @Override
    public TbQueueRequestTemplate<TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiRequestTemplate() {
        InMemoryTbQueueProducer<TbProtoQueueMsg<TransportApiRequestMsg>> producerTemplate =
                new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(transportApiSettings.getRequestsTopic()));

        InMemoryTbQueueConsumer<TbProtoQueueMsg<TransportApiResponseMsg>> consumerTemplate =
                new InMemoryTbQueueConsumer<>(storage, topicService.buildTopicName(transportApiSettings.getResponsesTopic() + "." + serviceInfoProvider.getServiceId()));

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> templateBuilder = DefaultTbQueueRequestTemplate.builder();

        templateBuilder.queueAdmin(new TbQueueAdmin() {
            @Override
            public void createTopicIfNotExists(String topic, String properties, boolean force) {}

            @Override
            public void destroy() {}

            @Override
            public void deleteTopic(String topic) {}
        });

        templateBuilder.requestTemplate(producerTemplate);
        templateBuilder.responseTemplate(consumerTemplate);
        templateBuilder.maxPendingRequests(transportApiSettings.getMaxPendingRequests());
        templateBuilder.maxRequestTimeout(transportApiSettings.getMaxRequestsTimeout());
        templateBuilder.pollInterval(transportApiSettings.getResponsePollInterval());
        return templateBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(transportApiSettings.getRequestsTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(coreSettings.getTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(coreSettings.getTopic()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsConsumer() {
        return new InMemoryTbQueueConsumer<>(storage, topicService.buildTopicName(transportNotificationSettings.getNotificationsTopic() + "." + serviceInfoProvider.getServiceId()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(coreSettings.getUsageStatsTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToHousekeeperServiceMsg>> createHousekeeperMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(coreSettings.getHousekeeperTopic()));
    }

}
