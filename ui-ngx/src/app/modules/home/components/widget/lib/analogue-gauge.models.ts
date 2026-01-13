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
import { FontSettings, getFontFamily } from '@home/components/widget/lib/settings.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { isDefined, isDefinedAndNotNull } from '@core/utils';
import tinycolor from 'tinycolor2';
import Highlight = CanvasGauges.Highlight;
import BaseGauge = CanvasGauges.BaseGauge;
import GenericOptions = CanvasGauges.GenericOptions;
import { TbUnit } from '@shared/models/unit.models';
import { ValueFormatProcessor } from '@shared/models/widget-settings.models';
import { UnitService } from '@core/services/unit.service';
import { DataKey } from '@shared/models/widget.models';

export type AnimationRule = 'linear' | 'quad' | 'quint' | 'cycle'
                            | 'bounce' | 'elastic' | 'dequad' | 'dequint'
                            | 'decycle' | 'debounce' | 'delastic';

export type AnimationTarget = 'needle' | 'plate';

export interface AnalogueGaugeSettings {
  formatValue: ValueFormatProcessor,
  minValue: number;
  maxValue: number;
  unitTitle: string;
  showUnitTitle: boolean;
  majorTicksCount: number;
  minorTicks: number;
  valueBox: boolean;
  valueInt: number;
  valueDec?: number;
  units?: string;
  defaultColor: string;
  colorPlate: string;
  colorMajorTicks: string;
  colorMinorTicks: string;
  colorNeedle: string;
  colorNeedleEnd: string;
  colorNeedleShadowUp: string;
  colorNeedleShadowDown: string;
  colorValueBoxRect: string;
  colorValueBoxRectEnd: string;
  colorValueBoxBackground: string;
  colorValueBoxShadow: string;
  highlights: Highlight[];
  highlightsWidth: number;
  showBorder: boolean;
  numbersFont: FontSettings;
  titleFont: FontSettings;
  unitsFont: FontSettings;
  valueFont: FontSettings;
  animation: boolean;
  animationDuration: number;
  animationRule: AnimationRule;
}

interface BaseGaugeModel extends BaseGauge {
  _value?: number;
}

export abstract class TbBaseGauge<S, O extends GenericOptions> {

  private gauge: BaseGaugeModel;
  protected formatValue: ValueFormatProcessor;

  protected constructor(protected ctx: WidgetContext, canvasId: string) {
    const gaugeElement = $('#' + canvasId, ctx.$container)[0];
    const settings: S = ctx.settings;
    const gaugeData: O = this.createGaugeOptions(gaugeElement, settings);
    this.gauge = this.createGauge(gaugeData as O).draw();
  }

  protected abstract createGaugeOptions(gaugeElement: HTMLElement, settings: S): O;

  protected abstract createGauge(gaugeData: O): BaseGaugeModel;

  update() {
    if (this.ctx.data.length > 0) {
      const cellData = this.ctx.data[0];
      if (cellData.data.length > 0) {
        const tvPair = cellData.data[cellData.data.length - 1];
        let value = parseFloat(tvPair[1]);
        if (this.formatValue) {
          value = parseFloat(this.formatValue.format(value));
        }
        if (value !== this.gauge.value) {
          if (!this.gauge.options.animation) {
            this.gauge._value = value;
          } else {
            delete this.gauge._value;
          }
          this.gauge.value = value;
        }
      }
    }
  }

  mobileModeChanged() {
    const animation = this.ctx.settings.animation !== false && !this.ctx.isMobile;
    this.gauge.update({animation} as GenericOptions);
  }

  resize() {
    if (this.ctx.width > 0 && this.ctx.height > 0) {
      this.gauge.update({width: this.ctx.width, height: this.ctx.height} as GenericOptions);
    }
  }

  destroy() {
    this.gauge.destroy();
    this.gauge = null;
  }
}

export abstract class TbAnalogueGauge<S extends AnalogueGaugeSettings, O extends GenericOptions> extends TbBaseGauge<S, O> {

  protected constructor(ctx: WidgetContext, canvasId: string) {
    super(ctx, canvasId);
  }

  protected createGaugeOptions(gaugeElement: HTMLElement, settings: S): O {

    const units = getUnits(this.ctx, settings);
    const valueDec = getValueDec(this.ctx, settings);
    this.formatValue = ValueFormatProcessor.fromSettings(this.ctx.$injector, {units, decimals: valueDec, ignoreUnitSymbol: true});
    const unitSymbols = this.ctx.$injector.get(UnitService).getTargetUnitSymbol(units);

    const minValue = Number(this.formatValue.format(settings.minValue || 0));
    const maxValue = Number(this.formatValue.format(settings.maxValue || 100));

    const dataKey = this.ctx.data[0].dataKey;
    const keyColor = settings.defaultColor || dataKey.color;

    const majorTicksCount = settings.majorTicksCount || 10;
    const total = maxValue - minValue;
    let step = (total / majorTicksCount);

    const valueInt = settings.valueInt || 3;

    step = parseFloat(parseFloat(step + '').toFixed(valueDec)) || 1;

    const majorTicks: number[] = [];
    const highlights: Highlight[] = [];
    let tick = minValue;

    let hasCustomHighlights = false;
    if (settings?.highlights?.length > 0) {
      hasCustomHighlights = true;
      settings.highlights.forEach((highlight: Highlight) => {
        highlights.push({
          from: Number(this.formatValue.format(highlight.from)),
          to: Number(this.formatValue.format(highlight.to)),
          color: highlight.color
        });
      })
    }

    while (tick <= maxValue) {
      majorTicks.push(tick);
      let nextTick = tick + step;
      nextTick = parseFloat(parseFloat(nextTick + '').toFixed(valueDec));
      if (tick < maxValue && !hasCustomHighlights) {
        const highlightColor = tinycolor(keyColor);
        const percent = (tick - minValue) / total;
        highlightColor.setAlpha(percent);
        const highlight: Highlight = {
          from: tick,
          to: nextTick,
          color: highlightColor.toRgbString()
        };
        highlights.push(highlight);
      }
      tick = nextTick;
    }

    const colorNumbers = tinycolor(keyColor).darken(20).toRgbString();

    const gaugeData: O = {
      renderTo: gaugeElement,

      /* Generic options */

      minValue,
      maxValue,
      majorTicks,
      minorTicks: settings.minorTicks || 2,
      units: unitSymbols,
      title: ((settings.showUnitTitle !== false) ?
        (settings.unitTitle && settings.unitTitle.length > 0 ?
          settings.unitTitle : dataKey.label) : ''),

      borders: settings.showBorder !== false,
      borderShadowWidth: (settings.showBorder !== false) ? 3 : 0,

      // number formats
      valueInt,
      valueDec,
      majorTicksInt: 1,
      majorTicksDec: 0,

      valueBox: settings.valueBox !== false,
      valueBoxStroke: 5,
      valueBoxWidth: 0,
      valueText: '',
      valueTextShadow: true,
      valueBoxBorderRadius: 2.5,

      // highlights
      highlights,
      highlightsWidth: (isDefined(settings.highlightsWidth) && settings.highlightsWidth !== null) ? settings.highlightsWidth : 15,

      // fonts
      fontNumbers: getFontFamily(settings.numbersFont),
      fontTitle: getFontFamily(settings.titleFont),
      fontUnits: getFontFamily(settings.unitsFont),
      fontValue: getFontFamily(settings.valueFont),

      fontNumbersSize: settings.numbersFont && settings.numbersFont.size ? settings.numbersFont.size : 18,
      fontTitleSize: settings.titleFont && settings.titleFont.size ? settings.titleFont.size : 24,
      fontUnitsSize: settings.unitsFont && settings.unitsFont.size ? settings.unitsFont.size : 22,
      fontValueSize: settings.valueFont && settings.valueFont.size ? settings.valueFont.size : 40,

      fontNumbersStyle: settings.numbersFont && settings.numbersFont.style ? settings.numbersFont.style : 'normal',
      fontTitleStyle: settings.titleFont && settings.titleFont.style ? settings.titleFont.style : 'normal',
      fontUnitsStyle: settings.unitsFont && settings.unitsFont.style ? settings.unitsFont.style : 'normal',
      fontValueStyle: settings.valueFont && settings.valueFont.style ? settings.valueFont.style : 'normal',

      fontNumbersWeight: settings.numbersFont && settings.numbersFont.weight ? settings.numbersFont.weight : '500',
      fontTitleWeight: settings.titleFont && settings.titleFont.weight ? settings.titleFont.weight : '500',
      fontUnitsWeight: settings.unitsFont && settings.unitsFont.weight ? settings.unitsFont.weight : '500',
      fontValueWeight: settings.valueFont && settings.valueFont.weight ? settings.valueFont.weight : '500',

      colorNumbers: settings.numbersFont && settings.numbersFont.color ? settings.numbersFont.color : colorNumbers,
      colorTitle: settings.titleFont && settings.titleFont.color ? settings.titleFont.color : '#888',
      colorUnits: settings.unitsFont && settings.unitsFont.color ? settings.unitsFont.color : '#888',
      colorValueText: settings.valueFont && settings.valueFont.color ? settings.valueFont.color : '#444',
      colorValueTextShadow: settings.valueFont && settings.valueFont.shadowColor ? settings.valueFont.shadowColor : 'rgba(0,0,0,0.3)',

      // colors
      colorPlate: settings.colorPlate || '#fff',
      colorMajorTicks: settings.colorMajorTicks || '#444',
      colorMinorTicks: settings.colorMinorTicks || '#666',
      colorNeedle: settings.colorNeedle || keyColor,
      colorNeedleEnd: settings.colorNeedleEnd || keyColor,

      colorValueBoxRect: settings.colorValueBoxRect || '#888',
      colorValueBoxRectEnd: settings.colorValueBoxRectEnd || '#666',
      colorValueBoxBackground: settings.colorValueBoxBackground || '#babab2',
      colorValueBoxShadow: settings.colorValueBoxShadow || 'rgba(0,0,0,1)',
      colorNeedleShadowUp: settings.colorNeedleShadowUp || 'rgba(2,255,255,0.2)',
      colorNeedleShadowDown: settings.colorNeedleShadowDown || 'rgba(188,143,143,0.45)',

      // animations
      animation: settings.animation !== false && !this.ctx.isMobile,
      animationDuration: (isDefined(settings.animationDuration) && settings.animationDuration !== null) ? settings.animationDuration : 500,
      animationRule: settings.animationRule || 'cycle',
      animatedValue: true
    } as O;

    this.prepareGaugeOptions(settings, gaugeData);
    return gaugeData;
  }

  protected abstract prepareGaugeOptions(settings: S, gaugeData: O);

}

function getValueDec(ctx: WidgetContext, _settings: AnalogueGaugeSettings): number {
  let dataKey: DataKey;
  if (ctx.data && ctx.data[0]) {
    dataKey = ctx.data[0].dataKey;
  }
  if (dataKey && isDefinedAndNotNull(dataKey.decimals)) {
    return dataKey.decimals;
  } else {
    return ctx.decimals ?? 0;
  }
}

function getUnits(ctx: WidgetContext, settings: AnalogueGaugeSettings): TbUnit {
  let dataKey: DataKey;
  if (ctx.data && ctx.data[0]) {
    dataKey = ctx.data[0].dataKey;
  }
  if (dataKey?.units) {
    return dataKey.units;
  } else {
    return settings.units ?? ctx.units;
  }
}
