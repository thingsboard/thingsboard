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

export type ElectricPolarizabilityUnits = 'C·m²/V';

const METRIC: TbMeasureUnits<ElectricPolarizabilityUnits> = {
  units: {
    'C·m²/V': {
      name: 'unit.coulomb-per-square-meter-per-volt',
      tags: ['electric field'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<ElectricPolarizabilityUnits> = {
  METRIC,
};

export default measure;
