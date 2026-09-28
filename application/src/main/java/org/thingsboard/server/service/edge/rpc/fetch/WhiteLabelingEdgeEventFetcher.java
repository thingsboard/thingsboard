// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.customer.CustomerService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@AllArgsConstructor
@Slf4j
public class WhiteLabelingEdgeEventFetcher implements EdgeEventFetcher {

    private final CustomerService customerService;

    @Override
    public PageLink getPageLink(int pageSize) {
        return null;
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        List<EdgeEvent> result = new ArrayList<>();
        List<EdgeEventType> wlTypes = Arrays.asList(EdgeEventType.WHITE_LABELING, EdgeEventType.LOGIN_WHITE_LABELING, EdgeEventType.MAIL_TEMPLATES);
        for (EdgeEventType wlType : wlTypes) {
            List<EdgeEvent> wlEdgeEvents = getWhiteLabelingEdgeEvents(tenantId, edge, wlType);
            result.addAll(wlEdgeEvents);
        }
        // returns PageData object to be in sync with other fetchers
        return new PageData<>(result, 1, result.size(), false);
    }

    private List<EdgeEvent> getWhiteLabelingEdgeEvents(TenantId tenantId, Edge edge, EdgeEventType eventType) {
        try {
            EntityId ownerId = edge.getOwnerId();
            List<EdgeEvent> result = new ArrayList<>();
            result.add(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(),
                    eventType, EdgeEventActionType.UPDATED, null, JacksonUtil.valueToTree(TenantId.SYS_TENANT_ID)));
            result.add(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(),
                    eventType, EdgeEventActionType.UPDATED, null, JacksonUtil.valueToTree(tenantId)));
            if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                CustomerId customerId = new CustomerId(ownerId.getId());
                result.add(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(),
                        eventType, EdgeEventActionType.UPDATED, null, JacksonUtil.valueToTree(customerId)));
                Customer customer = customerService.findCustomerById(tenantId, customerId);
                if (customer.isSubCustomer()) {
                    getParentCustomerEvents(tenantId, edge.getId(), customer.getParentCustomerId(), eventType, result);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Can't load white labeling params", e);
            throw new RuntimeException(e);
        }
    }

    private void getParentCustomerEvents(TenantId tenantId, EdgeId edgeId, CustomerId customerId, EdgeEventType eventType, List<EdgeEvent> events) {
        events.add(EdgeUtils.constructEdgeEvent(tenantId, edgeId, eventType, EdgeEventActionType.UPDATED, null, JacksonUtil.valueToTree(customerId)));
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer.isSubCustomer()) {
            getParentCustomerEvents(tenantId, edgeId, customer.getParentCustomerId(), eventType, events);
        }
    }

}
