/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.customer;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class CustomerEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        CustomerId customerId = new CustomerId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                Customer customer = edgeCtx.getCustomerService().findCustomerById(edgeEvent.getTenantId(), customerId);
                if (customer != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    CustomerUpdateMsg customerUpdateMsg = EdgeMsgConstructorUtils.constructCustomerUpdatedMsg(msgType, customer);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addCustomerUpdateMsg(customerUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                CustomerUpdateMsg customerUpdateMsg = EdgeMsgConstructorUtils.constructCustomerDeleteMsg(customerId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addCustomerUpdateMsg(customerUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        UUID uuid = new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB());
        CustomerId customerId = new CustomerId(EntityIdFactory.getByEdgeEventTypeAndUuid(type, uuid).getId());
        switch (actionType) {
            case ADDED:
                Customer customerById = edgeCtx.getCustomerService().findCustomerById(tenantId, customerId);
                if (customerById != null && customerById.isPublic()) {
                    return findEdgesAndSaveEdgeEvents(link -> edgeCtx.getEdgeService().findEdgesByTenantId(tenantId, link),
                            tenantId, type, actionType, customerId);
                }
                return Futures.immediateFuture(null);
            case UPDATED:
                return findEdgesAndSaveEdgeEvents(link -> edgeCtx.getEdgeService().findEdgesByTenantIdAndCustomerId(tenantId, customerId, link),
                        tenantId, type, actionType, customerId);
            case DELETED:
                EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEdgeIdMSB(), edgeNotificationMsg.getEdgeIdLSB()));
                return saveEdgeEvent(tenantId, edgeId, type, actionType, customerId, null);
            default:
                return Futures.immediateFuture(null);
        }
    }

    public ListenableFuture<Void> findEdgesAndSaveEdgeEvents(PageDataIterable.FetchFunction<Edge> edgeFetcher, TenantId tenantId,
                                                             EdgeEventType type, EdgeEventActionType actionType, CustomerId customerId) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        PageDataIterable<Edge> edges = new PageDataIterable<>(edgeFetcher, 1024);
        for (Edge edge : edges) {
            futures.add(saveEdgeEvent(tenantId, edge.getId(), type, actionType, customerId, null));
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.CUSTOMER;
    }

}
