// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.UUID;

public class AliasEntityIdSerializer extends JsonSerializer<AliasEntityId> {
    @Override
    public void serialize(AliasEntityId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        String entityType;
        if (value.isAliasEntityId()) {
            entityType = value.getAliasEntityType().name();
        } else {
            entityType = value.getEntityType().name();
        }
        gen.writeStringField("entityType", entityType);
        UUID id = null;
        if (value.getId() != null) {
            id = value.getId();
        } else if (value.defaultEntityId() != null) {
            id = value.defaultEntityId().getId();
        }
        if (id != null) {
            gen.writeStringField("id", id.toString());
        }
        gen.writeEndObject();
    }
}
