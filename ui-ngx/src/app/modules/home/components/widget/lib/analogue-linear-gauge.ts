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

import * as CanvasGauges from 'canvas-gauges';
import { WidgetContext } from '@home/models/widget-component.models';
import { TbAnalogueGauge } from '@home/components/widget/lib/analogue-gauge.models';
import {
  AnalogueLinearGaugeSettings
} from '@home/components/widget/lib/analogue-linear-gauge.models';
import { isDefined } from '@core/utils';
import tinycolor from 'tinycolor2';
import LinearGaugeOptions = CanvasGauges.LinearGaugeOptions;
import LinearGauge = CanvasGauges.LinearGauge;
import BaseGauge = CanvasGauges.BaseGauge;

// @dynamic
export class TbAnalogueLinearGauge extends TbAnalogueGauge<AnalogueLinearGaugeSettings, LinearGaugeOptions>{

  constructor(ctx: WidgetContext, canvasId: string) {
    super(ctx, canvasId);
  }

  protected prepareGaugeOptions(settings: AnalogueLinearGaugeSettings, gaugeData: LinearGaugeOptions) {
    const dataKey = this.ctx.data[0].dataKey;
    const keyColor = settings.defaultColor || dataKey.color;

    const barStrokeColor = tinycolor(keyColor).darken().setAlpha(0.6).toRgbString();
    const progressColorStart = tinycolor(keyColor).setAlpha(0.05).toRgbString();
    const progressColorEnd = tinycolor(keyColor).darken().toRgbString();

    gaugeData.barStrokeWidth = (isDefined(settings.barStrokeWidth) && settings.barStrokeWidth !== null) ? settings.barStrokeWidth : 2.5;
    gaugeData.colorBarStroke = settings.colorBarStroke || barStrokeColor;
    gaugeData.colorBar = settings.colorBar || '#fff';
    gaugeData.colorBarEnd = settings.colorBarEnd || '#ddd';
    gaugeData.colorBarProgress = settings.colorBarProgress || progressColorStart;
    gaugeData.colorBarProgressEnd = settings.colorBarProgressEnd || progressColorEnd;
  }

  protected createGauge(gaugeData: LinearGaugeOptions): BaseGauge {
    return new LinearGauge(gaugeData);
  }

}
