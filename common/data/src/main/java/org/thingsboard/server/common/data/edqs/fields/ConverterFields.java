// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.edqs.fields;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.converter.ConverterType;

import java.util.UUID;

import static org.thingsboard.server.common.data.edqs.fields.FieldsUtil.getText;

@Data
@NoArgsConstructor
@SuperBuilder
public class ConverterFields extends AbstractEntityFields {

    private String type;
    private String additionalInfo;

    public ConverterFields(UUID id, long createdTime, UUID tenantId, String name, Long version, ConverterType type, JsonNode additionalInfo) {
        super(id, createdTime, tenantId, name, version);
        this.type = type.name();
        this.additionalInfo = getText(additionalInfo);
    }
}
