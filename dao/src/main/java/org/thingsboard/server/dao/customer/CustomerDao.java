/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.customer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.model.CustomerEntity;
import org.thingsboard.server.dao.model.DeviceEntity;

/**
 * The Interface CustomerDao.
 */
public interface CustomerDao extends Dao<CustomerEntity> {

    /**
     * Save or update customer object
     *
     * @param customer the customer object
     * @return saved customer object
     */
    CustomerEntity save(Customer customer);
    
    /**
     * Find customers by tenant id and page link.
     *
     * @param tenantId the tenant id
     * @param pageLink the page link
     * @return the list of customer objects
     */
    List<CustomerEntity> findCustomersByTenantId(UUID tenantId, TextPageLink pageLink);

    /**
     * Find customers by tenantId and customer title.
     *
     * @param tenantId the tenantId
     * @param title the customer title
     * @return the optional customer object
     */
    Optional<CustomerEntity> findCustomersByTenantIdAndTitle(UUID tenantId, String title);
    
}
