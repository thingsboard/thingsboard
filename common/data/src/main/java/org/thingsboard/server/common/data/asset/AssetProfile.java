/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasRuleEngineProfile;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@ApiModel
@Data
@ToString(exclude = {"image"})
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class AssetProfile extends SearchTextBased<AssetProfileId> implements HasName, HasTenantId, HasRuleEngineProfile, ExportableEntity<AssetProfileId> {

    private static final long serialVersionUID = 6998485460273302018L;

    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id that owns the profile.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "name")
    @ApiModelProperty(position = 4, value = "Unique Asset Profile Name in scope of Tenant.", example = "Building")
    private String name;
    @NoXss
    @ApiModelProperty(position = 11, value = "Asset Profile description. ")
    private String description;
    @Length(fieldName = "image", max = 1000000)
    @ApiModelProperty(position = 12, value = "Either URL or Base64 data of the icon. Used in the mobile application to visualize set of asset profiles in the grid view. ")
    private String image;
    private boolean isDefault;
    @ApiModelProperty(position = 7, value = "Reference to the rule chain. " +
            "If present, the specified rule chain will be used to process all messages related to asset, including asset updates, telemetry, attribute updates, etc. " +
            "Otherwise, the root rule chain will be used to process those messages.")
    private RuleChainId defaultRuleChainId;
    @ApiModelProperty(position = 6, value = "Reference to the dashboard. Used in the mobile application to open the default dashboard when user navigates to asset details.")
    private DashboardId defaultDashboardId;

    @NoXss
    @ApiModelProperty(position = 8, value = "Rule engine queue name. " +
            "If present, the specified queue will be used to store all unprocessed messages related to asset, including asset updates, telemetry, attribute updates, etc. " +
            "Otherwise, the 'Main' queue will be used to store those messages.")
    private String defaultQueueName;

    @ApiModelProperty(position = 13, value = "Reference to the edge rule chain. " +
            "If present, the specified edge rule chain will be used on the edge to process all messages related to asset, including asset updates, telemetry, attribute updates, etc. " +
            "Otherwise, the edge root rule chain will be used to process those messages.")
    private RuleChainId defaultEdgeRuleChainId;

    private AssetProfileId externalId;

    public AssetProfile() {
        super();
    }

    public AssetProfile(AssetProfileId assetProfileId) {
        super(assetProfileId);
    }

    public AssetProfile(AssetProfile assetProfile) {
        super(assetProfile);
        this.tenantId = assetProfile.getTenantId();
        this.name = assetProfile.getName();
        this.description = assetProfile.getDescription();
        this.image = assetProfile.getImage();
        this.isDefault = assetProfile.isDefault();
        this.defaultRuleChainId = assetProfile.getDefaultRuleChainId();
        this.defaultDashboardId = assetProfile.getDefaultDashboardId();
        this.defaultQueueName = assetProfile.getDefaultQueueName();
        this.defaultEdgeRuleChainId = assetProfile.getDefaultEdgeRuleChainId();
        this.externalId = assetProfile.getExternalId();
    }

    @ApiModelProperty(position = 1, value = "JSON object with the asset profile Id. " +
            "Specify this field to update the asset profile. " +
            "Referencing non-existing asset profile Id will cause error. " +
            "Omit this field to create new asset profile.")
    @Override
    public AssetProfileId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the profile creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    public String getSearchText() {
        return getName();
    }

    @ApiModelProperty(position = 5, value = "Used to mark the default profile. Default profile is used when the asset profile is not specified during asset creation.")
    public boolean isDefault(){
        return isDefault;
    }

}
