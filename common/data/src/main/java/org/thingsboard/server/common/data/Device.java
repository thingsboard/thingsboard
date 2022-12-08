/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

@ApiModel
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class Device extends SearchTextBasedWithAdditionalInfo<DeviceId> implements HasName, HasLabel, HasTenantId, HasCustomerId, HasOtaPackage, ExportableEntity<DeviceId> {

    private static final long serialVersionUID = 2807343040519543363L;

    private TenantId tenantId;
    private CustomerId customerId;
    @NoXss
    @Length(fieldName = "name")
    private String name;
    @NoXss
    @Length(fieldName = "type")
    private String type;
    @NoXss
    @Length(fieldName = "label")
    private String label;
    private DeviceProfileId deviceProfileId;
    private transient DeviceData deviceData;
    @JsonIgnore
    private byte[] deviceDataBytes;

    private OtaPackageId firmwareId;
    private OtaPackageId softwareId;

    @Getter @Setter
    private DeviceId externalId;

    public Device() {
        super();
    }

    public Device(DeviceId id) {
        super(id);
    }

    public Device(Device device) {
        super(device);
        this.tenantId = device.getTenantId();
        this.customerId = device.getCustomerId();
        this.name = device.getName();
        this.type = device.getType();
        this.label = device.getLabel();
        this.deviceProfileId = device.getDeviceProfileId();
        this.setDeviceData(device.getDeviceData());
        this.firmwareId = device.getFirmwareId();
        this.softwareId = device.getSoftwareId();
        this.externalId = device.getExternalId();
    }

    public Device updateDevice(Device device) {
        this.tenantId = device.getTenantId();
        this.customerId = device.getCustomerId();
        this.name = device.getName();
        this.type = device.getType();
        this.label = device.getLabel();
        this.deviceProfileId = device.getDeviceProfileId();
        this.setDeviceData(device.getDeviceData());
        this.setFirmwareId(device.getFirmwareId());
        this.setSoftwareId(device.getSoftwareId());
        Optional.ofNullable(device.getAdditionalInfo()).ifPresent(this::setAdditionalInfo);
        this.setExternalId(device.getExternalId());
        return this;
    }

    @ApiModelProperty(position = 1, value = "JSON object with the Device Id. " +
            "Specify this field to update the Device. " +
            "Referencing non-existing Device Id will cause error. " +
            "Omit this field to create new Device." )
    @Override
    public DeviceId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the device creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id. Use 'assignDeviceToTenant' to change the Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    @ApiModelProperty(position = 4, value = "JSON object with Customer Id. Use 'assignDeviceToCustomer' to change the Customer Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public CustomerId getCustomerId() {
        return customerId;
    }

    public void setCustomerId(CustomerId customerId) {
        this.customerId = customerId;
    }

    @ApiModelProperty(position = 5, required = true, value = "Unique Device Name in scope of Tenant", example = "A4B72CCDFF33")
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(position = 6, required = true, value = "Device Profile Name", example = "Temperature Sensor")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ApiModelProperty(position = 7, required = true, value = "Label that may be used in widgets", example = "Room 234 Sensor")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @ApiModelProperty(position = 8, required = true, value = "JSON object with Device Profile Id.")
    public DeviceProfileId getDeviceProfileId() {
        return deviceProfileId;
    }

    public void setDeviceProfileId(DeviceProfileId deviceProfileId) {
        this.deviceProfileId = deviceProfileId;
    }

    @ApiModelProperty(position = 9, value = "JSON object with content specific to type of transport in the device profile.")
    public DeviceData getDeviceData() {
        if (deviceData != null) {
            return deviceData;
        } else {
            if (deviceDataBytes != null) {
                try {
                    deviceData = mapper.readValue(new ByteArrayInputStream(deviceDataBytes), DeviceData.class);
                } catch (IOException e) {
                    log.warn("Can't deserialize device data: ", e);
                    return null;
                }
                return deviceData;
            } else {
                return null;
            }
        }
    }

    public void setDeviceData(DeviceData data) {
        this.deviceData = data;
        try {
            this.deviceDataBytes = data != null ? mapper.writeValueAsBytes(data) : null;
        } catch (JsonProcessingException e) {
            log.warn("Can't serialize device data: ", e);
        }
    }

    @Override
    public String getSearchText() {
        return getName();
    }

    @ApiModelProperty(position = 10, value = "JSON object with Ota Package Id.")
    public OtaPackageId getFirmwareId() {
        return firmwareId;
    }

    public void setFirmwareId(OtaPackageId firmwareId) {
        this.firmwareId = firmwareId;
    }

    @ApiModelProperty(position = 11, value = "JSON object with Ota Package Id.")
    public OtaPackageId getSoftwareId() {
        return softwareId;
    }

    public void setSoftwareId(OtaPackageId softwareId) {
        this.softwareId = softwareId;
    }

    @ApiModelProperty(position = 12, value = "Additional parameters of the device", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Device [tenantId=");
        builder.append(tenantId);
        builder.append(", customerId=");
        builder.append(customerId);
        builder.append(", name=");
        builder.append(name);
        builder.append(", type=");
        builder.append(type);
        builder.append(", label=");
        builder.append(label);
        builder.append(", deviceProfileId=");
        builder.append(deviceProfileId);
        builder.append(", deviceData=");
        builder.append(firmwareId);
        builder.append(", firmwareId=");
        builder.append(deviceData);
        builder.append(", additionalInfo=");
        builder.append(getAdditionalInfo());
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append(", id=");
        builder.append(id);
        builder.append("]");
        return builder.toString();
    }

}
