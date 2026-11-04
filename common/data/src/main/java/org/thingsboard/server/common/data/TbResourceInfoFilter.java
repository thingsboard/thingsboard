// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Set;

@Data
@Builder
public class TbResourceInfoFilter {

    private TenantId tenantId;
    private CustomerId customerId;
    private Set<ResourceType> resourceTypes;
    private Set<ResourceSubType> resourceSubTypes;

}
