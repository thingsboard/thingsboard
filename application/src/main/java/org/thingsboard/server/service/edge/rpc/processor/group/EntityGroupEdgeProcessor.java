// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.group;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
@TbCoreComponent
public class EntityGroupEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        EntityGroupId entityGroupId = new EntityGroupId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
        switch (msgType) {
            case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                EntityGroup entityGroup = edgeCtx.getEntityGroupService().findEntityGroupById(edgeEvent.getTenantId(), entityGroupId);
                if (entityGroup != null) {
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder().setDownlinkMsgId(EdgeUtils.nextPositiveInt());
                    if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(msgType)) {
                        // the edge applies a downlink's customers before its groups, so the group never lands with an owner the edge lacks
                        for (Customer customer : ownerCustomersMissingOnEdge(edgeEvent.getTenantId(), edgeEvent.getEdgeId(), entityGroup.getOwnerId())) {
                            builder.addCustomerUpdateMsg(EdgeMsgConstructorUtils.constructCustomerUpdatedMsg(msgType, customer, null));
                        }
                    }
                    EntityGroupUpdateMsg entityGroupUpdateMsg = EdgeMsgConstructorUtils.constructEntityGroupUpdatedMsg(msgType, entityGroup);
                    downlinkMsg = builder.addEntityGroupUpdateMsg(entityGroupUpdateMsg).build();
                }
            }
            case ENTITY_DELETED_RPC_MESSAGE -> downlinkMsg = DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .addEntityGroupUpdateMsg(EdgeMsgConstructorUtils.constructEntityGroupDeleteMsg(entityGroupId))
                    .build();
        }
        return downlinkMsg;
    }

    // sync sends an edge only its owner's customer chain; a group owned further down brings the customers in between, topmost first
    private List<Customer> ownerCustomersMissingOnEdge(TenantId tenantId, EdgeId edgeId, EntityId groupOwnerId) {
        if (groupOwnerId == null || !EntityType.CUSTOMER.equals(groupOwnerId.getEntityType())) {
            return List.of();
        }
        Edge edge = edgeCtx.getEdgeService().findEdgeById(tenantId, edgeId);
        if (edge == null || !edgeCtx.getEdgeProfileApplier().isEligible(tenantId, groupOwnerId, edge.getOwnerId(), false)) {
            return List.of();
        }
        Set<EntityId> heldByEdge = ownersCacheService.fetchOwnersHierarchy(tenantId, edge.getOwnerId());
        List<Customer> customers = new ArrayList<>();
        for (EntityId ownerId : ownersCacheService.fetchOwnersHierarchy(tenantId, groupOwnerId)) {
            if (heldByEdge.contains(ownerId)) {
                break;
            }
            Customer customer = edgeCtx.getCustomerService().findCustomerById(tenantId, new CustomerId(ownerId.getId()));
            // sync also sends the public customer of every customer in that chain
            if (customer != null && !(customer.isPublic() && heldByEdge.contains(customer.getOwnerId()))) {
                customers.add(customer);
            }
        }
        Collections.reverse(customers);
        return customers;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.ENTITY_GROUP;
    }

}
