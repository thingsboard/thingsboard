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
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.QueueRoutingInfo;
import org.thingsboard.server.queue.discovery.TenantRoutingInfo;
import org.thingsboard.server.queue.discovery.RoutingInfoService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnExpression("'${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core' || '${service.type:null}'=='tb-rule-engine'")
public class DefaultRoutingInfoService implements RoutingInfoService {

    private final TenantService tenantService;
    private final QueueService queueService;

    public DefaultRoutingInfoService(TenantService tenantService, QueueService queueService) {
        this.tenantService = tenantService;
        this.queueService = queueService;
    }

    @Override
    public TenantRoutingInfo getTenantRoutingInfo(TenantId tenantId) {
        Tenant tenant = tenantService.findTenantById(tenantId);
        if (tenant != null) {
            return new TenantRoutingInfo(tenantId, tenant.isIsolatedTbCore(), tenant.isIsolatedTbRuleEngine());

        } else {
            throw new RuntimeException("Tenant not found!");
        }
    }

    @Override
    public QueueRoutingInfo getQueueRoutingInfo(TenantId tenantId) {
        List<TransportProtos.QueueInfo> queues = queueService.findQueues(tenantId).stream().map(queue ->
                TransportProtos.QueueInfo.newBuilder()
                        .setName(queue.getName())
                        .setTopic(queue.getTopic())
                        .setPartitions(queue.getPartitions()).build()
        ).collect(Collectors.toList());
        return new QueueRoutingInfo(tenantId, queues);
    }
}
