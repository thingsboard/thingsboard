// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edqs.fields;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class DeviceFields extends AbstractEntityFields implements ProfileAwareFields {

    private String label;
    private String type;
    private UUID deviceProfileId;
    private String additionalInfo;

    @JsonIgnore
    @Override
    public String getProfileName() {
        return type;
    }

    @JsonIgnore
    @Override
    public UUID getProfileId() {
        return deviceProfileId;
    }

    public DeviceFields(UUID id, long createdTime, UUID tenantId, UUID customerId, String name, Long version, String type,
                        String label, UUID deviceProfileId, JsonNode additionalInfo) {
        super(id, createdTime, tenantId, customerId,  name, version);
        this.label = label;
        this.type = type;
        this.deviceProfileId = deviceProfileId;
        this.additionalInfo = getText(additionalInfo);
    }
}
