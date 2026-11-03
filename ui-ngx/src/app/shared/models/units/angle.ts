// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type AngleUnits = 'rad' | 'deg' | 'grad' | 'arcmin' | 'arcsec' | 'mrad' | 'rev';

const METRIC: TbMeasureUnits<AngleUnits> = {
  units: {
    rad: {
      name: 'unit.rad',
      tags: ['radians'],
      to_anchor: 180 / Math.PI,
    },
    deg: {
      name: 'unit.degree',
      tags: ['degrees'],
      to_anchor: 1,
    },
    grad: {
      name: 'unit.gradian',
      tags: ['grades'],
      to_anchor: 9 / 10,
    },
    arcmin: {
      name: 'unit.arcminute',
      tags: ['arcminutes'],
      to_anchor: 1 / 60
    },
    arcsec: {
      name: 'unit.arcsecond',
      tags: ['arcseconds'],
      to_anchor: 1 / 3600
    },
    mrad: {
      name: 'unit.milliradian',
      tags: ['military angle', 'angular mil', 'mil'],
      to_anchor: 9 / (50 * Math.PI),
    },
    rev: {
      name: 'unit.revolution',
      tags: ['full circle', 'complete turn'],
      to_anchor: 360,
    },
  }
};

const measure: TbMeasure<AngleUnits> = {
  METRIC,
};

export default measure;
