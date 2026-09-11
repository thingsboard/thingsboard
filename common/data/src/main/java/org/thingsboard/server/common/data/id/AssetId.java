// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

@Schema
public class AssetId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public AssetId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static AssetId fromString(String assetId) {
        return new AssetId(UUID.fromString(assetId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "string", example = "ASSET", allowableValues = "ASSET")
    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET;
    }
}
