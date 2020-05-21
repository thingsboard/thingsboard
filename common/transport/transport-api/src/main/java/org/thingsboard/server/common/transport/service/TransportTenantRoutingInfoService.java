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
package org.thingsboard.server.common.transport.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos.GetQueueRoutingInfoRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetQueueRoutingInfoResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetTenantRoutingInfoRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetTenantRoutingInfoResponseMsg;
import org.thingsboard.server.queue.discovery.QueueRoutingInfo;
import org.thingsboard.server.queue.discovery.RoutingInfoService;
import org.thingsboard.server.queue.discovery.TenantRoutingInfo;

@Slf4j
@Service
@ConditionalOnExpression("'${service.type:null}'=='tb-transport'")
public class TransportTenantRoutingInfoService implements RoutingInfoService {

    private TransportService transportService;

    @Lazy
    @Autowired
    public void setTransportService(TransportService transportService) {
        this.transportService = transportService;
    }

    @Override
    public TenantRoutingInfo getTenantRoutingInfo(TenantId tenantId) {
        GetTenantRoutingInfoRequestMsg msg = GetTenantRoutingInfoRequestMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .build();
        GetTenantRoutingInfoResponseMsg routingInfo = transportService.getRoutingInfo(msg);
        return new TenantRoutingInfo(tenantId, routingInfo.getIsolatedTbCore(), routingInfo.getIsolatedTbRuleEngine());
    }

    @Override
    public QueueRoutingInfo getQueueRoutingInfo(TenantId tenantId) {
        GetQueueRoutingInfoRequestMsg msg = GetQueueRoutingInfoRequestMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .build();
        GetQueueRoutingInfoResponseMsg routingInfo = transportService.getRoutingInfo(msg);
        return new QueueRoutingInfo(tenantId, routingInfo.getRuleEngineQueuesList());
    }
}
