package org.thingsboard.server.common.transport.queue;

import org.thingsboard.server.TbQueueConsumer;
import org.thingsboard.server.TbQueueMsg;
import org.thingsboard.server.TbQueueProducer;

public interface TransportQueueProvider {

    TbQueueProducer<TransportApiCallRequest> getTransportApiCallRequestsProducer();

    TbQueueConsumer<TbQueueMsg> getTransportApiCallResponsesConsumer();

    TbQueueProducer<TbQueueMsg> getRuleEngineMsgProducer();

    TbQueueProducer<TbQueueMsg> getTbCoreMsgProducer();

    TbQueueConsumer<TbQueueMsg> getTransportNotificationsConsumer();

}
