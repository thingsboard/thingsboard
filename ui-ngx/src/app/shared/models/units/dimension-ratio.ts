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

export type DimensionRatioUnits = 'm/m' | '%';

const METRIC: TbMeasureUnits<DimensionRatioUnits> = {
  units: {
    '%': {
      name: 'unit.percent',
      tags: ['power source', 'state of charge (SoC)', 'battery', 'battery level', 'level', 'humidity', 'moisture', 'relative humidity', 'water content', 'soil moisture', 'irrigation', 'water in soil', 'soil water content', 'VWC', 'Volumetric Water Content', 'Total Harmonic Distortion', 'THD', 'power quality', 'UV Transmittance', 'capacity'],
      to_anchor: 1,
    },
    'm/m': {
      name: 'unit.meter-per-meter',
      tags: ['ratio of length to length'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<DimensionRatioUnits> = {
  METRIC,
};

export default measure;
