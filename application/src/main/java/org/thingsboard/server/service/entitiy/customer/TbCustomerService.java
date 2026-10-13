// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.entitiy.customer;

import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.service.entitiy.SimpleTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

public interface TbCustomerService extends SimpleTbEntityService<Customer> {

    Customer save(Customer customer, List<EntityGroup> entityGroups, NameConflictStrategy nameConflictStrategy, SecurityUser user) throws Exception;

}
