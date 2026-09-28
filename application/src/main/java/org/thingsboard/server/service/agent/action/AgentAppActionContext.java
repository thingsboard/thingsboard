// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.action;

import lombok.Value;
import org.thingsboard.server.common.data.id.TenantId;

@Value
public class AgentAppActionContext {
    TenantId tenantId;
}
