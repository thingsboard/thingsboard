// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
