// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device.claim;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.Customer;

@Data
@AllArgsConstructor
public class ReclaimResult {
    Customer unassignedCustomer;
}
