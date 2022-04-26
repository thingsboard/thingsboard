/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@TbCoreComponent
public class EdgeProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processEdgeNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        try {
            EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
            EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
            ListenableFuture<Edge> edgeFuture;
            switch (actionType) {
                case ASSIGNED_TO_CUSTOMER:
                    CustomerId customerId = mapper.readValue(edgeNotificationMsg.getBody(), CustomerId.class);
                    edgeFuture = edgeService.findEdgeByIdAsync(tenantId, edgeId);
                    return Futures.transformAsync(edgeFuture, edge -> {
                        if (edge == null || customerId.isNullUid()) {
                            return Futures.immediateFuture(null);
                        }
                        List<ListenableFuture<Void>> futures = new ArrayList<>();
                        futures.add(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.CUSTOMER, EdgeEventActionType.ADDED, customerId, null));
                        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
                        PageData<User> pageData;
                        do {
                            pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
                            if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                                log.trace("[{}] [{}] user(s) are going to be added to edge.", edge.getId(), pageData.getData().size());
                                for (User user : pageData.getData()) {
                                    futures.add(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.USER, EdgeEventActionType.ADDED, user.getId(), null));
                                }
                                if (pageData.hasNext()) {
                                    pageLink = pageLink.nextPageLink();
                                }
                            }
                        } while (pageData != null && pageData.hasNext());
                        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
                    }, dbCallbackExecutorService);
                case UNASSIGNED_FROM_CUSTOMER:
                    CustomerId customerIdToDelete = mapper.readValue(edgeNotificationMsg.getBody(), CustomerId.class);
                    edgeFuture = edgeService.findEdgeByIdAsync(tenantId, edgeId);
                    return Futures.transformAsync(edgeFuture, edge -> {
                        if (edge == null || customerIdToDelete.isNullUid()) {
                            return Futures.immediateFuture(null);
                        }
                        return saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.CUSTOMER, EdgeEventActionType.DELETED, customerIdToDelete, null);
                    }, dbCallbackExecutorService);
                default:
                    return Futures.immediateFuture(null);
            }
        } catch (Exception e) {
            log.error("Exception during processing edge event", e);
            return Futures.immediateFailedFuture(e);
        }
    }
}
