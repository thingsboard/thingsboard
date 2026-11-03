// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.CoapDeviceType;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "coapDeviceType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DefaultCoapDeviceTypeConfiguration.class, name = "DEFAULT"),
        @JsonSubTypes.Type(value = EfentoCoapDeviceTypeConfiguration.class, name = "EFENTO")})
public interface CoapDeviceTypeConfiguration extends Serializable {

    @JsonIgnore
    CoapDeviceType getCoapDeviceType();

}
