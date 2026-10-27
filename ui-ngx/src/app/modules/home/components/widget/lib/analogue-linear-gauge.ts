// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
