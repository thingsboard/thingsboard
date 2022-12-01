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
package org.thingsboard.server.service.entitiy.customer;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.List;

@Service
@AllArgsConstructor
public class DefaultTbCustomerService extends AbstractTbEntityService implements TbCustomerService {

    @Override
    public Customer save(Customer customer, User user) throws Exception {
        ActionType actionType = customer.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = customer.getTenantId();
        try {
            Customer savedCustomer = checkNotNull(customerService.saveCustomer(customer));
            autoCommit(user, savedCustomer.getId());
            notificationEntityService.notifyCreateOrUpdateEntity(tenantId, savedCustomer.getId(), savedCustomer, null, actionType, user);
            return savedCustomer;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.CUSTOMER), customer, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(Customer customer, User user) {
        TenantId tenantId = customer.getTenantId();
        CustomerId customerId = customer.getId();
        try {
            List<EdgeId> relatedEdgeIds = edgeService.findAllRelatedEdgeIds(tenantId, customer.getId());
            customerService.deleteCustomer(tenantId, customerId);
            notificationEntityService.notifyDeleteEntity(tenantId, customer.getId(), customer, customerId,
                    ActionType.DELETED, relatedEdgeIds, user, customerId.toString());
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, customer.getId(), ComponentLifecycleEvent.DELETED);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.CUSTOMER), ActionType.DELETED,
                    user, e, customerId.toString());
            throw e;
        }
    }
}
