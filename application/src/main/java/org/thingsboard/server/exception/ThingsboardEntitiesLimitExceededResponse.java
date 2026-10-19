// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
