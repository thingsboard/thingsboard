// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.edqs.fields;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

import static org.thingsboard.server.common.data.edqs.fields.FieldsUtil.getText;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class EntityGroupFields extends AbstractEntityFields {

    private String type;
    private String additionalInfo;
    private UUID ownerId;
    private EntityType ownerType;

    public EntityGroupFields(UUID id, long createdTime, String name, Long version,
                             EntityType type, JsonNode additionalInfo, UUID ownerId, EntityType ownerType) {
        super(id, createdTime, null, name, version);
        this.type = type.name();
        this.additionalInfo = getText(additionalInfo);
        this.ownerId = checkId(ownerId);
        this.ownerType = ownerType;
    }

    @Override
    public UUID getOwnerId() {
        return checkId(ownerId);
    }

}
