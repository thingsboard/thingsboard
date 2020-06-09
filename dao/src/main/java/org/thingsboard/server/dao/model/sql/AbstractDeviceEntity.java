/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractDeviceEntity<T extends Device> extends BaseSqlEntity<T> implements SearchTextEntity<T> {

    @Column(name = ModelConstants.DEVICE_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = ModelConstants.DEVICE_CUSTOMER_ID_PROPERTY)
    private String customerId;

    @Column(name = ModelConstants.ENTITY_PROFILE_ID_PROPERTY)
    private String entityProfileId;

    @Column(name = ModelConstants.DEVICE_TYPE_PROPERTY)
    private String type;

    @Column(name = ModelConstants.DEVICE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.DEVICE_LABEL_PROPERTY)
    private String label;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Type(type = "json")
    @Column(name = ModelConstants.DEVICE_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public AbstractDeviceEntity() {
        super();
    }

    public AbstractDeviceEntity(Device device) {
        if (device.getId() != null) {
            this.setUuid(device.getId().getId());
        }
        if (device.getTenantId() != null) {
            this.tenantId = toString(device.getTenantId().getId());
        }
        if (device.getCustomerId() != null) {
            this.customerId = toString(device.getCustomerId().getId());
        }
        if (device.getEntityProfileId() != null) {
            this.customerId = toString(device.getEntityProfileId().getId());
        }
        this.name = device.getName();
        this.type = device.getType();
        this.label = device.getLabel();
        this.additionalInfo = device.getAdditionalInfo();
    }

    public AbstractDeviceEntity(DeviceEntity deviceEntity) {
        this.setId(deviceEntity.getId());
        this.tenantId = deviceEntity.getTenantId();
        this.customerId = deviceEntity.getCustomerId();
        this.entityProfileId = deviceEntity.getEntityProfileId();
        this.type = deviceEntity.getType();
        this.name = deviceEntity.getName();
        this.label = deviceEntity.getLabel();
        this.searchText = deviceEntity.getSearchText();
        this.additionalInfo = deviceEntity.getAdditionalInfo();
    }

    @Override
    public String getSearchTextSource() {
        return name;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    protected Device toDevice() {
        Device device = new Device(new DeviceId(getUuid()));
        device.setCreatedTime(Uuids.unixTimestamp(getUuid()));
        if (tenantId != null) {
            device.setTenantId(new TenantId(toUUID(tenantId)));
        }
        if (customerId != null) {
            device.setCustomerId(new CustomerId(toUUID(customerId)));
        }
        if (entityProfileId != null) {
            device.setEntityProfileId(new EntityProfileId(toUUID(entityProfileId)));
        }
        device.setName(name);
        device.setType(type);
        device.setLabel(label);
        device.setAdditionalInfo(additionalInfo);
        return device;
    }

}
