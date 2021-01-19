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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.dao.util.mapping.JsonBinaryType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Table(name = ModelConstants.DEVICE_PROFILE_COLUMN_FAMILY_NAME)
@Slf4j
public final class DeviceProfileEntity extends BaseSqlEntity<DeviceProfile> implements SearchTextEntity<DeviceProfile> {

    @Column(name = ModelConstants.DEVICE_PROFILE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.DEVICE_PROFILE_NAME_PROPERTY)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.DEVICE_PROFILE_TYPE_PROPERTY)
    private DeviceProfileType type;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.DEVICE_PROFILE_TRANSPORT_TYPE_PROPERTY)
    private DeviceTransportType transportType;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.DEVICE_PROFILE_PROVISION_TYPE_PROPERTY)
    private DeviceProfileProvisionType provisionType;

    @Column(name = ModelConstants.DEVICE_PROFILE_DESCRIPTION_PROPERTY)
    private String description;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = ModelConstants.DEVICE_PROFILE_IS_DEFAULT_PROPERTY)
    private boolean isDefault;

    @Column(name = ModelConstants.DEVICE_PROFILE_DEFAULT_RULE_CHAIN_ID_PROPERTY, columnDefinition = "uuid")
    private UUID defaultRuleChainId;

    @Column(name = ModelConstants.DEVICE_PROFILE_DEFAULT_QUEUE_NAME_PROPERTY)
    private String defaultQueueName;

    @Type(type = "jsonb")
    @Column(name = ModelConstants.DEVICE_PROFILE_PROFILE_DATA_PROPERTY, columnDefinition = "jsonb")
    private JsonNode profileData;

    @Column(name=ModelConstants.DEVICE_PROFILE_PROVISION_DEVICE_KEY)
    private String provisionDeviceKey;

    @Column(name = ModelConstants.DEVICE_PROFILE_KEYS)
    private String keys;

    public DeviceProfileEntity() {
        super();
    }

    public DeviceProfileEntity(DeviceProfile deviceProfile) {
        if (deviceProfile.getId() != null) {
            this.setUuid(deviceProfile.getId().getId());
        }
        if (deviceProfile.getTenantId() != null) {
            this.tenantId = deviceProfile.getTenantId().getId();
        }
        this.setCreatedTime(deviceProfile.getCreatedTime());
        this.name = deviceProfile.getName();
        this.type = deviceProfile.getType();
        this.transportType = deviceProfile.getTransportType();
        this.provisionType = deviceProfile.getProvisionType();
        this.description = deviceProfile.getDescription();
        this.isDefault = deviceProfile.isDefault();
        this.profileData = JacksonUtil.convertValue(deviceProfile.getProfileData(), ObjectNode.class);
        if (deviceProfile.getDefaultRuleChainId() != null) {
            this.defaultRuleChainId = deviceProfile.getDefaultRuleChainId().getId();
        }
        this.defaultQueueName = deviceProfile.getDefaultQueueName();
        this.provisionDeviceKey = deviceProfile.getProvisionDeviceKey();
        try {
            this.keys = JacksonUtil.toString(deviceProfile.getKeys());
        } catch (Exception e) {
            log.error("Unable to serialize entity view keys!", e);
        }
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
    public DeviceProfile toData() {
        DeviceProfile deviceProfile = new DeviceProfile(new DeviceProfileId(this.getUuid()));
        deviceProfile.setCreatedTime(createdTime);
        if (tenantId != null) {
            deviceProfile.setTenantId(new TenantId(tenantId));
        }
        deviceProfile.setName(name);
        deviceProfile.setType(type);
        deviceProfile.setTransportType(transportType);
        deviceProfile.setProvisionType(provisionType);
        deviceProfile.setDescription(description);
        deviceProfile.setDefault(isDefault);
        deviceProfile.setProfileData(JacksonUtil.convertValue(profileData, DeviceProfileData.class));
        if (defaultRuleChainId != null) {
            deviceProfile.setDefaultRuleChainId(new RuleChainId(defaultRuleChainId));
        }
        deviceProfile.setDefaultQueueName(defaultQueueName);
        deviceProfile.setProvisionDeviceKey(provisionDeviceKey);
        try {
            deviceProfile.setKeys(JacksonUtil.fromString(keys, TelemetryEntityView.class));
        } catch (Exception e) {
            log.error("Unable to read entity view keys!", e);
        }
        return deviceProfile;
    }
}
