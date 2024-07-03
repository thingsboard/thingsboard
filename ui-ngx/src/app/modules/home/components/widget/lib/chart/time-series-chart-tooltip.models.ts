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
import { Renderer2, SecurityContext } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { CallbackDataParams } from 'echarts/types/dist/shared';
import { Interval } from '@shared/models/time/time.models';

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

    const tooltipElement: HTMLElement = this.renderer.createElement('div');
    this.renderer.setStyle(tooltipElement, 'display', 'flex');
    this.renderer.setStyle(tooltipElement, 'flex-direction', 'column');
    this.renderer.setStyle(tooltipElement, 'align-items', 'flex-start');
    this.renderer.setStyle(tooltipElement, 'gap', '16px');

    this.buildItemsTooltip(tooltipElement, tooltipParams.items, interval);
    this.buildItemsTooltip(tooltipElement, tooltipParams.comparisonItems, interval);

    return tooltipElement;
  }

  private buildItemsTooltip(tooltipElement: HTMLElement,
                            items: TooltipItem[], interval?: Interval) {
    if (items.length) {
      const tooltipItemsElement: HTMLElement = this.renderer.createElement('div');
      this.renderer.setStyle(tooltipItemsElement, 'display', 'flex');
      this.renderer.setStyle(tooltipItemsElement, 'flex-direction', 'column');
      this.renderer.setStyle(tooltipItemsElement, 'align-items', 'flex-start');
      this.renderer.setStyle(tooltipItemsElement, 'gap', '4px');
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
    const dateElement: HTMLElement = this.renderer.createElement('div');
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
    this.renderer.appendChild(dateElement, this.renderer.createText(dateText));
    this.renderer.setStyle(dateElement, 'font-family', this.settings.tooltipDateFont.family);
    this.renderer.setStyle(dateElement, 'font-size', this.settings.tooltipDateFont.size + this.settings.tooltipDateFont.sizeUnit);
    this.renderer.setStyle(dateElement, 'font-style', this.settings.tooltipDateFont.style);
    this.renderer.setStyle(dateElement, 'font-weight', this.settings.tooltipDateFont.weight);
    this.renderer.setStyle(dateElement, 'line-height', this.settings.tooltipDateFont.lineHeight);
    this.renderer.setStyle(dateElement, 'color', this.settings.tooltipDateColor);
    return dateElement;
  }

  private constructTooltipSeriesElement(item: TooltipItem): HTMLElement {
    const labelValueElement: HTMLElement = this.renderer.createElement('div');
    this.renderer.setStyle(labelValueElement, 'display', 'flex');
    this.renderer.setStyle(labelValueElement, 'flex-direction', 'row');
    this.renderer.setStyle(labelValueElement, 'align-items', 'center');
    this.renderer.setStyle(labelValueElement, 'align-self', 'stretch');
    this.renderer.setStyle(labelValueElement, 'gap', '12px');
    const labelElement: HTMLElement = this.renderer.createElement('div');
    this.renderer.setStyle(labelElement, 'display', 'flex');
    this.renderer.setStyle(labelElement, 'align-items', 'center');
    this.renderer.setStyle(labelElement, 'gap', '8px');
    this.renderer.appendChild(labelValueElement, labelElement);
    const circleElement: HTMLElement = this.renderer.createElement('div');
    this.renderer.setStyle(circleElement, 'width', '8px');
    this.renderer.setStyle(circleElement, 'height', '8px');
    this.renderer.setStyle(circleElement, 'border-radius', '50%');
    this.renderer.setStyle(circleElement, 'background', item.param.color);
    this.renderer.appendChild(labelElement, circleElement);
    const labelTextElement: HTMLElement = this.renderer.createElement('div');
    this.renderer.setProperty(labelTextElement, 'innerHTML', this.sanitizer.sanitize(SecurityContext.HTML, item.param.seriesName));
    this.renderer.setStyle(labelTextElement, 'font-family', this.settings.tooltipLabelFont.family);
    this.renderer.setStyle(labelTextElement, 'font-size', this.settings.tooltipLabelFont.size + this.settings.tooltipLabelFont.sizeUnit);
    this.renderer.setStyle(labelTextElement, 'font-style', this.settings.tooltipLabelFont.style);
    this.renderer.setStyle(labelTextElement, 'font-weight', this.settings.tooltipLabelFont.weight);
    this.renderer.setStyle(labelTextElement, 'line-height', this.settings.tooltipLabelFont.lineHeight);
    this.renderer.setStyle(labelTextElement, 'color', this.settings.tooltipLabelColor);
    this.renderer.appendChild(labelElement, labelTextElement);
    const valueElement: HTMLElement = this.renderer.createElement('div');
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
    this.renderer.setProperty(valueElement, 'innerHTML', this.sanitizer.sanitize(SecurityContext.HTML, value));
    this.renderer.setStyle(valueElement, 'flex', '1');
    this.renderer.setStyle(valueElement, 'text-align', 'end');
    this.renderer.setStyle(valueElement, 'font-family', this.settings.tooltipValueFont.family);
    this.renderer.setStyle(valueElement, 'font-size', this.settings.tooltipValueFont.size + this.settings.tooltipValueFont.sizeUnit);
    this.renderer.setStyle(valueElement, 'font-style', this.settings.tooltipValueFont.style);
    this.renderer.setStyle(valueElement, 'font-weight', this.settings.tooltipValueFont.weight);
    this.renderer.setStyle(valueElement, 'line-height', this.settings.tooltipValueFont.lineHeight);
    this.renderer.setStyle(valueElement, 'color', this.settings.tooltipValueColor);
    this.renderer.appendChild(labelValueElement, valueElement);
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
