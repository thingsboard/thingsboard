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
