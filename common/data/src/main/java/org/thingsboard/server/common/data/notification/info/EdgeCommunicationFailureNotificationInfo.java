// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdgeCommunicationFailureNotificationInfo implements RuleOriginatedNotificationInfo {

    private TenantId tenantId;
    private CustomerId customerId;
    private EdgeId edgeId;
    private String edgeName;
    private String failureMsg;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "edgeId", edgeId.toString(),
                "edgeName", edgeName,
                "failureMsg", failureMsg
        );
    }

    @Override
    public TenantId getAffectedTenantId() {
        return tenantId;
    }

    @Override
    public CustomerId getAffectedCustomerId() {
        return customerId;
    }

    @Override
    public EntityId getStateEntityId() {
        return edgeId;
    }
}
