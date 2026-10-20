// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.edqs.fields;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.util.UUID;

import static org.thingsboard.server.common.data.edqs.fields.FieldsUtil.getText;

@Data
@NoArgsConstructor
@SuperBuilder
public class SchedulerEventFields extends AbstractEntityFields {

    private String type;
    private String additionalInfo;
    private String configuration;
    private String schedule;
    private EntityId originatorId;

    public SchedulerEventFields(UUID id, long createdTime, UUID tenantId, UUID customerId, String name, Long version,
                                String type, JsonNode schedule, JsonNode configuration, JsonNode additionalInfo, UUID originatorId, EntityType originatorType) {
        super(id, createdTime, tenantId, customerId, name, version);
        this.type = type;
        this.schedule = getText(schedule);
        this.configuration = getText(configuration);
        this.additionalInfo = getText(additionalInfo);
        this.originatorId = (originatorId != null && originatorType != null) ? EntityIdFactory.getByTypeAndUuid(originatorType, originatorId) : null;
    }
}
