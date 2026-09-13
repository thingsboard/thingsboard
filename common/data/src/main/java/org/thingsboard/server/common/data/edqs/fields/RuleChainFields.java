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
public class RuleChainFields extends AbstractEntityFields {

    private String additionalInfo;

    public RuleChainFields(UUID id, long createdTime, UUID tenantId, String name, Long version, JsonNode additionalInfo) {
        super(id, createdTime, tenantId, name, version);
        this.additionalInfo = getText(additionalInfo);
    }
}
