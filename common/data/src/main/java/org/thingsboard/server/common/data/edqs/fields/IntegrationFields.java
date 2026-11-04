// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.edqs.fields;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.UUID;

import static org.thingsboard.server.common.data.edqs.fields.FieldsUtil.getText;

@Data
@NoArgsConstructor
@SuperBuilder
public class IntegrationFields extends AbstractEntityFields {

    private String type;
    private String additionalInfo;

    public IntegrationFields(UUID id, long createdTime, UUID tenantId, String name, Long version, IntegrationType type, JsonNode additionalInfo) {
        super(id, createdTime, tenantId, name, version);
        this.type = type.name();
        this.additionalInfo = getText(additionalInfo);
    }
}
