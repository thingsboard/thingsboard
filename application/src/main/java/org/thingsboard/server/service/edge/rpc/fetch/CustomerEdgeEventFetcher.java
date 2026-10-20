// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.customer.CustomerService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class CustomerEdgeEventFetcher extends BasePageableEdgeEventFetcher<Customer> {

    private final CustomerService customerService;
    private final CustomerId ownerId;

    @Override
    PageData<Customer> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        List<Customer> customersHierarchy = getCustomersHierarchy(tenantId, ownerId);
        return new PageData<>(customersHierarchy, 1, customersHierarchy.size(), false);
    }

    List<Customer> getCustomersHierarchy(TenantId tenantId, CustomerId customerId) {
        List<Customer> result = new ArrayList<>();
        Customer customerById = customerService.findCustomerById(tenantId, customerId);
        result.add(customerById);
        if (customerById != null && customerById.isPublic()) {
            return result;
        }
        Customer publicCustomer = customerService.findPublicCustomer(tenantId, customerId);
        if (publicCustomer != null) {
            result.add(publicCustomer);
        }
        if (customerById != null && customerById.getParentCustomerId() != null && !customerById.getParentCustomerId().isNullUid()) {
            result.addAll(getCustomersHierarchy(tenantId, customerById.getParentCustomerId()));
        }
        return result;
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, Customer customer) {
        return EdgeUtils.constructEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.CUSTOMER,
                EdgeEventActionType.ADDED, customer.getId(), null);
    }

}
