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
import org.thingsboard.server.common.data.id.AssetId;

@ApiModel
@Data
@EqualsAndHashCode(callSuper = true)
public class AssetInfo extends Asset {

    private static final long serialVersionUID = -4094528227011066194L;

    @ApiModelProperty(position = 10, value = "Title of the Customer that owns the asset.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String customerTitle;
    @ApiModelProperty(position = 11, value = "Indicates special 'Public' Customer that is auto-generated to use the assets on public dashboards.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private boolean customerIsPublic;

    @ApiModelProperty(position = 12, value = "Name of the corresponding Asset Profile.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String assetProfileName;


    public AssetInfo() {
        super();
    }

    public AssetInfo(AssetId assetId) {
        super(assetId);
    }

    public AssetInfo(Asset asset, String customerTitle, boolean customerIsPublic, String assetProfileName) {
        super(asset);
        this.customerTitle = customerTitle;
        this.customerIsPublic = customerIsPublic;
        this.assetProfileName = assetProfileName;
    }
}
