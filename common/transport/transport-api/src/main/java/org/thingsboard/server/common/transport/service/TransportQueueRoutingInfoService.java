// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos.GetAllQueueRoutingInfoRequestMsg;
import org.thingsboard.server.queue.discovery.QueueRoutingInfo;
import org.thingsboard.server.queue.discovery.QueueRoutingInfoService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnExpression("'${service.type:null}'=='tb-transport'")
public class TransportQueueRoutingInfoService implements QueueRoutingInfoService {

    private final TransportService transportService;

    public TransportQueueRoutingInfoService(@Lazy TransportService transportService) {
        this.transportService = transportService;
    }

    @Override
    public List<QueueRoutingInfo> getAllQueuesRoutingInfo() {
        GetAllQueueRoutingInfoRequestMsg msg = GetAllQueueRoutingInfoRequestMsg.newBuilder().build();
        return transportService.getQueueRoutingInfo(msg).stream().map(QueueRoutingInfo::new).collect(Collectors.toList());
    }
}
