// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class EdgeEntityProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        EdgeId edgeId = new EdgeId(edgeEvent.getEntityId());
        // TODO: check this
        if (EdgeEventActionType.CHANGE_OWNER.equals(edgeEvent.getAction())) {
            Edge edge = edgeCtx.getEdgeService().findEdgeById(edgeEvent.getTenantId(), edgeId);
            if (edge != null) {
                EdgeConfiguration edgeConfigMsg = EdgeMsgConstructorUtils.constructEdgeConfiguration(edge, edgeCtx.getSubscriptionService().getLicenseVersion());
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .setEdgeConfiguration(edgeConfigMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeId originatorEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        if (edgeId.equals(originatorEdgeId)) {
            return Futures.immediateFuture(null);
        }
        return switch (actionType) {
            case CHANGE_OWNER -> {
                ListenableFuture<Edge> edgeFuture = edgeCtx.getEdgeService().findEdgeByIdAsync(tenantId, edgeId);
                yield Futures.transformAsync(edgeFuture, edge -> {
                    List<ListenableFuture<Void>> futures = new ArrayList<>();
                    try {
                        EntityId previousOwnerId = JacksonUtil.fromString(edgeNotificationMsg.getBody(), EntityId.class);
                        List<Customer> previousOwnerCustomerHierarchy = new ArrayList<>();
                        if (previousOwnerId != null && EntityType.CUSTOMER.equals(previousOwnerId.getEntityType())) {
                            previousOwnerCustomerHierarchy =
                                    edgeCtx.getCustomersHierarchyEdgeService().getCustomersHierarchy(tenantId, new CustomerId(previousOwnerId.getId()));
                        }
                        List<Customer> ownerCustomerHierarchy = new ArrayList<>();
                        if (EntityType.CUSTOMER.equals(edge.getOwnerId().getEntityType())) {
                            ownerCustomerHierarchy =
                                    edgeCtx.getCustomersHierarchyEdgeService().getCustomersHierarchy(tenantId, new CustomerId(edge.getOwnerId().getId()));
                        }
                        List<Customer> removedCustomers = new ArrayList<>(previousOwnerCustomerHierarchy);
                        removedCustomers.removeAll(ownerCustomerHierarchy);
                        for (Customer removedCustomer : removedCustomers) {
                            if (!removedCustomer.getId().isNullUid()) {
                                CustomerId removedCustomerId = removedCustomer.getId();
                                futures.add(saveEdgeEvent(edge.getTenantId(), edge.getId(),
                                        EdgeEventType.CUSTOMER, EdgeEventActionType.DELETED, removedCustomerId, null));
                                unassignEntityGroupsOfRemovedCustomer(tenantId, edgeId, EntityType.DEVICE, removedCustomerId);
                                unassignEntityGroupsOfRemovedCustomer(tenantId, edgeId, EntityType.ASSET, removedCustomerId);
                                unassignEntityGroupsOfRemovedCustomer(tenantId, edgeId, EntityType.ENTITY_VIEW, removedCustomerId);
                                unassignEntityGroupsOfRemovedCustomer(tenantId, edgeId, EntityType.USER, removedCustomerId);
                                unassignEntityGroupsOfRemovedCustomer(tenantId, edgeId, EntityType.DASHBOARD, removedCustomerId);
                                unassignSchedulerEventsOfRemovedCustomer(tenantId, edgeId, removedCustomerId);
                            }
                        }
                        List<Customer> addedCustomers = new ArrayList<>(ownerCustomerHierarchy);
                        addedCustomers.removeAll(previousOwnerCustomerHierarchy);
                        futures.add(syncCustomersSequentially(tenantId, edge.getId(), addedCustomers));
                        edgeCtx.getCustomersHierarchyEdgeService().processEdgeChangeOwner(tenantId, edgeId, previousOwnerId);
                        futures.add(saveEdgeEvent(edge.getTenantId(), edgeId, EdgeEventType.EDGE, EdgeEventActionType.CHANGE_OWNER, edgeId, null));
                    } catch (Exception e) {
                        log.error("[{}] Failed to switch owner for edge [{}]", tenantId, edge, e);
                        return Futures.immediateFailedFuture(e);
                    }
                    return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
                }, dbCallbackExecutorService);
            }
            case ATTRIBUTES_UPDATED -> processAttributesUpdated(tenantId, edgeId, edgeNotificationMsg);
            default -> Futures.immediateFuture(null);
        };
    }

    private ListenableFuture<Void> processAttributesUpdated(TenantId tenantId, EdgeId edgeId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        List<String> attributeKeys = new ArrayList<>();
        try {
            ArrayNode attributes = (ArrayNode) JacksonUtil.toJsonNode(edgeNotificationMsg.getBody());
            for (JsonNode attribute : attributes) {
                attributeKeys.add(attribute.get("key").asText());
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Can't process attributes updated event {}", tenantId, edgeId, edgeNotificationMsg, e);
            return Futures.immediateFailedFuture(e);
        }
        Set<IntegrationId> integrationIds = new HashSet<>();
        PageDataIterable<Integration> integrationPageDataIterable = new PageDataIterable<>(
                link -> edgeCtx.getIntegrationService().findIntegrationsByTenantIdAndEdgeId(tenantId, edgeId, link), 1024);
        for (Integration integration : integrationPageDataIterable) {
            for (String attributeKey : attributeKeys) {
                if (integration.getConfiguration().toString().contains(EdgeUtils.formatAttributeKeyToPlaceholderFormat(attributeKey))) {
                    integrationIds.add(integration.getId());
                }
            }
        }
        if (integrationIds.isEmpty()) {
            return Futures.immediateFuture(null);
        } else {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            for (IntegrationId integrationId : integrationIds) {
                futures.add(saveEdgeEvent(tenantId, edgeId, EdgeEventType.INTEGRATION, EdgeEventActionType.UPDATED, integrationId, null));
            }
            return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
        }
    }

    private void unassignEntityGroupsOfRemovedCustomer(TenantId tenantId, EdgeId edgeId, EntityType groupType, EntityId customerId) {
        PageLink removalPageLink = new PageLink(1000, 0);
        while (true) {
            PageData<EntityGroup> toRemove = edgeCtx.getEntityGroupService().findEdgeEntityGroupsByOwnerIdAndType(tenantId, edgeId, customerId, groupType, removalPageLink);
            for (EntityGroup entityGroup : toRemove.getData()) {
                edgeCtx.getEntityGroupService().unassignEntityGroupFromEdge(tenantId, entityGroup.getId(), edgeId, groupType);
            }
            if (!toRemove.hasNext()) {
                break;
            }
        }
    }

    private void unassignSchedulerEventsOfRemovedCustomer(TenantId tenantId, EdgeId edgeId, CustomerId customerId) {
        PageLink removalPageLink = new PageLink(1000, 0);
        while (true) {
            PageData<SchedulerEventInfo> toRemove = edgeCtx.getSchedulerEventService().findSchedulerEventInfosByTenantIdAndEdgeIdAndCustomerId(tenantId, edgeId, customerId, removalPageLink);
            for (SchedulerEventInfo schedulerEventInfo : toRemove.getData()) {
                edgeCtx.getSchedulerEventService().unassignSchedulerEventFromEdge(tenantId, schedulerEventInfo.getId(), edgeId);
            }
            if (!toRemove.hasNext()) {
                break;
            }
        }
    }

    private ListenableFuture<Void> syncCustomersSequentially(TenantId tenantId, EdgeId edgeId, List<Customer> customers) {
        // Chain the syncs so each customer event is persisted (and gets its seqId) before the next is enqueued.
        // customers is ordered parent-first, and the edge rejects a sub-customer whose parent doesn't exist yet.
        ListenableFuture<Void> chain = Futures.immediateFuture(null);
        for (Customer customer : customers) {
            if (!customer.getId().isNullUid()) {
                chain = Futures.transformAsync(chain, ignored -> syncCustomer(tenantId, edgeId, customer), dbCallbackExecutorService);
            }
        }
        return chain;
    }

    private ListenableFuture<Void> syncCustomer(TenantId tenantId, EdgeId edgeId, Customer customer) {
        // Persist the customer before its roles: the edge rejects a role whose customer doesn't exist yet,
        // and the failed downlink is dropped (not retried). saveEdgeEvent is async, so chain the roles after the customer event to keep their order.
        ListenableFuture<Void> customerFuture = saveEdgeEvent(tenantId, edgeId, EdgeEventType.CUSTOMER, EdgeEventActionType.ADDED, customer.getId(), null, null);
        return Futures.transformAsync(customerFuture, ignored -> {
            List<ListenableFuture<Void>> roleFutures = new ArrayList<>();
            PageDataIterable<Role> roles = new PageDataIterable<>(
                    link -> edgeCtx.getRoleService().findRolesByTenantIdAndCustomerId(tenantId, customer.getId(), link), 1024);
            for (Role role : roles) {
                roleFutures.add(saveEdgeEvent(tenantId, edgeId,
                        EdgeEventType.ROLE, EdgeEventActionType.ADDED, role.getId(), null, null));
            }
            assignCustomerAdministratorsAndUsersGroupToEdge(tenantId, edgeId, customer.getId(), customer.getParentCustomerId());
            return Futures.transform(Futures.allAsList(roleFutures), voids -> null, dbCallbackExecutorService);
        }, dbCallbackExecutorService);
    }

    private void assignCustomerAdministratorsAndUsersGroupToEdge(TenantId tenantId,
                                                                 EdgeId edgeId,
                                                                 CustomerId customerId,
                                                                 CustomerId parentCustomerId) {
        EntityGroup customerAdmins = edgeCtx.getEntityGroupService().findOrCreateCustomerAdminsGroup(tenantId, customerId, parentCustomerId);
        edgeCtx.getEntityGroupService().assignEntityGroupToEdge(tenantId, customerAdmins.getId(), edgeId, customerAdmins.getType());
        EntityGroup customerUsers = edgeCtx.getEntityGroupService().findOrCreateCustomerUsersGroup(tenantId, customerId, parentCustomerId);
        edgeCtx.getEntityGroupService().assignEntityGroupToEdge(tenantId, customerUsers.getId(), edgeId, customerUsers.getType());
    }

    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.EDGE;
    }

}
