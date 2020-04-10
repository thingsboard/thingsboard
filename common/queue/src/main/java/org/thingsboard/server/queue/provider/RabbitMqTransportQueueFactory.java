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
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqConsumerTemplate;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqProducerTemplate;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportNotificationSettings;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='rabbitmq' && ('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-transport')")
@Slf4j
public class RabbitMqTransportQueueFactory implements TbTransportQueueFactory {
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    private final TbRabbitMqSettings rabbitMqSettings;
    private final TbQueueAdmin admin;
    private final TbServiceInfoProvider serviceInfoProvider;

    public RabbitMqTransportQueueFactory(TbQueueTransportApiSettings transportApiSettings,
                                         TbQueueTransportNotificationSettings transportNotificationSettings,
                                         TbRabbitMqSettings rabbitMqSettings,
                                         TbServiceInfoProvider serviceInfoProvider,
                                         TbQueueAdmin admin) {
        this.transportApiSettings = transportApiSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.rabbitMqSettings = rabbitMqSettings;
        this.admin = admin;
        this.serviceInfoProvider = serviceInfoProvider;
    }

    @Override
    public TbQueueRequestTemplate<TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiRequestTemplate() {
        TbRabbitMqProducerTemplate<TbProtoQueueMsg<TransportApiRequestMsg>> producerTemplate =
                new TbRabbitMqProducerTemplate<>(admin, rabbitMqSettings, transportApiSettings.getRequestsTopic());

        TbRabbitMqConsumerTemplate<TbProtoQueueMsg<TransportApiResponseMsg>> consumerTemplate =
                new TbRabbitMqConsumerTemplate<>(admin, rabbitMqSettings,
                        transportApiSettings.getResponsesTopic() + "." + serviceInfoProvider.getServiceId(),
                        msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportApiResponseMsg.parseFrom(msg.getData()), msg.getHeaders()));

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> templateBuilder = DefaultTbQueueRequestTemplate.builder();
        templateBuilder.queueAdmin(admin);
        templateBuilder.requestTemplate(producerTemplate);
        templateBuilder.responseTemplate(consumerTemplate);
        templateBuilder.maxPendingRequests(transportApiSettings.getMaxPendingRequests());
        templateBuilder.maxRequestTimeout(transportApiSettings.getMaxRequestsTimeout());
        templateBuilder.pollInterval(transportApiSettings.getResponsePollInterval());
        return templateBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        return new TbRabbitMqProducerTemplate<>(admin, rabbitMqSettings, transportApiSettings.getRequestsTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        return new TbRabbitMqProducerTemplate<>(admin, rabbitMqSettings, transportApiSettings.getRequestsTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsConsumer() {
        return new TbRabbitMqConsumerTemplate<>(admin, rabbitMqSettings, transportNotificationSettings.getNotificationsTopic() + "." + serviceInfoProvider.getServiceId(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToTransportMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }
}
