///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import {
  TimeSeriesChartStateSettings,
  TimeSeriesChartStateSourceType,
  TimeSeriesChartTicksFormatter,
  TimeSeriesChartTicksGenerator
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { UtilsService } from '@core/services/utils.service';
import { FormattedData } from '@shared/models/widget.models';
import { formatValue, isDefinedAndNotNull, isNumber, isNumeric } from '@core/utils';
import { LabelFormatterCallback } from 'echarts';
import {
  TimeSeriesChartTooltipValueFormatFunction
} from '@home/components/widget/lib/chart/time-series-chart-tooltip.models';

export class TimeSeriesChartStateValueConverter {

  private readonly constantsMap = new Map<any, number>();
  private readonly rangeStates: TimeSeriesChartStateSettings[] = [];
  private readonly ticks: {value: number}[] = [];
  private readonly labelsMap = new Map<number, string>();

  public readonly ticksGenerator: TimeSeriesChartTicksGenerator;
  public readonly ticksFormatter: TimeSeriesChartTicksFormatter;
  public readonly tooltipFormatter: TimeSeriesChartTooltipValueFormatFunction;
  public readonly labelFormatter: LabelFormatterCallback;
  public readonly valueConverter: (value: any) => any;

  constructor(utils: UtilsService,
              states: TimeSeriesChartStateSettings[]) {
    const ticks: number[] = [];
    for (const state of states) {
      if (state.sourceType === TimeSeriesChartStateSourceType.constant) {
        this.constantsMap.set(state.sourceValue, state.value);
      } else {
        this.rangeStates.push(state);
      }
      if (!ticks.includes(state.value)) {
        ticks.push(state.value);
        const label = utils.customTranslation(state.label, state.label);
        this.labelsMap.set(state.value, label);
      }
    }
    this.ticks = ticks.map(val => ({value: val}));
    this.ticksGenerator = () => this.ticks;
    this.ticksFormatter = (value: any) => {
      const result = this.labelsMap.get(value);
      return result || '';
    };
    this.tooltipFormatter = (value: any, latestData: FormattedData, units?: string, decimals?: number) => {
      const result = this.labelsMap.get(value);
      if (typeof result === 'string') {
        return result;
      } else {
        return formatValue(value, decimals, units, false);
      }
    };
    this.labelFormatter = (params) => {
      const value = params.value[1];
      const result = this.labelsMap.get(value);
      if (typeof result === 'string') {
        return `{value|${result}}`;
      } else {
        return undefined;
      }
    };
    this.valueConverter = (value: any) => {
      let key = value;
      if (key === 'true') {
        key = true;
      } else if (key === 'false') {
        key = false;
      }
      const result = this.constantsMap.get(key);
      if (typeof result === 'number') {
        return result;
      } else if (this.rangeStates.length && isDefinedAndNotNull(value) && isNumeric(value)) {
        for (const state of this.rangeStates) {
          const num = Number(value);
          if (TimeSeriesChartStateValueConverter.constantRange(state) && state.sourceRangeFrom === num) {
            return state.value;
          } else if ((!isNumber(state.sourceRangeFrom) || num >= state.sourceRangeFrom) &&
            (!isNumber(state.sourceRangeTo) || num < state.sourceRangeTo)) {
            return state.value;
          }
        }
      }
      return value;
    };
  }

  static constantRange(state: TimeSeriesChartStateSettings): boolean {
    return isNumber(state.sourceRangeFrom) && isNumber(state.sourceRangeTo) && state.sourceRangeFrom === state.sourceRangeTo;
  }

}
