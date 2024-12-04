/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.springframework.beans.factory.annotation.Autowired;
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
import org.thingsboard.server.service.edge.rpc.constructor.customer.CustomerMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.customer.CustomerMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@TbCoreComponent
public class CustomerEdgeProcessor extends BaseEdgeProcessor {

    @Autowired
    private CustomerMsgConstructorFactory customerMsgConstructorFactory;

    public DownlinkMsg convertCustomerEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        CustomerId customerId = new CustomerId(edgeEvent.getEntityId());
        var msgConstructor = (CustomerMsgConstructor) customerMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion);
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                Customer customer = edgeCtx.getCustomerService().findCustomerById(edgeEvent.getTenantId(), customerId);
                if (customer != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    CustomerUpdateMsg customerUpdateMsg = msgConstructor.constructCustomerUpdatedMsg(msgType, customer);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addCustomerUpdateMsg(customerUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                CustomerUpdateMsg customerUpdateMsg = msgConstructor.constructCustomerDeleteMsg(customerId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addCustomerUpdateMsg(customerUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    public ListenableFuture<Void> processCustomerNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        UUID uuid = new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB());
        CustomerId customerId = new CustomerId(EntityIdFactory.getByEdgeEventTypeAndUuid(type, uuid).getId());
        switch (actionType) {
            case UPDATED:
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                PageDataIterable<Edge> edges = new PageDataIterable<>(link -> edgeCtx.getEdgeService().findEdgesByTenantIdAndCustomerId(tenantId, customerId, link), 1024);
                for (Edge edge : edges) {
                    futures.add(saveEdgeEvent(tenantId, edge.getId(), type, actionType, customerId, null));
                }
                return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
            case DELETED:
                EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEdgeIdMSB(), edgeNotificationMsg.getEdgeIdLSB()));
                return saveEdgeEvent(tenantId, edgeId, type, actionType, customerId, null);
            default:
                return Futures.immediateFuture(null);
        }
    }

}
