// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ConcurrentReferenceHashMap.ReferenceType;
import org.thingsboard.server.common.data.EntityType;

import java.io.Serial;
import java.util.UUID;

public class EdgeId extends UUIDBased implements EntityId {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonIgnore
    static final ConcurrentReferenceHashMap<UUID, EdgeId> edges = new ConcurrentReferenceHashMap<>(16, ReferenceType.SOFT);

    @JsonCreator
    public EdgeId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static EdgeId fromString(String edgeId) {
        return new EdgeId(UUID.fromString(edgeId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "string", example = "EDGE", allowableValues = "EDGE")
    @Override
    public EntityType getEntityType() {
        return EntityType.EDGE;
    }

    @JsonCreator
    public static EdgeId fromUUID(@JsonProperty("id") UUID id) {
        return edges.computeIfAbsent(id, EdgeId::new);
    }

}
