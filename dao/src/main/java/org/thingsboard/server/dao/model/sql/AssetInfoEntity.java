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
