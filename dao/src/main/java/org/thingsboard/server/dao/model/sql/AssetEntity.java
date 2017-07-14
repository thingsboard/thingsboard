/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ASSET_COLUMN_FAMILY_NAME)
public final class AssetEntity extends BaseSqlEntity<Asset> implements SearchTextEntity<Asset> {

    @Transient
    private static final long serialVersionUID = -4089175869616037592L;

    @Column(name = ASSET_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = ASSET_CUSTOMER_ID_PROPERTY)
    private String customerId;

    @Column(name = ASSET_NAME_PROPERTY)
    private String name;

    @Column(name = ASSET_TYPE_PROPERTY)
    private String type;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Type(type = "json")
    @Column(name = ModelConstants.ASSET_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public AssetEntity() {
        super();
    }

    public AssetEntity(Asset asset) {
        if (asset.getId() != null) {
            this.setId(asset.getId().getId());
        }
        if (asset.getTenantId() != null) {
            this.tenantId = UUIDConverter.fromTimeUUID(asset.getTenantId().getId());
        }
        if (asset.getCustomerId() != null) {
            this.customerId = UUIDConverter.fromTimeUUID(asset.getCustomerId().getId());
        }
        this.name = asset.getName();
        this.type = asset.getType();
        this.additionalInfo = asset.getAdditionalInfo();
    }

    @Override
    public String getSearchTextSource() {
        return name;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public String getSearchText() {
        return searchText;
    }

    @Override
    public Asset toData() {
        Asset asset = new Asset(new AssetId(UUIDConverter.fromString(id)));
        asset.setCreatedTime(UUIDs.unixTimestamp(UUIDConverter.fromString(id)));
        if (tenantId != null) {
            asset.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
        }
        if (customerId != null) {
            asset.setCustomerId(new CustomerId(UUIDConverter.fromString(customerId)));
        }
        asset.setName(name);
        asset.setType(type);
        asset.setAdditionalInfo(additionalInfo);
        return asset;
    }

}