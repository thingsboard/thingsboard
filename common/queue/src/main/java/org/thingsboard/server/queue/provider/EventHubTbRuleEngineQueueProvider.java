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

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueCoreSettings;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.eventhub.TbEventHubsAdmin;
import org.thingsboard.server.queue.eventhub.TbEventHubsConsumerTemplate;
import org.thingsboard.server.queue.eventhub.TbEventHubsProducerTemplate;
import org.thingsboard.server.queue.eventhub.TbEventHubsSettings;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='eventhubs' && '${service.type:null}'=='tb-rule-engine'")
public class EventHubTbRuleEngineQueueProvider implements TbRuleEngineQueueProvider {

    private final PartitionService partitionService;
    private final TbQueueCoreSettings coreSettings;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbEventHubsSettings eventHubsSettings;
    private final TbQueueAdmin admin;

    public EventHubTbRuleEngineQueueProvider(PartitionService partitionService, TbQueueCoreSettings coreSettings,
                                             TbQueueRuleEngineSettings ruleEngineSettings,
                                             TbServiceInfoProvider serviceInfoProvider,
                                             TbEventHubsSettings eventHubsSettings) {
        this.partitionService = partitionService;
        this.coreSettings = coreSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.ruleEngineSettings = ruleEngineSettings;
        this.eventHubsSettings = eventHubsSettings;
        admin = new TbEventHubsAdmin(eventHubsSettings);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> getTransportNotificationsMsgProducer() {
        return new TbEventHubsProducerTemplate<>(admin, eventHubsSettings, coreSettings.getTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> getRuleEngineMsgProducer() {
        return new TbEventHubsProducerTemplate<>(admin, eventHubsSettings, coreSettings.getTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineNotificationMsg>> getRuleEngineNotificationsMsgProducer() {
        return new TbEventHubsProducerTemplate<>(admin, eventHubsSettings, ruleEngineSettings.getTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> getTbCoreMsgProducer() {
        return new TbEventHubsProducerTemplate<>(admin, eventHubsSettings, coreSettings.getTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> getTbCoreNotificationsMsgProducer() {
        return new TbEventHubsProducerTemplate<>(admin, eventHubsSettings, coreSettings.getTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> getToRuleEngineMsgConsumer() {
        return new TbEventHubsConsumerTemplate<>(admin, eventHubsSettings, ruleEngineSettings.getTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToRuleEngineMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineNotificationMsg>> getToRuleEngineNotificationsMsgConsumer() {
        return new TbEventHubsConsumerTemplate<>(admin, eventHubsSettings,
                partitionService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, serviceInfoProvider.getServiceId()).getFullTopicName(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.ToRuleEngineNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }
}
