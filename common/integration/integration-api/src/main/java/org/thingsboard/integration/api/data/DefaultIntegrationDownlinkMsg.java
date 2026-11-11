// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.data;

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;

@Data
public class DefaultIntegrationDownlinkMsg implements IntegrationDownlinkMsg {

    private final TenantId tenantId;
    private final IntegrationId integrationId;
    private final TbMsg tbMsg;
    private final String entityName;

    @Override
    public EntityId getEntityId() {
        return tbMsg.getOriginator();
    }
}
