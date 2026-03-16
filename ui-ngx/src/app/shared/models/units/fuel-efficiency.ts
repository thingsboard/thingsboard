///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type FuelEfficiencyMetricUnits = 'km/L' | 'L/100km';
export type FuelEfficiencyImperialUnits = 'mpg' | 'gal/mi';

export type FuelEfficiencyUnits = FuelEfficiencyMetricUnits | FuelEfficiencyImperialUnits;

const METRIC: TbMeasureUnits<FuelEfficiencyMetricUnits> = {
  ratio: 2.35214583,
  units: {
    'km/L': {
      name: 'unit.kilometers-per-liter',
      to_anchor: 1,
    },
    'L/100km': {
      name: 'unit.liters-per-100-km',
      to_anchor: 1,
      transform: (value) => 100 / value,
    },
  },
};

const IMPERIAL: TbMeasureUnits<FuelEfficiencyImperialUnits> = {
  ratio: 0.425144,
  units: {
    mpg: {
      name: 'unit.miles-per-gallon',
      to_anchor: 0.425144,
    },
    'gal/mi': {
      name: 'unit.gallons-per-mile',
      to_anchor: 2.35214583,
    },
  },
};

const measure: TbMeasure<FuelEfficiencyUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
