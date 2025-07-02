/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ConcurrentReferenceHashMap.ReferenceType;
import org.thingsboard.server.common.data.EntityType;

import java.io.Serial;
import java.util.UUID;

public final class TenantId extends UUIDBased implements EntityId {

    @JsonIgnore
    static final ConcurrentReferenceHashMap<UUID, TenantId> tenants = new ConcurrentReferenceHashMap<>(16, ReferenceType.SOFT);

    @JsonIgnore
    public static final TenantId SYS_TENANT_ID = TenantId.fromUUID(EntityId.NULL_UUID);

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonCreator
    public static TenantId fromUUID(@JsonProperty("id") UUID id) {
        return tenants.computeIfAbsent(id, TenantId::new);
    }

    // Please, use TenantId.fromUUID instead
    // Default constructor is still available due to possible usage in extensions
    @Deprecated
    public TenantId(UUID id) {
        super(id);
    }

    @JsonIgnore
    public boolean isSysTenantId() {
        return this.equals(SYS_TENANT_ID);
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "string", example = "TENANT", allowableValues = "TENANT")
    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT;
    }

}
