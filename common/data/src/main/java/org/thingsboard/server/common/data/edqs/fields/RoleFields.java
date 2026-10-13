// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.edqs.fields;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.role.RoleType;

import java.util.UUID;

import static org.thingsboard.server.common.data.edqs.fields.FieldsUtil.getText;

@Data
@NoArgsConstructor
@SuperBuilder
public class RoleFields extends AbstractEntityFields {

    private String type;
    private String additionalInfo;

    public RoleFields(UUID id, long createdTime, UUID tenantId, UUID customerId, String name, Long version, RoleType type, JsonNode additionalInfo) {
        super(id, createdTime, tenantId, customerId, name, version);
        this.type = type.name();
        this.additionalInfo = getText(additionalInfo);
    }
}
