// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.menu;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@Builder
public class CustomMenuFilter {

    private TenantId tenantId;
    private CustomerId customerId;
    private CMScope scope;
    private CMAssigneeType assigneeType;

}
