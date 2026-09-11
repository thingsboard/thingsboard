// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

@Schema(allOf = EntityId.class)
public class MobileAppId extends UUIDBased implements EntityId{

    @JsonCreator
    public MobileAppId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static MobileAppId fromString(String mobileAppId) {
        return new MobileAppId(UUID.fromString(mobileAppId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY, description = "string", example = "MOBILE_APP", allowableValues = "MOBILE_APP")
    @Override
    public EntityType getEntityType() {
        return EntityType.MOBILE_APP;
    }
}
