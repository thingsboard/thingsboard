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

export type ChargeUnits = ChargeMetricUnits;

export type ChargeMetricUnits = 'c' | 'mC' | 'μC' | 'nC' | 'pC' | 'mAh' | 'Ah' | 'kAh';

const METRIC: TbMeasureUnits<ChargeMetricUnits> = {
  units: {
    c: {
      name: 'unit.coulomb',
      tags: ['electricity', 'electrostatics'],
      to_anchor: 1,
    },
    mC: {
      name: 'unit.millicoulomb',
      tags: ['electricity', 'electrostatics'],
      to_anchor: 1 / 1000,
    },
    μC: {
      name: 'unit.microcoulomb',
      tags: [ 'electricity', 'electrostatics'],
      to_anchor: 1 / 1000000,
    },
    nC: {
      name: 'unit.nanocoulomb',
      tags: ['electricity', 'electrostatics',],
      to_anchor: 1e-9,
    },
    pC: {
      name: 'unit.picocoulomb',
      tags: ['electricity', 'electrostatics'],
      to_anchor: 1e-12,
    },
    mAh: {
      name: 'unit.milliampere-hour',
      tags: ['electric current', 'current flow', 'electric charge', 'current capacity', 'flow of electricity', 'electrical flow', 'milliampere-hours'],
      to_anchor: 3.6,
    },
    Ah: {
      name: 'unit.ampere-hours',
      tags: ['electric current', 'current flow', 'electric charge', 'current capacity', 'flow of electricity', 'electrical flow', 'ampere'],
      to_anchor: 3600,
    },
    kAh: {
      name: 'unit.kiloampere-hours',
      tags: ['electric current', 'current flow', 'electric charge', 'current capacity', 'flow of electricity', 'electrical flow', 'kiloampere-hours'],
      to_anchor: 3600000,
    },
  }
};

const measure: TbMeasure<ChargeUnits> = {
  METRIC,
};

export default measure;
