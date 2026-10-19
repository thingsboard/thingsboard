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
public class AssetFields extends AbstractEntityFields implements ProfileAwareFields {

    private String type;
    private UUID assetProfileId;
    private String label;
    private String additionalInfo;

    @JsonIgnore
    @Override
    public String getProfileName() {
        return type;
    }

    @JsonIgnore
    @Override
    public UUID getProfileId() {
        return assetProfileId;
    }

    public AssetFields(UUID id, long createdTime, UUID tenantId, UUID customerId, String name,
                       Long version, String type, String label, UUID assetProfileId, JsonNode additionalInfo) {
        super(id, createdTime, tenantId, customerId, name, version);
        this.type = type;
        this.assetProfileId = assetProfileId;
        this.label = label;
        this.additionalInfo = getText(additionalInfo);
    }
}
