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
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;

import java.io.Serializable;

@Schema(
        description = "Device profile provision configuration",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "DISABLED", schema = DisabledDeviceProfileProvisionConfiguration.class),
                @DiscriminatorMapping(value = "ALLOW_CREATE_NEW_DEVICES", schema = AllowCreateNewDevicesDeviceProfileProvisionConfiguration.class),
                @DiscriminatorMapping(value = "CHECK_PRE_PROVISIONED_DEVICES", schema = CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration.class),
                @DiscriminatorMapping(value = "X509_CERTIFICATE_CHAIN", schema = X509CertificateChainProvisionConfiguration.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DisabledDeviceProfileProvisionConfiguration.class, name = "DISABLED"),
        @JsonSubTypes.Type(value = AllowCreateNewDevicesDeviceProfileProvisionConfiguration.class, name = "ALLOW_CREATE_NEW_DEVICES"),
        @JsonSubTypes.Type(value = CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration.class, name = "CHECK_PRE_PROVISIONED_DEVICES"),
        @JsonSubTypes.Type(value = X509CertificateChainProvisionConfiguration.class, name = "X509_CERTIFICATE_CHAIN")})
public interface DeviceProfileProvisionConfiguration extends Serializable {

    @Schema(description = "Provision device secret", example = "secret123")
    String getProvisionDeviceSecret();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Device profile provision type")
    @JsonIgnore
    DeviceProfileProvisionType getType();

}
