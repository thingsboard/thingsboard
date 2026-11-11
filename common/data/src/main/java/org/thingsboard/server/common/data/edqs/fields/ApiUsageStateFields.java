// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.edqs.fields;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class ApiUsageStateFields extends AbstractEntityFields {

    private EntityId entityId;
    private ApiUsageStateValue transportState;
    private ApiUsageStateValue dbStorageState;
    private ApiUsageStateValue reExecState;
    private ApiUsageStateValue jsExecState;
    private ApiUsageStateValue tbelExecState;
    private ApiUsageStateValue emailExecState;
    private ApiUsageStateValue smsExecState;
    private ApiUsageStateValue alarmExecState;
    private ApiUsageStateValue reportExecState;
    private ApiUsageStateValue aiState;

    public ApiUsageStateFields(UUID id, long createdTime, UUID tenantId, UUID entityId, String entityType, ApiUsageStateValue transportState, ApiUsageStateValue dbStorageState,
                               ApiUsageStateValue reExecState, ApiUsageStateValue jsExecState, ApiUsageStateValue tbelExecState,
                               ApiUsageStateValue emailExecState, ApiUsageStateValue smsExecState, ApiUsageStateValue alarmExecState,
                               ApiUsageStateValue reportExecState, ApiUsageStateValue aiState, Long version) {
        super(id, createdTime, tenantId, null, null, version);
        this.entityId = (entityType != null && entityId != null) ? EntityIdFactory.getByTypeAndUuid(entityType, entityId) : null;
        this.transportState = transportState;
        this.dbStorageState = dbStorageState;
        this.reExecState = reExecState;
        this.jsExecState = jsExecState;
        this.tbelExecState = tbelExecState;
        this.emailExecState = emailExecState;
        this.smsExecState = smsExecState;
        this.alarmExecState = alarmExecState;
        this.reportExecState = reportExecState;
        this.aiState = aiState;
    }

}
