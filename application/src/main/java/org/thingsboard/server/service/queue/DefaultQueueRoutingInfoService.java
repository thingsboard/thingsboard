// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.queue.discovery.QueueRoutingInfo;
import org.thingsboard.server.queue.discovery.QueueRoutingInfoService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnExpression("'${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core' || '${service.type:null}'=='tb-rule-engine'")
public class DefaultQueueRoutingInfoService implements QueueRoutingInfoService {

    private final QueueService queueService;

    public DefaultQueueRoutingInfoService(QueueService queueService) {
        this.queueService = queueService;
    }

    @Override
    public List<QueueRoutingInfo> getAllQueuesRoutingInfo() {
        return queueService.findAllQueues().stream().map(QueueRoutingInfo::new).collect(Collectors.toList());
    }

}
