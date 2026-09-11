// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type CapacitanceUnits = 'F' | 'mF' | 'μF' | 'nF' | 'pF' | 'kF' | 'MF' | 'GF' | 'TF';

const METRIC: TbMeasureUnits<CapacitanceUnits> = {
  units: {
    F: {
      name: 'unit.farad',
      tags: ['electric capacitance'],
      to_anchor: 1,
    },
    mF: {
      name: 'unit.millifarad',
      tags: ['electric capacitance'],
      to_anchor: 1e-3,
    },
    μF: {
      name: 'unit.microfarad',
      tags: ['electric capacitance'],
      to_anchor: 1e-6,
    },
    nF: {
      name: 'unit.nanofarad',
      tags: ['electric capacitance'],
      to_anchor: 1e-9,
    },
    pF: {
      name: 'unit.picofarad',
      tags: ['electric capacitance'],
      to_anchor: 1e-12,
    },
    kF: {
      name: 'unit.kilofarad',
      tags: ['electric capacitance'],
      to_anchor: 1e3,
    },
    MF: {
      name: 'unit.megafarad',
      tags: ['electric capacitance'],
      to_anchor: 1e6,
    },
    GF: {
      name: 'unit.gigafarad',
      tags: ['electric capacitance'],
      to_anchor: 1e9,
    },
    TF: {
      name: 'unit.terfarad',
      tags: ['electric capacitance'],
      to_anchor: 1e12,
    },
  },
};

const measure: TbMeasure<CapacitanceUnits> = {
  METRIC,
};

export default measure;
