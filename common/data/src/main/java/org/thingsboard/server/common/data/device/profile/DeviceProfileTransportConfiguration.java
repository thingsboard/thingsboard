// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.DeviceTransportType;

import java.io.Serializable;

@Schema(
        description = "Configuration for device profile transport",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "DEFAULT", schema = DefaultDeviceProfileTransportConfiguration.class),
                @DiscriminatorMapping(value = "MQTT", schema = MqttDeviceProfileTransportConfiguration.class),
                @DiscriminatorMapping(value = "LWM2M", schema = Lwm2mDeviceProfileTransportConfiguration.class),
                @DiscriminatorMapping(value = "COAP", schema = CoapDeviceProfileTransportConfiguration.class),
                @DiscriminatorMapping(value = "SNMP", schema = SnmpDeviceProfileTransportConfiguration.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DefaultDeviceProfileTransportConfiguration.class, name = "DEFAULT"),
        @JsonSubTypes.Type(value = MqttDeviceProfileTransportConfiguration.class, name = "MQTT"),
        @JsonSubTypes.Type(value = Lwm2mDeviceProfileTransportConfiguration.class, name = "LWM2M"),
        @JsonSubTypes.Type(value = CoapDeviceProfileTransportConfiguration.class, name = "COAP"),
        @JsonSubTypes.Type(value = SnmpDeviceProfileTransportConfiguration.class, name = "SNMP")
})
public interface DeviceProfileTransportConfiguration extends Serializable {

    @JsonIgnore
    DeviceTransportType getType();

    default void validate() {
    }

}
