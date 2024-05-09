import { Renderer2 } from '@angular/core';
import { DateFormatProcessor } from '@shared/models/widget-settings.models';
import { Interval } from '@shared/models/time/time.models';
import {
  TimeSeriesChartDataItem,
  TimeSeriesChartTooltipValueFormatFunction,
  TimeSeriesChartTooltipWidgetSettings
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { CallbackDataParams } from 'echarts/types/dist/shared';
import { FormattedData } from '@shared/models/widget.models';
import { isFunction } from '@core/utils';
import { DomSanitizer } from '@angular/platform-browser';

interface TooltipItem {
  param: CallbackDataParams;
  dataItem: TimeSeriesChartDataItem;
}

interface TooltipParams {
  items: TooltipItem[];
  comparisonItems: TooltipItem[];
}

export class TimeSeriesChartTooltip {

  constructor(private sanitizer: DomSanitizer) {
  }

  public timeSeriesChartTooltipFormatter(renderer: Renderer2,
                                                  tooltipDateFormat: DateFormatProcessor,
                                                  settings: TimeSeriesChartTooltipWidgetSettings,
                                                  params: CallbackDataParams[] | CallbackDataParams,
                                                  valueFormatFunction: TimeSeriesChartTooltipValueFormatFunction,
                                                  focusedSeriesIndex: number,
                                                  series?: TimeSeriesChartDataItem[],
                                                  interval?: Interval): null | HTMLElement {

    const tooltipParams = this.mapTooltipParams(params, series, focusedSeriesIndex);
    if (!tooltipParams.items.length && !tooltipParams.comparisonItems.length) {
      return null;
    }

    const tooltipElement: HTMLElement = renderer.createElement('div');
    renderer.setStyle(tooltipElement, 'display', 'flex');
    renderer.setStyle(tooltipElement, 'flex-direction', 'column');
    renderer.setStyle(tooltipElement, 'align-items', 'flex-start');
    renderer.setStyle(tooltipElement, 'gap', '16px');
    this.buildItemsTooltip(tooltipElement, tooltipParams.items, renderer, tooltipDateFormat, settings, valueFormatFunction, interval);
    this.buildItemsTooltip(tooltipElement, tooltipParams.comparisonItems, renderer, tooltipDateFormat, settings, valueFormatFunction,
      interval);

    return tooltipElement;
  };

  public createTooltipValueFormatFunction(tooltipValueFormatter: string | TimeSeriesChartTooltipValueFormatFunction):
    TimeSeriesChartTooltipValueFormatFunction {
      let tooltipValueFormatFunction: TimeSeriesChartTooltipValueFormatFunction;
      if (isFunction(tooltipValueFormatter)) {
        tooltipValueFormatFunction = tooltipValueFormatter as TimeSeriesChartTooltipValueFormatFunction;
      } else if (typeof tooltipValueFormatter === 'string' && tooltipValueFormatter.length) {
        try {
          tooltipValueFormatFunction =
            new Function('value', 'latestData', tooltipValueFormatter) as TimeSeriesChartTooltipValueFormatFunction;
        } catch (e) {}
      }
      return tooltipValueFormatFunction;
    };

  private mapTooltipParams(params: CallbackDataParams[] | CallbackDataParams,
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
      this.appendTooltipItem(result, seriesParams, series);
    } else if (Array.isArray(params)) {
      for (seriesParams of params) {
        this.appendTooltipItem(result, seriesParams, series);
      }
    }
    return result;
  };

  private appendTooltipItem(tooltipParams: TooltipParams, seriesParams: CallbackDataParams, series?: TimeSeriesChartDataItem[]): void {
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


  private buildItemsTooltip(tooltipElement: HTMLElement,
          items: TooltipItem[],
          renderer: Renderer2,
          tooltipDateFormat: DateFormatProcessor,
          settings: TimeSeriesChartTooltipWidgetSettings,
          valueFormatFunction: TimeSeriesChartTooltipValueFormatFunction,
          interval?: Interval) {
      if (items.length) {
        const tooltipItemsElement: HTMLElement = renderer.createElement('div');
        renderer.setStyle(tooltipItemsElement, 'display', 'flex');
        renderer.setStyle(tooltipItemsElement, 'flex-direction', 'column');
        renderer.setStyle(tooltipItemsElement, 'align-items', 'flex-start');
        renderer.setStyle(tooltipItemsElement, 'gap', '4px');
        renderer.appendChild(tooltipElement, tooltipItemsElement);
        if (settings.tooltipShowDate) {
        renderer.appendChild(tooltipItemsElement,
          this.constructTooltipDateElement(renderer, tooltipDateFormat, settings, items[0].param, interval));
      }
      for (const item of items) {
        renderer.appendChild(tooltipItemsElement,
          this.constructTooltipSeriesElement(renderer, settings, item, valueFormatFunction));
      }
    }
  };

  private constructTooltipDateElement(renderer: Renderer2,
                                               tooltipDateFormat: DateFormatProcessor,
                                               settings: TimeSeriesChartTooltipWidgetSettings,
                                               param: CallbackDataParams,
                                               interval?: Interval): HTMLElement {
    const dateElement: HTMLElement = renderer.createElement('div');
    let dateText: string;
    const startTs = param.value[2];
    const endTs = param.value[3];
    if (settings.tooltipDateInterval && startTs && endTs && (endTs - 1) > startTs) {
      const startDateText = tooltipDateFormat.update(startTs, interval);
      const endDateText = tooltipDateFormat.update(endTs - 1, interval);
      if (startDateText === endDateText) {
        dateText = startDateText;
      } else {
        dateText = startDateText + ' - ' + endDateText;
      }
    } else {
      const ts = param.value[0];
      dateText = tooltipDateFormat.update(ts, interval);
    }
    renderer.appendChild(dateElement, renderer.createText(dateText));
    renderer.setStyle(dateElement, 'font-family', settings.tooltipDateFont.family);
    renderer.setStyle(dateElement, 'font-size', settings.tooltipDateFont.size + settings.tooltipDateFont.sizeUnit);
    renderer.setStyle(dateElement, 'font-style', settings.tooltipDateFont.style);
    renderer.setStyle(dateElement, 'font-weight', settings.tooltipDateFont.weight);
    renderer.setStyle(dateElement, 'line-height', settings.tooltipDateFont.lineHeight);
    renderer.setStyle(dateElement, 'color', settings.tooltipDateColor);
    return dateElement;
  };

  private constructTooltipSeriesElement(renderer: Renderer2,
                                         settings: TimeSeriesChartTooltipWidgetSettings,
                                         item: TooltipItem,
                                         valueFormatFunction: TimeSeriesChartTooltipValueFormatFunction): HTMLElement {
    const labelValueElement: HTMLDivElement= renderer.createElement('div');
    renderer.setStyle(labelValueElement, 'display', 'flex');
    renderer.setStyle(labelValueElement, 'flex-direction', 'row');
    renderer.setStyle(labelValueElement, 'align-items', 'center');
    renderer.setStyle(labelValueElement, 'align-self', 'stretch');
    renderer.setStyle(labelValueElement, 'gap', '12px');
    const labelElement: HTMLElement = renderer.createElement('div');
    renderer.setStyle(labelElement, 'display', 'flex');
    renderer.setStyle(labelElement, 'align-items', 'center');
    renderer.setStyle(labelElement, 'gap', '8px');
    renderer.appendChild(labelValueElement, labelElement);
    const circleElement: HTMLElement = renderer.createElement('div');
    renderer.setStyle(circleElement, 'width', '8px');
    renderer.setStyle(circleElement, 'height', '8px');
    renderer.setStyle(circleElement, 'border-radius', '50%');
    renderer.setStyle(circleElement, 'background', item.param.color);
    renderer.appendChild(labelElement, circleElement);
    const labelTextElement: HTMLElement = renderer.createElement('div');
    labelTextElement.innerHTML = this.sanitizer.sanitize(1, item.param.seriesName);
    renderer.setStyle(labelTextElement, 'font-family', settings.tooltipLabelFont.family);
    renderer.setStyle(labelTextElement, 'font-size', settings.tooltipLabelFont.size + settings.tooltipLabelFont.sizeUnit);
    renderer.setStyle(labelTextElement, 'font-style', settings.tooltipLabelFont.style);
    renderer.setStyle(labelTextElement, 'font-weight', settings.tooltipLabelFont.weight);
    renderer.setStyle(labelTextElement, 'line-height', settings.tooltipLabelFont.lineHeight);
    renderer.setStyle(labelTextElement, 'color', settings.tooltipLabelColor);
    renderer.appendChild(labelElement, labelTextElement);
    const valueElement: HTMLElement = renderer.createElement('div');
    let formatFunction = valueFormatFunction;
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
    valueElement.innerHTML = this.sanitizer.sanitize(1, value);
    renderer.setStyle(valueElement, 'flex', '1');
    renderer.setStyle(valueElement, 'text-align', 'end');
    renderer.setStyle(valueElement, 'font-family', settings.tooltipValueFont.family);
    renderer.setStyle(valueElement, 'font-size', settings.tooltipValueFont.size + settings.tooltipValueFont.sizeUnit);
    renderer.setStyle(valueElement, 'font-style', settings.tooltipValueFont.style);
    renderer.setStyle(valueElement, 'font-weight', settings.tooltipValueFont.weight);
    renderer.setStyle(valueElement, 'line-height', settings.tooltipValueFont.lineHeight);
    renderer.setStyle(valueElement, 'color', settings.tooltipValueColor);
    renderer.appendChild(labelValueElement, valueElement);
    return labelValueElement;
  };
}
