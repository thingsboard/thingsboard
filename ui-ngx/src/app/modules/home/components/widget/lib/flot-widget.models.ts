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

// tslint:disable-next-line:no-reference
/// <reference path="../../../../../../../src/typings/jquery.flot.typings.d.ts" />

import { DataKey, Datasource, DatasourceData, JsonSettingsSchema } from '@shared/models/widget.models';
import * as moment_ from 'moment';
import { DataKeyType } from "@shared/models/telemetry/telemetry.models";

export declare type ChartType = 'line' | 'pie' | 'bar' | 'state' | 'graph';

export declare type TbFlotSettings = TbFlotBaseSettings & TbFlotGraphSettings & TbFlotBarSettings & TbFlotPieSettings;

export declare type TooltipValueFormatFunction = (value: any) => string;

export declare type TbFlotTicksFormatterFunction = (t: number, a?: TbFlotPlotAxis) => string;

export interface TbFlotSeries extends DatasourceData, JQueryPlotSeriesOptions {
  dataKey: TbFlotDataKey;
  xaxisIndex?: number;
  yaxisIndex?: number;
  yaxis?: number;
}

export interface TbFlotDataKey extends DataKey {
  settings?: TbFlotKeySettings;
  tooltipValueFormatFunction?: TooltipValueFormatFunction;
}

export interface TbFlotPlotAxis extends JQueryPlotAxis, TbFlotAxisOptions {
  options: TbFlotAxisOptions;
}

export interface TbFlotAxisOptions extends JQueryPlotAxisOptions {
  tickUnits?: string;
  hidden?: boolean;
  keysInfo?: Array<{hidden: boolean}>;
  ticksFormatterFunction?: TbFlotTicksFormatterFunction;
}

export interface TbFlotPlotDataSeries extends JQueryPlotDataSeries {
  datasource?: Datasource;
  dataKey?: TbFlotDataKey;
  percent?: number;
}

export interface TbFlotPlotItem extends jquery.flot.item {
  series: TbFlotPlotDataSeries;
}

export interface TbFlotHoverInfo {
  seriesHover: Array<TbFlotSeriesHoverInfo>;
  time?: any;
}

export interface TbFlotSeriesHoverInfo {
  hoverIndex: number;
  units: string;
  decimals: number;
  label: string;
  color: string;
  index: number;
  tooltipValueFormatFunction: TooltipValueFormatFunction;
  value: any;
  time: any;
  distance: number;
}

export interface TbFlotThresholdMarking {
  lineWidth?: number;
  color?: string;
  [key: string]: any;
}

export interface TbFlotThresholdKeySettings {
  yaxis: number;
  lineWidth: number;
  color: string;
}

export interface TbFlotGridSettings {
  color: string;
  backgroundColor: string;
  tickColor: string;
  outlineWidth: number;
  verticalLines: boolean;
  horizontalLines: boolean;
  minBorderMargin: number;
  margin: number;
}

export interface TbFlotXAxisSettings {
  showLabels: boolean;
  title: string;
  color: boolean;
}

export interface TbFlotSecondXAxisSettings {
  axisPosition: TbFlotXAxisPosition;
  showLabels: boolean;
  title: string;
}

export interface TbFlotYAxisSettings {
  min: number;
  max: number;
  showLabels: boolean;
  title: string;
  color: string;
  ticksFormatter: string;
  tickDecimals: number;
  tickSize: number;
}

export interface TbFlotBaseSettings {
  stack: boolean;
  shadowSize: number;
  fontColor: string;
  fontSize: number;
  tooltipIndividual: boolean;
  tooltipCumulative: boolean;
  tooltipValueFormatter: string;
  hideZeros: boolean;
  grid: TbFlotGridSettings;
  xaxis: TbFlotXAxisSettings;
  yaxis: TbFlotYAxisSettings;
}

export interface TbFlotComparisonSettings {
  comparisonEnabled: boolean;
  timeForComparison: moment_.unitOfTime.DurationConstructor;
  xaxisSecond: TbFlotSecondXAxisSettings;
}

export interface TbFlotThresholdsSettings {
  thresholdsLineWidth: number;
}

export interface TbFlotCustomLegendSettings {
  customLegendEnabled: boolean;
  dataKeysListForLabels: Array<TbFlotLabelPatternSettings>;
}

export interface TbFlotLabelPatternSettings {
  name: string;
  type: DataKeyType;
  settings?: any;
}

export interface TbFlotGraphSettings extends TbFlotBaseSettings, TbFlotThresholdsSettings, TbFlotComparisonSettings, TbFlotCustomLegendSettings {
  smoothLines: boolean;
}

export declare type BarAlignment = 'left' | 'right' | 'center';

export interface TbFlotBarSettings extends TbFlotBaseSettings, TbFlotThresholdsSettings, TbFlotComparisonSettings, TbFlotCustomLegendSettings {
  defaultBarWidth: number;
  barAlignment: BarAlignment;
}

export interface TbFlotPieSettings {
  radius: number;
  innerRadius: number;
  tilt: number;
  animatedPie: boolean;
  stroke: {
    color: string;
    width: number;
  };
  showLabels: boolean;
  fontColor: string;
  fontSize: number;
}

export declare type TbFlotYAxisPosition = 'left' | 'right';
export declare type TbFlotXAxisPosition = 'top' | 'bottom';

export declare type TbFlotThresholdValueSource = 'predefinedValue' | 'entityAttribute';

export interface TbFlotKeyThreshold {
  thresholdValueSource: TbFlotThresholdValueSource;
  thresholdEntityAlias: string;
  thresholdAttribute: string;
  thresholdValue: number;
  lineWidth: number;
  color: string;
}

export interface TbFlotKeyComparisonSettings {
  showValuesForComparison: boolean;
  comparisonValuesLabel: string;
  color: string;
}

export interface TbFlotKeySettings {
  excludeFromStacking: boolean;
  hideDataByDefault: boolean;
  disableDataHiding: boolean;
  removeFromLegend: boolean;
  showLines: boolean;
  fillLines: boolean;
  showPoints: boolean;
  showPointShape: string;
  pointShapeFormatter: string;
  showPointsLineWidth: number;
  showPointsRadius: number;
  lineWidth: number;
  tooltipValueFormatter: string;
  showSeparateAxis: boolean;
  axisMin: number;
  axisMax: number;
  axisTitle: string;
  axisTickDecimals: number;
  axisTickSize: number;
  axisPosition: TbFlotYAxisPosition;
  axisTicksFormatter: string;
  thresholds: TbFlotKeyThreshold[];
  comparisonSettings: TbFlotKeyComparisonSettings;
}

export function flotSettingsSchema(chartType: ChartType): JsonSettingsSchema {

  const schema: JsonSettingsSchema = {
    schema: {
      type: 'object',
      title: 'Settings',
      properties: {
      }
    }
  };

  const properties: any = schema.schema.properties;
  properties.stack = {
    title: 'Stacking',
    type: 'boolean',
    default: false
  };
  if (chartType === 'graph') {
    properties.smoothLines = {
      title: 'Display smooth (curved) lines',
      type: 'boolean',
      default: false
    };
  }
  if (chartType === 'bar') {
    properties.defaultBarWidth = {
      title: 'Default bar width for non-aggregated data (milliseconds)',
      type: 'number',
      default: 600
    };
    properties.barAlignment = {
      title: 'Bar alignment',
      type: 'string',
      default: 'left'
    };
  }
  if (chartType === 'graph' || chartType === 'bar') {
    properties.thresholdsLineWidth = {
      title: 'Default line width for all thresholds',
      type: 'number'
    };
  }
  properties.shadowSize = {
    title: 'Shadow size',
    type: 'number',
    default: 4
  };
  properties.fontColor =  {
    title: 'Font color',
    type: 'string',
    default: '#545454'
  };
  properties.fontSize = {
    title: 'Font size',
    type: 'number',
    default: 10
  };
  properties.tooltipIndividual = {
    title: 'Hover individual points',
    type: 'boolean',
    default: false
  };
  properties.tooltipCumulative = {
    title: 'Show cumulative values in stacking mode',
    type: 'boolean',
    default: false
  };
  properties.tooltipValueFormatter = {
    title: 'Tooltip value format function, f(value)',
    type: 'string',
    default: ''
  };
  properties.hideZeros = {
    title: 'Hide zero/false values from tooltip',
    type: 'boolean',
    default: false
  };

  properties.grid = {
    title: 'Grid settings',
    type: 'object',
    properties: {
      color: {
        title: 'Primary color',
        type: 'string',
        default: '#545454'
      },
      backgroundColor: {
        title: 'Background color',
        type: 'string',
        default: null
      },
      tickColor: {
        title: 'Ticks color',
        type: 'string',
        default: '#DDDDDD'
      },
      outlineWidth: {
        title: 'Grid outline/border width (px)',
        type: 'number',
        default: 1
      },
      verticalLines: {
        title: 'Show vertical lines',
        type: 'boolean',
        default: true
      },
      horizontalLines: {
        title: 'Show horizontal lines',
        type: 'boolean',
        default: true
      }
    }
  };

  properties.xaxis = {
    title: 'X axis settings',
    type: 'object',
    properties: {
      showLabels: {
        title: 'Show labels',
        type: 'boolean',
        default: true
      },
      title: {
        title: 'Axis title',
        type: 'string',
        default: null
      },
      color: {
        title: 'Ticks color',
        type: 'string',
        default: null
      }
    }
  };

  properties.yaxis = {
    title: 'Y axis settings',
    type: 'object',
    properties: {
      min: {
        title: 'Minimum value on the scale',
        type: 'number',
        default: null
      },
      max: {
        title: 'Maximum value on the scale',
        type: 'number',
        default: null
      },
      showLabels: {
        title: 'Show labels',
        type: 'boolean',
        default: true
      },
      title: {
        title: 'Axis title',
        type: 'string',
        default: null
      },
      color: {
        title: 'Ticks color',
        type: 'string',
        default: null
      },
      ticksFormatter: {
        title: 'Ticks formatter function, f(value)',
        type: 'string',
        default: ''
      },
      tickDecimals: {
        title: 'The number of decimals to display',
        type: 'number',
        default: 0
      },
      tickSize: {
        title: 'Step size between ticks',
        type: 'number',
        default: null
      }
    }
  };

  schema.schema.required = [];
  schema.form = ['stack'];
  if (chartType === 'graph') {
    schema.form.push('smoothLines');
  }
  if (chartType === 'bar') {
    schema.form.push('defaultBarWidth');
    schema.form.push({
      key: 'barAlignment',
      type: 'rc-select',
      multiple: false,
      items: [
        {
          value: 'left',
          label: 'Left'
        },
        {
          value: 'right',
          label: 'Right'
        },
        {
          value: 'center',
          label: 'Center'
        }
      ]
    });
  }
  if (chartType === 'graph' || chartType === 'bar') {
    schema.form.push('thresholdsLineWidth');
  }
  schema.form.push('shadowSize');
  schema.form.push({
    key: 'fontColor',
    type: 'color'
  });
  schema.form.push('fontSize');
  schema.form.push('tooltipIndividual');
  schema.form.push('tooltipCumulative');
  schema.form.push({
    key: 'tooltipValueFormatter',
    type: 'javascript'
  });
  schema.form.push('hideZeros');
  schema.form.push({
    key: 'grid',
    items: [
      {
        key: 'grid.color',
        type: 'color'
      },
      {
        key: 'grid.backgroundColor',
        type: 'color'
      },
      {
        key: 'grid.tickColor',
        type: 'color'
      },
      'grid.outlineWidth',
      'grid.verticalLines',
      'grid.horizontalLines'
    ]
  });
  schema.form.push({
    key: 'xaxis',
    items: [
      'xaxis.showLabels',
      'xaxis.title',
      {
        key: 'xaxis.color',
        type: 'color'
      }
    ]
  });
  schema.form.push({
    key: 'yaxis',
    items: [
      'yaxis.min',
      'yaxis.max',
      'yaxis.tickDecimals',
      'yaxis.tickSize',
      'yaxis.showLabels',
      'yaxis.title',
      {
        key: 'yaxis.color',
        type: 'color'
      },
      {
        key: 'yaxis.ticksFormatter',
        type: 'javascript'
      }
    ]
  });
  if (chartType === 'graph' || chartType === 'bar') {
    schema.groupInfoes = [{
      formIndex: 0,
      GroupTitle: 'Common Settings'
    }];
    schema.form = [schema.form];
    schema.schema.properties = {...schema.schema.properties, ...chartSettingsSchemaForComparison.schema.properties, ...chartSettingsSchemaForCustomLegend.schema.properties};
    schema.schema.required = schema.schema.required.concat(chartSettingsSchemaForComparison.schema.required, chartSettingsSchemaForCustomLegend.schema.required);
    schema.form.push(chartSettingsSchemaForComparison.form, chartSettingsSchemaForCustomLegend.form);
    schema.groupInfoes.push({
      formIndex: schema.groupInfoes.length,
      GroupTitle:'Comparison Settings'
    });
    schema.groupInfoes.push({
      formIndex: schema.groupInfoes.length,
      GroupTitle:'Custom Legend Settings'
    });
  }
  return schema;
}

const chartSettingsSchemaForComparison: JsonSettingsSchema = {
  schema: {
    title: 'Comparison Settings',
    type: 'object',
    properties: {
      comparisonEnabled: {
        title: 'Enable comparison',
        type: 'boolean',
        default: false
      },
      timeForComparison: {
        title: 'Time to show historical data',
        type: 'string',
        default: 'months'
      },
      xaxisSecond: {
        title: 'Second X axis',
        type: 'object',
        properties: {
          axisPosition: {
            title: 'Axis position',
            type: 'string',
            default: 'top'
          },
          showLabels: {
            title: 'Show labels',
            type: 'boolean',
            default: true
          },
          title: {
            title: 'Axis title',
            type: 'string',
            default: null
          }
        }
      }
    },
    required: []
  },
  form: [
    'comparisonEnabled',
    {
      key: 'timeForComparison',
      type: 'rc-select',
      multiple: false,
      items: [
        {
          value: 'days',
          label: 'Day ago'
        },
        {
          value: 'weeks',
          label: 'Week ago'
        },
        {
          value: 'months',
          label: 'Month ago (default)'
        },
        {
          value: 'years',
          label: 'Year ago'
        }
      ]
    },
    {
      key: 'xaxisSecond',
      items: [
        {
          key: 'xaxisSecond.axisPosition',
          type: 'rc-select',
          multiple: false,
          items: [
            {
              value: 'top',
              label: 'Top (default)'
            },
            {
              value: 'bottom',
              label: 'Bottom'
            }
          ]
        },
        'xaxisSecond.showLabels',
        'xaxisSecond.title',
      ]
    }
  ]
};

const chartSettingsSchemaForCustomLegend: JsonSettingsSchema = {
  schema: {
    title: 'Custom Legend Settings',
    type: 'object',
    properties: {
      customLegendEnabled: {
        title: 'Enable custom legend (this will allow you to use attribute/timeseries values in key labels)',
        type: 'boolean',
        default: false
      },
      dataKeysListForLabels: {
        title: 'Datakeys list to use in labels',
        type: 'array',
        items: {
          type: 'object',
          properties: {
            name: {
              title: 'Key name',
              type: 'string'
            },
            type: {
              title: 'Key type',
              type: 'string',
              default: 'attribute'
            }
          },
          required: [
            'name'
          ]
        }
      }
    },
    required: []
  },
  form: [
    'customLegendEnabled',
    {
      key: 'dataKeysListForLabels',
      condition: 'model.customLegendEnabled === true',
      items: [
        {
          key: 'dataKeysListForLabels[].type',
          type: 'rc-select',
          multiple: false,
          items: [
            {
              value: 'attribute',
              label: 'Attribute'
            },
            {
              value: 'timeseries',
              label: 'Timeseries'
            }
          ]
        },
        'dataKeysListForLabels[].name'
      ]
    }
  ]
};

export const flotPieSettingsSchema: JsonSettingsSchema = {
    schema: {
      type: 'object',
      title: 'Settings',
      properties: {
        radius: {
          title: 'Radius',
          type: 'number',
          default: 1
        },
        innerRadius: {
          title: 'Inner radius',
          type: 'number',
          default: 0
        },
        tilt: {
          title: 'Tilt',
          type: 'number',
          default: 1
        },
        animatedPie: {
          title: 'Enable pie animation (experimental)',
          type: 'boolean',
          default: false
        },
        stroke: {
          title: 'Stroke',
          type: 'object',
          properties: {
            color: {
              title: 'Color',
              type: 'string',
              default: ''
            },
            width: {
              title: 'Width (pixels)',
              type: 'number',
              default: 0
            }
          }
        },
        showLabels: {
          title: 'Show labels',
          type: 'boolean',
          default: false
        },
        fontColor: {
          title: 'Font color',
          type: 'string',
          default: '#545454'
        },
        fontSize: {
          title: 'Font size',
          type: 'number',
          default: 10
        }
      },
      required: []
    },
    form: [
      'radius',
      'innerRadius',
      'animatedPie',
      'tilt',
      {
        key: 'stroke',
        items: [
          {
            key: 'stroke.color',
            type: 'color'
          },
          'stroke.width'
        ]
      },
      'showLabels',
      {
        key: 'fontColor',
        type: 'color'
      },
      'fontSize'
    ]
};

export const flotPieDatakeySettingsSchema: JsonSettingsSchema = {
  schema: {
    type: 'object',
    title: 'DataKeySettings',
    properties: {
      hideDataByDefault: {
        title: 'Data is hidden by default',
        type: 'boolean',
        default: false
      },
      disableDataHiding: {
        title: 'Disable data hiding',
        type: 'boolean',
        default: false
      },
      removeFromLegend: {
        title: 'Remove datakey from legend',
        type: 'boolean',
        default: false
      }
    },
    required: []
  },
  form: [
    'hideDataByDefault',
    'disableDataHiding',
    'removeFromLegend'
  ]
};

export function flotDatakeySettingsSchema(defaultShowLines: boolean, chartType: ChartType): JsonSettingsSchema {
  const schema: JsonSettingsSchema = {
    schema: {
      type: 'object',
      title: 'DataKeySettings',
      properties: {
        excludeFromStacking: {
          title: 'Exclude from stacking(available in "Stacking" mode)',
          type: 'boolean',
          default: false
        },
        hideDataByDefault: {
          title: 'Data is hidden by default',
          type: 'boolean',
          default: false
        },
        disableDataHiding: {
          title: 'Disable data hiding',
          type: 'boolean',
          default: false
        },
        removeFromLegend: {
          title: 'Remove datakey from legend',
          type: 'boolean',
          default: false
        },
        showLines: {
          title: 'Show lines',
          type: 'boolean',
          default: defaultShowLines
        },
        fillLines: {
          title: 'Fill lines',
          type: 'boolean',
          default: false
        },
        showPoints: {
          title: 'Show points',
          type: 'boolean',
          default: false
        },
        showPointShape: {
          title: 'Select point shape:',
          type: 'string',
          default: 'circle'
        },
        pointShapeFormatter: {
          title: 'Point shape format function, f(ctx, x, y, radius, shadow)',
          type: 'string',
          default: 'var size = radius * Math.sqrt(Math.PI) / 2;\n' +
            'ctx.moveTo(x - size, y - size);\n' +
            'ctx.lineTo(x + size, y + size);\n' +
            'ctx.moveTo(x - size, y + size);\n' +
            'ctx.lineTo(x + size, y - size);'
        },
        showPointsLineWidth: {
          title: 'Line width of points',
          type: 'number',
          default: 5
        },
        showPointsRadius: {
          title: 'Radius of points',
          type: 'number',
          default: 3
        },
        tooltipValueFormatter: {
          title: 'Tooltip value format function, f(value)',
          type: 'string',
          default: ''
        },
        showSeparateAxis: {
          title: 'Show separate axis',
          type: 'boolean',
          default: false
        },
        axisMin: {
          title: 'Minimum value on the axis scale',
          type: 'number',
          default: null
        },
        axisMax: {
          title: 'Maximum value on the axis scale',
          type: 'number',
          default: null
        },
        axisTitle: {
          title: 'Axis title',
          type: 'string',
          default: ''
        },
        axisTickDecimals: {
          title: 'Axis tick number of digits after floating point',
          type: 'number',
          default: null
        },
        axisTickSize: {
          title: 'Axis step size between ticks',
          type: 'number',
          default: null
        },
        axisPosition: {
          title: 'Axis position',
          type: 'string',
          default: 'left'
        },
        axisTicksFormatter: {
          title: 'Ticks formatter function, f(value)',
          type: 'string',
          default: ''
        }
      },
      required: ['showLines', 'fillLines', 'showPoints']
    },
    form: [
      'hideDataByDefault',
      'disableDataHiding',
      'removeFromLegend',
      'excludeFromStacking',
      'showLines',
      'fillLines',
      'showPoints',
      {
        key: 'showPointShape',
        type: 'rc-select',
        multiple: false,
        items: [
          {
            value: 'circle',
            label: 'Circle'
          },
          {
            value: 'cross',
            label: 'Cross'
          },
          {
            value: 'diamond',
            label: 'Diamond'
          },
          {
            value: 'square',
            label: 'Square'
          },
          {
            value: 'triangle',
            label: 'Triangle'
          },
          {
            value: 'custom',
            label: 'Custom function'
          }
        ]
      },
      {
        key: 'pointShapeFormatter',
        type: 'javascript'
      },
      'showPointsLineWidth',
      'showPointsRadius',
      {
        key: 'tooltipValueFormatter',
        type: 'javascript'
      },
      'showSeparateAxis',
      'axisMin',
      'axisMax',
      'axisTitle',
      'axisTickDecimals',
      'axisTickSize',
      {
        key: 'axisPosition',
        type: 'rc-select',
        multiple: false,
        items: [
          {
            value: 'left',
            label: 'Left'
          },
          {
            value: 'right',
            label: 'Right'
          }
        ]
      },
      {
        key: 'axisTicksFormatter',
        type: 'javascript'
      }
    ]
  };

  const properties = schema.schema.properties;
  if (chartType === 'graph' || chartType === 'bar') {
    properties.thresholds = {
      title: 'Thresholds',
      type: 'array',
      items: {
        title: 'Threshold',
        type: 'object',
        properties: {
          thresholdValueSource: {
            title: 'Threshold value source',
            type: 'string',
            default: 'predefinedValue'
          },
          thresholdEntityAlias: {
            title: 'Thresholds source entity alias',
            type: 'string'
          },
          thresholdAttribute: {
            title: 'Threshold source entity attribute',
            type: 'string'
          },
          thresholdValue: {
            title: 'Threshold value (if predefined value is selected)',
            type: 'number'
          },
          lineWidth: {
            title: 'Line width',
            type: 'number'
          },
          color: {
            title: 'Color',
            type: 'string'
          }
        }
      },
      required: []
    };
    schema.form.push({
      key: 'thresholds',
      items: [
        {
          key: 'thresholds[].thresholdValueSource',
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
        'thresholds[].thresholdValue',
        'thresholds[].thresholdEntityAlias',
        'thresholds[].thresholdAttribute',
        {
          key: 'thresholds[].color',
          type: 'color'
        },
        'thresholds[].lineWidth'
      ]
    });
    properties.comparisonSettings = {
      title: 'Comparison Settings',
      type: 'object',
      properties: {
        showValuesForComparison: {
          title: 'Show historical values for comparison',
          type: 'boolean',
          default: true
        },
        comparisonValuesLabel: {
          title: 'Historical values label',
          type: 'string',
          default: ''
        },
        color: {
          title: 'Color',
          type: 'string',
          default: ''
        }
      },
      required: ['showValuesForComparison']
    };
    schema.form.push({
      key: 'comparisonSettings',
      items: [
        'comparisonSettings.showValuesForComparison',
        'comparisonSettings.comparisonValuesLabel',
        {
          key: 'comparisonSettings.color',
          type: 'color'
        }
      ]
    });
  }

  return schema;
}
