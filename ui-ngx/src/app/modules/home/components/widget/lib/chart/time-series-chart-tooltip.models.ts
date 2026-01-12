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

import { isDefined, isFunction, isNotEmptyStr } from '@core/utils';
import { FormattedData } from '@shared/models/widget.models';
import { DateFormatProcessor, DateFormatSettings, Font } from '@shared/models/widget-settings.models';
import { TimeSeriesChartDataItem } from '@home/components/widget/lib/chart/time-series-chart.models';
import { Renderer2, SecurityContext } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { CallbackDataParams } from 'echarts/types/dist/shared';
import { Interval } from '@shared/models/time/time.models';
import { TranslateService } from '@ngx-translate/core';

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
  tooltipHideZeroValues?: boolean;
  tooltipDateFormat: DateFormatSettings;
  tooltipDateFont: Font;
  tooltipDateColor: string;
  tooltipBackgroundColor: string;
  tooltipBackgroundBlur: number;
  tooltipStackedShowTotal?: boolean
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
              private valueFormatFunction: TimeSeriesChartTooltipValueFormatFunction,
              private translate: TranslateService) {

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
    if (this.settings.tooltipHideZeroValues && !tooltipParams.items.some(value => value.param.value[1] && value.param.value[1] !== 'false')) {
      return undefined;
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
      let total = 0;
      let isStacked = false;
      const totalUnits = new Set<string>();
      let totalDecimal = 0;
      for (const item of items) {
        if (this.shouldShowItem(item)) {
          this.renderer.appendChild(tooltipItemsElement, this.constructTooltipSeriesElement(item));
          if (item.dataItem?.option?.stack !== undefined && !isNaN(Number(item.param.value[1]))) {
            isStacked = true;
            total += Number(item.param.value[1]);
            if (isNotEmptyStr(item.dataItem.units)) {
              totalUnits.add(item.dataItem.units);
            }
            if (isDefined(item.dataItem.decimals)) {
              totalDecimal = Math.max(item.dataItem.decimals, totalDecimal);
            }
          }
        }
      }
      if (isStacked && this.settings.tooltipStackedShowTotal) {
        const unit = totalUnits.size === 1 ? Array.from(totalUnits.values())[0] : "";
        const totalValue = this.valueFormatFunction(total, {} as FormattedData, unit, totalDecimal);
        this.renderer.appendChild(tooltipItemsElement, this.constructTooltipTotalStackedElement(totalValue));
      }
    }
  }

  private shouldShowItem(item: TooltipItem): boolean {
    if (!this.settings.tooltipHideZeroValues) return true;
    const value = item.param?.value?.[1];
    return value && value !== 'false';
  }

  private createElement(tag = 'div', styles?: Record<string, string>): HTMLElement {
    const node = this.renderer.createElement(tag);
    if (styles) {
      for (const [k, v] of Object.entries(styles)) {
        this.renderer.setStyle(node, k, v);
      }
    }
    return node;
  }

  private applyFont(el: HTMLElement, font: {family: string; size: number; sizeUnit: string; style: string; weight: string; lineHeight: string}, color: string, overrides?: Partial<CSSStyleDeclaration>) {
    this.renderer.setStyle(el, 'font-family', font.family);
    this.renderer.setStyle(el, 'font-size', `${font.size}${font.sizeUnit}`);
    this.renderer.setStyle(el, 'font-style', font.style);
    this.renderer.setStyle(el, 'font-weight', font.weight);
    this.renderer.setStyle(el, 'line-height', font.lineHeight);
    this.renderer.setStyle(el, 'color', color);
    if (overrides) {
      for (const [k, v] of Object.entries(overrides)) {
        if (v != null) this.renderer.setStyle(el, k, v as string);
      }
    }
  }

  private constructTooltipDateElement(param: CallbackDataParams, interval?: Interval): HTMLElement {
    const dateElement = this.createElement();
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
    this.applyFont(dateElement, this.settings.tooltipDateFont, this.settings.tooltipDateColor);
    return dateElement;
  }

  private constructTooltipSeriesElement(item: TooltipItem): HTMLElement {
    const row = this.createElement('div', {display: 'flex', 'flex-direction': 'row', 'align-items': 'center', 'align-self': 'stretch', gap: '12px'});

    const label = this.createElement('div', { display: 'flex', 'align-items': 'center', gap: '8px' });
    this.renderer.appendChild(row, label);

    const dot = this.createElement('div', { width: '8px', height: '8px', 'border-radius': '50%', background: item.param.color as string });
    this.renderer.appendChild(label, dot);

    const labelText = this.createElement('div');
    this.renderer.setProperty(labelText, 'innerHTML', this.sanitizer.sanitize(SecurityContext.HTML, item.param.seriesName));
    this.applyFont(labelText, this.settings.tooltipLabelFont, this.settings.tooltipLabelColor);
    this.renderer.appendChild(label, labelText);

    const valueElement: HTMLElement = this.createElement('div', { flex: '1', 'text-align': 'end' });
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
    this.applyFont(valueElement, this.settings.tooltipValueFont, this.settings.tooltipValueColor);
    this.renderer.appendChild(row, valueElement);

    return row;
  }

  private constructTooltipTotalStackedElement(total: string): HTMLElement {
    const row = this.createElement('div', {display: 'flex', 'flex-direction': 'row', 'align-items': 'center', 'align-self': 'stretch', gap: '12px'});

    const label = this.createElement('div', { display: 'flex', 'align-items': 'center', gap: '8px' });
    this.renderer.appendChild(row, label);

    const labelText = this.createElement('div');
    this.renderer.setProperty(labelText, 'innerHTML', this.sanitizer.sanitize(SecurityContext.HTML, this.translate.instant('legend.Total')));
    this.applyFont(labelText, this.settings.tooltipLabelFont, this.settings.tooltipLabelColor, { fontWeight: 'bold' });
    this.renderer.appendChild(label, labelText);

    const valueEl = this.createElement('div', { flex: '1', 'text-align': 'end' });
    this.renderer.setProperty(valueEl, 'innerHTML', this.sanitizer.sanitize(SecurityContext.HTML, total));
    this.applyFont(valueEl, this.settings.tooltipValueFont, this.settings.tooltipValueColor, { fontWeight: 'bold' });
    this.renderer.appendChild(row, valueEl);

    return row;
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
