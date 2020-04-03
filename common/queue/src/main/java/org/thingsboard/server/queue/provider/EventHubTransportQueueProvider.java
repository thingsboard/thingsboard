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
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.TbQueueTransportNotificationSettings;
import org.thingsboard.server.queue.common.DefaultTbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.eventhub.TbEventHubsAdmin;
import org.thingsboard.server.queue.eventhub.TbEventHubsConsumerTemplate;
import org.thingsboard.server.queue.eventhub.TbEventHubsProducerTemplate;
import org.thingsboard.server.queue.eventhub.TbEventHubsSettings;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='eventhubs' && ('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-transport')")
@Slf4j
public class EventHubTransportQueueProvider implements TbTransportQueueProvider {
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    private final TbEventHubsSettings eventHubsSettings;
    private final TbQueueAdmin admin;
    private final TbServiceInfoProvider serviceInfoProvider;

    public EventHubTransportQueueProvider(TbQueueTransportApiSettings transportApiSettings,
                                          TbQueueTransportNotificationSettings transportNotificationSettings,
                                          TbEventHubsSettings eventHubsSettings,
                                          TbServiceInfoProvider serviceInfoProvider) {
        this.transportApiSettings = transportApiSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.eventHubsSettings = eventHubsSettings;
        admin = new TbEventHubsAdmin(eventHubsSettings);
        this.serviceInfoProvider = serviceInfoProvider;
    }

    @Override
    public TbQueueRequestTemplate<TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>, TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> getTransportApiRequestTemplate() {
        TbQueueProducer<TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>> producerTemplate =
                new TbEventHubsProducerTemplate<>(admin, eventHubsSettings, transportApiSettings.getRequestsTopic());

        TbQueueConsumer<TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> consumerTemplate =
                new TbEventHubsConsumerTemplate<>(admin, eventHubsSettings,
                        transportApiSettings.getResponsesTopic() + "." + serviceInfoProvider.getServiceId(),
                        msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.TransportApiResponseMsg.parseFrom(msg.getData()), msg.getHeaders()));

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>, TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> templateBuilder = DefaultTbQueueRequestTemplate.builder();
        templateBuilder.queueAdmin(admin);
        templateBuilder.requestTemplate(producerTemplate);
        templateBuilder.responseTemplate(consumerTemplate);
        templateBuilder.maxPendingRequests(transportApiSettings.getMaxPendingRequests());
        templateBuilder.maxRequestTimeout(transportApiSettings.getMaxRequestsTimeout());
        templateBuilder.pollInterval(transportApiSettings.getResponsePollInterval());
        return templateBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> getRuleEngineMsgProducer() {
        return new TbEventHubsProducerTemplate<>(admin, eventHubsSettings, transportApiSettings.getRequestsTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> getTbCoreMsgProducer() {
        return new TbEventHubsProducerTemplate<>(admin, eventHubsSettings, transportApiSettings.getRequestsTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToTransportMsg>> getTransportNotificationsConsumer() {
        return new TbEventHubsConsumerTemplate<>(admin, eventHubsSettings,
                transportNotificationSettings.getNotificationsTopic() + "." + serviceInfoProvider.getServiceId(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.ToTransportMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }
}
