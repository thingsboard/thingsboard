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
package org.thingsboard.server.service.install.update;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.customer.CustomerDao;

import java.util.ArrayList;
import java.util.List;

@Component
public class TenantCustomersTitleUpdaterComponent {

    @Autowired
    private CustomerDao customerDao;

    private static final int DEFAULT_LIMIT = 100;

    public List<Customer> updateDuplicateCustomersTitle() {
        PageLink pageLink = new PageLink(DEFAULT_LIMIT);
        boolean hasNext = true;
        List<Customer> customers = new ArrayList<>();
        while (hasNext) {
            PageData<Customer> entities = customerDao.findCustomerWithEqualTitle(pageLink);
            customers.addAll(entities.getData());

            hasNext = entities.hasNext();
            if (hasNext) {
                pageLink = pageLink.nextPageLink();
            }
        }
        return updateDuplicateCustomersTitle(customers);
    }

    protected List<Customer> updateDuplicateCustomersTitle(List<Customer> customers) {
        if (customers == null || customers.isEmpty()) return customers;
        sortCustomersByTitleAndCreatedTime(customers);
        int countEqualsTitleAndTenantIdBefore = 0;
        String lastCustomerName = customers.get(0).getName();
        for (int i = 1; i < customers.size(); i++) {
            Customer customer = customers.get(i);
            if (customer.getName().equals(lastCustomerName)) {
                countEqualsTitleAndTenantIdBefore++;
                customer.setTitle(customer.getName() + "-" + countEqualsTitleAndTenantIdBefore);
                customers.set(i, updateCustomerTitle(customer.getTenantId(), customer));
            } else {
                lastCustomerName = customer.getName();
                countEqualsTitleAndTenantIdBefore = 0;
            }
        }
        return customers;
    }

    protected Customer updateCustomerTitle(TenantId id, Customer customer) {
        return customerDao.save(id, customer);
    }

    protected void sortCustomersByTitleAndCreatedTime(List<Customer> customers) {
        customers.sort((o1, o2) -> {
            if (!o1.getName().equals(o2.getName())) return o1.getName().compareTo(o2.getName());
            else return Long.compare(o2.getCreatedTime(), o1.getCreatedTime());
        });
    }
}
