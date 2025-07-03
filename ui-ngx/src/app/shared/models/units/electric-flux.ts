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

import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ElectricFluxUnits = 'V·m' | 'kV·m' | 'MV·m' | 'µV·m' | 'mV·m' | 'nV·m';

const METRIC: TbMeasureUnits<ElectricFluxUnits> = {
  units: {
    'V·m': {
      name: 'unit.volt-meter',
      to_anchor: 1,
    },
    'kV·m': {
      name: 'unit.kilovolt-meter',
      to_anchor: 1000,
    },
    'MV·m': {
      name: 'unit.megavolt-meter',
      to_anchor: 1000000,
    },
    'µV·m': {
      name: 'unit.microvolt-meter',
      to_anchor: 0.000001,
    },
    'mV·m': {
      name: 'unit.millivolt-meter',
      to_anchor: 0.001,
    },
    'nV·m': {
      name: 'unit.nanovolt-meter',
      to_anchor: 0.000000001,
    },
  },
};

const measure: TbMeasure<ElectricFluxUnits> = {
  METRIC,
};

export default measure;
