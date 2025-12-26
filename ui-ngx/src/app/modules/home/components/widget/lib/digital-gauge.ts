///
/// Copyright © 2016-2025 The Thingsboard Authors
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
import {
  convertLevelColorsSettingsToColorProcessor,
  DigitalGaugeSettings
} from '@home/components/widget/lib/digital-gauge.models';
import tinycolor from 'tinycolor2';
import { isDefinedAndNotNull } from '@core/utils';
import { prepareFontSettings } from '@home/components/widget/lib/settings.models';
import { CanvasDigitalGauge, CanvasDigitalGaugeOptions } from '@home/components/widget/lib/canvas-digital-gauge';
import { DatePipe } from '@angular/common';
import { IWidgetSubscription } from '@core/api/widget-api.models';
import {
  ColorProcessor,
  createValueSubscription,
  ValueFormatProcessor,
  ValueSourceType
} from '@shared/models/widget-settings.models';
import { UnitService } from '@core/services/unit.service';
import { isNotEmptyTbUnits } from '@shared/models/unit.models';
import GenericOptions = CanvasGauges.GenericOptions;

// @dynamic
export class TbCanvasDigitalGauge {

  constructor(protected ctx: WidgetContext, canvasId: string) {
    const gaugeElement = $('#' + canvasId, ctx.$container)[0];
    const settings: DigitalGaugeSettings = ctx.settings;

    this.localSettings = {};
    this.localSettings.minValue = settings.minValue || 0;
    this.localSettings.maxValue = settings.maxValue || 100;
    this.localSettings.gaugeType = settings.gaugeType || 'arc';
    this.localSettings.neonGlowBrightness = settings.neonGlowBrightness || 0;
    this.localSettings.dashThickness = settings.dashThickness || 0;
    this.localSettings.roundedLineCap = settings.roundedLineCap === true;

    const dataKey = ctx.data[0].dataKey;
    const keyColor = settings.defaultColor || dataKey.color;

    this.localSettings.unitTitle = ((settings.showUnitTitle === true) ?
      (settings.unitTitle && settings.unitTitle.length > 0 ?
        settings.unitTitle : dataKey.label) : '');

    this.localSettings.showUnitTitle = settings.showUnitTitle === true;
    this.localSettings.showTimestamp = settings.showTimestamp === true;
    this.localSettings.timestampFormat = settings.timestampFormat && settings.timestampFormat.length ?
      settings.timestampFormat : 'yyyy-MM-dd HH:mm:ss';

    this.localSettings.gaugeWidthScale = settings.gaugeWidthScale || 0.75;
    this.localSettings.gaugeColor = settings.gaugeColor || tinycolor(keyColor).setAlpha(0.2).toRgbString();

    convertLevelColorsSettingsToColorProcessor(settings, keyColor);
    this.localSettings.barColor = settings.barColor;

    this.localSettings.showTicks = settings.showTicks || false;
    this.localSettings.ticks = [];
    this.localSettings.ticksValue = settings.ticksValue || [];
    this.localSettings.tickWidth = settings.tickWidth || 4;
    this.localSettings.colorTicks = settings.colorTicks || '#666';

    this.localSettings.decimals = isDefinedAndNotNull(dataKey.decimals) ? dataKey.decimals :
      (isDefinedAndNotNull(settings.decimals) ? settings.decimals : ctx.decimals);

    this.localSettings.units = isNotEmptyTbUnits(dataKey.units) ? dataKey.units :
      (isNotEmptyTbUnits(settings.units) ? settings.units : ctx.units);

    this.localSettings.hideValue = settings.showValue !== true;
    this.localSettings.hideMinMax = settings.showMinMax !== true;
    this.localSettings.donutStartAngle = isDefinedAndNotNull(settings.donutStartAngle) ?
      -TbCanvasDigitalGauge.toRadians(settings.donutStartAngle) : null;

    this.localSettings.title = ((settings.showTitle === true) ?
      (settings.title && settings.title.length > 0 ?
        settings.title : dataKey.label) : '');

    if (!this.localSettings.unitTitle && this.localSettings.showTimestamp) {
      this.localSettings.unitTitle = ' ';
    }

    this.localSettings.titleFont = prepareFontSettings(settings.titleFont, {
      size: 12,
      style: 'normal',
      weight: '500',
      color: keyColor
    });

    this.localSettings.valueFont = prepareFontSettings(settings.valueFont, {
      size: 18,
      style: 'normal',
      weight: '500',
      color: keyColor
    });

    this.localSettings.minMaxFont = prepareFontSettings(settings.minMaxFont, {
      size: 10,
      style: 'normal',
      weight: '500',
      color: keyColor
    });

    this.localSettings.labelFont = prepareFontSettings(settings.labelFont, {
      size: 8,
      style: 'normal',
      weight: '500',
      color: keyColor
    });

    this.barColorProcessor = ColorProcessor.fromSettings(settings.barColor, this.ctx);
    this.valueFormat = ValueFormatProcessor.fromSettings(this.ctx.$injector, {
      units: this.localSettings.units,
      decimals: this.localSettings.decimals,
      ignoreUnitSymbol: true
    });

    const gaugeData: CanvasDigitalGaugeOptions = {
      renderTo: gaugeElement,

      gaugeWidthScale: this.localSettings.gaugeWidthScale,
      gaugeColor: this.localSettings.gaugeColor,

      barColorProcessor: this.barColorProcessor,
      valueFormat: this.valueFormat,

      colorTicks: this.localSettings.colorTicks,
      tickWidth: this.localSettings.tickWidth,
      ticks: this.localSettings.ticks,

      title: this.localSettings.title,
      fontTitleSize: this.localSettings.titleFont.size,
      fontTitleStyle: this.localSettings.titleFont.style,
      fontTitleWeight: this.localSettings.titleFont.weight,
      colorTitle: this.localSettings.titleFont.color,
      fontTitle: this.localSettings.titleFont.family,

      fontValueSize:  this.localSettings.valueFont.size,
      fontValueStyle: this.localSettings.valueFont.style,
      fontValueWeight: this.localSettings.valueFont.weight,
      colorValue: this.localSettings.valueFont.color,
      fontValue: this.localSettings.valueFont.family,

      fontMinMaxSize: this.localSettings.minMaxFont.size,
      fontMinMaxStyle: this.localSettings.minMaxFont.style,
      fontMinMaxWeight: this.localSettings.minMaxFont.weight,
      colorMinMax: this.localSettings.minMaxFont.color,
      fontMinMax: this.localSettings.minMaxFont.family,

      fontLabelSize: this.localSettings.labelFont.size,
      fontLabelStyle: this.localSettings.labelFont.style,
      fontLabelWeight: this.localSettings.labelFont.weight,
      colorLabel: this.localSettings.labelFont.color,
      fontLabel: this.localSettings.labelFont.family,

      minValue: this.localSettings.minValue,
      maxValue: this.localSettings.maxValue,
      gaugeType: this.localSettings.gaugeType,
      dashThickness: this.localSettings.dashThickness,
      roundedLineCap: this.localSettings.roundedLineCap,

      symbol: this.ctx.$injector.get(UnitService).getTargetUnitSymbol(this.localSettings.units),
      unitTitle: this.localSettings.unitTitle,
      showUnitTitle: this.localSettings.showUnitTitle,
      showTimestamp: this.localSettings.showTimestamp,
      hideValue: this.localSettings.hideValue,
      hideMinMax: this.localSettings.hideMinMax,

      donutStartAngle: this.localSettings.donutStartAngle,

      valueDec: this.localSettings.decimals,

      neonGlowBrightness: this.localSettings.neonGlowBrightness,

      // animations
      animation: settings.animation !== false && !ctx.isMobile,
      animationDuration: isDefinedAndNotNull(settings.animationDuration) ? settings.animationDuration : 500,
      animationRule: settings.animationRule || 'linear',

      isMobile: ctx.isMobile
    };

    this.gauge = new CanvasDigitalGauge(gaugeData).draw();
    this.init();
  }

  private localSettings: DigitalGaugeSettings;
  private ticksSourcesSubscription: IWidgetSubscription;

  private readonly barColorProcessor: ColorProcessor;
  private readonly valueFormat: ValueFormatProcessor;

  private gauge: CanvasDigitalGauge;

  private static toRadians(angle: number): number {
    return angle * (Math.PI / 180);
  }

  init() {
    let updateSetting = false;
    if (this.localSettings.showTicks && this.localSettings.ticksValue?.length) {
      this.localSettings.ticks = this.localSettings.ticksValue
        .map(tick => tick.type === ValueSourceType.constant && isFinite(tick.value) ? tick.value : null);

      createValueSubscription(
        this.ctx,
        this.localSettings.ticksValue,
        this.updateAttribute.bind(this)
      ).subscribe((subscription) => {
        this.ticksSourcesSubscription = subscription;
      });
      updateSetting = true;
    }

    if (updateSetting) {
      this.updateSetting();
    }

    this.barColorProcessor.colorUpdated?.subscribe(() => {
      this.gauge.update({} as CanvasDigitalGaugeOptions);
    });
  }

  updateAttribute(subscription: IWidgetSubscription) {
    for (const keyData of subscription.data) {
      if (keyData && keyData.data && keyData.data[0]) {
        const attrValue = keyData.data[0][1];
        if (isFinite(attrValue)) {
          for (const index of keyData.dataKey.settings.indexes) {
            this.localSettings.ticks[index] = attrValue;
          }
        }
      }
    }
    this.updateSetting();
  }

  updateSetting() {
    (this.gauge.options as CanvasDigitalGaugeOptions).ticks = this.localSettings.ticks;
    this.gauge.options = CanvasDigitalGauge.configure(this.gauge.options as CanvasDigitalGaugeOptions);
    this.gauge.update({} as CanvasDigitalGaugeOptions);
  }

  update() {
    if (this.ctx.data.length > 0) {
      const cellData = this.ctx.data[0];
      if (cellData.data.length > 0) {
        const tvPair = cellData.data[cellData.data.length -
        1];
        let timestamp: number;
        if (this.localSettings.showTimestamp) {
          timestamp = tvPair[0];
          const filter = this.ctx.$injector.get(DatePipe);
          (this.gauge.options as CanvasDigitalGaugeOptions).labelTimestamp =
            filter.transform(timestamp, this.localSettings.timestampFormat);
        }
        const value = parseFloat(tvPair[1]);
        if (value !== this.gauge.value) {
          if (!this.gauge.options.animation) {
            this.gauge._value = value;
          } else {
            delete this.gauge._value;
          }
          this.gauge.value = value;
        } else if (this.localSettings.showTimestamp && this.gauge.timestamp !== timestamp) {
          this.gauge.timestamp = timestamp;
        }
      }
    }
  }

  mobileModeChanged() {
    const animation = this.ctx.settings.animation !== false && !this.ctx.isMobile;
    this.gauge.update({animation, isMobile: this.ctx.isMobile} as CanvasDigitalGaugeOptions);
  }

  resize() {
    this.gauge.update({width: this.ctx.width, height: this.ctx.height} as GenericOptions);
  }

  destroy() {
    this.gauge.destroy();
    this.barColorProcessor.destroy();
    if (this.ticksSourcesSubscription) {
      this.ctx.subscriptionApi.removeSubscription(this.ticksSourcesSubscription.id);
    }
    this.gauge = null;
  }
}
