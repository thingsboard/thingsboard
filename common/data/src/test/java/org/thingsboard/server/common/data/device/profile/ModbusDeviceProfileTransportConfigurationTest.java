// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.transport.modbus.ModbusByteOrder;
import org.thingsboard.server.common.data.transport.modbus.ModbusDataType;
import org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode;
import org.thingsboard.server.common.data.transport.modbus.ModbusMapping;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModbusDeviceProfileTransportConfigurationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String METER_JSON = """
            {
              "type": "MODBUS",
              "timeoutMs": 3000, "retries": 1, "pollPeriodMs": 5000,
              "byteOrder": "BIG", "wordOrder": "LITTLE", "sendDataOnlyOnChange": true,
              "timeseries": [
                { "tag": "voltage",  "type": "FLOAT32", "functionCode": 4, "address": 0,  "objectsCount": 2 },
                { "tag": "energyWh", "type": "UINT64",  "functionCode": 4, "address": 72, "objectsCount": 4, "multiplier": 1000 },
                { "tag": "alarm",    "type": "BITS",    "functionCode": 3, "address": 10, "objectsCount": 1, "bit": 3 }
              ],
              "attributes": [
                { "tag": "serial", "type": "STRING", "functionCode": 3, "address": 64, "objectsCount": 8 }
              ],
              "attributeUpdates": [
                { "tag": "ctRatio", "type": "UINT16", "functionCode": 6, "address": 20, "objectsCount": 1 }
              ],
              "rpc": [
                { "tag": "resetEnergy", "type": "BITS", "functionCode": 5, "address": 1, "objectsCount": 1 }
              ]
            }
            """;

    @Test
    void deserializesThroughInterfaceAndValidates() throws Exception {
        DeviceProfileTransportConfiguration parsed = MAPPER.readValue(METER_JSON, DeviceProfileTransportConfiguration.class);

        assertThat(parsed).isInstanceOf(ModbusDeviceProfileTransportConfiguration.class);
        assertThat(parsed.getType()).isEqualTo(DeviceTransportType.MODBUS);
        ModbusDeviceProfileTransportConfiguration cfg = (ModbusDeviceProfileTransportConfiguration) parsed;
        assertThat(cfg.getWordOrder()).isEqualTo(ModbusByteOrder.LITTLE);
        assertThat(cfg.getTimeseries().get(0).getFunctionCode()).isEqualTo(ModbusFunctionCode.READ_INPUT_REGISTERS);
        assertThatCode(cfg::validate).doesNotThrowAnyException();
    }

    @Test
    void roundTripKeepsNumericFunctionCodeAndType() throws Exception {
        ModbusDeviceProfileTransportConfiguration cfg = MAPPER.readValue(METER_JSON, ModbusDeviceProfileTransportConfiguration.class);

        String json = MAPPER.writeValueAsString((DeviceProfileTransportConfiguration) cfg);

        assertThat(MAPPER.readTree(json).get("type").asText()).isEqualTo("MODBUS");
        assertThat(MAPPER.readTree(json).get("timeseries").get(0).get("functionCode").asInt()).isEqualTo(4);
        assertThat(MAPPER.readValue(json, DeviceProfileTransportConfiguration.class)).isEqualTo(cfg);
    }

    @Test
    void defaultsToBigEndian() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        assertThat(cfg.getByteOrder()).isEqualTo(ModbusByteOrder.BIG);
        assertThat(cfg.getWordOrder()).isEqualTo(ModbusByteOrder.BIG);
    }

    @Test
    void rejectsUnknownFunctionCode() {
        assertThatThrownBy(() -> MAPPER.readValue("""
                {"tag": "x", "type": "INT16", "functionCode": 7, "address": 0, "objectsCount": 1}
                """, ModbusMapping.class))
                .hasRootCauseMessage("Unsupported Modbus function code: 7");
    }

    @Test
    void rejectsMissingGeneralSettings() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setPollPeriodMs(0L);
        assertThatThrownBy(cfg::validate).hasMessage("pollPeriodMs must be positive");
    }

    @Test
    void attributesPollPeriodDefaultsToPollPeriod() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        assertThat(cfg.getAttributesPollPeriodMs()).isNull();
        assertThat(cfg.getEffectiveAttributesPollPeriodMs()).isEqualTo(5000L);
        assertThatCode(cfg::validate).doesNotThrowAnyException();
    }

    @Test
    void attributesPollPeriodOverridesPollPeriod() throws Exception {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setAttributesPollPeriodMs(3_600_000L);

        assertThat(cfg.getEffectiveAttributesPollPeriodMs()).isEqualTo(3_600_000L);
        assertThatCode(cfg::validate).doesNotThrowAnyException();
        var tree = MAPPER.readTree(MAPPER.writeValueAsString((DeviceProfileTransportConfiguration) cfg));
        assertThat(tree.get("attributesPollPeriodMs").asLong()).isEqualTo(3_600_000L);
        assertThat(tree.has("effectiveAttributesPollPeriodMs")).isFalse();
    }

    @Test
    void rejectsNonPositiveAttributesPollPeriod() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setAttributesPollPeriodMs(0L);
        assertThatThrownBy(cfg::validate).hasMessage("attributesPollPeriodMs must be positive");
    }

    @Test
    void rejectsWriteFunctionCodeInTimeseries() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setTimeseries(List.of(mapping("t", ModbusDataType.UINT16, ModbusFunctionCode.WRITE_SINGLE_REGISTER, 0, 1)));
        assertThatThrownBy(cfg::validate).hasMessage("Modbus mapping timeseries[0]: function code 6 is not allowed here");
    }

    @Test
    void rejectsReadFunctionCodeInAttributeUpdates() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setAttributeUpdates(List.of(mapping("a", ModbusDataType.UINT16, ModbusFunctionCode.READ_HOLDING_REGISTERS, 0, 1)));
        assertThatThrownBy(cfg::validate).hasMessage("Modbus mapping attributeUpdates[0]: function code 3 is not allowed here");
    }

    @Test
    void rejectsObjectsCountNotMatchingType() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setTimeseries(List.of(mapping("t", ModbusDataType.FLOAT32, ModbusFunctionCode.READ_HOLDING_REGISTERS, 0, 1)));
        assertThatThrownBy(cfg::validate).hasMessage("Modbus mapping timeseries[0]: FLOAT32 requires objectsCount 2");
    }

    @Test
    void rejectsCountAboveFunctionCodeLimit() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setAttributes(List.of(mapping("s", ModbusDataType.STRING, ModbusFunctionCode.READ_HOLDING_REGISTERS, 0, 126)));
        assertThatThrownBy(cfg::validate).hasMessage("Modbus mapping attributes[0]: objectsCount must be in range 1..125 for function code 3");
    }

    @Test
    void rejectsSingleRegisterWriteOfWideType() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setAttributeUpdates(List.of(mapping("w", ModbusDataType.INT32, ModbusFunctionCode.WRITE_SINGLE_REGISTER, 0, 2)));
        assertThatThrownBy(cfg::validate).hasMessage("Modbus mapping attributeUpdates[0]: INT32 spans 2 registers; function code 6 handles only 1, use function code 16");
    }

    @Test
    void derivesObjectsCountForNumericTypes() throws Exception {
        ModbusMapping energy = MAPPER.readValue("""
                {"tag": "energyWh", "type": "UINT64", "functionCode": 4, "address": 72}
                """, ModbusMapping.class);
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setTimeseries(List.of(energy));

        assertThatCode(cfg::validate).doesNotThrowAnyException();
        assertThat(energy.getObjectsCount()).isNull();
        assertThat(energy.getEffectiveObjectsCount()).isEqualTo(4);
        assertThat(MAPPER.readTree(MAPPER.writeValueAsString(energy)).has("effectiveObjectsCount")).isFalse();
    }

    @Test
    void derivesObjectsCountForBits() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        ModbusMapping running = mapping("running", ModbusDataType.BITS, ModbusFunctionCode.READ_HOLDING_REGISTERS, 100, null);
        running.setBit(1);
        cfg.setTimeseries(List.of(running, mapping("door", ModbusDataType.BITS, ModbusFunctionCode.READ_DISCRETE_INPUTS, 0, null)));

        assertThatCode(cfg::validate).doesNotThrowAnyException();
        assertThat(running.getEffectiveObjectsCount()).isEqualTo(1);
    }

    @Test
    void requiresObjectsCountForString() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setAttributes(List.of(mapping("serial", ModbusDataType.STRING, ModbusFunctionCode.READ_HOLDING_REGISTERS, 64, null)));
        assertThatThrownBy(cfg::validate).hasMessage("Modbus mapping attributes[0]: objectsCount is required for STRING");
    }

    @Test
    void rejectsMultipleCoilsForBits() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setTimeseries(List.of(mapping("inputs", ModbusDataType.BITS, ModbusFunctionCode.READ_COILS, 0, 8)));
        assertThatThrownBy(cfg::validate).hasMessage("Modbus mapping timeseries[0]: objectsCount must be 1 for BITS");
    }

    @Test
    void rejectsAddressRangeOverflow() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setTimeseries(List.of(mapping("t", ModbusDataType.INT32, ModbusFunctionCode.READ_HOLDING_REGISTERS, 0xFFFF, 2)));
        assertThatThrownBy(cfg::validate).hasMessage("Modbus mapping timeseries[0]: address range exceeds 65535");
    }

    @Test
    void rejectsNumericTypeOnCoils() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setTimeseries(List.of(mapping("t", ModbusDataType.INT16, ModbusFunctionCode.READ_COILS, 0, 1)));
        assertThatThrownBy(cfg::validate).hasMessage("Modbus mapping timeseries[0]: coil and discrete input function codes support only BITS");
    }

    @Test
    void rejectsBitsFromRegisterWithoutBitIndex() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setTimeseries(List.of(mapping("t", ModbusDataType.BITS, ModbusFunctionCode.READ_HOLDING_REGISTERS, 0, 1)));
        assertThatThrownBy(cfg::validate).hasMessage("Modbus mapping timeseries[0]: bit must be in range 0..15");
    }

    @Test
    void acceptsMaskWriteOfRegisterBit() throws Exception {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        ModbusMapping remote = mapping("remoteControl", ModbusDataType.BITS, ModbusFunctionCode.MASK_WRITE_REGISTER, 100, 1);
        remote.setBit(7);
        cfg.setAttributeUpdates(List.of(remote));
        cfg.setRpc(List.of(remote));

        assertThatCode(cfg::validate).doesNotThrowAnyException();
        assertThat(MAPPER.readTree(MAPPER.writeValueAsString(remote)).get("functionCode").asInt()).isEqualTo(22);
    }

    @Test
    void rejectsMaskWriteInTimeseries() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        ModbusMapping remote = mapping("remoteControl", ModbusDataType.BITS, ModbusFunctionCode.MASK_WRITE_REGISTER, 100, 1);
        remote.setBit(7);
        cfg.setTimeseries(List.of(remote));
        assertThatThrownBy(cfg::validate).hasMessage("Modbus mapping timeseries[0]: function code 22 is not allowed here");
    }

    @Test
    void rejectsRegisterBitWriteWithFullRegisterFunctionCode() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        ModbusMapping remote = mapping("remoteControl", ModbusDataType.BITS, ModbusFunctionCode.WRITE_SINGLE_REGISTER, 100, 1);
        remote.setBit(7);
        cfg.setAttributeUpdates(List.of(remote));
        assertThatThrownBy(cfg::validate).hasMessage("Modbus mapping attributeUpdates[0]: writing a single bit of a register requires function code 22 (mask write); "
                + "function codes 6 and 16 overwrite the whole register");
    }

    @Test
    void rejectsMaskWriteWithoutBits() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setAttributeUpdates(List.of(mapping("ctRatio", ModbusDataType.UINT16, ModbusFunctionCode.MASK_WRITE_REGISTER, 20, 1)));
        assertThatThrownBy(cfg::validate).hasMessage("Modbus mapping attributeUpdates[0]: function code 22 writes a single bit; use type BITS with bit");
    }

    @Test
    void rejectsMaskWriteWithoutBitIndex() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setAttributeUpdates(List.of(mapping("remoteControl", ModbusDataType.BITS, ModbusFunctionCode.MASK_WRITE_REGISTER, 100, 1)));
        assertThatThrownBy(cfg::validate).hasMessage("Modbus mapping attributeUpdates[0]: bit must be in range 0..15");
    }

    @Test
    void rejectsMultiplierOnString() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        ModbusMapping serial = mapping("s", ModbusDataType.STRING, ModbusFunctionCode.READ_HOLDING_REGISTERS, 0, 4);
        serial.setMultiplier(10.0);
        cfg.setAttributes(List.of(serial));
        assertThatThrownBy(cfg::validate).hasMessage("Modbus mapping attributes[0]: multiplier is only applicable to numeric types");
    }

    @Test
    void rejectsDuplicateTagsWithinSection() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setTimeseries(List.of(
                mapping("t", ModbusDataType.INT16, ModbusFunctionCode.READ_HOLDING_REGISTERS, 0, 1),
                mapping("t", ModbusDataType.INT16, ModbusFunctionCode.READ_HOLDING_REGISTERS, 1, 1)));
        assertThatThrownBy(cfg::validate).hasMessage("Modbus mapping timeseries[1]: duplicate tag 't' in timeseries");
    }

    @Test
    void allowsSameTagAcrossSections() {
        ModbusDeviceProfileTransportConfiguration cfg = validConfig();
        cfg.setAttributes(List.of(mapping("ctRatio", ModbusDataType.UINT16, ModbusFunctionCode.READ_HOLDING_REGISTERS, 20, 1)));
        cfg.setAttributeUpdates(List.of(mapping("ctRatio", ModbusDataType.UINT16, ModbusFunctionCode.WRITE_SINGLE_REGISTER, 20, 1)));
        assertThatCode(cfg::validate).doesNotThrowAnyException();
    }

    private static ModbusDeviceProfileTransportConfiguration validConfig() {
        ModbusDeviceProfileTransportConfiguration cfg = new ModbusDeviceProfileTransportConfiguration();
        cfg.setTimeoutMs(3000);
        cfg.setRetries(1);
        cfg.setPollPeriodMs(5000L);
        cfg.setTimeseries(List.of(mapping("voltage", ModbusDataType.FLOAT32, ModbusFunctionCode.READ_INPUT_REGISTERS, 0, 2)));
        return cfg;
    }

    private static ModbusMapping mapping(String tag, ModbusDataType type, ModbusFunctionCode functionCode, int address, Integer count) {
        ModbusMapping mapping = new ModbusMapping();
        mapping.setTag(tag);
        mapping.setType(type);
        mapping.setFunctionCode(functionCode);
        mapping.setAddress(address);
        mapping.setObjectsCount(count);
        return mapping;
    }

}
