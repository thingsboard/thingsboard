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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.TbQueueAdmin;
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
import org.thingsboard.server.memory.InMemoryTbQueueConsumer;
import org.thingsboard.server.memory.InMemoryTbQueueProducer;

@Component
@ConditionalOnExpression("('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-transport') && '${queue.type:null}'=='in-memory'")
@Slf4j
public class InMemoryTransportQueueProvider implements TransportQueueProvider {

    private final TbQueueCoreSettings coreSettings;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings notificationSettings;

    public InMemoryTransportQueueProvider(TbQueueCoreSettings coreSettings,
                                          TbQueueRuleEngineSettings ruleEngineSettings,
                                          TbQueueTransportApiSettings transportApiSettings,
                                          TbQueueTransportNotificationSettings notificationSettings) {
        this.coreSettings = coreSettings;
        this.ruleEngineSettings = ruleEngineSettings;
        this.transportApiSettings = transportApiSettings;
        this.notificationSettings = notificationSettings;
    }

    @Override
    public TbQueueRequestTemplate<TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> getTransportApiRequestTemplate() {
        InMemoryTbQueueProducer<TbProtoQueueMsg<TransportApiRequestMsg>> producer = new InMemoryTbQueueProducer<>(transportApiSettings.getRequestsTopic());
        InMemoryTbQueueConsumer<TbProtoQueueMsg<TransportApiResponseMsg>> consumer = new InMemoryTbQueueConsumer<>(transportApiSettings.getResponsesTopic());

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> templateBuilder = DefaultTbQueueRequestTemplate.builder();
        templateBuilder.queueAdmin(topic -> Futures.immediateFuture(null));
        templateBuilder.requestTemplate(producer);
        templateBuilder.responseTemplate(consumer);
        templateBuilder.maxPendingRequests(transportApiSettings.getMaxPendingRequests());
        templateBuilder.maxRequestTimeout(transportApiSettings.getMaxRequestsTimeout());
        templateBuilder.pollInterval(transportApiSettings.getResponsePollInterval());
        return templateBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> getRuleEngineMsgProducer() {
        return new InMemoryTbQueueProducer<>(ruleEngineSettings.getTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> getTbCoreMsgProducer() {
        return new InMemoryTbQueueProducer<>(coreSettings.getTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToTransportMsg>> getTransportNotificationsConsumer() {
        return new InMemoryTbQueueConsumer<>(notificationSettings.getNotificationsTopic());
    }
}
