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

export type EnergyUnits = EnergyMetricUnits | EnergyImperialUnits;

export type EnergyMetricUnits =
  | 'Wm'
  | 'Wh'
  | 'mWh'
  | 'kWh'
  | 'MWh'
  | 'GWh'
  | 'μJ'
  | 'mJ'
  | 'J'
  | 'kJ'
  | 'MJ'
  | 'GJ'
  | 'eV';

export type EnergyImperialUnits = 'kcal' | 'cal' | 'Cal' | 'BTU' | 'MBtu' | 'MMBtu' | 'ft·lb' | 'thm';

const METRIC: TbMeasureUnits<EnergyMetricUnits> = {
  ratio: 1 / 4.184,
  units: {
    Wm: {
      name: 'unit.watt-minute',
      to_anchor: 60,
    },
    Wh: {
      name: 'unit.watt-hour',
      tags: ['energy usage', 'power consumption', 'energy consumption', 'electricity usage'],
      to_anchor: 3600,
    },
    mWh: {
      name: 'unit.milliwatt-hour',
      to_anchor: 3.6,
    },
    kWh: {
      name: 'unit.kilowatt-hour',
      tags: ['energy usage', 'power consumption', 'energy consumption', 'electricity usage'],
      to_anchor: 3600000,
    },
    MWh: {
      name: 'unit.megawatt-hour',
      to_anchor: 3600000000,
    },
    GWh: {
      name: 'unit.gigawatt-hour',
      to_anchor: 3600000000000,
    },
    μJ: {
      name: 'unit.microjoule',
      to_anchor: 0.000001,
    },
    mJ: {
      name: 'unit.millijoule',
      to_anchor: 0.001,
    },
    J: {
      name: 'unit.joule',
      tags: ['joule', 'joules', 'energy', 'work done', 'heat', 'electricity', 'mechanical work'],
      to_anchor: 1,
    },
    kJ: {
      name: 'unit.kilojoule',
      to_anchor: 1000,
    },
    MJ: {
      name: 'unit.megajoule',
      to_anchor: 1000000,
    },
    GJ: {
      name: 'unit.gigajoule',
      to_anchor: 1000000000,
    },
    eV: {
      name: 'unit.electron-volts',
      tags: ['subatomic particles', 'radiation'],
      to_anchor: 1.602176634e-19,
    },
  },
};

const IMPERIAL: TbMeasureUnits<EnergyImperialUnits> = {
  ratio: 4.184,
  units: {
    cal: {
      name: 'unit.small-calorie',
      to_anchor: 1,
    },
    Cal: {
      name: 'unit.calorie',
      tags: ['food energy'],
      to_anchor: 1000,
    },
    kcal: {
      name: 'unit.kilocalorie',
      tags: ['small calorie'],
      to_anchor: 1000,
    },
    BTU: {
      name: 'unit.british-thermal-unit',
      tags: ['heat', 'work done'],
      to_anchor: 252.1644007218,
    },
    MBtu: {
      name: 'unit.thousand-british-thermal-unit',
      tags: ['heat', 'work done'],
      to_anchor: 252164.4007218,
    },
    MMBtu : {
      name: 'unit.million-british-thermal-unit',
      tags: ['heat', 'work done'],
      to_anchor: 252164400.7218,
    },
    'ft·lb': {
      name: 'unit.foot-pound',
      tags: ['ft⋅lbf'],
      to_anchor: 0.32404875717017,
    },
    thm: {
      name: 'unit.therm',
      tags: ['natural gas consumption', 'BTU'],
      to_anchor: 25219021.687207,
    },
  },
};

const measure: TbMeasure<EnergyUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
