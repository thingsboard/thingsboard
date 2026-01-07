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

export type TorqueUnits = TorqueMetricUnits | TorqueImperialUnits;

export type TorqueMetricUnits = 'Nm';
export type TorqueImperialUnits = 'lbf-ft' | 'in·lbf';

const METRIC: TbMeasureUnits<TorqueMetricUnits> = {
  ratio: 1 / 1.355818,
  units: {
    Nm: {
      name: 'unit.newton-meter',
      tags: ['rotational force', 'newton meter', 'Nm'],
      to_anchor: 1,
    },
  },
};

const IMPERIAL: TbMeasureUnits<TorqueImperialUnits> = {
  ratio: 1.355818,
  units: {
    'lbf-ft': {
      name: 'unit.foot-pounds',
      tags: ['rotational force'],
      to_anchor: 1,
    },
    'in·lbf': {
      name: 'unit.inch-pounds',
      tags: ['rotational force'],
      to_anchor: 1 / 12,
    },
  },
};

const measure: TbMeasure<TorqueUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
