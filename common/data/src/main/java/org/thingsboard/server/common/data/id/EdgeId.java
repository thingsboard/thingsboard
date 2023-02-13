/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ConcurrentReferenceHashMap.ReferenceType;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

public class EdgeId extends UUIDBased implements EntityId {

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

    @ApiModelProperty(position = 2, required = true, value = "string", example = "EDGE", allowableValues = "EDGE")
    @Override
    public EntityType getEntityType() {
        return EntityType.EDGE;
    }

    @JsonCreator
    public static EdgeId fromUUID(@JsonProperty("id") UUID id) {
        return edges.computeIfAbsent(id, EdgeId::new);
    }
}
