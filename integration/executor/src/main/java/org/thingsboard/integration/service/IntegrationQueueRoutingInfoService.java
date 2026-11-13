// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.service.api.IntegrationApiService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.QueueRoutingInfo;
import org.thingsboard.server.queue.discovery.QueueRoutingInfoService;

import java.util.List;

@Service
public class IntegrationQueueRoutingInfoService implements QueueRoutingInfoService {

    private final IntegrationApiService integrationApiService;

    public IntegrationQueueRoutingInfoService(@Lazy IntegrationApiService integrationApiService) {
        this.integrationApiService = integrationApiService;
    }

    @Override
    public List<QueueRoutingInfo> getAllQueuesRoutingInfo() {
        TransportProtos.GetAllQueueRoutingInfoRequestMsg msg = TransportProtos.GetAllQueueRoutingInfoRequestMsg.newBuilder().build();
        return integrationApiService.getQueueRoutingInfo(msg).stream().map(QueueRoutingInfo::new).toList();
    }

}
