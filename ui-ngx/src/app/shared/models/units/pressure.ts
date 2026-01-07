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

export type PressureUnits = PressureMetricUnits | PressureImperialUnits;

export type PressureMetricUnits =
  | 'Pa'
  | 'kPa'
  | 'MPa'
  | 'GPa'
  | 'hPa'
  | 'mb'
  | 'mbar'
  | 'bar'
  | 'kbar'
  | 'Torr'
  | 'mmHg'
  | 'atm'
  | 'Pa/m²'
  | 'N/mm²'
  | 'N/m²'
  | 'kN/m²'
  | 'kgf/m²'
  | 'Pa/cm²';

export type PressureImperialUnits = 'psi' | 'ksi' | 'inHg' | 'psi/in²' | 'tonf/in²';

const METRIC: TbMeasureUnits<PressureMetricUnits> = {
  ratio: 0.00014503768078,
  units: {
    Pa: {
      name: 'unit.pascal',
      tags: ['force', 'compression', 'tension', 'atmospheric pressure', 'air pressure', 'weather', 'altitude', 'flight'],
      to_anchor: 0.001,
    },
    kPa: {
      name: 'unit.kilopascal',
      tags: ['force', 'compression', 'tension'],
      to_anchor: 1,
    },
    MPa: {
      name: 'unit.megapascal',
      tags: ['force', 'compression', 'tension'],
      to_anchor: 1000,
    },
    GPa: {
      name: 'unit.gigapascal',
      tags: ['force', 'compression', 'tension'],
      to_anchor: 1000000,
    },
    hPa: {
      name: 'unit.hectopascal',
      tags: ['force', 'compression', 'tension', 'atmospheric pressure'],
      to_anchor: 0.1,
    },
    mbar: {
      name: 'unit.millibar',
      tags: ['force', 'compression', 'tension'],
      to_anchor: 0.1,
    },
    mb: {
      name: 'unit.millibar',
      tags: ['atmospheric pressure', 'air pressure', 'weather', 'altitude', 'flight'],
      to_anchor: 0.1,
    },
    bar: {
      name: 'unit.bar',
      tags: ['force', 'compression', 'tension'],
      to_anchor: 100,
    },
    kbar: {
      name: 'unit.kilobar',
      tags: ['force', 'compression', 'tension'],
      to_anchor: 100000,
    },
    Torr: {
      name: 'unit.torr',
      tags: ['force', 'compression', 'tension', 'vacuum pressure'],
      to_anchor: 101325 / 760000,
    },
    mmHg: {
      name: 'unit.millimeters-of-mercury',
      tags: ['force', 'compression', 'tension', 'vacuum pressure'],
      to_anchor: 0.133322,
    },
    atm: {
      name: 'unit.atmospheres',
      tags: ['force', 'compression', 'tension', 'atmospheric pressure'],
      to_anchor: 101.325,
    },
    'Pa/m²': {
      name: 'unit.pascal-per-square-meter',
      tags: ['stress', 'mechanical strength'],
      to_anchor: 0.001,
    },
    'N/mm²': {
      name: 'unit.newton-per-square-millimeter',
      tags: ['stress', 'mechanical strength'],
      to_anchor: 1000,
    },
    'N/m²': {
      name: 'unit.newton-per-square-meter',
      tags: ['stress', 'mechanical strength'],
      to_anchor: 0.001,
    },
    'kN/m²': {
      name: 'unit.kilonewton-per-square-meter',
      tags: ['stress', 'mechanical strength'],
      to_anchor: 1,
    },
    'kgf/m²': {
      name: 'unit.kilogram-force-per-square-meter',
      tags: ['stress', 'mechanical strength'],
      to_anchor: 0.00980665,
    },
    'Pa/cm²': {
      name: 'unit.pascal-per-square-centimeter',
      tags: ['stress', 'mechanical strength'],
      to_anchor: 0.1,
    },
  },
};

const IMPERIAL: TbMeasureUnits<PressureImperialUnits> = {
  ratio: 1 / 0.00014503768078,
  units: {
    psi: {
      name: 'unit.pounds-per-square-inch',
      tags: ['force', 'compression', 'tension'],
      to_anchor: 0.001,
    },
    ksi: {
      name: 'unit.kilopound-per-square-inch',
      tags: ['force', 'compression', 'tension'],
      to_anchor: 1,
    },
    inHg: {
      name: 'unit.inch-of-mercury',
      tags: ['force', 'compression', 'tension', 'vacuum pressure','atmospheric pressure', 'barometric pressure'],
      to_anchor: 0.000491154,
    },
    'psi/in²': {
      name: 'unit.pound-per-square-inch',
      tags: ['stress', 'mechanical strength'],
      to_anchor: 0.001,
    },
    'tonf/in²': {
      name: 'unit.ton-force-per-square-inch',
      tags: ['stress', 'mechanical strength'],
      to_anchor: 2,
    },
  },
};

const measure: TbMeasure<PressureUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
