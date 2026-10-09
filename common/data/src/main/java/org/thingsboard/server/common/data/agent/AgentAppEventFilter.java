// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@Builder
public class AgentAppEventFilter {

    private TenantId tenantId;
    private AgentApplicationId applicationId;
    private AgentAppEventActionType actionType;
    private AgentProcessingStatus processingStatus;

}
