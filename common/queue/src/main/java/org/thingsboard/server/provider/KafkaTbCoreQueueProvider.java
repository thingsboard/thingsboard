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

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.TbQueueConsumer;
import org.thingsboard.server.TbQueueProducer;
import org.thingsboard.server.common.TbProtoQueueMsg;
import org.thingsboard.server.gen.transport.TransportProtos;

@Component
@ConditionalOnExpression("'${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core') && ${queue.type:null}'=='kafka'")
public class KafkaTbCoreQueueProvider implements TbCoreQueueProvider{
    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToTransportMsg>> getTransportMsgProducer() {
        return null;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> getRuleEngineMsgProducer() {
        return null;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> getTbCoreMsgProducer() {
        return null;
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> getToCoreMsgConsumer() {
        return null;
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>> getTransportApiRequestConsumer() {
        return null;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> getTransportApiResponseProducer() {
        return null;
    }
}
