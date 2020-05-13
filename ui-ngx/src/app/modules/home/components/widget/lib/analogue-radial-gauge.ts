///
/// Copyright © 2016-2020 The Thingsboard Authors
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
import {
  AnalogueRadialGaugeSettings,
  getAnalogueRadialGaugeSettingsSchema
} from '@home/components/widget/lib/analogue-radial-gauge.models';
import { JsonSettingsSchema } from '@shared/models/widget.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { TbAnalogueGauge } from '@home/components/widget/lib/analogue-gauge.models';
import RadialGauge = CanvasGauges.RadialGauge;
import RadialGaugeOptions = CanvasGauges.RadialGaugeOptions;
import BaseGauge = CanvasGauges.BaseGauge;

const analogueRadialGaugeSettingsSchemaValue = getAnalogueRadialGaugeSettingsSchema();

export class TbAnalogueRadialGauge extends TbAnalogueGauge<AnalogueRadialGaugeSettings,RadialGaugeOptions>{

  static get settingsSchema(): JsonSettingsSchema {
    return analogueRadialGaugeSettingsSchemaValue;
  }

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
