// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.transport.modbus.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * Where a Modbus device is reached. Devices with the same {@link #getKey()} share one connection (or, later, one serial bus).
 * Only TCP exists today; a SERIAL endpoint can be added as another subtype without changing the device configuration.
 */
@Schema(
        description = "Modbus endpoint",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "TCP", schema = ModbusTcpEndpoint.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ModbusTcpEndpoint.class, name = "TCP")
})
public interface ModbusEndpoint extends Serializable {

    @JsonIgnore
    ModbusEndpointType getType();

    /** Connection identity used by the transport to share one client between devices, e.g. {@code tcp://10.0.0.30:502}. */
    @JsonIgnore
    String getKey();

    @JsonIgnore
    int getMinUnitId();

    @JsonIgnore
    int getMaxUnitId();

    void validate();

}
