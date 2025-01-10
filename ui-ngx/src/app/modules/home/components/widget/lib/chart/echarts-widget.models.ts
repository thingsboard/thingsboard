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

import * as echarts from 'echarts/core';
import AxisModel from 'echarts/types/src/coord/cartesian/AxisModel';
import { estimateLabelUnionRect } from 'echarts/lib/coord/axisHelper';
import {
  DataZoomComponent,
  DataZoomComponentOption,
  GridComponent,
  GridComponentOption,
  MarkLineComponent,
  MarkLineComponentOption,
  PolarComponent,
  PolarComponentOption,
  RadarComponent,
  RadarComponentOption,
  TooltipComponent,
  TooltipComponentOption,
  VisualMapComponent,
  VisualMapComponentOption
} from 'echarts/components';
import {
  BarChart,
  BarSeriesOption,
  CustomChart,
  CustomSeriesOption,
  LineChart,
  LineSeriesOption,
  PieChart,
  PieSeriesOption,
  RadarChart,
  RadarSeriesOption
} from 'echarts/charts';
import { LabelLayout } from 'echarts/features';
import { CanvasRenderer, SVGRenderer } from 'echarts/renderers';
import { CallbackDataParams, XAXisOption, YAXisOption, ZRColor } from 'echarts/types/dist/shared';
import GlobalModel from 'echarts/types/src/model/Global';
import Axis2D from 'echarts/types/src/coord/cartesian/Axis2D';
import SeriesModel from 'echarts/types/src/model/Series';
import { MarkLine2DDataItemOption } from 'echarts/types/src/component/marker/MarkLineModel';
import { measureSymbolOffset } from '@home/components/widget/lib/chart/chart.models';
import {
  cssUnits,
  fontStyles,
  fontWeights
} from '@shared/models/widget-settings.models';
import { TimeSeriesChartTooltipWidgetSettings } from '@home/components/widget/lib/chart/time-series-chart-tooltip.models';
import { Renderer2, SecurityContext } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

class EChartsModule {
  private initialized = false;

  init() {
    if (!this.initialized) {
      echarts.use([
        TooltipComponent,
        GridComponent,
        VisualMapComponent,
        DataZoomComponent,
        MarkLineComponent,
        PolarComponent,
        RadarComponent,
        LineChart,
        BarChart,
        PieChart,
        RadarChart,
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
  | PolarComponentOption
  | RadarComponentOption
  | LineSeriesOption
  | CustomSeriesOption
  | BarSeriesOption
  | PieSeriesOption
  | RadarSeriesOption
>;

export type ECharts = echarts.ECharts;

export const getAxis = (chart: ECharts, mainType: string, axisId: string): Axis2D => {
  const model: GlobalModel = (chart as any).getModel();
  const models = model.queryComponents({mainType, id: axisId});
  if (models?.length) {
    const axisModel = models[0] as AxisModel;
    return axisModel.axis;
  }
  return null;
};

export const calculateAxisSize = (chart: ECharts, mainType: string, axisId: string): number => {
  const axis = getAxis(chart, mainType, axisId);
  return _calculateAxisSize(axis);
};

export const measureAxisNameSize = (chart: ECharts, mainType: string, axisId: string, name: string): number => {
  const axis = getAxis(chart, mainType, axisId);
  if (axis) {
    return axis.model.getModel('nameTextStyle').getTextRect(name).height;
  }
  return 0;
};

const _calculateAxisSize = (axis: Axis2D): number => {
  let size = 0;
  if (axis && axis.model.option.show) {
    const labelUnionRect = estimateLabelUnionRect(axis);
    if (labelUnionRect) {
      const margin = axis.model.get(['axisLabel', 'margin']);
      const dimension = axis.isHorizontal() ? 'height' : 'width';
      size += labelUnionRect[dimension] + margin;
    }
    if (!axis.scale.isBlank() && axis.model.get(['axisTick', 'show'])) {
      const tickLength = axis.model.get(['axisTick', 'length']);
      size += tickLength;
    }
  }
  return size;
};

export const measureThresholdOffset = (chart: ECharts, axisId: string, thresholdId: string, value: any): [number, number] => {
  const offset: [number, number] = [0,0];
  const axis = getAxis(chart, 'yAxis', axisId);
  if (axis && !axis.scale.isBlank()) {
    const extent = axis.scale.getExtent();
    const model: GlobalModel = (chart as any).getModel();
    const models = model.queryComponents({mainType: 'series', id: thresholdId});
    if (models?.length) {
      const lineSeriesModel = models[0] as SeriesModel<LineSeriesOption>;
      const markLineModel = lineSeriesModel.getModel('markLine');
      const dataOption = markLineModel.get('data');
      for (const dataItemOption of dataOption) {
        const dataItem = dataItemOption as MarkLine2DDataItemOption;
        const start = dataItem[0];
        const startOffset = measureSymbolOffset(start.symbol, start.symbolSize);
        offset[0] = Math.max(offset[0], startOffset);
        const end = dataItem[1];
        const endOffset = measureSymbolOffset(end.symbol, end.symbolSize);
        offset[1] = Math.max(offset[1], endOffset);
      }
      const labelPosition = markLineModel.get(['label', 'position']);
      if (labelPosition === 'start' || labelPosition === 'end') {
        const labelModel = markLineModel.getModel('label');
        const formatter = markLineModel.get(['label', 'formatter']);
        let textWidth = 0;
        if (Array.isArray(value)) {
          for (const val of value) {
            if (val >= extent[0] && val <= extent[1]) {
              const textVal = typeof formatter === 'string' ? formatter : formatter({value: val} as CallbackDataParams);
              textWidth = Math.max(textWidth, labelModel.getTextRect(textVal).width);
            }
          }
        } else {
          if (value >= extent[0] && value <= extent[1]) {
            const textVal = typeof formatter === 'string' ? formatter : formatter({value} as CallbackDataParams);
            textWidth = labelModel.getTextRect(textVal).width;
          }
        }
        if (!textWidth) {
          return offset;
        }
        const distanceOpt = markLineModel.get(['label', 'distance']);
        let distance = 5;
        if (distanceOpt) {
          distance = typeof distanceOpt === 'number' ? distanceOpt : distanceOpt[0];
        }
        const paddingOpt = markLineModel.get(['label', 'padding']);
        let leftPadding = 0;
        let rightPadding = 0;
        if (paddingOpt) {
          if (Array.isArray(paddingOpt)) {
            if (paddingOpt.length === 4) {
              leftPadding = paddingOpt[3];
              rightPadding = paddingOpt[1];
            } else if (paddingOpt.length === 2) {
              leftPadding = rightPadding = paddingOpt[1];
            }
          } else {
            leftPadding = rightPadding = paddingOpt;
          }
        }
        const textOffset = distance + textWidth + leftPadding + rightPadding;
        if (labelPosition === 'start') {
          offset[0] = Math.max(offset[0], textOffset);
        } else {
          offset[1] = Math.max(offset[1], textOffset);
        }
      }
    }
  }
  return offset;
};

export const getAxisExtent = (chart: ECharts, axisId: string): [number, number] => {
  const axis = getAxis(chart, 'yAxis', axisId);
  if (axis) {
    return axis.scale.getExtent();
  }
  return [0,0];
};

let componentBlurredKey: string;

const isBlurred = (model: SeriesModel): boolean => {
  if (!componentBlurredKey) {
    const innerKeys = Object.keys(model).filter(k => k.startsWith('__ec_inner_'));
    for (const k of innerKeys) {
      const obj = model[k];
      if (obj.hasOwnProperty('isBlured')) {
        componentBlurredKey = k;
        break;
      }
    }
  }
  if (componentBlurredKey) {
    const obj = model[componentBlurredKey];
    return !!obj?.isBlured;
  } else {
    return false;
  }
};

export const getFocusedSeriesIndex = (chart: ECharts): number => {
  const model: GlobalModel = (chart as any).getModel();
  const models = model.queryComponents({mainType: 'series'});
  if (models) {
    let hasBlurred = false;
    let notBlurredIndex = -1;
    for (const _model of models) {
      const seriesModel = _model as SeriesModel;
      const blurred = isBlurred(seriesModel);
      if (!blurred) {
        notBlurredIndex = seriesModel.seriesIndex;
      }
      hasBlurred = blurred || hasBlurred;
    }
    if (hasBlurred) {
      return notBlurredIndex;
    }
  }
  return -1;
};

export const defaultTooltipSettings: Partial<TimeSeriesChartTooltipWidgetSettings> = {
  tooltipDateFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: cssUnits[0],
    style: fontStyles[0],
    weight: fontWeights[0],
    lineHeight: '16px'
  },
  tooltipDateColor: 'rgba(0, 0, 0, 0.76)'
}

export const defaultValueSettings: Partial<TimeSeriesChartTooltipWidgetSettings> = {
  tooltipDateFont: {
    family: 'Roboto',
    size: 11,
    sizeUnit: cssUnits[0],
    style: fontStyles[0],
    weight: '500',
    lineHeight: '16px'
  },
  tooltipDateColor: 'rgba(0, 0, 0, 0.76)'
}

export const getDefaultXAxis = (min: number, max: number): XAXisOption => {
  return {
    id: 'xAxis',
    mainType: 'xAxis',
    show: true,
    type: 'time',
    position: "bottom",
    offset: 0,
    nameLocation: 'middle',
    max,
    min,
    nameTextStyle: {
      color: 'rgba(0, 0, 0, 0.54)',
      fontStyle: 'normal',
      fontWeight: 600,
      fontFamily: 'Roboto',
      fontSize: 12,
    },
    axisPointer: {
      shadowStyle: {
        color: 'rgba(210,219,238,0.2)'
      }
    },
    splitLine: {
      show: true
    },
    axisTick: {
      show: true,
      lineStyle: {
        color: 'rgba(0, 0, 0, 0.54)'
      }
    },
    axisLine: {
      onZero: false,
      show: true,
      lineStyle: {
        color: 'rgba(0, 0, 0, 0.54)'
      }
    },
    axisLabel: {
      color: 'rgba(0, 0, 0, 0.54)',
      fontFamily: 'Roboto',
      fontSize: 10,
      fontStyle: 'normal',
      fontWeight: 400,
      show: true,
      hideOverlap: true,
    }
  }
}

export const getDefaultYAxis = (formatter: (value: unknown) => string): YAXisOption => {
  return {
    type: 'value',
    position: 'left',
    mainType: 'yAxis',
    id: 'yAxis',
    offset: 0,
    nameLocation: 'middle',
    nameRotate: 90,
    alignTicks: true,
    scale: true,
    show: true,
    axisLabel: {
      color: 'rgba(0, 0, 0, 0.54)',
      fontFamily: 'Roboto',
      fontSize: 12,
      fontStyle: 'normal',
      fontWeight: 400,
      show: true,
      formatter,
    },
    splitLine: {
      show: true,
    },
    axisLine: {
      show: true,
      lineStyle: {
        color: 'rgba(0, 0, 0, 0.54)'
      }
    },
    axisTick: {
      lineStyle: {
        color: 'rgba(0, 0, 0, 0.54)'
      },
      show: true
    },
    nameTextStyle: {
      color: 'rgba(0, 0, 0, 0.54)',
      fontFamily: 'Roboto',
      fontSize: 12,
      fontStyle: 'normal',
      fontWeight: 600
    }
  }
}

export const getDefaultChartOptions = (): Partial<EChartsOption> => {
  return {
    animation: true,
    animationDelay: 0,
    animationDelayUpdate: 0,
    animationDuration: 500,
    animationDurationUpdate: 300,
    animationEasing: "cubicOut",
    animationEasingUpdate: "cubicOut",
    animationThreshold: 2000,
    backgroundColor: "transparent",
    darkMode: false,
    tooltip: {
      show: true,
      trigger: 'axis',
      confine: true,
      padding: [8, 12],
      appendTo: 'body',
      textStyle: {
        fontFamily: 'Roboto',
        fontSize: 12,
        fontWeight: 'normal',
        lineHeight: 16
      }
    },
    grid: [{
      backgroundColor: null,
      borderColor: "#ccc",
      borderWidth: 1,
      bottom: 45,
      left: 5,
      right: 5,
      show: false,
      top: 10
    }],
    dataZoom: [
      {
        type: 'inside',
        disabled: false,
        realtime: true,
        filterMode:  'none'
      },
      {
        type: 'slider',
        show: true,
        showDetail: false,
        realtime: true,
        filterMode: 'none',
        bottom: 5
      }
    ]
  }
}

export const getTooltipDateElement = (renderer: Renderer2, dateText: string, settings = defaultTooltipSettings): HTMLElement => {
  const dateElement: HTMLElement = renderer.createElement('div');
  renderer.appendChild(dateElement, renderer.createText(dateText));
  renderer.setStyle(dateElement, 'font-family', settings.tooltipDateFont.family);
  renderer.setStyle(dateElement, 'font-size', settings.tooltipDateFont.size + settings.tooltipDateFont.sizeUnit);
  renderer.setStyle(dateElement, 'font-style', settings.tooltipDateFont.style);
  renderer.setStyle(dateElement, 'font-weight', settings.tooltipDateFont.weight);
  renderer.setStyle(dateElement, 'line-height', settings.tooltipDateFont.lineHeight);
  renderer.setStyle(dateElement, 'color', settings.tooltipDateColor);
  return dateElement
}

export const getTooltipElement = (renderer: Renderer2, gapSize: string): HTMLElement => {
  const tooltipElement: HTMLElement = renderer.createElement('div');
  renderer.setStyle(tooltipElement, 'display', 'flex');
  renderer.setStyle(tooltipElement, 'flex-direction', 'column');
  renderer.setStyle(tooltipElement, 'align-items', 'flex-start');
  renderer.setStyle(tooltipElement, 'gap', gapSize);
  return tooltipElement;
}

export const getLabelValueElement = (renderer: Renderer2): HTMLElement => {
  const labelValueElement: HTMLElement = renderer.createElement('div');
  renderer.setStyle(labelValueElement, 'display', 'flex');
  renderer.setStyle(labelValueElement, 'flex-direction', 'row');
  renderer.setStyle(labelValueElement, 'align-items', 'center');
  renderer.setStyle(labelValueElement, 'align-self', 'stretch');
  renderer.setStyle(labelValueElement, 'gap', '12px');
  return labelValueElement;
}

export const getLabelElement = (renderer: Renderer2): HTMLElement => {
  const labelElement: HTMLElement = renderer.createElement('div');
  renderer.setStyle(labelElement, 'display', 'flex');
  renderer.setStyle(labelElement, 'align-items', 'center');
  renderer.setStyle(labelElement, 'gap', '8px');
  return labelElement;
}

export const getCircleElement = (renderer: Renderer2, color: ZRColor): HTMLElement => {
  const circleElement: HTMLElement = renderer.createElement('div');
  renderer.setStyle(circleElement, 'width', '8px');
  renderer.setStyle(circleElement, 'height', '8px');
  renderer.setStyle(circleElement, 'border-radius', '50%');
  renderer.setStyle(circleElement, 'background', color);
  return circleElement;
}

export const getLabelTextElement = (renderer: Renderer2, sanitizer: DomSanitizer, seriesName: string): HTMLElement => {
  const labelTextElement: HTMLElement = renderer.createElement('div');
  renderer.setProperty(labelTextElement, 'innerHTML', sanitizer.sanitize(SecurityContext.HTML, seriesName));
  renderer.setStyle(labelTextElement, 'font-family', 'Roboto');
  renderer.setStyle(labelTextElement, 'font-size', '12px');
  renderer.setStyle(labelTextElement, 'font-style', 'normal');
  renderer.setStyle(labelTextElement, 'font-weight', 400);
  renderer.setStyle(labelTextElement, 'line-height', '16px');
  renderer.setStyle(labelTextElement, 'color', 'rgba(0, 0, 0, 0.76)');
  return labelTextElement;
}

export const getValueElement = (renderer: Renderer2, sanitizer: DomSanitizer, value: string, settings = defaultValueSettings): HTMLElement => {
  const valueElement: HTMLElement = renderer.createElement('div');
  renderer.setProperty(valueElement, 'innerHTML', sanitizer.sanitize(SecurityContext.HTML, value));
  renderer.setStyle(valueElement, 'flex', '1');
  renderer.setStyle(valueElement, 'text-align', 'end');
  renderer.setStyle(valueElement, 'font-family', settings.tooltipDateFont.family);
  renderer.setStyle(valueElement, 'font-size', settings.tooltipDateFont.size + settings.tooltipDateFont.sizeUnit);
  renderer.setStyle(valueElement, 'font-style', settings.tooltipDateFont.style);
  renderer.setStyle(valueElement, 'font-weight', settings.tooltipDateFont.weight);
  renderer.setStyle(valueElement, 'line-height', settings.tooltipDateFont.lineHeight);
  renderer.setStyle(valueElement, 'color', settings.tooltipDateColor);
  return valueElement;
}
