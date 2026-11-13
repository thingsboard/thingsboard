// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1

/**
 * Modbus transport models. Mirror the backend classes in common/data:
 * ModbusDeviceProfileTransportConfiguration, ModbusMapping, ModbusFunctionCode, ModbusDataType,
 * ModbusDeviceTransportConfiguration and ModbusTcpEndpoint. The server validates the same rules on save.
 */

export const MODBUS_MAX_ADDRESS = 0xFFFF;

export enum ModbusDataType {
  BITS = 'BITS',
  STRING = 'STRING',
  BYTES = 'BYTES',
  INT16 = 'INT16',
  UINT16 = 'UINT16',
  FLOAT16 = 'FLOAT16',
  INT32 = 'INT32',
  UINT32 = 'UINT32',
  FLOAT32 = 'FLOAT32',
  INT64 = 'INT64',
  UINT64 = 'UINT64',
  FLOAT64 = 'FLOAT64'
}

/** Registers a numeric type occupies; types not listed (BITS, STRING, BYTES) are not numeric. */
export const modbusDataTypeRegisters = new Map<ModbusDataType, number>([
  [ModbusDataType.INT16, 1],
  [ModbusDataType.UINT16, 1],
  [ModbusDataType.FLOAT16, 1],
  [ModbusDataType.INT32, 2],
  [ModbusDataType.UINT32, 2],
  [ModbusDataType.FLOAT32, 2],
  [ModbusDataType.INT64, 4],
  [ModbusDataType.UINT64, 4],
  [ModbusDataType.FLOAT64, 4]
]);

export const isModbusNumericType = (type: ModbusDataType): boolean => modbusDataTypeRegisters.has(type);

/** Types whose register count the user sets; for the others it follows from the type. */
export const isModbusSizedType = (type: ModbusDataType): boolean => type === ModbusDataType.STRING || type === ModbusDataType.BYTES;

export enum ModbusFunctionCode {
  READ_COILS = 1,
  READ_DISCRETE_INPUTS = 2,
  READ_HOLDING_REGISTERS = 3,
  READ_INPUT_REGISTERS = 4,
  WRITE_SINGLE_COIL = 5,
  WRITE_SINGLE_REGISTER = 6,
  WRITE_MULTIPLE_COILS = 15,
  WRITE_MULTIPLE_REGISTERS = 16,
  MASK_WRITE_REGISTER = 22
}

export interface ModbusFunctionCodeInfo {
  /** Coils and discrete inputs (1 bit each) rather than 16-bit registers. */
  bitAccess: boolean;
  write: boolean;
  /** Maximum number of coils or registers in one request. */
  maxCount: number;
  name: string;
}

export const modbusFunctionCodeInfoMap = new Map<ModbusFunctionCode, ModbusFunctionCodeInfo>([
  [ModbusFunctionCode.READ_COILS, {bitAccess: true, write: false, maxCount: 2000, name: 'device-profile.modbus.function-code-1'}],
  [ModbusFunctionCode.READ_DISCRETE_INPUTS, {bitAccess: true, write: false, maxCount: 2000, name: 'device-profile.modbus.function-code-2'}],
  [ModbusFunctionCode.READ_HOLDING_REGISTERS, {bitAccess: false, write: false, maxCount: 125, name: 'device-profile.modbus.function-code-3'}],
  [ModbusFunctionCode.READ_INPUT_REGISTERS, {bitAccess: false, write: false, maxCount: 125, name: 'device-profile.modbus.function-code-4'}],
  [ModbusFunctionCode.WRITE_SINGLE_COIL, {bitAccess: true, write: true, maxCount: 1, name: 'device-profile.modbus.function-code-5'}],
  [ModbusFunctionCode.WRITE_SINGLE_REGISTER, {bitAccess: false, write: true, maxCount: 1, name: 'device-profile.modbus.function-code-6'}],
  [ModbusFunctionCode.WRITE_MULTIPLE_COILS, {bitAccess: true, write: true, maxCount: 1968, name: 'device-profile.modbus.function-code-15'}],
  [ModbusFunctionCode.WRITE_MULTIPLE_REGISTERS, {bitAccess: false, write: true, maxCount: 123, name: 'device-profile.modbus.function-code-16'}],
  [ModbusFunctionCode.MASK_WRITE_REGISTER, {bitAccess: false, write: true, maxCount: 1, name: 'device-profile.modbus.function-code-22'}]
]);

export enum ModbusMappingSection {
  TIMESERIES = 'timeseries',
  ATTRIBUTES = 'attributes',
  ATTRIBUTE_UPDATES = 'attributeUpdates',
  RPC = 'rpc'
}

/** Function codes allowed in a list: reads are polled, writes follow shared attribute updates, RPC may do either. */
export const modbusSectionAllowsFunctionCode = (section: ModbusMappingSection, functionCode: ModbusFunctionCode): boolean => {
  const write = modbusFunctionCodeInfoMap.get(functionCode).write;
  switch (section) {
    case ModbusMappingSection.TIMESERIES:
    case ModbusMappingSection.ATTRIBUTES:
      return !write;
    case ModbusMappingSection.ATTRIBUTE_UPDATES:
      return write;
    default:
      return true;
  }
};

/**
 * Function codes that fit a data type:
 * - BITS: coil codes, register reads (one bit of the register) and FC 22; not 6/16, which overwrite the whole register;
 * - other types: register codes except FC 22, and only those that can carry all registers of a numeric type
 *   (FLOAT32 needs FC 16, not FC 6).
 */
export const modbusTypeAllowsFunctionCode = (type: ModbusDataType, functionCode: ModbusFunctionCode): boolean => {
  const info = modbusFunctionCodeInfoMap.get(functionCode);
  if (type === ModbusDataType.BITS) {
    return functionCode !== ModbusFunctionCode.WRITE_SINGLE_REGISTER && functionCode !== ModbusFunctionCode.WRITE_MULTIPLE_REGISTERS;
  }
  if (info.bitAccess || functionCode === ModbusFunctionCode.MASK_WRITE_REGISTER) {
    return false;
  }
  const registers = modbusDataTypeRegisters.get(type);
  return registers === undefined || registers <= info.maxCount;
};

/** A BITS value read from or written to a register needs the bit index. */
export const modbusMappingNeedsBit = (type: ModbusDataType, functionCode: ModbusFunctionCode): boolean =>
  type === ModbusDataType.BITS && !!functionCode && !modbusFunctionCodeInfoMap.get(functionCode).bitAccess;

export enum ModbusByteOrder {
  BIG = 'BIG',
  LITTLE = 'LITTLE'
}

export interface ModbusMapping {
  tag: string;
  type: ModbusDataType;
  functionCode: ModbusFunctionCode;
  address: number;
  objectsCount?: number;
  bit?: number;
  multiplier?: number;
}

export interface ModbusDeviceProfileTransportConfiguration {
  timeoutMs?: number;
  retries?: number;
  pollPeriodMs?: number;
  attributesPollPeriodMs?: number;
  byteOrder?: ModbusByteOrder;
  wordOrder?: ModbusByteOrder;
  sendDataOnlyOnChange?: boolean;
  timeseries?: ModbusMapping[];
  attributes?: ModbusMapping[];
  attributeUpdates?: ModbusMapping[];
  rpc?: ModbusMapping[];
}

export enum ModbusTcpFraming {
  MBAP = 'MBAP',
  RTU = 'RTU'
}

/** Valid unit ids per framing: RTU excludes 0 (broadcast) and 248-255 (reserved). */
export const modbusUnitIdRange = new Map<ModbusTcpFraming, { min: number; max: number }>([
  [ModbusTcpFraming.MBAP, {min: 0, max: 255}],
  [ModbusTcpFraming.RTU, {min: 1, max: 247}]
]);

export interface ModbusTcpEndpoint {
  type: 'TCP';
  host: string;
  port: number;
  framing: ModbusTcpFraming;
}

export interface ModbusDeviceTransportConfiguration {
  endpoint?: ModbusTcpEndpoint;
  unitId?: number;
  timeoutMs?: number;
  pollPeriodMs?: number;
}
