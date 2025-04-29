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

export type MassUnits = MassMetricUnits | MassImperialUnits;

export type MassMetricUnits = 'ng' | 'mcg' | 'mg' | 'g' | 'kg' | 't' | 'Da';
export type MassImperialUnits = 'oz' | 'lb' | 'st' | 'short tons' | 'gr' | 'dr' | 'qr' | 'cwt' | 'slug';

const METRIC: TbMeasureUnits<MassMetricUnits> = {
  ratio: 1 / 453.59237,
  units: {
    ng: {
      name: 'unit.nanogram',
      tags: ['mass', 'weight', 'heaviness', 'load', 'nanogram', 'nanograms', 'ng'],
      to_anchor: 1e-9,
    },
    mcg: {
      name: 'unit.microgram',
      tags: ['mass', 'weight', 'heaviness', 'load', 'μg', 'microgram'],
      to_anchor: 1e-6,
    },
    mg: {
      name: 'unit.milligram',
      tags: ['mass', 'weight', 'heaviness', 'load', 'milligram', 'miligrams', 'mg'],
      to_anchor: 1e-3,
    },
    g: {
      name: 'unit.gram',
      tags: ['mass', 'weight', 'heaviness', 'load', 'gram', 'grams', 'g'],
      to_anchor: 1,
    },
    kg: {
      name: 'unit.kilogram',
      tags: ['mass', 'weight', 'heaviness', 'load', 'kilogram', 'kilograms', 'kg'],
      to_anchor: 1000, // 1 kg = 1000 g
    },
    t: {
      name: 'unit.tonne',
      tags: ['mass', 'weight', 'heaviness', 'load', 'tonne', 'tons', 't'],
      to_anchor: 1000000,
    },
    Da: {
      name: 'unit.dalton',
      tags: ['atomic mass unit', 'AMU', 'unified atomic mass unit', 'dalton', 'Da'],
      to_anchor: 1.66053906660e-24,
    },
  },
};

const IMPERIAL: TbMeasureUnits<MassImperialUnits> = {
  ratio: 453.59237,
  units: {
    oz: {
      name: 'unit.ounce',
      tags: ['mass', 'weight', 'heaviness', 'load', 'ounce', 'ounces', 'oz'],
      to_anchor: 1 / 16,
    },
    lb: {
      name: 'unit.pound',
      tags: ['mass', 'weight', 'heaviness', 'load', 'pound', 'pounds', 'lb'],
      to_anchor: 1,
    },
    st: {
      name: 'unit.stone',
      tags: ['mass', 'weight', 'heaviness', 'load', 'stone', 'stones', 'st'],
      to_anchor: 14,
    },
    'short tons': {
      name: 'unit.short-tons',
      tags: ['mass', 'weight', 'heaviness', 'load', 'short ton', 'short tons'],
      to_anchor: 2000,
    },
    gr: {
      name: 'unit.grain',
      tags: ['mass', 'measurement', 'grain', 'gr'],
      to_anchor: 1 / 7000,
    },
    dr: {
      name: 'unit.drachm',
      tags: ['mass', 'measurement', 'drachm', 'dr'],
      to_anchor: 1 / 256,
    },
    qr: {
      name: 'unit.quarter',
      tags: ['mass', 'measurement', 'quarter', 'qr'],
      to_anchor: 28,
    },
    cwt: {
      name: 'unit.hundredweight-countt',
      tags: ['mass', 'weight', 'heaviness', 'load', 'hundredweight count', 'cwt'],
      to_anchor: 100,
    },
    slug: {
      name: 'unit.slug',
      tags: ['mass', 'measurement', 'slug'],
      to_anchor: 32.174,
    },
  },
};

const measure: TbMeasure<MassUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
