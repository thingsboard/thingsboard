// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.queue.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.memory.InMemoryStorage;
import org.thingsboard.server.queue.memory.InMemoryTbQueueConsumer;
import org.thingsboard.server.queue.util.TbReportComponent;

@Component
@TbReportComponent
@ConditionalOnExpression("'${queue.type:null}'=='in-memory'")
@Slf4j
@RequiredArgsConstructor
public class InMemoryTbReportQueueFactory implements TbReportQueueFactory {

    private final InMemoryStorage storage;
    private final TopicService topicService;
    private final TbServiceInfoProvider serviceInfoProvider;

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToTbReportNotificationMsg>> createTbReportNotificationsConsumer() {
        return new InMemoryTbQueueConsumer<>(storage, topicService.getNotificationsTopic(ServiceType.TB_REPORT, serviceInfoProvider.getServiceId()).getFullTopicName());
    }

}
