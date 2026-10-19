// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
