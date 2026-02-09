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

export type AreaMetricUnits = 'mm²' | 'cm²' | 'm²' | 'a' | 'ha' | 'km²' | 'barn';
export type AreaImperialUnits = 'in²' | 'yd²' | 'ft²' | 'ac' | 'ml²' | 'cin';

export type AreaUnits = AreaMetricUnits | AreaImperialUnits;

const METRIC: TbMeasureUnits<AreaMetricUnits> = {
  ratio: 10.7639,
  units: {
    'mm²': {
      name: 'unit.square-millimeter',
      tags: ['lot', 'zone', 'space', 'region', 'square millimeters', 'sq-mm'],
      to_anchor: 1 / 1000000,
    },
    'cm²': {
      name: 'unit.square-centimeter',
      tags: ['lot', 'zone', 'space', 'region', 'square centimeters', 'sq-cm'],
      to_anchor: 1 / 10000,
    },
    'm²': {
      name: 'unit.square-meter',
      tags: ['lot', 'zone', 'space', 'region', 'square meters', 'sq-m'],
      to_anchor: 1,
    },
    a: {
      name: 'unit.are',
      tags: ['land measurement'],
      to_anchor: 100,
    },
    ha: {
      name: 'unit.hectare',
      tags: ['lot', 'zone', 'space', 'region', 'hectares'],
      to_anchor: 10000,
    },
    'km²': {
      name: 'unit.square-kilometer',
      tags: ['lot', 'zone', 'space', 'region', 'square kilometers', 'sq-km'],
      to_anchor: 1000000,
    },
    barn: {
      name: 'unit.barn',
      tags: ['cross-sectional area', 'particle physics', 'nuclear physics'],
      to_anchor: 1e-28,
    },
  }
};

const IMPERIAL: TbMeasureUnits<AreaImperialUnits> = {
  ratio: 1 / 10.7639,
  units: {
    'in²': {
      name: 'unit.square-inch',
      tags: ['lot', 'zone', 'space', 'region', 'square inches', 'sq-in'],
      to_anchor: 1 / 144,
    },
    'yd²': {
      name: 'unit.square-yard',
      tags: ['lot', 'zone', 'space', 'region', 'square yards', 'sq-yd'],
      to_anchor: 9,
    },
    'ft²': {
      name: 'unit.square-foot',
      tags: ['lot', 'zone', 'space', 'region', 'square feet', 'sq-ft'],
      to_anchor: 1,
    },
    ac: {
      name: 'unit.acre',
      tags: ['lot', 'zone', 'space', 'region', 'acres', 'a'],
      to_anchor: 43560,
    },
    'ml²': {
      name: 'unit.square-mile',
      tags: ['lot', 'zone', 'space', 'region', 'square mile', 'sq-mi'],
      to_anchor: 27878400,
    },
    cin: {
      name: 'unit.circular-inch',
      tags: ['circular measurement', 'circin'],
      to_anchor: Math.PI / 576
    }
  }
}

const measure: TbMeasure<AreaUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
