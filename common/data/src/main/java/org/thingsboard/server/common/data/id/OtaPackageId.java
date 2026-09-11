// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.io.Serial;
import java.util.UUID;

public class OtaPackageId extends UUIDBased implements EntityId {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonCreator
    public OtaPackageId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static OtaPackageId fromString(String firmwareId) {
        return new OtaPackageId(UUID.fromString(firmwareId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "string", example = "OTA_PACKAGE", allowableValues = "OTA_PACKAGE")
    @Override
    public EntityType getEntityType() {
        return EntityType.OTA_PACKAGE;
    }

}
