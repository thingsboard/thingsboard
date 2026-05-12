/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.rule.engine;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;

import java.util.List;
import java.util.UUID;

/**
 * One element of the {@code tb_aclSnapshot} metadata JSON array. Describes which
 * {@link org.thingsboard.server.service.security.permission.Operation} values the calling
 * user is allowed to perform on the referenced entity. An empty {@link #allowed} list means
 * the caller has no operations on this entity, or the entity is not addressable for ACL
 * (unmapped EntityType, missing/cross-tenant entity, non-tenant-scoped entity).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityAclEntry {

    @Schema(description = "Entity type of the referenced entity.")
    private EntityType entityType;

    @Schema(description = "UUID of the referenced entity.")
    private UUID entityId;

    @Schema(description = "Names of Operation values the caller is allowed to perform on the entity. " +
            "Operation names are serialized as strings so JS/TBEL rule nodes can iterate them naturally.")
    private List<String> allowed;

}
