///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import { GaugeType } from '@home/components/widget/lib/canvas-digital-gauge';
import { AnimationRule } from '@home/components/widget/lib/analogue-gauge.models';
import { FontSettings } from '@home/components/widget/lib/settings.models';

export interface AttributeSourceProperty {
  valueSource: string;
  entityAlias?: string;
  attribute?: string;
  value?: number;
}

export interface FixedLevelColors {
  from?: AttributeSourceProperty;
  to?: AttributeSourceProperty;
  color: string;
}

export interface ColorLevelSetting {
  value: number;
  color: string;
}

export type colorLevel = Array<string | ColorLevelSetting>;

export type attributesGaugeType = 'levelColors' | 'ticks';

export interface DigitalGaugeSettings {
  minValue?: number;
  maxValue?: number;
  gaugeType?: GaugeType;
  donutStartAngle?: number;
  neonGlowBrightness?: number;
  dashThickness?: number;
  roundedLineCap?: boolean;
  title?: string;
  showTitle?: boolean;
  unitTitle?: string;
  showUnitTitle?: boolean;
  showTimestamp?: boolean;
  timestampFormat?: string;
  showValue?: boolean;
  showMinMax?: boolean;
  gaugeWidthScale?: number;
  defaultColor?: string;
  gaugeColor?: string;
  useFixedLevelColor?: boolean;
  levelColors?: colorLevel;
  fixedLevelColors?: FixedLevelColors[];
  animation?: boolean;
  animationDuration?: number;
  animationRule?: AnimationRule;
  titleFont?: FontSettings;
  labelFont?: FontSettings;
  valueFont?: FontSettings;
  minMaxFont?: FontSettings;
  decimals?: number;
  units?: string;
  hideValue?: boolean;
  hideMinMax?: boolean;
  showTicks?: boolean;
  ticksValue?: AttributeSourceProperty[];
  ticks?: number[];
  colorTicks?: string;
  tickWidth?: number;
}

export const digitalGaugeSettingsSchema: JsonSettingsSchema = {
  schema: {
    type: 'object',
    title: 'Settings',
    properties: {
      minValue: {
        title: 'Minimum value',
        type: 'number',
        default: 0
      },
      maxValue: {
        title: 'Maximum value',
        type: 'number',
        default: 100
      },
      gaugeType: {
        title: 'Gauge type',
        type: 'string',
        default: 'arc'
      },
      donutStartAngle: {
        title: 'Angle to start from when in donut mode',
        type: 'number',
        default: 90
      },
      neonGlowBrightness: {
        title: 'Neon glow effect brightness, (0-100), 0 - disable effect',
        type: 'number',
        default: 0
      },
      dashThickness: {
        title: 'Thickness of the stripes, 0 - no stripes',
        type: 'number',
        default: 0
      },
      roundedLineCap: {
        title: 'Display rounded line cap',
        type: 'boolean',
        default: false
      },
      title: {
        title: 'Gauge title',
        type: 'string',
        default: null
      },
      showTitle: {
        title: 'Show gauge title',
        type: 'boolean',
        default: false
      },
      unitTitle: {
        title: 'Unit title',
        type: 'string',
        default: null
      },
      showUnitTitle: {
        title: 'Show unit title',
        type: 'boolean',
        default: false
      },
      showTimestamp: {
        title: 'Show value timestamp',
        type: 'boolean',
        default: false
      },
      timestampFormat: {
        title: 'Timestamp format',
        type: 'string',
        default: 'yyyy-MM-dd HH:mm:ss'
      },
      showValue: {
        title: 'Show value text',
        type: 'boolean',
        default: true
      },
      showMinMax: {
        title: 'Show min and max values',
        type: 'boolean',
        default: true
      },
      gaugeWidthScale: {
        title: 'Width of the gauge element',
        type: 'number',
        default: 0.75
      },
      defaultColor: {
        title: 'Default color',
        type: 'string',
        default: null
      },
      gaugeColor: {
        title: 'Background color of the gauge element',
        type: 'string',
        default: null
      },
      useFixedLevelColor: {
        title: 'Use precise value for the color indicator',
        type: 'boolean',
        default: false
      },
      levelColors: {
        title: 'Colors of indicator, from lower to upper',
        type: 'array',
        items: {
          title: 'Color',
          type: 'string'
        }
      },
      fixedLevelColors: {
        title: 'The colors for the indicator using boundary values',
        type: 'array',
        items: {
          title: 'levelColor',
          type: 'object',
          properties: {
            from: {
              title: 'From',
              type: 'object',
              properties: {
                valueSource: {
                  title: '[From] Value source',
                  type: 'string',
                  default: 'predefinedValue'
                },
                entityAlias: {
                  title: '[From] Source entity alias',
                  type: 'string'
                },
                attribute: {
                  title: '[From] Source entity attribute',
                  type: 'string'
                },
                value: {
                  title: '[From] Value (if predefined value is selected)',
                  type: 'number'
                }
              }
            },
            to: {
              title: 'To',
              type: 'object',
              properties: {
                valueSource: {
                  title: '[To] Value source',
                  type: 'string',
                  default: 'predefinedValue'
                },
                entityAlias: {
                  title: '[To] Source entity alias',
                  type: 'string'
                },
                attribute: {
                  title: '[To] Source entity attribute',
                  type: 'string'
                },
                value: {
                  title: '[To] Value (if predefined value is selected)',
                  type: 'number'
                }
              }
            },
            color: {
              title: 'Color',
              type: 'string'
            }
          }
        }
      },
      showTicks: {
        title: 'Show ticks',
        type: 'boolean',
        default: false
      },
      tickWidth: {
        title: 'Width ticks',
        type: 'number',
        default: 4
      },
      colorTicks: {
        title: 'Color ticks',
        type: 'string',
        default: '#666'
      },
      ticksValue: {
        title: 'The ticks predefined value',
        type: 'array',
        items: {
          title: 'tickValue',
          type: 'object',
          properties: {
            valueSource: {
              title: 'Value source',
              type: 'string',
              default: 'predefinedValue'
            },
            entityAlias: {
              title: 'Source entity alias',
              type: 'string'
            },
            attribute: {
              title: 'Source entity attribute',
              type: 'string'
            },
            value: {
              title: 'Value (if predefined value is selected)',
              type: 'number'
            }
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
        default: 'linear'
      },
      titleFont: {
        title: 'Gauge title font',
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
            default: 12
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
            default: null
          }
        }
      },
      labelFont: {
        title: 'Font of label showing under value',
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
            default: 8
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
            default: null
          }
        }
      },
      valueFont: {
        title: 'Font of label showing current value',
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
            default: 18
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
            default: null
          }
        }
      },
      minMaxFont: {
        title: 'Font of minimum and maximum labels',
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
            default: 10
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
            default: null
          }
        }
      }
    }
  },
  form: [
    'minValue',
    'maxValue',
    {
      key: 'gaugeType',
      type: 'rc-select',
      multiple: false,
      items: [
        {
          value: 'arc',
          label: 'Arc'
        },
        {
          value: 'donut',
          label: 'Donut'
        },
        {
          value: 'horizontalBar',
          label: 'Horizontal bar'
        },
        {
          value: 'verticalBar',
          label: 'Vertical bar'
        }
      ]
    },
    'donutStartAngle',
    'neonGlowBrightness',
    'dashThickness',
    'roundedLineCap',
    'title',
    'showTitle',
    'unitTitle',
    'showUnitTitle',
    'showTimestamp',
    'timestampFormat',
    'showValue',
    'showMinMax',
    'gaugeWidthScale',
    {
      key: 'defaultColor',
      type: 'color'
    },
    {
      key: 'gaugeColor',
      type: 'color'
    },
    'useFixedLevelColor',
    {
      key: 'levelColors',
      condition: 'model.useFixedLevelColor !== true',
      items: [
        {
          key: 'levelColors[]',
          type: 'color'
        }
      ]
    },
    {
      key: 'fixedLevelColors',
      condition: 'model.useFixedLevelColor === true',
      items: [
        {
          key: 'fixedLevelColors[].from.valueSource',
          type: 'rc-select',
          multiple: false,
          items: [
            {
              value: 'predefinedValue',
              label: 'Predefined value (Default)'
            },
            {
              value: 'entityAttribute',
              label: 'Value taken from entity attribute'
            }
          ]
        },
        'fixedLevelColors[].from.value',
        'fixedLevelColors[].from.entityAlias',
        'fixedLevelColors[].from.attribute',
        {
          key: 'fixedLevelColors[].to.valueSource',
          type: 'rc-select',
          multiple: false,
          items: [
            {
              value: 'predefinedValue',
              label: 'Predefined value (Default)'
            },
            {
              value: 'entityAttribute',
              label: 'Value taken from entity attribute'
            }
          ]
        },
        'fixedLevelColors[].to.value',
        'fixedLevelColors[].to.entityAlias',
        'fixedLevelColors[].to.attribute',
        {
          key: 'fixedLevelColors[].color',
          type: 'color'
        }
      ]
    },
    'showTicks',
    {
      key: 'tickWidth',
      condition: 'model.showTicks === true'
    },
    {
      key: 'colorTicks',
      condition: 'model.showTicks === true',
      type: 'color'
    },
    {
      key: 'ticksValue',
      condition: 'model.showTicks === true',
      items: [
        {
          key: 'ticksValue[].valueSource',
          type: 'rc-select',
          multiple: false,
          items: [
            {
              value: 'predefinedValue',
              label: 'Predefined value (Default)'
            },
            {
              value: 'entityAttribute',
              label: 'Value taken from entity attribute'
            }
          ]
        },
        'ticksValue[].value',
        'ticksValue[].entityAlias',
        'ticksValue[].attribute'
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
      key: 'titleFont',
      items: [
        'titleFont.family',
        'titleFont.size',
        {
          key: 'titleFont.style',
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
          key: 'titleFont.weight',
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
          key: 'titleFont.color',
          type: 'color'
        }
      ]
    },
    {
      key: 'labelFont',
      items: [
        'labelFont.family',
        'labelFont.size',
        {
          key: 'labelFont.style',
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
          key: 'labelFont.weight',
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
          key: 'labelFont.color',
          type: 'color'
        }
      ]
    },
    {
      key: 'valueFont',
      items: [
        'valueFont.family',
        'valueFont.size',
        {
          key: 'valueFont.style',
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
          key: 'valueFont.weight',
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
          key: 'valueFont.color',
          type: 'color'
        }
      ]
    },
    {
      key: 'minMaxFont',
      items: [
        'minMaxFont.family',
        'minMaxFont.size',
        {
          key: 'minMaxFont.style',
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
          key: 'minMaxFont.weight',
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
          key: 'minMaxFont.color',
          type: 'color'
        }
      ]
    }
  ]
};
