// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

    @JsonIgnore
    DeviceProfileProvisionType getType();

}
