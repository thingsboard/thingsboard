// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.edqs.fields;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

import static org.thingsboard.server.common.data.edqs.fields.FieldsUtil.getText;

@Data
@NoArgsConstructor
@SuperBuilder
public class BlobEntityFields extends AbstractEntityFields {

    private String type;
    private String additionalInfo;

    public BlobEntityFields(UUID id, long createdTime, UUID tenantId, UUID customerId, String name,
                            String type, JsonNode additionalInfo) {
        super(id, createdTime, tenantId, customerId, name, null);
        this.type = type;
        this.additionalInfo = getText(additionalInfo);
    }
}
