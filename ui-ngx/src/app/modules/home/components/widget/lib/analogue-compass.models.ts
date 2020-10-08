///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { JsonSettingsSchema } from '@shared/models/widget.models';
import { FontSettings } from '@home/components/widget/lib/settings.models';
import { AnimationRule, AnimationTarget } from '@home/components/widget/lib/analogue-gauge.models';

export interface AnalogueCompassSettings {
  majorTicks: string[];
  minorTicks: number;
  showStrokeTicks: boolean;
  needleCircleSize: number;
  showBorder: boolean;
  borderOuterWidth: number;
  colorPlate: string;
  colorMajorTicks: string;
  colorMinorTicks: string;
  colorNeedle: string;
  colorNeedleCircle: string;
  colorBorder: string;
  majorTickFont: FontSettings;
  animation: boolean;
  animationDuration: number;
  animationRule: AnimationRule;
  animationTarget: AnimationTarget;
}

export const analogueCompassSettingsSchema: JsonSettingsSchema = {
  schema: {
    type: 'object',
    title: 'Settings',
    properties: {
      majorTicks: {
        title: 'Major ticks names',
        type: 'array',
        items: {
          title: 'Tick name',
          type: 'string'
        }
      },
      minorTicks: {
        title: 'Minor ticks count',
        type: 'number',
        default: 22
      },
      showStrokeTicks: {
        title: 'Show ticks stroke',
        type: 'boolean',
        default: false
      },
      needleCircleSize: {
        title: 'Needle circle size',
        type: 'number',
        default: 15
      },
      showBorder: {
        title: 'Show border',
        type: 'boolean',
        default: true
      },
      borderOuterWidth: {
        title: 'Border width',
        type: 'number',
        default: 10
      },
      colorPlate: {
        title: 'Plate color',
        type: 'string',
        default: '#222'
      },
      colorMajorTicks: {
        title: 'Major ticks color',
        type: 'string',
        default: '#f5f5f5'
      },
      colorMinorTicks: {
        title: 'Minor ticks color',
        type: 'string',
        default: '#ddd'
      },
      colorNeedle: {
        title: 'Needle color',
        type: 'string',
        default: '#f08080'
      },
      colorNeedleCircle: {
        title: 'Needle circle color',
        type: 'string',
        default: '#e8e8e8'
      },
      colorBorder: {
        title: 'Border color',
        type: 'string',
        default: '#ccc'
      },
      majorTickFont: {
        title: 'Major tick font',
        type: 'object',
        properties: {
          family: {
            title: 'Font family',
            type: 'string',
            default: 'Roboto'
          },
          size: {
            title: 'Size',
            type: 'number',
            default: 20
          },
          style: {
            title: 'Style',
            type: 'string',
            default: 'normal'
          },
          weight: {
            title: 'Weight',
            type: 'string',
            default: '500'
          },
          color: {
            title: 'color',
            type: 'string',
            default: '#ccc'
          }
        }
      },
      animation: {
        title: 'Enable animation',
        type: 'boolean',
        default: true
      },
      animationDuration: {
        title: 'Animation duration',
        type: 'number',
        default: 500
      },
      animationRule: {
        title: 'Animation rule',
        type: 'string',
        default: 'cycle'
      },
      animationTarget: {
        title: 'Animation target',
        type: 'string',
        default: 'needle'
      }
    },
    required: []
  },
  form: [
    {
      key: 'majorTicks',
      items: [
        'majorTicks[]'
      ]
    },
    'minorTicks',
    'showStrokeTicks',
    'needleCircleSize',
    'showBorder',
    'borderOuterWidth',
    {
      key: 'colorPlate',
      type: 'color'
    },
    {
      key: 'colorMajorTicks',
      type: 'color'
    },
    {
      key: 'colorMinorTicks',
      type: 'color'
    },
    {
      key: 'colorNeedle',
      type: 'color'
    },
    {
      key: 'colorNeedleCircle',
      type: 'color'
    },
    {
      key: 'colorBorder',
      type: 'color'
    },
    {
      key: 'majorTickFont',
      items: [
        'majorTickFont.family',
        'majorTickFont.size',
        {
          key: 'majorTickFont.style',
          type: 'rc-select',
          multiple: false,
          items: [
            {
              value: 'normal',
              label: 'Normal'
            },
            {
              value: 'italic',
              label: 'Italic'
            },
            {
              value: 'oblique',
              label: 'Oblique'
            }
          ]
        },
        {
          key: 'majorTickFont.weight',
          type: 'rc-select',
          multiple: false,
          items: [
            {
              value: 'normal',
              label: 'Normal'
            },
            {
              value: 'bold',
              label: 'Bold'
            },
            {
              value: 'bolder',
              label: 'Bolder'
            },
            {
              value: 'lighter',
              label: 'Lighter'
            },
            {
              value: '100',
              label: '100'
            },
            {
              value: '200',
              label: '200'
            },
            {
              value: '300',
              label: '300'
            },
            {
              value: '400',
              label: '400'
            },
            {
              value: '500',
              label: '500'
            },
            {
              value: '600',
              label: '600'
            },
            {
              value: '700',
              label: '700'
            },
            {
              value: '800',
              label: '800'
            },
            {
              value: '900',
              label: '900'
            }
          ]
        },
        {
          key: 'majorTickFont.color',
          type: 'color'
        }
      ]
    },
    'animation',
    'animationDuration',
    {
      key: 'animationRule',
      type: 'rc-select',
      multiple: false,
      items: [
        {
          value: 'linear',
          label: 'Linear'
        },
        {
          value: 'quad',
          label: 'Quad'
        },
        {
          value: 'quint',
          label: 'Quint'
        },
        {
          value: 'cycle',
          label: 'Cycle'
        },
        {
          value: 'bounce',
          label: 'Bounce'
        },
        {
          value: 'elastic',
          label: 'Elastic'
        },
        {
          value: 'dequad',
          label: 'Dequad'
        },
        {
          value: 'dequint',
          label: 'Dequint'
        },
        {
          value: 'decycle',
          label: 'Decycle'
        },
        {
          value: 'debounce',
          label: 'Debounce'
        },
        {
          value: 'delastic',
          label: 'Delastic'
        }
      ]
    },
    {
      key: 'animationTarget',
      type: 'rc-select',
      multiple: false,
      items: [
        {
          value: 'needle',
          label: 'Needle'
        },
        {
          value: 'plate',
          label: 'Plate'
        }
      ]
    }
  ]
};
