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

export type AreaDensityUnits = 'kg/m²';

const METRIC: TbMeasureUnits<AreaDensityUnits> = {
  units: {
    'kg/m²': {
      name: 'unit.kilogram-per-square-meter',
      tags: ['surface density', 'mass per unit area'],
      to_anchor: 1
    },
  },
};

const measure: TbMeasure<AreaDensityUnits> = {
  METRIC,
};

export default measure;
