// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy.customer;

import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.service.entitiy.SimpleTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface TbCustomerService extends SimpleTbEntityService<Customer> {

    Customer save(Customer customer, NameConflictStrategy nameConflictStrategy, SecurityUser user) throws Exception;

}
