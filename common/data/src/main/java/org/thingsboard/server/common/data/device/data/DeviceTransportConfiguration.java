// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.DeviceTransportType;

import java.io.Serializable;

@Schema(
        description = "Configuration for device transport",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "DEFAULT", schema = DefaultDeviceTransportConfiguration.class),
                @DiscriminatorMapping(value = "MQTT", schema = MqttDeviceTransportConfiguration.class),
                @DiscriminatorMapping(value = "COAP", schema = CoapDeviceTransportConfiguration.class),
                @DiscriminatorMapping(value = "LWM2M", schema = Lwm2mDeviceTransportConfiguration.class),
                @DiscriminatorMapping(value = "SNMP", schema = SnmpDeviceTransportConfiguration.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DefaultDeviceTransportConfiguration.class, name = "DEFAULT"),
        @JsonSubTypes.Type(value = MqttDeviceTransportConfiguration.class, name = "MQTT"),
        @JsonSubTypes.Type(value = CoapDeviceTransportConfiguration.class, name = "COAP"),
        @JsonSubTypes.Type(value = Lwm2mDeviceTransportConfiguration.class, name = "LWM2M"),
        @JsonSubTypes.Type(value = SnmpDeviceTransportConfiguration.class, name = "SNMP")})
public interface DeviceTransportConfiguration extends Serializable {

    @JsonIgnore
    DeviceTransportType getType();

    default void validate() {
    }

}
