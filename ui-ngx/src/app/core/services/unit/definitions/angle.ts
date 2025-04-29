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

export type AngleUnits = AngleMetricUnits;

export type AngleMetricUnits = 'rad' | 'deg' | 'grad' | 'arcmin' | 'arcsec' | 'mil' | 'rev';

const METRIC: TbMeasureUnits<AngleMetricUnits> = {
  units: {
    rad: {
      name: 'unit.rad',
      tags: ['angle', 'radian', 'radians', 'rad'],
      to_anchor: 180 / Math.PI,
    },
    deg: {
      name: 'unit.degree',
      tags: ['angle', 'degree', 'degrees', 'deg'],
      to_anchor: 1,
    },
    grad: {
      name: 'unit.gradian',
      tags: ['angle', 'gradian', 'grades', 'grad'],
      to_anchor: 9 / 10,
    },
    arcmin: {
      name: 'unit.arcminute',
      tags: ['angle', 'arcminute', 'arcminutes', 'arcmin'],
      to_anchor: 1 / 60
    },
    arcsec: {
      name: 'unit.arcsecond',
      tags: ['angle', 'arcsecond', 'arcseconds', 'arcsec'],
      to_anchor: 1 / 3600
    },
    mil: {
      name: 'unit.mil',
      tags: ['angle', 'military angle', 'angular mil', 'mil'],
      to_anchor: 9 / (50 * Math.PI),
    },
    rev: {
      name: 'revolution',
      tags: ['angle', 'revolution', 'full circle', 'complete turn', 'rev'],
      to_anchor: 360,
    },
  }
};

const measure: TbMeasure<AngleUnits> = {
  METRIC,
};

export default measure;
