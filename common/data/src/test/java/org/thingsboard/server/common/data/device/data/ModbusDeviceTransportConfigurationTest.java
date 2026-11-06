// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.device.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusTcpEndpoint;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusTcpFraming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModbusDeviceTransportConfigurationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void deserializesThroughInterface() throws Exception {
        DeviceTransportConfiguration parsed = MAPPER.readValue("""
                {
                  "type": "MODBUS",
                  "endpoint": { "type": "TCP", "host": "10.0.0.30", "port": 4001, "framing": "RTU" },
                  "unitId": 3,
                  "timeoutMs": 5000
                }
                """, DeviceTransportConfiguration.class);

        assertThat(parsed).isInstanceOf(ModbusDeviceTransportConfiguration.class);
        assertThat(parsed.getType()).isEqualTo(DeviceTransportType.MODBUS);
        ModbusDeviceTransportConfiguration cfg = (ModbusDeviceTransportConfiguration) parsed;
        assertThat(cfg.getEndpoint()).isInstanceOf(ModbusTcpEndpoint.class);
        assertThat(((ModbusTcpEndpoint) cfg.getEndpoint()).getFraming()).isEqualTo(ModbusTcpFraming.RTU);
        assertThat(cfg.getEndpoint().getKey()).isEqualTo("tcp://10.0.0.30:4001");
        assertThat(cfg.getPollPeriodMs()).isNull();
        assertThatCode(cfg::validate).doesNotThrowAnyException();
    }

    @Test
    void roundTripWritesTypesAndOmitsDerivedFields() throws Exception {
        ModbusDeviceTransportConfiguration cfg = new ModbusDeviceTransportConfiguration();

        String json = MAPPER.writeValueAsString((DeviceTransportConfiguration) cfg);

        var tree = MAPPER.readTree(json);
        assertThat(tree.get("type").asText()).isEqualTo("MODBUS");
        assertThat(tree.get("endpoint").get("type").asText()).isEqualTo("TCP");
        assertThat(tree.get("endpoint").has("key")).isFalse();
        assertThat(tree.get("endpoint").has("minUnitId")).isFalse();
        assertThat(MAPPER.readValue(json, DeviceTransportConfiguration.class)).isEqualTo(cfg);
    }

    @Test
    void defaultsAreValid() {
        ModbusDeviceTransportConfiguration cfg = new ModbusDeviceTransportConfiguration();
        assertThat(cfg.getEndpoint().getKey()).isEqualTo("tcp://localhost:502");
        assertThat(cfg.getUnitId()).isEqualTo(1);
        assertThatCode(cfg::validate).doesNotThrowAnyException();
    }

    @Test
    void keyIgnoresHostCaseAndFraming() {
        ModbusTcpEndpoint a = tcp(" Bridge.Local ", 502, ModbusTcpFraming.MBAP);
        ModbusTcpEndpoint b = tcp("bridge.local", 502, ModbusTcpFraming.RTU);
        assertThat(a.getKey()).isEqualTo(b.getKey()).isEqualTo("tcp://bridge.local:502");
    }

    @Test
    void keyBracketsIpv6() {
        assertThat(tcp("fe80::1", 502, ModbusTcpFraming.MBAP).getKey()).isEqualTo("tcp://[fe80::1]:502");
    }

    @Test
    void mbapAllowsUnitIdZeroAnd255() {
        ModbusDeviceTransportConfiguration cfg = config(tcp("h", 502, ModbusTcpFraming.MBAP), 0);
        assertThatCode(cfg::validate).doesNotThrowAnyException();
        cfg.setUnitId(255);
        assertThatCode(cfg::validate).doesNotThrowAnyException();
    }

    @Test
    void rtuRejectsBroadcastAndReservedUnitIds() {
        ModbusDeviceTransportConfiguration cfg = config(tcp("h", 502, ModbusTcpFraming.RTU), 0);
        assertThatThrownBy(cfg::validate).hasMessage("Modbus unit id must be in range 1..247 for this endpoint");
        cfg.setUnitId(248);
        assertThatThrownBy(cfg::validate).hasMessage("Modbus unit id must be in range 1..247 for this endpoint");
    }

    @Test
    void rejectsInvalidEndpoint() {
        assertThatThrownBy(config(tcp(" ", 502, ModbusTcpFraming.MBAP), 1)::validate).hasMessage("Modbus endpoint host is required");
        assertThatThrownBy(config(tcp("h", 0, ModbusTcpFraming.MBAP), 1)::validate).hasMessage("Modbus endpoint port must be in range 1..65535");
        assertThatThrownBy(config(tcp("h", 502, null), 1)::validate).hasMessage("Modbus endpoint framing is required");
        assertThatThrownBy(config(null, 1)::validate).hasMessage("Modbus endpoint is required");
    }

    @Test
    void rejectsNonPositiveOverrides() {
        ModbusDeviceTransportConfiguration cfg = new ModbusDeviceTransportConfiguration();
        cfg.setTimeoutMs(0);
        assertThatThrownBy(cfg::validate).hasMessage("Modbus timeoutMs override must be positive");
        cfg.setTimeoutMs(null);
        cfg.setPollPeriodMs(-1L);
        assertThatThrownBy(cfg::validate).hasMessage("Modbus pollPeriodMs override must be positive");
    }

    private static ModbusDeviceTransportConfiguration config(ModbusTcpEndpoint endpoint, int unitId) {
        ModbusDeviceTransportConfiguration cfg = new ModbusDeviceTransportConfiguration();
        cfg.setEndpoint(endpoint);
        cfg.setUnitId(unitId);
        return cfg;
    }

    private static ModbusTcpEndpoint tcp(String host, int port, ModbusTcpFraming framing) {
        ModbusTcpEndpoint endpoint = new ModbusTcpEndpoint();
        endpoint.setHost(host);
        endpoint.setPort(port);
        endpoint.setFraming(framing);
        return endpoint;
    }

}
