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
package org.thingsboard.server.common.data.edqs.fields;

import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.UUID;

@Data
@SuperBuilder
public class AbstractEntityFields implements EntityFields {

    private UUID id;
    private long createdTime;
    private UUID tenantId;
    private UUID customerId;
    private String name;
    private Long version;

    public AbstractEntityFields(UUID id, long createdTime, UUID tenantId, UUID customerId, String name, Long version) {
        this.id = id;
        this.createdTime = createdTime;
        this.tenantId = tenantId;
        this.customerId = checkId(customerId);
        this.name = name;
        this.version = version;
    }

    public AbstractEntityFields() {
    }

    public AbstractEntityFields(UUID id, long createdTime, UUID tenantId, String name, Long version) {
        this(id, createdTime, tenantId, null, name, version);
    }

    public AbstractEntityFields(UUID id, long createdTime, UUID tenantId, UUID customerId, Long version) {
        this(id, createdTime, tenantId, customerId, null, version);

    }

    public AbstractEntityFields(UUID id, long createdTime, String name, Long version) {
        this(id, createdTime, null, name, version);
    }


    public AbstractEntityFields(UUID id, long createdTime, UUID tenantId) {
        this(id, createdTime, tenantId, null, null, null);
    }

    protected UUID checkId(UUID id) {
        return id == null || id.equals(EntityId.NULL_UUID) ? null : id;
    }

    @Override
    public UUID getCustomerId() {
        return checkId(customerId);
    }

}
