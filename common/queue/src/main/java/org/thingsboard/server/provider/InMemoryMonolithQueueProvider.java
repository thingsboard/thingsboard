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
import org.thingsboard.server.TbQueueRuleEngineSettings;
import org.thingsboard.server.TbQueueTransportApiSettings;
import org.thingsboard.server.TbQueueTransportNotificationSettings;
import org.thingsboard.server.common.TbProtoQueueMsg;
import org.thingsboard.server.discovery.PartitionChangeEvent;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.memory.InMemoryTbQueueConsumer;
import org.thingsboard.server.memory.InMemoryTbQueueProducer;

@Slf4j
@Component
@ConditionalOnExpression("'${queue.type:null}'=='in-memory' && '${service.type:null}'=='monolith'")
public class InMemoryMonolithQueueProvider implements TbCoreQueueProvider, TbRuleEngineQueueProvider {

    private final TbQueueCoreSettings coreSettings;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings notificationSettings;

    public InMemoryMonolithQueueProvider(TbQueueCoreSettings coreSettings,
                                         TbQueueRuleEngineSettings ruleEngineSettings,
                                         TbQueueTransportApiSettings transportApiSettings,
                                         TbQueueTransportNotificationSettings notificationSettings) {
        this.coreSettings = coreSettings;
        this.ruleEngineSettings = ruleEngineSettings;
        this.transportApiSettings = transportApiSettings;
        this.notificationSettings = notificationSettings;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> getTransportMsgProducer() {
        return new InMemoryTbQueueProducer<>(notificationSettings.getNotificationsTopic());
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
    public TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> getToRuleEngineMsgConsumer() {
        return new InMemoryTbQueueConsumer<>(ruleEngineSettings.getTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> getToCoreMsgConsumer() {
        return new InMemoryTbQueueConsumer<>(coreSettings.getTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportApiRequestMsg>> getTransportApiRequestConsumer() {
        return new InMemoryTbQueueConsumer<>(transportApiSettings.getRequestsTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportApiResponseMsg>> getTransportApiResponseProducer() {
        return new InMemoryTbQueueProducer<>(transportApiSettings.getResponsesTopic());
    }
}
