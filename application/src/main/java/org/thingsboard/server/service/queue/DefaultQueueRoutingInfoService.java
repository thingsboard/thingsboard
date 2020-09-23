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
package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
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

    @Override
    public List<QueueRoutingInfo> getMainQueuesRoutingInfo() {
        return queueService.findAllMainQueues().stream().map(QueueRoutingInfo::new).collect(Collectors.toList());
    }

    @Override
    public List<QueueRoutingInfo> getQueuesRoutingInfo(TenantId tenantId) {
        return queueService.findQueues(tenantId).stream().map(QueueRoutingInfo::new).collect(Collectors.toList());
    }
}
