// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type RadiationDoseUnits = 'Gy' | 'Sv' | 'Rad' | 'Rem' | 'R' | 'C/kg' | 'cps';

const METRIC: TbMeasureUnits<RadiationDoseUnits> = {
  units: {
    Sv: {
      name: 'unit.sievert',
      tags: ['sievert', 'radiation dose equivalent', 'Sv'],
      to_anchor: 1,
    },
    Gy: {
      name: 'unit.gray',
      tags: ['absorbed dose', 'gray', 'Gy'],
      to_anchor: 1,
    },
    Rad: {
      name: 'unit.rad',
      tags: ['rad'],
      to_anchor: 0.01,
    },
    Rem: {
      name: 'unit.rem',
      tags: ['radiation dose equivalent'],
      to_anchor: 0.01,
    },
    R: {
      name: 'unit.roentgen',
      tags: ['radiation exposure'],
      to_anchor: 0.0093,
    },
    'C/kg': {
      name: 'unit.coulombs-per-kilogram',
      tags: ['radiation exposure', 'electric charge-to-mass ratio'],
      to_anchor: 34,
    },
    cps: {
      name: 'unit.cps',
      tags: ['radiation detection'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<RadiationDoseUnits> = {
  METRIC,
};

export default measure;
