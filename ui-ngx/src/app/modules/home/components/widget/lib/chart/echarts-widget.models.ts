///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import * as echarts from 'echarts/core';
import { Axis } from 'echarts';
import AxisModel from 'echarts/types/src/coord/cartesian/AxisModel';
import { formatValue, isNumber } from '@core/utils';
import TimeScale from 'echarts/types/src/scale/Time';
import {
  DataZoomComponent, DataZoomComponentOption,
  GridComponent, GridComponentOption,
  MarkLineComponent, MarkLineComponentOption,
  TooltipComponent, TooltipComponentOption,
  VisualMapComponent, VisualMapComponentOption
} from 'echarts/components';
import {
  BarChart,
  LineChart,
  CustomChart,
  CustomSeriesOption,
  LineSeriesOption,
  BarSeriesOption, PieSeriesOption, PieChart
} from 'echarts/charts';
import { LabelLayout } from 'echarts/features';
import { CanvasRenderer, SVGRenderer } from 'echarts/renderers';
import { DataSet } from '@shared/models/widget.models';
import { CallbackDataParams } from 'echarts/types/dist/shared';
import { Renderer2 } from '@angular/core';
import { DateFormatProcessor, DateFormatSettings, Font } from '@shared/models/widget-settings.models';

class EChartsModule {
  private initialized = false;

  init() {
    if (!this.initialized) {
      const axisGetBandWidth = Axis.prototype.getBandWidth;

      Axis.prototype.getBandWidth = function(){
        const model: AxisModel = this.model;
        const axisOption = model.option;
        const tbTimewindowInterval = (axisOption as any).tbTimewindowInterval;
        if (this.scale.type === 'time' && isNumber(tbTimewindowInterval)) {
          const timeScale: TimeScale = this.scale;
          const axisExtent: [number, number] = this._extent;
          const dataExtent = timeScale.getExtent();
          const size = Math.abs(axisExtent[1] - axisExtent[0]);
          return tbTimewindowInterval * (size / (dataExtent[1] - dataExtent[0]));
        } else {
          return axisGetBandWidth.call(this);
        }
      };

      echarts.use([
        TooltipComponent,
        GridComponent,
        VisualMapComponent,
        DataZoomComponent,
        MarkLineComponent,
        LineChart,
        BarChart,
        PieChart,
        CustomChart,
        LabelLayout,
        CanvasRenderer,
        SVGRenderer
      ]);

      this.initialized = true;
    }
  }
}

export const echartsModule = new EChartsModule();

export type EChartsOption = echarts.ComposeOption<
  | TooltipComponentOption
  | GridComponentOption
  | VisualMapComponentOption
  | DataZoomComponentOption
  | MarkLineComponentOption
  | LineSeriesOption
  | CustomSeriesOption
  | BarSeriesOption
  | PieSeriesOption
>;

export type ECharts = echarts.ECharts;

export type NamedDataSet = {name: string; value: [number, any]}[];

export const toNamedData = (data: DataSet): NamedDataSet => {
  if (!data?.length) {
    return [];
  } else {
    return data.map(d => ({
      name: d[0] + '',
      value: d
    }));
  }
};

export interface EChartsTooltipWidgetSettings {
  showTooltip: boolean;
  tooltipValueFont: Font;
  tooltipValueColor: string;
  tooltipShowDate: boolean;
  tooltipDateFormat: DateFormatSettings;
  tooltipDateFont: Font;
  tooltipDateColor: string;
  tooltipBackgroundColor: string;
  tooltipBackgroundBlur: number;
}

export const echartsTooltipFormatter = (renderer: Renderer2,
                                        tooltipDateFormat: DateFormatProcessor,
                                        settings: EChartsTooltipWidgetSettings,
                                        params: CallbackDataParams[],
                                        decimals: number,
                                        units: string,
                                        focusedSeriesIndex: number): null | HTMLElement => {
  if (!params.length || !params[0]) {
    return null;
  }
  const tooltipElement: HTMLElement = renderer.createElement('div');
  renderer.setStyle(tooltipElement, 'display', 'flex');
  renderer.setStyle(tooltipElement, 'flex-direction', 'column');
  renderer.setStyle(tooltipElement, 'align-items', 'flex-start');
  renderer.setStyle(tooltipElement, 'gap', '4px');
  if (settings.tooltipShowDate) {
    const dateElement: HTMLElement = renderer.createElement('div');
    const ts = params[0].value[0];
    tooltipDateFormat.update(ts);
    renderer.appendChild(dateElement, renderer.createText(tooltipDateFormat.formatted));
    renderer.setStyle(dateElement, 'font-family', settings.tooltipDateFont.family);
    renderer.setStyle(dateElement, 'font-size', settings.tooltipDateFont.size + settings.tooltipDateFont.sizeUnit);
    renderer.setStyle(dateElement, 'font-style', settings.tooltipDateFont.style);
    renderer.setStyle(dateElement, 'font-weight', settings.tooltipDateFont.weight);
    renderer.setStyle(dateElement, 'line-height', settings.tooltipDateFont.lineHeight);
    renderer.setStyle(dateElement, 'color', settings.tooltipDateColor);
    renderer.appendChild(tooltipElement, dateElement);
  }
  let seriesParams: CallbackDataParams = null;
  if (focusedSeriesIndex > -1) {
    seriesParams = params.find(param => param.seriesIndex === focusedSeriesIndex);
  }
  if (seriesParams) {
    renderer.appendChild(tooltipElement, constructEchartsTooltipSeriesElement(renderer, settings, seriesParams, decimals, units));
  } else {
    for (seriesParams of params) {
      renderer.appendChild(tooltipElement, constructEchartsTooltipSeriesElement(renderer, settings, seriesParams, decimals, units));
    }
  }
  return tooltipElement;
};

const constructEchartsTooltipSeriesElement = (renderer: Renderer2,
                                              settings: EChartsTooltipWidgetSettings,
                                              seriesParams: CallbackDataParams,
                                              decimals: number,
                                              units: string): HTMLElement => {
  const labelValueElement: HTMLElement = renderer.createElement('div');
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
  renderer.setStyle(circleElement, 'background', seriesParams.color);
  renderer.appendChild(labelElement, circleElement);
  const labelTextElement: HTMLElement = renderer.createElement('div');
  renderer.appendChild(labelTextElement, renderer.createText(seriesParams.seriesName));
  renderer.setStyle(labelTextElement, 'font-family', 'Roboto');
  renderer.setStyle(labelTextElement, 'font-size', '12px');
  renderer.setStyle(labelTextElement, 'font-style', 'normal');
  renderer.setStyle(labelTextElement, 'font-weight', '400');
  renderer.setStyle(labelTextElement, 'line-height', '16px');
  renderer.setStyle(labelTextElement, 'letter-spacing', '0.4px');
  renderer.setStyle(labelTextElement, 'color', 'rgba(0, 0, 0, 0.76)');
  renderer.appendChild(labelElement, labelTextElement);
  const valueElement: HTMLElement = renderer.createElement('div');
  const value = formatValue(seriesParams.value[1], decimals, units, false);
  renderer.appendChild(valueElement, renderer.createText(value));
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
