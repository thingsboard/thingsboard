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

export type SpecificHeatCapacityUnits = 'J/(kg·K)';

const METRIC: TbMeasureUnits<SpecificHeatCapacityUnits> = {
  units: {
    'J/(kg·K)': {
      name: 'unit.joule-per-kilogram-kelvin',
      tags: ['heat capacity per unit mass and temperature'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<SpecificHeatCapacityUnits> = {
  METRIC,
};

export default measure;
