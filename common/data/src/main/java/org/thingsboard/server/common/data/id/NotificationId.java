// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

public class NotificationId extends UUIDBased implements EntityId {

    @JsonCreator
    public NotificationId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "string", example = "NOTIFICATION", allowableValues = "NOTIFICATION")
    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION;
    }

}
