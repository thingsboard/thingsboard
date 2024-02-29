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

import { LinearGradientObject } from 'zrender/lib/graphic/LinearGradient';
import { Interval, IntervalMath } from '@shared/models/time/time.models';
import { LabelFormatterCallback, SeriesLabelOption } from 'echarts/types/src/util/types';
import { TimeSeriesChartDataItem } from '@home/components/widget/lib/chart/time-series-chart.models';
import { CustomSeriesRenderItemParams } from 'echarts';
import { CustomSeriesRenderItemAPI, CustomSeriesRenderItemReturn } from 'echarts/types/dist/shared';
import { isNumeric } from '@core/utils';
import * as echarts from 'echarts/core';

export interface BarVisualSettings {
  color: string | LinearGradientObject;
  borderColor: string;
  borderWidth: number;
  borderRadius: number;
}

export interface BarRenderContext {
  barsCount?: number;
  barIndex?: number;
  timeInterval?: Interval;
  visualSettings?: BarVisualSettings;
  labelOption?: SeriesLabelOption;
  barStackIndex?: number;
  currentStackItems?: TimeSeriesChartDataItem[];
}

export const renderTimeSeriesBar = (params: CustomSeriesRenderItemParams, api: CustomSeriesRenderItemAPI,
                                    renderCtx: BarRenderContext): CustomSeriesRenderItemReturn => {
  const time = api.value(0) as number;
  let start = api.value(2) as number;
  const end = api.value(3) as number;
  let interval = end - start;
  const ts = start ? start : time;
  if (!start || !end || !interval) {
    interval = IntervalMath.numberValue(renderCtx.timeInterval);
    start = time - interval / 2;
  }
  const gap = 0.3;
  const barInterval = interval / (renderCtx.barsCount + gap * (renderCtx.barsCount + 3));
  const intervalGap = barInterval * gap * 2;
  const barGap = barInterval * gap;
  const value = api.value(1);
  const startTime = start + intervalGap + (barInterval + barGap) * renderCtx.barIndex;
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
    lowerLeft = api.coord([startTime, value >= 0 ? Number(value) + offset : offset]);
  } else {
    lowerLeft = api.coord([startTime, value >= 0 ? value : 0]);
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

  const zeroPos = api.coord([0, offset]);

  const style: any = {
    fill: renderCtx.visualSettings.color,
    stroke: renderCtx.visualSettings.borderColor,
    lineWidth: renderCtx.visualSettings.borderWidth
  };

  if (renderCtx.labelOption.show) {
    let position = renderCtx.labelOption.position;
    if (value < 0) {
      if (position === 'top') {
        position = 'bottom';
      } else if (position === 'bottom') {
        position = 'top';
      }
    }
    style.text = (renderCtx.labelOption.formatter as LabelFormatterCallback)({value: [null, value]} as any);
    style.textDistance = 5;
    style.textPosition = position;
    style.rich = renderCtx.labelOption.rich;
  }

  return rectShape && {
    type: 'rect',
    id: time + '',
    shape: {...rectShape, r: renderCtx.visualSettings.borderRadius},
    style,
    focus: 'series',
    transition: 'all',
    enterFrom: {
      style: { opacity: 0 },
      shape: { height: 0, y: zeroPos[1] }
    }
  };
};
