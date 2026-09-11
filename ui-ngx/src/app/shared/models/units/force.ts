// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ForceUnits = ForceMetricUnits | ForceImperialUnits;

export type ForceMetricUnits = 'N' | 'kN' | 'dyn';
export type ForceImperialUnits = 'lbf' | 'kgf' | 'klbf' | 'pdl' | 'kip';

const METRIC: TbMeasureUnits<ForceMetricUnits> = {
  ratio: 0.224809,
  units: {
    N: {
      name: 'unit.newton',
      tags: ['pressure', 'push', 'pull', 'weight'],
      to_anchor: 1,
    },
    kN: {
      name: 'unit.kilonewton',
      to_anchor: 1000,
    },
    dyn: {
      name: 'unit.dyne',
      to_anchor: 0.00001,
    },
  },
};

const IMPERIAL: TbMeasureUnits<ForceImperialUnits> = {
  ratio: 4.44822,
  units: {
    lbf: {
      name: 'unit.pound-force',
      to_anchor: 1,
    },
    kgf: {
      name: 'unit.kilogram-force',
      to_anchor: 2.20462,
    },
    klbf: {
      name: 'unit.kilopound-force',
      to_anchor: 1000,
    },
    pdl: {
      name: 'unit.poundal',
      to_anchor: 0.031081,
    },
    kip: {
      name: 'unit.kip',
      to_anchor: 1000,
    },
  },
};

const measure: TbMeasure<ForceUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
