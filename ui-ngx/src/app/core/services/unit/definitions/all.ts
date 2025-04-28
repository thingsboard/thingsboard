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
import temperature, { TemperatureUnits } from './temperature';
import time, { TimeUnits } from './time';

export type AllMeasuresUnits =
  | AccelerationUnits
  | AngleUnits
  | AngularAccelerationUnits
  | AreaUnits
  | TemperatureUnits
  | TimeUnits;

export type AllMeasures =
  | 'acceleration'
  | 'angle'
  | 'angular-acceleration'
  | 'area'
  | 'temperature'
  | 'time';

const allMeasures: Record<
  AllMeasures,
  TbMeasure<AllMeasuresUnits>
> = {
  acceleration,
  angle,
  'angular-acceleration': angularAcceleration,
  area,
  temperature,
  time,
};

export default allMeasures;
