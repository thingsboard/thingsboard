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
package org.thingsboard.server.service.edge.rpc.processor.edge;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class EdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent) {
        EdgeId edgeId = new EdgeId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER -> {
                Edge edge = edgeService.findEdgeById(edgeEvent.getTenantId(), edgeId);
                if (edge != null) {
                    EdgeConfiguration edgeConfigMsg =
                            edgeMsgConstructor.constructEdgeConfiguration(edge);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .setEdgeConfiguration(edgeConfigMsg)
                            .build();
                }
            }
        }
        return downlinkMsg;
    }

    public ListenableFuture<Void> processEdgeNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        try {
            EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
            EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
            switch (actionType) {
                case ASSIGNED_TO_CUSTOMER:
                    CustomerId customerId = JacksonUtil.fromString(edgeNotificationMsg.getBody(), CustomerId.class);
                    Edge edge = edgeService.findEdgeById(tenantId, edgeId);
                    if (customerId != null && (edge == null || customerId.isNullUid())) {
                        return Futures.immediateFuture(null);
                    }
                    List<ListenableFuture<Void>> futures = new ArrayList<>();
                    futures.add(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.CUSTOMER, EdgeEventActionType.ADDED, customerId, null));
                    futures.add(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.EDGE, EdgeEventActionType.ASSIGNED_TO_CUSTOMER, edgeId, null));
                    PageLink pageLink = new PageLink(1000);
                    PageData<User> pageData;
                    do {
                        pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
                        if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                            log.trace("[{}][{}][{}] user(s) are going to be added to edge.", tenantId, edge.getId(), pageData.getData().size());
                            for (User user : pageData.getData()) {
                                futures.add(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.USER, EdgeEventActionType.ADDED, user.getId(), null));
                            }
                            if (pageData.hasNext()) {
                                pageLink = pageLink.nextPageLink();
                            }
                        }
                    } while (pageData != null && pageData.hasNext());
                    return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
                case UNASSIGNED_FROM_CUSTOMER:
                    CustomerId customerIdToDelete = JacksonUtil.fromString(edgeNotificationMsg.getBody(), CustomerId.class);
                    edge = edgeService.findEdgeById(tenantId, edgeId);
                    if (customerIdToDelete != null && (edge == null || customerIdToDelete.isNullUid())) {
                        return Futures.immediateFuture(null);
                    }
                    return Futures.transformAsync(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.EDGE, EdgeEventActionType.UNASSIGNED_FROM_CUSTOMER, edgeId, null),
                            voids -> saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.CUSTOMER, EdgeEventActionType.DELETED, customerIdToDelete, null),
                            dbCallbackExecutorService);
                default:
                    return Futures.immediateFuture(null);
            }
        } catch (Exception e) {
            log.error("[{}] Exception during processing edge event", tenantId, e);
            return Futures.immediateFailedFuture(e);
        }
    }

}
