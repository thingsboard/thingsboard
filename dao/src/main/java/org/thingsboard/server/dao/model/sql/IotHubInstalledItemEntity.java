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

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.proxy.HibernateProxy;
import org.thingsboard.server.common.data.id.IotHubInstalledItemId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItemDescriptor;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@ToString
@Entity
@Table(name = ModelConstants.IOT_HUB_INSTALLED_ITEM_TABLE_NAME)
public class IotHubInstalledItemEntity extends BaseSqlEntity<IotHubInstalledItem> {

    @Column(name = ModelConstants.IOT_HUB_INSTALLED_ITEM_TENANT_ID_COLUMN, nullable = false, columnDefinition = "UUID")
    private UUID tenantId;

    @Column(name = ModelConstants.IOT_HUB_INSTALLED_ITEM_ITEM_ID_COLUMN, nullable = false, columnDefinition = "UUID")
    private UUID itemId;

    @Column(name = ModelConstants.IOT_HUB_INSTALLED_ITEM_ITEM_VERSION_ID_COLUMN, nullable = false, columnDefinition = "UUID")
    private UUID itemVersionId;

    @Column(name = ModelConstants.IOT_HUB_INSTALLED_ITEM_ITEM_NAME_COLUMN, nullable = false)
    private String itemName;

    @Column(name = ModelConstants.IOT_HUB_INSTALLED_ITEM_ITEM_TYPE_COLUMN, nullable = false)
    private String itemType;

    @Column(name = ModelConstants.IOT_HUB_INSTALLED_ITEM_VERSION_COLUMN, nullable = false)
    private String version;

    @Type(JsonBinaryType.class)
    @Column(name = ModelConstants.IOT_HUB_INSTALLED_ITEM_DESCRIPTOR_COLUMN, nullable = false, columnDefinition = "JSONB")
    private IotHubInstalledItemDescriptor descriptor;

    public IotHubInstalledItemEntity() {
    }

    public IotHubInstalledItemEntity(IotHubInstalledItem item) {
        super(item);
        tenantId = getTenantUuid(item.getTenantId());
        itemId = item.getItemId();
        itemVersionId = item.getItemVersionId();
        itemName = item.getItemName();
        itemType = item.getItemType();
        version = item.getVersion();
        descriptor = item.getDescriptor();
    }

    @Override
    public IotHubInstalledItem toData() {
        var item = new IotHubInstalledItem(new IotHubInstalledItemId(id));
        item.setCreatedTime(createdTime);
        item.setTenantId(TenantId.fromUUID(tenantId));
        item.setItemId(itemId);
        item.setItemVersionId(itemVersionId);
        item.setItemName(itemName);
        item.setItemType(itemType);
        item.setVersion(version);
        item.setDescriptor(descriptor);
        return item;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        IotHubInstalledItemEntity that = (IotHubInstalledItemEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

}
