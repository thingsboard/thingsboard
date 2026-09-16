// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.exception;

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;

public class EntitiesLimitExceededException extends DataValidationException {

    @Getter
    private final TenantId tenantId;
    @Getter
    private final EntityType entityType;

    @Getter
    private final long limit;

    public EntitiesLimitExceededException(TenantId tenantId, EntityType entityType, long limit) {
        super(entityType.getNormalName() + "s limit reached");
        this.tenantId = tenantId;
        this.entityType = entityType;
        this.limit = limit;
    }

}
