/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import org.thingsboard.server.common.data.id.CustomerId;

import java.util.HashSet;
import java.util.Set;

public interface HasMultipleCustomers {

    Set<ShortCustomerInfo> getAssignedCustomers();
    void setAssignedCustomers(Set<ShortCustomerInfo> assignedCustomers);

    default boolean isAssignedToCustomer(CustomerId customerId) {
        return this.getAssignedCustomers() != null && this.getAssignedCustomers().contains(new ShortCustomerInfo(customerId, null, false));
    }

    default ShortCustomerInfo getAssignedCustomerInfo(CustomerId customerId) {
        if (this.getAssignedCustomers() != null) {
            for (ShortCustomerInfo customerInfo : this.getAssignedCustomers()) {
                if (customerInfo.getCustomerId().equals(customerId)) {
                    return customerInfo;
                }
            }
        }
        return null;
    }

    default boolean addAssignedCustomer(Customer customer) {
        ShortCustomerInfo customerInfo = customer.toShortCustomerInfo();
        Set<ShortCustomerInfo> assignedCustomers = this.getAssignedCustomers();
        if (assignedCustomers != null && assignedCustomers.contains(customerInfo)) {
            return false;
        } else {
            if (assignedCustomers == null) {
                assignedCustomers = new HashSet<>();
            }
            assignedCustomers.add(customerInfo);
            this.setAssignedCustomers(assignedCustomers);
            return true;
        }
    }

    default boolean updateAssignedCustomer(Customer customer) {
        ShortCustomerInfo customerInfo = customer.toShortCustomerInfo();
        Set<ShortCustomerInfo> assignedCustomers = this.getAssignedCustomers();
        if (assignedCustomers != null && assignedCustomers.contains(customerInfo)) {
            assignedCustomers.add(customerInfo);
            this.setAssignedCustomers(assignedCustomers);
            return true;
        } else {
            return false;
        }
    }

    default boolean removeAssignedCustomer(Customer customer) {
        ShortCustomerInfo customerInfo = customer.toShortCustomerInfo();
        Set<ShortCustomerInfo> assignedCustomers = this.getAssignedCustomers();
        if (assignedCustomers != null && assignedCustomers.contains(customerInfo)) {
            assignedCustomers.remove(customerInfo);
            this.setAssignedCustomers(assignedCustomers);
            return true;
        } else {
            return false;
        }
    }
}
