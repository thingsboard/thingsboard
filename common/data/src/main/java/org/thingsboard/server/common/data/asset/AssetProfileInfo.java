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
package org.thingsboard.server.common.data.asset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "image")
public class AssetProfileInfo extends EntityInfo {

    @Schema(description = "Either URL or Base64 data of the icon. Used in the mobile application to visualize set of asset profiles in the grid view. ")
    private final String image;
    @Schema(description = "Reference to the dashboard. Used in the mobile application to open the default dashboard when user navigates to asset details.")
    private final DashboardId defaultDashboardId;

    @Schema(description = "Tenant id.")
    private final TenantId tenantId;

    @JsonCreator
    public AssetProfileInfo(@JsonProperty("id") EntityId id,
                            @JsonProperty("tenantId") TenantId tenantId,
                            @JsonProperty("name") String name,
                            @JsonProperty("image") String image,
                            @JsonProperty("defaultDashboardId") DashboardId defaultDashboardId) {
        super(id, name);
        this.tenantId = tenantId;
        this.image = image;
        this.defaultDashboardId = defaultDashboardId;
    }

    public AssetProfileInfo(UUID uuid, UUID tenantId, String name, String image, UUID defaultDashboardId) {
        super(EntityIdFactory.getByTypeAndUuid(EntityType.ASSET_PROFILE, uuid), name);
        this.tenantId = TenantId.fromUUID(tenantId);
        this.image = image;
        this.defaultDashboardId = defaultDashboardId != null ? new DashboardId(defaultDashboardId) : null;
    }

    public AssetProfileInfo(AssetProfile profile) {
        this(profile.getId(), profile.getTenantId(), profile.getName(), profile.getImage(), profile.getDefaultDashboardId());
    }

}
