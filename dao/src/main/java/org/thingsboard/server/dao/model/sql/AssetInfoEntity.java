// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class AssetInfoEntity extends AbstractAssetEntity<AssetInfo> {

    public static final Map<String,String> assetInfoColumnMap = new HashMap<>();
    static {
        assetInfoColumnMap.put("customerTitle", "c.title");
        assetInfoColumnMap.put("assetProfileName", "p.name");
    }

    private String customerTitle;
    private boolean customerIsPublic;
    private String assetProfileName;

    public AssetInfoEntity() {
        super();
    }

    public AssetInfoEntity(AssetEntity assetEntity,
                           String customerTitle,
                           Object customerAdditionalInfo,
                           String assetProfileName) {
        super(assetEntity);
        this.customerTitle = customerTitle;
        if (customerAdditionalInfo != null && ((JsonNode)customerAdditionalInfo).has("isPublic")) {
            this.customerIsPublic = ((JsonNode)customerAdditionalInfo).get("isPublic").asBoolean();
        } else {
            this.customerIsPublic = false;
        }
        this.assetProfileName = assetProfileName;
    }

    @Override
    public AssetInfo toData() {
        return new AssetInfo(super.toAsset(), customerTitle, customerIsPublic, assetProfileName);
    }
}
