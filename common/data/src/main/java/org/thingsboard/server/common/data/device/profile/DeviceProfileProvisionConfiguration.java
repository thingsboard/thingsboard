// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;

import java.io.Serializable;

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

    String getProvisionDeviceSecret();

    @JsonIgnore
    DeviceProfileProvisionType getType();

}
