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

import { DataKey, Datasource, LegendPosition } from '@shared/models/widget.models';
import { BackgroundSettings, BackgroundType, Font, ValueFormatProcessor } from '@shared/models/widget-settings.models';
import { Renderer2 } from '@angular/core';
import { CallbackDataParams } from 'echarts/types/dist/shared';
import { formatValue, isDefinedAndNotNull, mergeDeep } from '@core/utils';
import { chartAnimationDefaultSettings, ChartAnimationSettings } from '@home/components/widget/lib/chart/chart.models';

export interface LatestChartDataItem {
  id: number;
  datasource: Datasource;
  dataKey: DataKey;
  value: number;
  hasValue: boolean;
  enabled: boolean;
}

export interface LatestChartLegendItem {
  dataKey?: DataKey;
  color: string;
  label: string;
  value: string;
  hasValue: boolean;
  total?: boolean;
}

export enum LatestChartTooltipValueType {
  absolute = 'absolute',
  percentage = 'percentage'
}

export const latestChartTooltipValueTypes = Object.keys(LatestChartTooltipValueType) as LatestChartTooltipValueType[];

export const latestChartTooltipValueTypeTranslations = new Map<LatestChartTooltipValueType, string>(
  [
    [LatestChartTooltipValueType.absolute, 'widgets.latest-chart.tooltip-value-type-absolute'],
    [LatestChartTooltipValueType.percentage, 'widgets.latest-chart.tooltip-value-type-percentage']
  ]
);

export interface LatestChartTooltipSettings {
  showTooltip: boolean;
  tooltipValueType: LatestChartTooltipValueType;
  tooltipValueDecimals: number;
  tooltipValueFormater: ValueFormatProcessor;
  tooltipValueFont: Font;
  tooltipValueColor: string;
  tooltipBackgroundColor: string;
  tooltipBackgroundBlur: number;
}

export const latestChartTooltipDefaultSettings: LatestChartTooltipSettings = {
  showTooltip: true,
  tooltipValueType: LatestChartTooltipValueType.percentage,
  tooltipValueDecimals: 0,
  tooltipValueFont: {
    family: 'Roboto',
    size: 13,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '16px'
  },
  tooltipValueColor: 'rgba(0, 0, 0, 0.76)',
  tooltipBackgroundColor: 'rgba(255, 255, 255, 0.76)',
  tooltipBackgroundBlur: 4,
  tooltipValueFormater: null
};

export interface LatestChartSettings extends LatestChartTooltipSettings {
  autoScale?: boolean;
  sortSeries: boolean;
  showTotal?: boolean;
  showLegend: boolean;
  legendShowTotal: boolean;
  animation: ChartAnimationSettings;
}

export const latestChartDefaultSettings: LatestChartSettings = {
  ...latestChartTooltipDefaultSettings,
  autoScale: false,
  sortSeries: false,
  showTotal: false,
  showLegend: true,
  legendShowTotal: true,
  animation: mergeDeep({} as ChartAnimationSettings, chartAnimationDefaultSettings)
};

export interface LatestChartWidgetSettings extends LatestChartSettings {
  legendPosition: LegendPosition;
  legendLabelFont: Font;
  legendLabelColor: string;
  legendValueFont: Font;
  legendValueColor: string;
  background: BackgroundSettings;
  padding: string;
}

export const latestChartWidgetDefaultSettings: LatestChartWidgetSettings = {
  ...latestChartDefaultSettings,
  legendPosition: LegendPosition.bottom,
  legendLabelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  legendLabelColor: 'rgba(0, 0, 0, 0.38)',
  legendValueFont: {
    family: 'Roboto',
    size: 14,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '20px'
  },
  legendValueColor: 'rgba(0, 0, 0, 0.87)',
  background: {
    type: BackgroundType.color,
    color: '#fff',
    overlay: {
      enabled: false,
      color: 'rgba(255,255,255,0.72)',
      blur: 3
    }
  },
  padding: '12px'
};

export const latestChartTooltipFormatter = (renderer: Renderer2,
                                            settings: LatestChartTooltipSettings,
                                            params: CallbackDataParams,
                                            total: number,
                                            dataItems: LatestChartDataItem[]): null | HTMLElement => {
  if (params.value && Array.isArray(params.value)) {
    const enabledDataItems = dataItems.filter(d => d.enabled && d.hasValue);
    if (enabledDataItems.length && enabledDataItems.length === params.value.length) {
      const tooltipElement: HTMLElement = renderer.createElement('div');
      renderer.setStyle(tooltipElement, 'display', 'flex');
      renderer.setStyle(tooltipElement, 'flex-direction', 'column');
      renderer.setStyle(tooltipElement, 'align-items', 'flex-start');
      renderer.setStyle(tooltipElement, 'gap', '4px');
      for (let i = 0; i < enabledDataItems.length; i++) {
        const dataItem = enabledDataItems[i];
        const value = params.value[i];
        renderer.appendChild(tooltipElement,
          constructTooltipSeriesElement(renderer, settings, dataItem.dataKey.label, value as number, null,
            total, dataItem.dataKey.color));
      }
      return tooltipElement;
    } else {
      return null;
    }
  } else if (params.name) {
    return constructTooltipSeriesElement(renderer, settings, params.name,
      params.value as number, params.percent, total);
  } else {
    return null;
  }
};

const constructTooltipSeriesElement = (renderer: Renderer2,
                                       settings: LatestChartTooltipSettings,
                                       label: string,
                                       value: number,
                                       percent: number | undefined,
                                       total: number,
                                       circleColor?: string): HTMLElement => {
  let formattedValue: string;
  if (settings.tooltipValueType === LatestChartTooltipValueType.percentage) {
    const percents = isDefinedAndNotNull(percent) ? percent : value / total * 100;
    formattedValue = formatValue(percents, settings.tooltipValueDecimals, '%', false);
  } else {
    formattedValue = settings.tooltipValueFormater?.format(value);
  }
  const textElement: HTMLElement = renderer.createElement('div');
  renderer.setStyle(textElement, 'display', 'flex');
  renderer.setStyle(textElement, 'flex-direction', 'row');
  renderer.setStyle(textElement, 'align-items', 'center');
  renderer.setStyle(textElement, 'align-self', 'stretch');
  renderer.setStyle(textElement, 'gap', '12px');
  const labelElement: HTMLElement = renderer.createElement('div');
  renderer.setStyle(labelElement, 'display', 'flex');
  renderer.setStyle(labelElement, 'align-items', 'center');
  renderer.setStyle(labelElement, 'gap', '8px');
  renderer.appendChild(textElement, labelElement);
  if (circleColor) {
    const circleElement: HTMLElement = renderer.createElement('div');
    renderer.setStyle(circleElement, 'width', '8px');
    renderer.setStyle(circleElement, 'height', '8px');
    renderer.setStyle(circleElement, 'border-radius', '50%');
    renderer.setStyle(circleElement, 'background', circleColor);
    renderer.appendChild(labelElement, circleElement);
  }
  const labelTextElement: HTMLElement = renderer.createElement('div');
  renderer.appendChild(labelTextElement, renderer.createText(label));
  renderer.setStyle(labelTextElement, 'font-family', 'Roboto');
  renderer.setStyle(labelTextElement, 'font-size', '11px');
  renderer.setStyle(labelTextElement, 'font-style', 'normal');
  renderer.setStyle(labelTextElement, 'font-weight', '400');
  renderer.setStyle(labelTextElement, 'line-height', '16px');
  renderer.setStyle(labelTextElement, 'letter-spacing', '0.25px');
  renderer.setStyle(labelTextElement, 'color', 'rgba(0, 0, 0, 0.38)');
  renderer.appendChild(labelElement, labelTextElement);
  const valueElement: HTMLElement = renderer.createElement('div');
  renderer.appendChild(valueElement, renderer.createText(formattedValue));
  renderer.setStyle(valueElement, 'flex', '1');
  renderer.setStyle(valueElement, 'text-align', 'end');
  renderer.setStyle(valueElement, 'font-family', settings.tooltipValueFont.family);
  renderer.setStyle(valueElement, 'font-size', settings.tooltipValueFont.size + settings.tooltipValueFont.sizeUnit);
  renderer.setStyle(valueElement, 'font-style', settings.tooltipValueFont.style);
  renderer.setStyle(valueElement, 'font-weight', settings.tooltipValueFont.weight);
  renderer.setStyle(valueElement, 'line-height', settings.tooltipValueFont.lineHeight);
  renderer.setStyle(valueElement, 'color', settings.tooltipValueColor);
  renderer.appendChild(textElement, labelElement);
  renderer.appendChild(textElement, valueElement);
  return textElement;
};
