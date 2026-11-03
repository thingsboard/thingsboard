// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
public class RuleNodeFields implements EntityFields {

    private UUID id;
    private long createdTime;
    private String name;
    private String additionalInfo;

    public RuleNodeFields(UUID id, long createdTime, String name, JsonNode additionalInfo) {
        this.id = id;
        this.createdTime = createdTime;
        this.name = name;
        this.additionalInfo = getText(additionalInfo);
    }
}
