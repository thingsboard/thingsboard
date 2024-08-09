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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.memory.InMemoryStorage;
import org.thingsboard.server.queue.memory.InMemoryTbQueueConsumer;
import org.thingsboard.server.queue.memory.InMemoryTbQueueProducer;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueEdgeSettings;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportNotificationSettings;
import org.thingsboard.server.queue.settings.TbQueueVersionControlSettings;

@Slf4j
@Component
@ConditionalOnExpression("'${queue.type:null}'=='in-memory' && '${service.type:null}'=='monolith'")
public class InMemoryMonolithQueueFactory implements TbCoreQueueFactory, TbRuleEngineQueueFactory, TbVersionControlQueueFactory {

    private final TopicService topicService;
    private final TbQueueCoreSettings coreSettings;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbQueueVersionControlSettings vcSettings;
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    private final TbQueueEdgeSettings edgeSettings;
    private final InMemoryStorage storage;

    public InMemoryMonolithQueueFactory(TopicService topicService, TbQueueCoreSettings coreSettings,
                                        TbQueueRuleEngineSettings ruleEngineSettings,
                                        TbQueueVersionControlSettings vcSettings,
                                        TbServiceInfoProvider serviceInfoProvider,
                                        TbQueueTransportApiSettings transportApiSettings,
                                        TbQueueTransportNotificationSettings transportNotificationSettings,
                                        TbQueueEdgeSettings edgeSettings,
                                        InMemoryStorage storage) {
        this.topicService = topicService;
        this.coreSettings = coreSettings;
        this.vcSettings = vcSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.ruleEngineSettings = ruleEngineSettings;
        this.transportApiSettings = transportApiSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.edgeSettings = edgeSettings;
        this.storage = storage;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToTransportMsg>> createTransportNotificationsMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(transportNotificationSettings.getNotificationsTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(ruleEngineSettings.getTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineNotificationMsg>> createRuleEngineNotificationsMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(ruleEngineSettings.getTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> createTbCoreMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(coreSettings.getTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.getNotificationsTopic(ServiceType.TB_CORE, serviceInfoProvider.getServiceId()).getFullTopicName());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> createToVersionControlMsgConsumer() {
        return new InMemoryTbQueueConsumer<>(storage, topicService.buildTopicName(vcSettings.getTopic()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> createToRuleEngineMsgConsumer(Queue configuration) {
        return new InMemoryTbQueueConsumer<>(storage, topicService.buildTopicName(configuration.getTopic()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineNotificationMsg>> createToRuleEngineNotificationsMsgConsumer() {
        return new InMemoryTbQueueConsumer<>(storage, topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, serviceInfoProvider.getServiceId()).getFullTopicName());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> createToCoreMsgConsumer() {
        return new InMemoryTbQueueConsumer<>(storage, topicService.buildTopicName(coreSettings.getTopic()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> createToCoreNotificationsMsgConsumer() {
        return new InMemoryTbQueueConsumer<>(storage, topicService.getNotificationsTopic(ServiceType.TB_CORE, serviceInfoProvider.getServiceId()).getFullTopicName());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>> createTransportApiRequestConsumer() {
        return new InMemoryTbQueueConsumer<>(storage, topicService.buildTopicName(transportApiSettings.getRequestsTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> createTransportApiResponseProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(transportApiSettings.getResponsesTopic()));
    }

    @Override
    public TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> createRemoteJsRequestTemplate() {
        return null;
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgConsumer() {
        return new InMemoryTbQueueConsumer<>(storage, topicService.buildTopicName(coreSettings.getUsageStatsTopic()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgConsumer() {
        return new InMemoryTbQueueConsumer<>(storage, topicService.buildTopicName(coreSettings.getOtaPackageTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(coreSettings.getOtaPackageTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(coreSettings.getUsageStatsTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> createVersionControlMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(vcSettings.getTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToHousekeeperServiceMsg>> createHousekeeperMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(coreSettings.getHousekeeperTopic()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToHousekeeperServiceMsg>> createHousekeeperMsgConsumer() {
        return new InMemoryTbQueueConsumer<>(storage, topicService.buildTopicName(coreSettings.getHousekeeperTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToHousekeeperServiceMsg>> createHousekeeperReprocessingMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(coreSettings.getHousekeeperReprocessingTopic()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToHousekeeperServiceMsg>> createHousekeeperReprocessingMsgConsumer() {
        return new InMemoryTbQueueConsumer<>(storage, topicService.buildTopicName(coreSettings.getHousekeeperReprocessingTopic()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToEdgeMsg>> createEdgeMsgConsumer() {
        return new InMemoryTbQueueConsumer<>(storage, topicService.buildTopicName(edgeSettings.getTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToEdgeMsg>> createEdgeMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(edgeSettings.getTopic()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToEdgeNotificationMsg>> createToEdgeNotificationsMsgConsumer() {
        return new InMemoryTbQueueConsumer<>(storage, topicService.getEdgeNotificationsTopic(serviceInfoProvider.getServiceId()).getFullTopicName());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToEdgeNotificationMsg>> createEdgeNotificationsMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.getEdgeNotificationsTopic(serviceInfoProvider.getServiceId()).getFullTopicName());
    }

    @Scheduled(fixedRateString = "${queue.in_memory.stats.print-interval-ms:60000}")
    private void printInMemoryStats() {
        storage.printStats();
    }

}
