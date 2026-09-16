// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type DimensionRatioUnits = 'm/m' | '%';

const METRIC: TbMeasureUnits<DimensionRatioUnits> = {
  units: {
    '%': {
      name: 'unit.percent',
      tags: ['power source', 'state of charge (SoC)', 'battery', 'battery level', 'level', 'humidity', 'moisture', 'relative humidity', 'water content', 'soil moisture', 'irrigation', 'water in soil', 'soil water content', 'VWC', 'Volumetric Water Content', 'Total Harmonic Distortion', 'THD', 'power quality', 'UV Transmittance', 'capacity'],
      to_anchor: 1,
    },
    'm/m': {
      name: 'unit.meter-per-meter',
      tags: ['ratio of length to length'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<DimensionRatioUnits> = {
  METRIC,
};

export default measure;
