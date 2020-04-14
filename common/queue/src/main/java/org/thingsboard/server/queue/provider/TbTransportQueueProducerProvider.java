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
import org.springframework.stereotype.Service;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import javax.annotation.PostConstruct;

@Service
@ConditionalOnExpression("'${service.type:null}'=='tb-transport'")
public class TbTransportQueueProducerProvider implements TbQueueProducerProvider {

    private final TbTransportQueueFactory tbQueueProvider;
    private TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> toRuleEngine;
    private TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> toTbCore;

    public TbTransportQueueProducerProvider(TbTransportQueueFactory tbQueueProvider) {
        this.tbQueueProvider = tbQueueProvider;
    }

    @PostConstruct
    public void init() {
        this.toTbCore = tbQueueProvider.createTbCoreMsgProducer();
        this.toRuleEngine = tbQueueProvider.createRuleEngineMsgProducer();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToTransportMsg>> getTransportNotificationsMsgProducer() {
        throw new RuntimeException("Not Implemented! Should not be used by Transport!");
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> getRuleEngineMsgProducer() {
        return toRuleEngine;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> getTbCoreMsgProducer() {
        return toTbCore;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineNotificationMsg>> getRuleEngineNotificationsMsgProducer() {
        throw new RuntimeException("Not Implemented! Should not be used by Transport!");
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> getTbCoreNotificationsMsgProducer() {
        throw new RuntimeException("Not Implemented! Should not be used by Transport!");
    }
}
