///
/// Copyright © 2016-2025 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { TbMeasure } from '@shared/models/unit.models';
import acceleration, { AccelerationUnits } from '@core/services/unit/definitions/acceleration';
import angle, { AngleUnits } from '@core/services/unit/definitions/angle';
import angularAcceleration, { AngularAccelerationUnits } from '@core/services/unit/definitions/angular-acceleration';
import area, { AreaUnits } from '@core/services/unit/definitions/area';
import charge, { ChargeUnits } from '@core/services/unit/definitions/charge';
import digital, { DigitalUnits } from '@core/services/unit/definitions/digital';
import electricCurrent, { ElectricCurrentUnits } from '@core/services/unit/definitions/electric-current';
import energy, { EnergyUnits } from '@core/services/unit/definitions/energy';
import force, { ForceUnits } from '@core/services/unit/definitions/force';
import frequency, { FrequencyUnits } from '@core/services/unit/definitions/frequency';
import illuminance,{ IlluminanceUnits } from '@core/services/unit/definitions/illuminance';
import length, { LengthUnits } from '@core/services/unit/definitions/length';
import mass, { MassUnits } from '@core/services/unit/definitions/mass';
import partsPer, { PartsPerUnits } from '@core/services/unit/definitions/parts-per';
import power, { PowerUnits } from '@core/services/unit/definitions/power';
import pressure, { PressureUnits } from '@core/services/unit/definitions/pressure';
import speed, { SpeedUnits } from '@core/services/unit/definitions/speed';
import temperature, { TemperatureUnits } from './temperature';
import time, { TimeUnits } from './time';
import torque, { TorqueUnits } from '@core/services/unit/definitions/torque';
import voltage, { VoltageUnits } from '@core/services/unit/definitions/voltage';
import volume, { VolumeUnits } from '@core/services/unit/definitions/volume';
import volumeFlowRate, { VolumeFlowRateUnits } from '@core/services/unit/definitions/volume-flow-rate';

export type AllMeasuresUnits =
  | AccelerationUnits
  | AngleUnits
  | AngularAccelerationUnits
  | AreaUnits
  | ChargeUnits
  | DigitalUnits
  | ElectricCurrentUnits
  | EnergyUnits
  | ForceUnits
  | FrequencyUnits
  | IlluminanceUnits
  | LengthUnits
  | MassUnits
  | PartsPerUnits
  | PowerUnits
  | PressureUnits
  | SpeedUnits
  | TemperatureUnits
  | TimeUnits
  | TorqueUnits
  | VoltageUnits
  | VolumeUnits
  | VolumeFlowRateUnits;

export type AllMeasures =
  | 'acceleration'
  | 'angle'
  | 'angular-acceleration'
  | 'area'
  | 'charge'
  | 'digital'
  | 'electric-current'
  | 'energy'
  | 'force'
  | 'frequency'
  | 'illuminance'
  | 'length'
  | 'mass'
  | 'parts-per'
  | 'power'
  | 'pressure'
  | 'speed'
  | 'temperature'
  | 'time'
  | 'torque'
  | 'voltage'
  | 'volume'
  | 'volume-flow-rate';

const allMeasures: Record<
  AllMeasures,
  TbMeasure<AllMeasuresUnits>
> = {
  acceleration,
  angle,
  'angular-acceleration': angularAcceleration,
  area,
  charge,
  digital,
  'electric-current': electricCurrent,
  energy,
  force,
  frequency,
  illuminance,
  length,
  mass,
  'parts-per': partsPer,
  power,
  pressure,
  speed,
  temperature,
  time,
  torque,
  voltage,
  volume,
  'volume-flow-rate': volumeFlowRate,
};

export default allMeasures;
