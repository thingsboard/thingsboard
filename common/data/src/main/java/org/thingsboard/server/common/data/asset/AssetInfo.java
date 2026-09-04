// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.asset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.AssetId;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class AssetInfo extends Asset {

    private static final long serialVersionUID = -4094528227011066194L;

    @Schema(description = "Title of the Customer that owns the asset.", accessMode = Schema.AccessMode.READ_ONLY)
    private String customerTitle;
    @Schema(description = "Indicates special 'Public' Customer that is auto-generated to use the assets on public dashboards.", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean customerIsPublic;

    @Schema(description = "Name of the corresponding Asset Profile.", accessMode = Schema.AccessMode.READ_ONLY)
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
