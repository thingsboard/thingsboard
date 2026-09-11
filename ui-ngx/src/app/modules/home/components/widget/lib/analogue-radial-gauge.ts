// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import * as CanvasGauges from 'canvas-gauges';
import {
  AnalogueRadialGaugeSettings
} from '@home/components/widget/lib/analogue-radial-gauge.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { TbAnalogueGauge } from '@home/components/widget/lib/analogue-gauge.models';
import RadialGauge = CanvasGauges.RadialGauge;
import RadialGaugeOptions = CanvasGauges.RadialGaugeOptions;
import BaseGauge = CanvasGauges.BaseGauge;

// @dynamic
export class TbAnalogueRadialGauge extends TbAnalogueGauge<AnalogueRadialGaugeSettings, RadialGaugeOptions>{

  constructor(ctx: WidgetContext, canvasId: string) {
    super(ctx, canvasId);
  }

  protected prepareGaugeOptions(settings: AnalogueRadialGaugeSettings, gaugeData: RadialGaugeOptions) {
    gaugeData.ticksAngle = settings.ticksAngle || 270;
    gaugeData.startAngle = settings.startAngle || 45;

    // colors

    gaugeData.colorNeedleCircleOuter = '#f0f0f0';
    gaugeData.colorNeedleCircleOuterEnd = '#ccc';
    gaugeData.colorNeedleCircleInner = '#e8e8e8'; // tinycolor(keyColor).lighten(30).toRgbString(),//'#e8e8e8',
    gaugeData.colorNeedleCircleInnerEnd = '#f5f5f5';

    // needle
    gaugeData.needleCircleSize = settings.needleCircleSize || 10;
    gaugeData.needleCircleInner = true;
    gaugeData.needleCircleOuter = true;

    // custom animations
    gaugeData.animationTarget = 'needle'; // 'needle' or 'plate'
  }

  protected createGauge(gaugeData: RadialGaugeOptions): BaseGauge {
    return new RadialGauge(gaugeData);
  }

}
