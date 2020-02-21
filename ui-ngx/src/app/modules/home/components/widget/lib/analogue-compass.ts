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

import { WidgetContext } from '@home/models/widget-component.models';
import * as CanvasGauges from 'canvas-gauges';
import {
  AnalogueCompassSettings,
  analogueCompassSettingsSchema
} from '@home/components/widget/lib/analogue-compass.models';
import { deepClone, isDefined } from '@core/utils';
import { JsonSettingsSchema } from '@shared/models/widget.models';
import { getFontFamily } from '@home/components/widget/lib/settings.models';
import { TbBaseGauge } from '@home/components/widget/lib/analogue-gauge.models';
import RadialGaugeOptions = CanvasGauges.RadialGaugeOptions;
import BaseGauge = CanvasGauges.BaseGauge;
import RadialGauge = CanvasGauges.RadialGauge;

const analogueCompassSettingsSchemaValue = analogueCompassSettingsSchema;

export class TbAnalogueCompass extends TbBaseGauge<AnalogueCompassSettings, RadialGaugeOptions> {

  static get settingsSchema(): JsonSettingsSchema {
    return analogueCompassSettingsSchemaValue;
  }

  constructor(ctx: WidgetContext, canvasId: string) {
    super(ctx, canvasId);
  }

  protected createGaugeOptions(gaugeElement: HTMLElement, settings: AnalogueCompassSettings): RadialGaugeOptions {

    const majorTicks = (settings.majorTicks && settings.majorTicks.length > 0) ? deepClone(settings.majorTicks) :
      ['N','NE','E','SE','S','SW','W','NW'];
    majorTicks.push(majorTicks[0]);

    return {
      renderTo: gaugeElement,

      // Generic options
      minValue: 0,
      maxValue: 360,
      majorTicks,
      minorTicks: settings.minorTicks || 22,
      ticksAngle: 360,
      startAngle: 180,
      strokeTicks: settings.showStrokeTicks || false,
      highlights: [],
      valueBox: false,

      // needle
      needleCircleSize: settings.needleCircleSize || 15,
      needleType: 'line',
      needleStart: 75,
      needleEnd: 99,
      needleWidth: 3,
      needleCircleOuter: false,

      // borders
      borders: settings.showBorder || false,
      borderInnerWidth: 0,
      borderMiddleWidth: 0,
      borderOuterWidth: settings.borderOuterWidth || 10,
      borderShadowWidth: 0,

      // colors
      colorPlate: settings.colorPlate || '#222',
      colorMajorTicks: settings.colorMajorTicks || '#f5f5f5',
      colorMinorTicks: settings.colorMinorTicks || '#ddd',
      colorNeedle: settings.colorNeedle || '#f08080',
      colorNeedleEnd: settings.colorNeedle || '#f08080',
      colorNeedleCircleInner: settings.colorNeedleCircle || '#e8e8e8',
      colorNeedleCircleInnerEnd: settings.colorNeedleCircle || '#e8e8e8',
      colorBorderOuter: settings.colorBorder || '#ccc',
      colorBorderOuterEnd: settings.colorBorder || '#ccc',
      colorNeedleShadowDown: '#222',

      // fonts
      fontNumbers: getFontFamily(settings.majorTickFont),
      fontNumbersSize: settings.majorTickFont && settings.majorTickFont.size ? settings.majorTickFont.size : 20,
      fontNumbersStyle: settings.majorTickFont && settings.majorTickFont.style ? settings.majorTickFont.style : 'normal',
      fontNumbersWeight: settings.majorTickFont && settings.majorTickFont.weight ? settings.majorTickFont.weight : '500',
      colorNumbers: settings.majorTickFont && settings.majorTickFont.color ? settings.majorTickFont.color : '#ccc',

      // animations
      animation: settings.animation !== false && !this.ctx.isMobile,
      animationDuration: (isDefined(settings.animationDuration) && settings.animationDuration !== null) ? settings.animationDuration : 500,
      animationRule: settings.animationRule || 'cycle',
      animationTarget: settings.animationTarget || 'needle'
    };
  }

  protected createGauge(gaugeData: RadialGaugeOptions): BaseGauge {
    return new RadialGauge(gaugeData);
  }
}
