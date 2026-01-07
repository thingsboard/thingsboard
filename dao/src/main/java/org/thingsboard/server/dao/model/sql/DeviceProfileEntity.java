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
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLJsonPGObjectJsonbType;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.DEVICE_PROFILE_TABLE_NAME)
@ToString(callSuper = true)
public final class DeviceProfileEntity extends BaseVersionedEntity<DeviceProfile> {

    @Column(name = ModelConstants.DEVICE_PROFILE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.DEVICE_PROFILE_NAME_PROPERTY)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.DEVICE_PROFILE_TYPE_PROPERTY)
    private DeviceProfileType type;

    @Column(name = ModelConstants.DEVICE_PROFILE_IMAGE_PROPERTY)
    private String image;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.DEVICE_PROFILE_TRANSPORT_TYPE_PROPERTY)
    private DeviceTransportType transportType;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.DEVICE_PROFILE_PROVISION_TYPE_PROPERTY)
    private DeviceProfileProvisionType provisionType;

    @Column(name = ModelConstants.DEVICE_PROFILE_DESCRIPTION_PROPERTY)
    private String description;

    @Column(name = ModelConstants.DEVICE_PROFILE_IS_DEFAULT_PROPERTY)
    private boolean isDefault;

    @Column(name = ModelConstants.DEVICE_PROFILE_DEFAULT_RULE_CHAIN_ID_PROPERTY, columnDefinition = "uuid")
    private UUID defaultRuleChainId;

    @Column(name = ModelConstants.DEVICE_PROFILE_DEFAULT_DASHBOARD_ID_PROPERTY)
    private UUID defaultDashboardId;

    @Column(name = ModelConstants.DEVICE_PROFILE_DEFAULT_QUEUE_NAME_PROPERTY)
    private String defaultQueueName;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.DEVICE_PROFILE_PROFILE_DATA_PROPERTY, columnDefinition = "jsonb")
    private JsonNode profileData;

    @Column(name = ModelConstants.DEVICE_PROFILE_PROVISION_DEVICE_KEY)
    private String provisionDeviceKey;

    @Column(name = ModelConstants.DEVICE_PROFILE_FIRMWARE_ID_PROPERTY)
    private UUID firmwareId;

    @Column(name = ModelConstants.DEVICE_PROFILE_SOFTWARE_ID_PROPERTY)
    private UUID softwareId;

    @Column(name = ModelConstants.DEVICE_PROFILE_DEFAULT_EDGE_RULE_CHAIN_ID_PROPERTY, columnDefinition = "uuid")
    private UUID defaultEdgeRuleChainId;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public DeviceProfileEntity() {
        super();
    }

    public DeviceProfileEntity(DeviceProfile deviceProfile) {
        super(deviceProfile);
        if (deviceProfile.getTenantId() != null) {
            this.tenantId = deviceProfile.getTenantId().getId();
        }
        this.name = deviceProfile.getName();
        this.type = deviceProfile.getType();
        this.image = deviceProfile.getImage();
        this.transportType = deviceProfile.getTransportType();
        this.provisionType = deviceProfile.getProvisionType();
        this.description = deviceProfile.getDescription();
        this.isDefault = deviceProfile.isDefault();
        this.profileData = JacksonUtil.convertValue(deviceProfile.getProfileData(), ObjectNode.class);
        if (deviceProfile.getDefaultRuleChainId() != null) {
            this.defaultRuleChainId = deviceProfile.getDefaultRuleChainId().getId();
        }
        if (deviceProfile.getDefaultDashboardId() != null) {
            this.defaultDashboardId = deviceProfile.getDefaultDashboardId().getId();
        }
        this.defaultQueueName = deviceProfile.getDefaultQueueName();
        this.provisionDeviceKey = deviceProfile.getProvisionDeviceKey();
        if (deviceProfile.getFirmwareId() != null) {
            this.firmwareId = deviceProfile.getFirmwareId().getId();
        }
        if (deviceProfile.getSoftwareId() != null) {
            this.softwareId = deviceProfile.getSoftwareId().getId();
        }
        if (deviceProfile.getDefaultEdgeRuleChainId() != null) {
            this.defaultEdgeRuleChainId = deviceProfile.getDefaultEdgeRuleChainId().getId();
        }
        if (deviceProfile.getExternalId() != null) {
            this.externalId = deviceProfile.getExternalId().getId();
        }
    }

    @Override
    public DeviceProfile toData() {
        DeviceProfile deviceProfile = new DeviceProfile(new DeviceProfileId(this.getUuid()));
        deviceProfile.setCreatedTime(createdTime);
        deviceProfile.setVersion(version);
        if (tenantId != null) {
            deviceProfile.setTenantId(TenantId.fromUUID(tenantId));
        }
        deviceProfile.setName(name);
        deviceProfile.setType(type);
        deviceProfile.setImage(image);
        deviceProfile.setTransportType(transportType);
        deviceProfile.setProvisionType(provisionType);
        deviceProfile.setDescription(description);
        deviceProfile.setDefault(isDefault);
        deviceProfile.setDefaultQueueName(defaultQueueName);
        deviceProfile.setProfileData(JacksonUtil.convertValue(profileData, DeviceProfileData.class));
        if (defaultRuleChainId != null) {
            deviceProfile.setDefaultRuleChainId(new RuleChainId(defaultRuleChainId));
        }
        if (defaultDashboardId != null) {
            deviceProfile.setDefaultDashboardId(new DashboardId(defaultDashboardId));
        }
        deviceProfile.setProvisionDeviceKey(provisionDeviceKey);

        if (firmwareId != null) {
            deviceProfile.setFirmwareId(new OtaPackageId(firmwareId));
        }
        if (softwareId != null) {
            deviceProfile.setSoftwareId(new OtaPackageId(softwareId));
        }
        if (defaultEdgeRuleChainId != null) {
            deviceProfile.setDefaultEdgeRuleChainId(new RuleChainId(defaultEdgeRuleChainId));
        }
        if (externalId != null) {
            deviceProfile.setExternalId(new DeviceProfileId(externalId));
        }

        return deviceProfile;
    }

}
