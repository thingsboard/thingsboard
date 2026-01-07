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
package org.thingsboard.server.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;

@Schema
public class ThingsboardEntitiesLimitExceededResponse extends ThingsboardErrorResponse {

    private final EntityType entityType;

    private final Long limit;

    protected ThingsboardEntitiesLimitExceededResponse(String message, EntityType entityType, Long limit) {
        super(message, ThingsboardErrorCode.ENTITIES_LIMIT_EXCEEDED, HttpStatus.FORBIDDEN);
        this.entityType = entityType;
        this.limit = limit;
    }

    public static ThingsboardEntitiesLimitExceededResponse of(final String message, final EntityType entityType, final Long limit) {
        return new ThingsboardEntitiesLimitExceededResponse(message, entityType, limit);
    }

    @Schema(description = "Entity type", accessMode = Schema.AccessMode.READ_ONLY)
    public EntityType getEntityType() {
        return entityType;
    }

    @Schema(description = "Limit", accessMode = Schema.AccessMode.READ_ONLY)
    public Long getLimit() {
        return limit;
    }

}
