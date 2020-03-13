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

import org.thingsboard.server.TbQueueConsumer;
import org.thingsboard.server.TbQueueProducer;
import org.thingsboard.server.common.TbProtoQueueMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;

/**
 * Responsible for initialization of various Producers and Consumers used by TB Core Node.
 * Implementation Depends on the queue queue.type from yml or TB_QUEUE_TYPE environment variable
 */
public interface TbCoreQueueProvider {

    /**
     * Used to push messages to instances of TB Transport Service
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> getTransportMsgProducer();

    /**
     * Used to push messages to instances of TB RuleEngine Service
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> getRuleEngineMsgProducer();

    /**
     * Used to push messages to other instances of TB Core Service
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> getTbCoreMsgProducer();

    /**
     * Used to consume messages by TB Core Service
     *
     * @return
     */
    TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> getToCoreMsgConsumer();

    /**
     * Used to consume Transport API Calls
     *
     * @return
     */
    TbQueueConsumer<TbProtoQueueMsg<TransportApiRequestMsg>> getTransportApiRequestConsumer();

    /**
     * Used to push replies to Transport API Calls
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<TransportApiResponseMsg>> getTransportApiResponseProducer();


}
