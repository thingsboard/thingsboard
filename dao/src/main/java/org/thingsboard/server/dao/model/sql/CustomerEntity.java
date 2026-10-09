// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.dao.model.ModelConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.CUSTOMER_TABLE_NAME)
public final class CustomerEntity extends AbstractCustomerEntity<Customer> {

    public CustomerEntity() {
        super();
    }

    public CustomerEntity(Customer customer) {
        super(customer);
    }

    @Override
    public Customer toData() {
        return super.toCustomer();
    }

}
