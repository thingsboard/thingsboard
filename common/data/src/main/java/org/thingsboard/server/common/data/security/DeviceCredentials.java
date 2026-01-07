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
package org.thingsboard.server.common.data.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.DeviceCredentialsId;
import org.thingsboard.server.common.data.id.DeviceId;

import java.io.Serial;

@Schema
@EqualsAndHashCode(callSuper = true)
public class DeviceCredentials extends BaseData<DeviceCredentialsId> implements DeviceCredentialsFilter, HasVersion {

    @Serial
    private static final long serialVersionUID = -7869261127032877765L;

    private DeviceId deviceId;
    private DeviceCredentialsType credentialsType;
    private String credentialsId;
    private String credentialsValue;

    @Getter @Setter
    private Long version;

    public DeviceCredentials() {
        super();
    }

    public DeviceCredentials(DeviceCredentialsId id) {
        super(id);
    }

    public DeviceCredentials(DeviceCredentials deviceCredentials) {
        super(deviceCredentials);
        this.deviceId = deviceCredentials.getDeviceId();
        this.credentialsType = deviceCredentials.getCredentialsType();
        this.credentialsId = deviceCredentials.getCredentialsId();
        this.credentialsValue = deviceCredentials.getCredentialsValue();
        this.version = deviceCredentials.getVersion();
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY, description = "The Id is automatically generated during device creation. " +
            "Use 'getDeviceCredentialsByDeviceId' to obtain the id based on device id. " +
            "Use 'updateDeviceCredentials' to update device credentials. ", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    @Override
    public DeviceCredentialsId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the device credentials creation, in milliseconds", example = "1609459200000")
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JSON object with the device Id.")
    public DeviceId getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    @Schema(description = "Type of the credentials", allowableValues = {"ACCESS_TOKEN", "X509_CERTIFICATE", "MQTT_BASIC", "LWM2M_CREDENTIALS"})
    @Override
    public DeviceCredentialsType getCredentialsType() {
        return credentialsType;
    }

    public void setCredentialsType(DeviceCredentialsType credentialsType) {
        this.credentialsType = credentialsType;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Unique Credentials Id per platform instance. " +
            "Used to lookup credentials from the database. " +
            "By default, new access token for your device. " +
            "Depends on the type of the credentials."
            , example = "Access token or other value that depends on the credentials type")
    @Override
    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @Schema(description = "Value of the credentials. " +
            "Null in case of ACCESS_TOKEN credentials type. Base64 value in case of X509_CERTIFICATE. " +
            "Complex object in case of MQTT_BASIC and LWM2M_CREDENTIALS", example = "Null in case of ACCESS_TOKEN. See model definition.")
    public String getCredentialsValue() {
        return credentialsValue;
    }

    public void setCredentialsValue(String credentialsValue) {
        this.credentialsValue = credentialsValue;
    }

    @Override
    public String toString() {
        return "DeviceCredentials [deviceId=" + deviceId + ", credentialsType=" + credentialsType + ", credentialsId="
                + credentialsId + ", credentialsValue=" + credentialsValue + ", createdTime=" + createdTime + ", id="
                + id + "]";
    }

}
