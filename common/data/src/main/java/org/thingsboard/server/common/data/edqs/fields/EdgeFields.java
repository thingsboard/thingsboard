// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edqs.fields;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

import static org.thingsboard.server.common.data.edqs.fields.FieldsUtil.getText;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class EdgeFields extends AbstractEntityFields {

    private String type;
    private String label;
    private String additionalInfo;

    public EdgeFields(UUID id, long createdTime, UUID tenantId, UUID customerId, String name, Long version,
                      String type, String label, JsonNode additionalInfo) {
        super(id, createdTime, tenantId, customerId, name, version);
        this.type = type;
        this.label = label;
        this.additionalInfo = getText(additionalInfo);
    }
}
