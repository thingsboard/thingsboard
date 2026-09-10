// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

@Schema
public class DashboardId extends UUIDBased implements EntityId {

    @JsonCreator
    public DashboardId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static DashboardId fromString(String dashboardId) {
        return new DashboardId(UUID.fromString(dashboardId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "string", example = "DASHBOARD", allowableValues = "DASHBOARD")
    @Override
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }
}
