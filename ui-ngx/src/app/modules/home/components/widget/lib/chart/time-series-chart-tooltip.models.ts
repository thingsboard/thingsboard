///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { isFunction } from '@core/utils';
import { FormattedData } from '@shared/models/widget.models';
import { DateFormatProcessor, DateFormatSettings, Font } from '@shared/models/widget-settings.models';
import {
  TimeSeriesChartDataItem,
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { Renderer2 } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { CallbackDataParams } from 'echarts/types/dist/shared';
import { Interval } from '@shared/models/time/time.models';
import {
  getCircleElement,
  getLabelElement,
  getLabelTextElement,
  getLabelValueElement,
  getTooltipDateElement,
  getTooltipElement,
  getValueElement
} from '@home/components/widget/lib/chart/echarts-widget.models';

export type TimeSeriesChartTooltipValueFormatFunction =
  (value: any, latestData: FormattedData, units?: string, decimals?: number) => string;

export interface TimeSeriesChartTooltipWidgetSettings {
  showTooltip: boolean;
  tooltipTrigger?: TimeSeriesChartTooltipTrigger;
  tooltipShowFocusedSeries?: boolean;
  tooltipLabelFont: Font;
  tooltipLabelColor: string;
  tooltipValueFont: Font;
  tooltipValueColor: string;
  tooltipValueFormatter?: string | TimeSeriesChartTooltipValueFormatFunction;
  tooltipShowDate: boolean;
  tooltipDateInterval?: boolean;
  tooltipDateFormat: DateFormatSettings;
  tooltipDateFont: Font;
  tooltipDateColor: string;
  tooltipBackgroundColor: string;
  tooltipBackgroundBlur: number;
}

export enum TimeSeriesChartTooltipTrigger {
  point = 'point',
  axis = 'axis'
}

export const tooltipTriggerTranslationMap = new Map<TimeSeriesChartTooltipTrigger, string>(
  [
    [TimeSeriesChartTooltipTrigger.point, 'tooltip.trigger-point'],
    [TimeSeriesChartTooltipTrigger.axis, 'tooltip.trigger-axis']
  ]
);

interface TooltipItem {
  param: CallbackDataParams;
  dataItem: TimeSeriesChartDataItem;
}

interface TooltipParams {
  items: TooltipItem[];
  comparisonItems: TooltipItem[];
}

export const createTooltipValueFormatFunction =
  (tooltipValueFormatter: string | TimeSeriesChartTooltipValueFormatFunction): TimeSeriesChartTooltipValueFormatFunction => {
    let tooltipValueFormatFunction: TimeSeriesChartTooltipValueFormatFunction;
    if (isFunction(tooltipValueFormatter)) {
      tooltipValueFormatFunction = tooltipValueFormatter as TimeSeriesChartTooltipValueFormatFunction;
    } else if (typeof tooltipValueFormatter === 'string' && tooltipValueFormatter.length) {
      try {
        tooltipValueFormatFunction =
          new Function('value', 'latestData', tooltipValueFormatter) as TimeSeriesChartTooltipValueFormatFunction;
      } catch (e) {
      }
    }
    return tooltipValueFormatFunction;
  };

export class TimeSeriesChartTooltip {

  constructor(private renderer: Renderer2,
              private sanitizer: DomSanitizer,
              private settings: TimeSeriesChartTooltipWidgetSettings,
              private tooltipDateFormat: DateFormatProcessor,
              private valueFormatFunction: TimeSeriesChartTooltipValueFormatFunction) {

  }

  formatted(params: CallbackDataParams[] | CallbackDataParams, focusedSeriesIndex: number,
            series?: TimeSeriesChartDataItem[], interval?: Interval): HTMLElement {
    if (!this.settings.showTooltip) {
      return undefined;
    }

    const tooltipParams = TimeSeriesChartTooltip.mapTooltipParams(params, series, focusedSeriesIndex);
    if (!tooltipParams.items.length && !tooltipParams.comparisonItems.length) {
      return null;
    }

    const tooltipElement: HTMLElement = getTooltipElement(this.renderer, '16px');

    this.buildItemsTooltip(tooltipElement, tooltipParams.items, interval);
    this.buildItemsTooltip(tooltipElement, tooltipParams.comparisonItems, interval);

    return tooltipElement;
  }

  private buildItemsTooltip(tooltipElement: HTMLElement,
                            items: TooltipItem[], interval?: Interval) {
    if (items.length) {
      const tooltipItemsElement: HTMLElement = getTooltipElement(this.renderer, '4px');
      this.renderer.appendChild(tooltipElement, tooltipItemsElement);
      if (this.settings.tooltipShowDate) {
        this.renderer.appendChild(tooltipItemsElement, this.constructTooltipDateElement(items[0].param, interval));
      }
      for (const item of items) {
        this.renderer.appendChild(tooltipItemsElement, this.constructTooltipSeriesElement(item));
      }
    }
  }

  private constructTooltipDateElement(param: CallbackDataParams, interval?: Interval): HTMLElement {
    let dateText: string;
    const startTs = param.value[2];
    const endTs = param.value[3];
    if (this.settings.tooltipDateInterval && startTs && endTs && (endTs - 1) > startTs) {
      const startDateText = this.tooltipDateFormat.update(startTs, interval);
      const endDateText = this.tooltipDateFormat.update(endTs - 1, interval);
      if (startDateText === endDateText) {
        dateText = startDateText;
      } else {
        dateText = startDateText + ' - ' + endDateText;
      }
    } else {
      const ts = param.value[0];
      dateText = this.tooltipDateFormat.update(ts, interval);
    }
    return getTooltipDateElement(this.renderer, dateText, this.settings);
  }

  private constructTooltipSeriesElement(item: TooltipItem): HTMLElement {
    const labelValueElement: HTMLElement = getLabelValueElement(this.renderer);
    const labelElement: HTMLElement = getLabelElement(this.renderer);
    this.renderer.appendChild(labelValueElement, labelElement);
    const circleElement: HTMLElement = getCircleElement(this.renderer, item.param.color);
    this.renderer.appendChild(labelElement, circleElement);
    const labelTextElement: HTMLElement = getLabelTextElement(this.renderer, this.sanitizer, item.param.seriesName);
    this.renderer.appendChild(labelElement, labelTextElement);
    let formatFunction = this.valueFormatFunction;
    let latestData: FormattedData;
    let units = '';
    let decimals = 0;
    if (item.dataItem) {
      if (item.dataItem.tooltipValueFormatFunction) {
        formatFunction = item.dataItem.tooltipValueFormatFunction;
      }
      latestData = item.dataItem.latestData;
      units = item.dataItem.units;
      decimals = item.dataItem.decimals;
    }
    if (!latestData) {
      latestData = {} as FormattedData;
    }
    const value = formatFunction(item.param.value[1], latestData, units, decimals);
    this.renderer.appendChild(labelValueElement, getValueElement(this.renderer, this.sanitizer, value, this.settings));
    return labelValueElement;
  }

  private static mapTooltipParams(params: CallbackDataParams[] | CallbackDataParams,
                                  series?: TimeSeriesChartDataItem[],
                                  focusedSeriesIndex?: number): TooltipParams {
    const result: TooltipParams = {
      items: [],
      comparisonItems: []
    };
    if (!params || Array.isArray(params) && !params[0]) {
      return result;
    }
    const firstParam = Array.isArray(params) ? params[0] : params;
    if (!firstParam.value) {
      return result;
    }
    let seriesParams: CallbackDataParams = null;
    if (Array.isArray(params) && focusedSeriesIndex > -1) {
      seriesParams = params.find(param => param.seriesIndex === focusedSeriesIndex);
    } else if (!Array.isArray(params)) {
      seriesParams = params;
    }
    if (seriesParams) {
      TimeSeriesChartTooltip.appendTooltipItem(result, seriesParams, series);
    } else if (Array.isArray(params)) {
      for (seriesParams of params) {
        TimeSeriesChartTooltip.appendTooltipItem(result, seriesParams, series);
      }
    }
    return result;
  }

  private static appendTooltipItem(tooltipParams: TooltipParams, seriesParams: CallbackDataParams, series?: TimeSeriesChartDataItem[]) {
    const dataItem = series?.find(s => s.id === seriesParams.seriesId);
    const tooltipItem: TooltipItem = {
      param: seriesParams,
      dataItem
    };
    if (dataItem?.comparisonItem) {
      tooltipParams.comparisonItems.push(tooltipItem);
    } else {
      tooltipParams.items.push(tooltipItem);
    }
  };
}
