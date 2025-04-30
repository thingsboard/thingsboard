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
      tags: ['pressure', 'force', 'compression', 'tension', 'pascal', 'pascals', 'Pa', 'atmospheric pressure', 'air pressure', 'weather', 'altitude', 'flight'],
      to_anchor: 0.001, // 1 Pa = 0.001 kPa
    },
    kPa: {
      name: 'unit.kilopascal',
      tags: ['pressure', 'force', 'compression', 'tension', 'kilopascal', 'kilopascals', 'kPa'],
      to_anchor: 1,
    },
    MPa: {
      name: 'unit.megapascal',
      tags: ['pressure', 'force', 'compression', 'tension', 'megapascal', 'megapascals', 'MPa'],
      to_anchor: 1000,
    },
    GPa: {
      name: 'unit.gigapascal',
      tags: ['pressure', 'force', 'compression', 'tension', 'gigapascal', 'gigapascals', 'GPa'],
      to_anchor: 1000000,
    },
    hPa: {
      name: 'unit.hectopascal',
      tags: ['pressure', 'force', 'compression', 'tension', 'hectopascal', 'hectopascals', 'hPa', 'atmospheric pressure'],
      to_anchor: 0.1,
    },
    mbar: {
      name: 'unit.millibar',
      tags: ['pressure', 'force', 'compression', 'tension', 'millibar', 'millibars', 'mbar'],
      to_anchor: 0.1,
    },
    mb: {
      name: 'unit.millibar',
      tags: ['atmospheric pressure', 'air pressure', 'weather', 'altitude', 'flight', 'mb'],
      to_anchor: 0.1,
    },
    bar: {
      name: 'unit.bar',
      tags: ['pressure', 'force', 'compression', 'tension', 'bar', 'bars'],
      to_anchor: 100,
    },
    kbar: {
      name: 'unit.kilobar',
      tags: ['pressure', 'force', 'compression', 'tension', 'kilobar', 'kilobars', 'kbar'],
      to_anchor: 100000,
    },
    Torr: {
      name: 'unit.torr',
      tags: ['pressure', 'force', 'compression', 'tension', 'vacuum pressure', 'torr'],
      to_anchor: 101325 / 760000,
    },
    mmHg: {
      name: 'unit.millimeters-of-mercury',
      tags: ['pressure', 'force', 'compression', 'tension', 'millimeter of mercury', 'millimeters of mercury', 'mmHg', 'vacuum pressure'],
      to_anchor: 0.133322,
    },
    atm: {
      name: 'unit.atmospheres',
      tags: ['pressure', 'force', 'compression', 'tension', 'atmosphere', 'atmospheres', 'atmospheric pressure', 'atm'],
      to_anchor: 101.325,
    },
    'Pa/m²': {
      name: 'unit.pascal-per-square-meter',
      tags: ['pressure', 'stress', 'mechanical strength', 'pascal per square meter', 'Pa/m²'],
      to_anchor: 0.001,
    },
    'N/mm²': {
      name: 'unit.newton-per-square-millimeter',
      tags: ['pressure', 'stress', 'mechanical strength', 'newton per square millimeter', 'N/mm²'],
      to_anchor: 1000,
    },
    'N/m²': {
      name: 'unit.newton-per-square-meter',
      tags: ['pressure', 'stress', 'mechanical strength', 'newton per square meter', 'N/m²'],
      to_anchor: 0.001,
    },
    'kN/m²': {
      name: 'unit.kilonewton-per-square-meter',
      tags: ['pressure', 'stress', 'mechanical strength', 'kilonewton per square meter', 'kN/m²'],
      to_anchor: 1,
    },
    'kgf/m²': {
      name: 'unit.kilogram-force-per-square-meter',
      tags: ['pressure', 'stress', 'mechanical strength', 'kilogram-force per square meter', 'kgf/m²'],
      to_anchor: 0.00980665,
    },
    'Pa/cm²': {
      name: 'unit.pascal-per-square-centimeter',
      tags: ['pressure', 'stress', 'mechanical strength', 'pascal per square centimeter', 'Pa/cm²'],
      to_anchor: 0.1,
    },
  },
};

const IMPERIAL: TbMeasureUnits<PressureImperialUnits> = {
  ratio: 1 / 0.00014503768078,
  units: {
    psi: {
      name: 'unit.pounds-per-square-inch',
      tags: ['pressure', 'force', 'compression', 'tension', 'pounds per square inch', 'psi'],
      to_anchor: 0.001,
    },
    ksi: {
      name: 'unit.kilopound-per-square-inch',
      tags: ['pressure', 'force', 'compression', 'tension', 'kilopound per square inch', 'kilopounds per square inch', 'ksi'],
      to_anchor: 1,
    },
    inHg: {
      name: 'unit.inch-of-mercury',
      tags: ['pressure', 'force', 'compression', 'tension', 'vacuum pressure', 'inHg', 'atmospheric pressure', 'barometric pressure'],
      to_anchor: 0.000491154,
    },
    'psi/in²': {
      name: 'unit.pound-per-square-inch',
      tags: ['pressure', 'stress', 'mechanical strength', 'pound per square inch', 'psi/in²'],
      to_anchor: 0.001,
    },
    'tonf/in²': {
      name: 'unit.ton-force-per-square-inch',
      tags: ['pressure', 'stress', 'mechanical strength', 'ton-force per square inch', 'tonf/in²'],
      to_anchor: 2,
    },
  },
};

const measure: TbMeasure<PressureUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
