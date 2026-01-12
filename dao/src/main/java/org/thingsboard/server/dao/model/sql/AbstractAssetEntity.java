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
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ASSET_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ASSET_LABEL_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ASSET_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ASSET_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ASSET_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EXTERNAL_ID_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractAssetEntity<T extends Asset> extends BaseVersionedEntity<T> {

    @Column(name = ASSET_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ASSET_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = ASSET_NAME_PROPERTY)
    private String name;

    @Column(name = ASSET_TYPE_PROPERTY)
    private String type;

    @Column(name = ASSET_LABEL_PROPERTY)
    private String label;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.ASSET_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Column(name = ModelConstants.ASSET_ASSET_PROFILE_ID_PROPERTY, columnDefinition = "uuid")
    private UUID assetProfileId;

    @Column(name = EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public AbstractAssetEntity() {
        super();
    }

    public AbstractAssetEntity(T asset) {
        super(asset);
        if (asset.getTenantId() != null) {
            this.tenantId = asset.getTenantId().getId();
        }
        if (asset.getCustomerId() != null) {
            this.customerId = asset.getCustomerId().getId();
        }
        if (asset.getAssetProfileId() != null) {
            this.assetProfileId = asset.getAssetProfileId().getId();
        }
        this.name = asset.getName();
        this.type = asset.getType();
        this.label = asset.getLabel();
        this.additionalInfo = asset.getAdditionalInfo();
        if (asset.getExternalId() != null) {
            this.externalId = asset.getExternalId().getId();
        }
    }

    public AbstractAssetEntity(AssetEntity assetEntity) {
        super(assetEntity);
        this.tenantId = assetEntity.getTenantId();
        this.customerId = assetEntity.getCustomerId();
        this.assetProfileId = assetEntity.getAssetProfileId();
        this.type = assetEntity.getType();
        this.name = assetEntity.getName();
        this.label = assetEntity.getLabel();
        this.additionalInfo = assetEntity.getAdditionalInfo();
        this.externalId = assetEntity.getExternalId();
    }

    protected Asset toAsset() {
        Asset asset = new Asset(new AssetId(id));
        asset.setCreatedTime(createdTime);
        asset.setVersion(version);
        if (tenantId != null) {
            asset.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            asset.setCustomerId(new CustomerId(customerId));
        }
        if (assetProfileId != null) {
            asset.setAssetProfileId(new AssetProfileId(assetProfileId));
        }
        asset.setName(name);
        asset.setType(type);
        asset.setLabel(label);
        asset.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            asset.setExternalId(new AssetId(externalId));
        }
        return asset;
    }

}
