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

export type VolumeUnits = VolumeMetricUnits | VolumeImperialUnits;

export type VolumeMetricUnits =
  | 'mm³'
  | 'cm³'
  | 'µL'
  | 'mL'
  | 'L'
  | 'hL'
  | 'm³'
  | 'km³';

export type VolumeImperialUnits =
  | 'tsp'
  | 'tbsp'
  | 'in³'
  | 'fl-oz'
  | 'cup'
  | 'pt'
  | 'qt'
  | 'gal'
  | 'ft³'
  | 'yd³'
  | 'bbl'
  | 'gi'
  | 'hhd';

const METRIC: TbMeasureUnits<VolumeMetricUnits> = {
  ratio: 33.8140226,
  units: {
    'mm³': {
      name: 'unit.cubic-millimeter',
      tags: ['capacity', 'extent'],
      to_anchor: 1 / 1000000,
    },
    'cm³': {
      name: 'unit.cubic-centimeter',
      tags: ['capacity', 'extent'],
      to_anchor: 1 / 1000,
    },
    µL: {
      name: 'unit.microliter',
      tags: ['liquid measurement'],
      to_anchor: 0.000001,
    },
    mL: {
      name: 'unit.milliliter',
      tags: ['capacity', 'extent'],
      to_anchor: 1 / 1000,
    },
    L: {
      name: 'unit.liter',
      tags: ['capacity', 'extent'],
      to_anchor: 1,
    },
    hL: {
      name: 'unit.hectoliter',
      tags: ['capacity', 'extent'],
      to_anchor: 100,
    },
    'm³': {
      name: 'unit.cubic-meter',
      tags: ['capacity', 'extent'],
      to_anchor: 1000,
    },
    'km³': {
      name: 'unit.cubic-kilometer',
      tags: ['capacity', 'extent'],
      to_anchor: 1000000000000,
    },
  },
};

const IMPERIAL: TbMeasureUnits<VolumeImperialUnits> = {
  ratio: 1 / 33.8140226,
  units: {
    tsp: {
      name: 'unit.teaspoon',
      tags: ['cooking measurement'],
      to_anchor: 1 / 6,
    },
    tbsp: {
      name: 'unit.tablespoon',
      tags: ['cooking measurement'],
      to_anchor: 1 / 2,
    },
    'in³': {
      name: 'unit.cubic-inch',
      tags: ['capacity', 'extent'],
      to_anchor: 0.55411,
    },
    'fl-oz': {
      name: 'unit.fluid-ounce',
      tags: ['capacity', 'extent'],
      to_anchor: 1,
    },
    cup: {
      name: 'unit.cup',
      tags: ['cooking measurement'],
      to_anchor: 8,
    },
    pt: {
      name: 'unit.pint',
      tags: ['capacity', 'extent'],
      to_anchor: 16,
    },
    qt: {
      name: 'unit.quart',
      tags: ['capacity', 'extent'],
      to_anchor: 32,
    },
    gal: {
      name: 'unit.gallon',
      tags: ['capacity', 'extent'],
      to_anchor: 128,
    },
    'ft³': {
      name: 'unit.cubic-foot',
      tags: ['capacity', 'extent'],
      to_anchor: 957.506,
    },
    'yd³': {
      name: 'unit.cubic-yard',
      tags: ['capacity', 'extent'],
      to_anchor: 25852.7,
    },
    bbl: {
      name: 'unit.oil-barrels',
      tags: ['capacity', 'extent'],
      to_anchor: 5376,
    },
    gi: {
      name: 'unit.gill',
      tags: ['liquid measurement'],
      to_anchor: 4,
    },
    hhd: {
      name: 'unit.hogshead',
      tags: ['liquid measurement'],
      to_anchor: 8064,
    },
  },
};

const measure: TbMeasure<VolumeUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
