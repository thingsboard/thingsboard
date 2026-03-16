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

import { LinearGradientObject } from 'zrender/lib/graphic/LinearGradient';
import { Interval, IntervalMath } from '@shared/models/time/time.models';
import { LabelFormatterCallback } from 'echarts/types/src/util/types';
import {
  TimeSeriesChartDataItem,
  TimeSeriesChartNoAggregationBarWidthStrategy
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { CustomSeriesRenderItemParams } from 'echarts';
import { CallbackDataParams, CustomSeriesRenderItemAPI, CustomSeriesRenderItemReturn } from 'echarts/types/dist/shared';
import { isNumeric } from '@core/utils';
import * as echarts from 'echarts/core';
import { BarSeriesLabelOption } from 'echarts/types/src/chart/bar/BarSeries';

export interface BarVisualSettings {
  color: string | LinearGradientObject;
  borderColor: string;
  borderWidth: number;
  borderRadius: number;
}

export interface BarRenderSharedContext {
  barGap: number;
  intervalGap: number;
  timeInterval: Interval;
  noAggregationBarWidthStrategy: TimeSeriesChartNoAggregationBarWidthStrategy;
  noAggregationWidthRelative: boolean;
  noAggregationWidth: number;
}

export interface BarRenderContext {
  shared: BarRenderSharedContext;
  barsCount?: number;
  barIndex?: number;
  noAggregation?: boolean;
  visualSettings?: BarVisualSettings;
  labelOption?: BarSeriesLabelOption;
  additionalLabelOption?: {[key: string]: any};
  barStackIndex?: number;
  currentStackItems?: TimeSeriesChartDataItem[];
}

export const renderTimeSeriesBar = (params: CustomSeriesRenderItemParams, api: CustomSeriesRenderItemAPI,
                                    renderCtx: BarRenderContext): CustomSeriesRenderItemReturn => {
  const time = api.value(0) as number;
  let start = api.value(2) as number;
  let end = api.value(3) as number;
  let interval = end - start;
  const ts = start ? start : time;

  const separateBar = renderCtx.noAggregation &&
    renderCtx.shared.noAggregationBarWidthStrategy === TimeSeriesChartNoAggregationBarWidthStrategy.separate;

  if (renderCtx.noAggregation) {
    if (renderCtx.shared.noAggregationWidthRelative) {
      const scaleWidth = api.getWidth() / api.size([1,0])[0];
      interval = scaleWidth * (renderCtx.shared.noAggregationWidth / 100);
    } else {
      interval = renderCtx.shared.noAggregationWidth;
    }
    start = time - interval / 2;
    end = time + interval / 2;
  }
  if (!start || !end || !interval) {
    interval = IntervalMath.numberValue(renderCtx.shared.timeInterval);
    start = time - interval / 2;
  }

  const barGapRatio = renderCtx.shared.barGap;
  const intervalGapRatio = renderCtx.shared.intervalGap;
  const barInterval = separateBar
    ? interval
    : interval / (renderCtx.barsCount + barGapRatio * (renderCtx.barsCount - 1) + intervalGapRatio * 2);
  const intervalGap = barInterval * intervalGapRatio;
  const barGap = barInterval * barGapRatio;
  const value = api.value(1);
  const startTime = separateBar ? start : start + intervalGap + (barInterval + barGap) * renderCtx.barIndex;
  const delta = barInterval;
  let offset = 0;
  if (renderCtx.currentStackItems?.length) {
    for (let i = 0; i < renderCtx.barStackIndex; i++) {
      const stackItem = renderCtx.currentStackItems[i];
      const dataName = ts + '';
      const data = stackItem.data.find(d => d.name === dataName);
      if (data) {
        const val = data.value[1];
        if (isNumeric(val)) {
          offset += Number(val);
        }
      }
    }
  }
  let lowerLeft: number[];
  if (offset !== 0 && isNumeric(value)) {
    lowerLeft = api.coord([startTime, Number(value) >= 0 ? Number(value) + offset : offset]);
  } else {
    lowerLeft = api.coord([startTime, Number(value) >= 0 ? Number(value) : 0]);
  }
  const size = api.size([delta, value]);
  const width = size[0];
  const height = size[1];

  const coordSys: {x: number; y: number; width: number; height: number} = params.coordSys as any;

  const rectShape = echarts.graphic.clipRectByRect({
    x: lowerLeft[0],
    y: lowerLeft[1],
    width,
    height
  }, {
    x: coordSys.x,
    y: coordSys.y,
    width: coordSys.width,
    height: coordSys.height
  });

  const zeroCoord = api.coord([0, offset]);
  const zeroPos = Math.min(zeroCoord[1], coordSys.y + coordSys.height);

  let style: any = {
    fill: renderCtx.visualSettings.color,
    stroke: renderCtx.visualSettings.borderColor,
    lineWidth: renderCtx.visualSettings.borderWidth
  };

  if (renderCtx.labelOption.show) {
    let position = renderCtx.labelOption.position;
    if (Number(value) < 0) {
      if (position === 'top') {
        position = 'bottom';
      } else if (position === 'bottom') {
        position = 'top';
      }
    }
    style.text = (renderCtx.labelOption.formatter as LabelFormatterCallback)(
      {
        seriesName: params.seriesName,
        value: [null, value]
      } as CallbackDataParams);
    style.textDistance = 5;
    style.textPosition = position;
    style.textBackgroundColor = renderCtx.labelOption.backgroundColor;
    style.textPadding = renderCtx.labelOption.padding;
    style.textBorderRadius = renderCtx.labelOption.borderRadius;
    style.rich = renderCtx.labelOption.rich;
    if (renderCtx.additionalLabelOption) {
      style = {...style, ...renderCtx.additionalLabelOption};
    }
  }

  let borderRadius: number[];
  if (Number(value) < 0) {
    borderRadius = [0, 0, renderCtx.visualSettings.borderRadius, renderCtx.visualSettings.borderRadius];
  } else {
    borderRadius = [renderCtx.visualSettings.borderRadius, renderCtx.visualSettings.borderRadius, 0, 0];
  }
  return rectShape && {
    type: 'rect',
    id: time + '',
    shape: {...rectShape, r: borderRadius},
    style,
    focus: 'series',
    transition: 'all',
    enterFrom: {
      style: { opacity: 0 },
      shape: { height: 0, y: zeroPos }
    }
  };
};
