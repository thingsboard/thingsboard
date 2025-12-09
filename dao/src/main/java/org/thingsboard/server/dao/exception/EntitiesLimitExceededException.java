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
package org.thingsboard.server.dao.exception;

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;

public class EntitiesLimitExceededException extends DataValidationException {
    private static final long serialVersionUID = -9211462514373279196L;

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
