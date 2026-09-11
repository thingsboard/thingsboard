// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ElectricCurrentUnits = 'A' | 'pA' | 'nA' | 'μA' | 'mA' | 'kA' | 'MA' | 'GA';

const METRIC: TbMeasureUnits<ElectricCurrentUnits> = {
  units: {
    A: {
      name: 'unit.ampere',
      tags: ['current flow', 'flow of electricity', 'electrical flow', 'amperes', 'amperage'],
      to_anchor: 1,
    },
    pA: {
      name: 'unit.picoampere',
      tags: ['picoamperes'],
      to_anchor: 1e-12,
    },
    nA: {
      name: 'unit.nanoampere',
      tags: ['nanoamperes'],
      to_anchor: 1e-9,
    },
    μA: {
      name: 'unit.microampere',
      tags: ['microamperes'],
      to_anchor: 1e-6,
    },
    mA: {
      name: 'unit.milliampere',
      tags: ['milliamperes'],
      to_anchor: 0.001,
    },
    kA: {
      name: 'unit.kiloampere',
      tags: ['kiloamperes'],
      to_anchor: 1000,
    },
    MA: {
      name: 'unit.megaampere',
      tags: ['megaamperes'],
      to_anchor: 1e6,
    },
    GA: {
      name: 'unit.gigaampere',
      tags: ['gigaamperes'],
      to_anchor: 1e9,
    },
  }
};

const measure: TbMeasure<ElectricCurrentUnits> = {
  METRIC,
};

export default measure;
