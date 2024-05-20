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

import { isDefinedAndNotNull, isNumber, isNumeric, isUndefinedOrNull, mergeDeep, parseFunction } from '@core/utils';
import {
  DataEntry,
  DataKey,
  Datasource,
  DatasourceData,
  DatasourceType,
  TargetDevice,
  TargetDeviceType,
  widgetType
} from '@shared/models/widget.models';
import { ChangeDetectorRef, EventEmitter, Injector } from '@angular/core';
import { DatePipe } from '@angular/common';
import { DateAgoPipe } from '@shared/pipe/date-ago.pipe';
import { TranslateService } from '@ngx-translate/core';
import { AlarmFilterConfig } from '@shared/models/query/query.models';
import { AlarmSearchStatus } from '@shared/models/alarm.models';
import { EMPTY, Observable, of } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { map } from 'rxjs/operators';
import { DomSanitizer } from '@angular/platform-browser';
import { AVG_MONTH, DAY, HOUR, Interval, IntervalMath, MINUTE, SECOND, YEAR } from '@shared/models/time/time.models';
import moment from 'moment';
import tinycolor from 'tinycolor2';
import { WidgetContext } from '@home/models/widget-component.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { IWidgetSubscription, WidgetSubscriptionOptions } from '@core/api/widget-api.models';

export type ComponentStyle = {[klass: string]: any};

export const cssUnits = ['px', 'em', '%', 'rem', 'pt', 'pc', 'in', 'cm', 'mm', 'ex', 'ch', 'vw', 'vh', 'vmin', 'vmax'] as const;
type cssUnitTuple = typeof cssUnits;
export type cssUnit = cssUnitTuple[number];

export const fontWeights = ['normal', 'bold', 'bolder', 'lighter', '100', '200', '300', '400', '500', '600', '700', '800', '900'] as const;
type fontWeightTuple = typeof fontWeights;
export type fontWeight = fontWeightTuple[number];

export const fontWeightTranslations = new Map<fontWeight, string>(
  [
    ['normal', 'widgets.widget-font.font-weight-normal'],
    ['bold', 'widgets.widget-font.font-weight-bold'],
    ['bolder', 'widgets.widget-font.font-weight-bolder'],
    ['lighter', 'widgets.widget-font.font-weight-lighter']
  ]
);

export const fontStyles = ['normal', 'italic', 'oblique'] as const;
type fontStyleTuple = typeof fontStyles;
export type fontStyle = fontStyleTuple[number];

export const fontStyleTranslations = new Map<fontStyle, string>(
  [
    ['normal', 'widgets.widget-font.font-style-normal'],
    ['italic', 'widgets.widget-font.font-style-italic'],
    ['oblique', 'widgets.widget-font.font-style-oblique']
  ]
);

export const commonFonts = ['Roboto', 'monospace', 'sans-serif', 'serif'];

export interface Font {
  size: number;
  sizeUnit: cssUnit;
  family: string;
  weight: fontWeight;
  style: fontStyle;
  lineHeight: string;
}

export enum ColorType {
  constant = 'constant',
  gradient = 'gradient',
  range = 'range',
  function = 'function'
}

export const colorTypeTranslations = new Map<ColorType, string>(
  [
    [ColorType.constant, 'widgets.color.color-type-constant'],
    [ColorType.gradient, 'widgets.color.color-type-gradient'],
    [ColorType.range, 'widgets.color.color-type-range'],
    [ColorType.function, 'widgets.color.color-type-function']
  ]
);

export interface ColorRange {
  from?: number;
  to?: number;
  color: string;
}

export enum ValueSourceDataKeyType {
  constant = 'constant',
  latestKey = 'latestKey',
  entity = 'entity'
}

export const ValueSourceDataKeyTypes = Object.keys(ValueSourceDataKeyType) as ValueSourceDataKeyType[];

export const ValueSourceDataKeyTypeTranslation = new Map<ValueSourceDataKeyType, string>(
  [
    [ValueSourceDataKeyType.constant, 'widgets.value-source.type-constant'],
    [ValueSourceDataKeyType.latestKey, 'widgets.value-source.type-latest-key'],
    [ValueSourceDataKeyType.entity, 'widgets.value-source.type-entity']
  ]
);

export interface ValueSourceWithDataKey {
  type: ValueSourceDataKeyType;
  value?: number;
  latestKeyType?: DataKeyType.attribute | DataKeyType.timeseries;
  latestKey?: string;
  entityKeyType?: DataKeyType.attribute | DataKeyType.timeseries;
  entityAlias?: string;
  entityKey?: string;
}

export interface AdvancedColorRange {
  from?: ValueSourceWithDataKey;
  to?: ValueSourceWithDataKey;
  color: string;
}

export const colorRangeIncludes = (range: ColorRange, toCheck: ColorRange): boolean => {
  if (isNumber(range.from) && isNumber(range.to)) {
    if (isNumber(toCheck.from) && isNumber(toCheck.to)) {
      return toCheck.from >= range.from && toCheck.to < range.to;
    } else {
      return false;
    }
  } else if (isNumber(range.from)) {
    if (isNumber(toCheck.from)) {
      return toCheck.from >= range.from;
    } else {
      return false;
    }
  } else if (isNumber(range.to)) {
    if (isNumber(toCheck.to)) {
      return toCheck.to < range.to;
    } else {
      return false;
    }
  } else {
    return false;
  }
};

export const filterIncludingColorRanges = (ranges: Array<ColorRange>): Array<ColorRange> => {
  const result = [...ranges];
  let includes = true;
  while (includes) {
    let index = -1;
    for (let i = 0; i < result.length; i++) {
      const range = result[i];
      if (result.some((value, i1) => i1 !== i && colorRangeIncludes(value, range))) {
        index = i;
        break;
      }
    }
    if (index > -1) {
      result.splice(index, 1);
    } else {
      includes = false;
    }
  }
  return result;
};

export const sortedColorRange = (ranges: Array<ColorRange>): Array<ColorRange> => ranges ? [...ranges].sort(
    (a, b) => {
      if (isNumber(a.from) && isNumber(a.to) && isNumber(b.from) && isNumber(b.to)) {
        if (b.from >= a.from && b.to < a.to) {
          return 1;
        } else if (a.from >= b.from && a.to < b.to) {
          return -1;
        } else {
          return a.from - b.from;
        }
      } else if (isNumber(a.from) && isNumber(b.from)) {
        return a.from - b.from;
      } else if (isNumber(a.to) && isNumber(b.to)) {
        return a.to - b.to;
      } else if (isNumber(a.from) && isUndefinedOrNull(b.from)) {
        return 1;
      } else if (isUndefinedOrNull(a.from) && isNumber(b.from)) {
        return -1;
      } else if (isNumber(a.to) && isUndefinedOrNull(b.to)) {
        return 1;
      } else if (isUndefinedOrNull(a.to) && isNumber(b.to)) {
        return -1;
      } else {
        return 0;
      }
    }
  ) : [];

export interface ColorSettings {
  type: ColorType;
  color: string;
  rangeList?: ColorRangeSettings;
  gradient?: ColorGradientSettings;
  colorFunction?: string;
}

export interface ColorRangeSettings {
  advancedMode?: boolean;
  range?: ColorRange[];
  rangeAdvanced?: AdvancedColorRange[];
}

export interface AdvancedGradient {
  source: ValueSourceWithDataKey;
  color: string;
}

export interface ColorGradientSettings {
  advancedMode?: boolean;
  gradient?: Array<string>;
  gradientAdvanced?: Array<AdvancedGradient>;
}

export interface DataKeySettings {
  color?: string;
  index: number;
}

export interface TimewindowStyle {
  showIcon: boolean;
  icon: string;
  iconSize: string;
  iconPosition: 'left' | 'right';
  font?: Font;
  color?: string;
  displayTypePrefix?: boolean;
}

export const defaultTimewindowStyle: TimewindowStyle = {
  showIcon: true,
  icon: 'query_builder',
  iconSize: '24px',
  iconPosition: 'left',
  displayTypePrefix: true
};

export const constantColor = (color: string): ColorSettings => ({
  type: ColorType.constant,
  color,
  colorFunction: defaultColorFunction
});

export const defaultGradient = (): ColorGradientSettings => ({
  advancedMode: false,
  gradient: ['rgba(0, 255, 0, 1)', 'rgba(255, 0, 0, 1)'],
  gradientAdvanced: [
    {
      source: {type: ValueSourceDataKeyType.constant},
      color: 'rgba(0, 255, 0, 1)'
    },
    {
      source: {type: ValueSourceDataKeyType.constant},
      color: 'rgba(255, 0, 0, 1)'
    }
  ]
});

export const defaultColorFunction = 'var temperature = value;\n' +
    'if (typeof temperature !== undefined) {\n' +
    '  var percent = (temperature + 60)/120 * 100;\n' +
    '  return tinycolor.mix(\'blue\', \'red\', percent).toHexString();\n' +
    '}\n' +
    'return \'blue\';';

export const cssSizeToStrSize = (size?: number, unit?: cssUnit): string => (isDefinedAndNotNull(size) ? size + '' : '0') + (unit || 'px');

export const resolveCssSize = (strSize?: string): [number, cssUnit] => {
  if (!strSize || !strSize.trim().length) {
    return [null, 'px'];
  }
  let resolvedUnit: cssUnit;
  let resolvedSize = strSize;
  for (const unit of cssUnits) {
    if (strSize.endsWith(unit)) {
      resolvedUnit = unit;
      break;
    }
  }
  if (resolvedUnit) {
    resolvedSize = strSize.substring(0, strSize.length - resolvedUnit.length);
  }
  resolvedUnit = resolvedUnit || 'px';
  let numericSize: number = null;
  if (isNumeric(resolvedSize)) {
    numericSize = Number(resolvedSize);
  }
  return [numericSize, resolvedUnit];
};

export const validateCssSize = (strSize?: string): string | undefined => {
  const resolved = resolveCssSize(strSize);
  if (!!resolved[0] && !!resolved[1]) {
    return cssSizeToStrSize(resolved[0], resolved[1]);
  } else {
    return undefined;
  }
};

type ValueColorFunction = (value: any) => string;

export abstract class ColorProcessor {

  static fromSettings(color: ColorSettings, ctx?: WidgetContext, cd?: ChangeDetectorRef, maxValue = 100): ColorProcessor {
    const settings = color || constantColor(null);
    switch (settings.type) {
      case ColorType.constant:
        return new ConstantColorProcessor(settings);
      case ColorType.range:
        return new RangeColorProcessor(settings, ctx, cd);
      case ColorType.function:
        return new FunctionColorProcessor(settings);
      case ColorType.gradient:
        return new GradientColorProcessor(settings, ctx, cd, maxValue);
      default:
        return new ConstantColorProcessor(settings);
    }
  }

  color: string;

  colorUpdated: EventEmitter<void> = new EventEmitter<void>();

  protected constructor(protected settings: ColorSettings) {
    this.color = settings.color;
  }

  abstract update(value: any): void;
  abstract destroy(): void;

  static generateDatasource(
    ctx: WidgetContext,
    datasources: Datasource[],
    valueSource: ValueSourceWithDataKey,
    settings: DataKeySettings): Datasource[]{
    if (valueSource.type === ValueSourceDataKeyType.latestKey) {
      if (ctx.datasources.length) {
        if (isDefinedAndNotNull(ctx.datasources[0].aliasName)) {
          datasources = this.parseDatasource(ctx, valueSource, ctx.datasources[0].aliasName, settings, datasources, true);
        } else {
          datasources = this.parseDatasource(ctx, valueSource, '', settings, datasources, true);
        }
      }
    } else if (valueSource.type === ValueSourceDataKeyType.entity) {
      datasources = this.parseDatasource(ctx, valueSource, valueSource.entityAlias, settings, datasources, false);
    }
    return datasources;
  }

  static parseDatasource(ctx: WidgetContext,
                  valueSource: ValueSourceWithDataKey,
                  entityAlias: string, settings: DataKeySettings,
                  datasources: Datasource[],
                  isLatest: boolean) {
    let dataKey: DataKey = null;
    const dataKeySettings = [settings];
    const entityAliasId = ctx.aliasController.getEntityAliasId(entityAlias);
    let datasource = datasources.find(d =>
      entityAlias ? (d.entityAliasId === entityAliasId) : (d.deviceId === ctx.datasources[0].deviceId)
    );
    dataKey = {
      type: isLatest ? valueSource.latestKeyType : valueSource.entityKeyType,
      name: isLatest ? valueSource.latestKey : valueSource.entityKey,
      label: isLatest ? valueSource.latestKey : valueSource.entityKey,
      settings: {dataKeySettings}
    };
    if (datasource) {
      const findDataKey = datasource.dataKeys.find(dataKeyIteration =>
        dataKeyIteration.name === (isLatest ? valueSource.latestKey : valueSource.entityKey));
      if (findDataKey) {
        findDataKey.settings.dataKeySettings.push(settings);
      } else {
        datasource.dataKeys.push(dataKey);
      }
    } else {
      if (entityAlias) {
        datasource = {
          type: DatasourceType.entity,
          name: entityAlias,
          aliasName: entityAlias,
          entityAliasId,
          dataKeys: [ dataKey ]
        };
      } else {
        datasource = {
          type: DatasourceType.device,
          name: ctx.datasources[0].name,
          deviceName: ctx.datasources[0].name,
          deviceId: ctx.datasources[0].deviceId,
          dataKeys: [ dataKey ]
        };
      }
      datasources.push(datasource);
    }
    return datasources;
  }

}

class ConstantColorProcessor extends ColorProcessor {
  constructor(protected settings: ColorSettings) {
    super(settings);
  }

  update(value: any): void {}
  destroy(): void {}
}

class RangeColorProcessor extends ColorProcessor {

  private advancedRangeSourcesSubscription: IWidgetSubscription;

  private progress;

  constructor(protected settings: ColorSettings,
              protected ctx: WidgetContext,
              protected cd: ChangeDetectorRef) {
    super(settings);
  }

  advancedRangeSubscribe(options: ColorRangeSettings){
    let advancedRangeDatasource: Datasource[] = [];
    let index = 0;

    function setRangeColor(rangeSetting: ValueSourceWithDataKey, color: string) {
      if (rangeSetting.type === ValueSourceDataKeyType.constant && isFinite(rangeSetting.value)) {
        index++;
      } else {
        try {
          advancedRangeDatasource = this.generateDatasource(this.ctx, advancedRangeDatasource,
            rangeSetting, {color, index});
        } catch (e) {
          return;
        }
        index++;
      }
    }

    for (const range of options.rangeAdvanced) {
      if (range.from) {
        setRangeColor.call(this, range.from, range.color);
      }
      if (range.to) {
        setRangeColor.call(this, range.to, range.color);
      }
    }

    if (advancedRangeDatasource.length) {
      this.subscribeAttributes(advancedRangeDatasource).subscribe((subscription) => {
        this.advancedRangeSourcesSubscription = subscription;
      });
    } else {
      this.color = this.computeFromRange(this.progress);
      this.colorUpdated.emit();
    }
  }

  subscribeAttributes(datasource: Datasource[]): Observable<IWidgetSubscription> {
    if (!datasource.length) {
      return EMPTY;
    }

    const levelColorsSourcesSubscriptionOptions: WidgetSubscriptionOptions = {
      datasources: datasource,
      useDashboardTimewindow: false,
      type: widgetType.latest,
      callbacks: {
        onDataUpdated: (subscription) => {
          this.updateAttribute(subscription.data);
          this.color = this.computeFromRange(this.progress);
          this.colorUpdated.emit();
        }
      }
    };

    return this.ctx.subscriptionApi.createSubscription(levelColorsSourcesSubscriptionOptions, true);
  }

  updateAttribute(data: Array<DatasourceData>) {
    for (const keyData of data) {
      if (keyData && keyData.data && keyData.data[0]) {
        const attrValue = keyData.data[0][1];
        if (isFinite(attrValue)) {
          for (const setting of keyData.dataKey.settings.dataKeySettings) {
            const index = Math.floor(setting.index/2);
            if (index === setting.index/2) {
              this.settings.rangeList.rangeAdvanced[index].from.value = attrValue;
            } else {
              this.settings.rangeList.rangeAdvanced[index].to.value = attrValue;
            }
          }
        }
      }
    }
  }

  update(value: any): void {
    this.progress = value;
    if(isDefinedAndNotNull(this.settings.rangeList?.advancedMode) && this.settings.rangeList?.advancedMode) {
      this.advancedRangeSubscribe(this.settings.rangeList);
    } else {
      this.color = this.computeFromRange(value);
    }
  }

  destroy(): void {
    if (this.advancedRangeSourcesSubscription) {
      this.ctx.subscriptionApi.removeSubscription(this.advancedRangeSourcesSubscription.id);
    }
  }

  private computeFromRange(value: any): string {
    let rangeList: any = this.settings.rangeList as Array<ColorRange>;
    let advancedMode = false;

    if (isDefinedAndNotNull(this.settings.rangeList?.advancedMode)) {
      advancedMode = this.settings.rangeList.advancedMode;
      rangeList = advancedMode ?
        this.settings.rangeList.rangeAdvanced as Array<AdvancedColorRange> :
        this.settings.rangeList.range as Array<ColorRange>;
    }

    if (rangeList.length && isDefinedAndNotNull(value) && isNumeric(value)) {
      const num = Number(value);
      for (const range of rangeList) {
        if (advancedMode ?
          (RangeColorProcessor.constantAdvancedRange(range) && range.from.value === num ) :
          (RangeColorProcessor.constantRange(range) && range.from === num)
        ) {
          return range.color;
        } else if (advancedMode ?
          ((!isNumber(range.from.value) || num >= range.from.value) && (!isNumber(range.to.value) || num < range.to.value)) :
          ((!isNumber(range.from) || num >= range.from) && (!isNumber(range.to) || num < range.to))
        ) {
          return range.color;
        }
      }
    }
    return this.settings.color;
  }

  public static constantRange(range): boolean {
    return isNumber(range.from) && isNumber(range.to) && range.from === range.to;
  }
  public static constantAdvancedRange(range): boolean {
    return isNumber(range.from.value) && isNumber(range.to.value) && range.from.value === range.to.value;
  }
}

class FunctionColorProcessor extends ColorProcessor {

  private readonly colorFunction: ValueColorFunction;

  constructor(protected settings: ColorSettings) {
    super(settings);
    this.colorFunction = parseFunction(settings.colorFunction, ['value']);
  }

  update(value: any): void {
    if (this.colorFunction) {
      this.color = this.colorFunction(value) || this.settings.color;
    }
  }

  destroy(): void {}
}

class GradientColorProcessor extends ColorProcessor {

  private progress: number;

  private advancedGradientSourcesSubscription: IWidgetSubscription;

  constructor(protected settings: ColorSettings,
              protected ctx: WidgetContext,
              protected cd: ChangeDetectorRef,
              protected maxValue: number) {
    super(settings);
  }

  update(value: any): void {
    this.progress = value / this.maxValue;
    if(isDefinedAndNotNull(this.settings.gradient.advancedMode) && this.settings.gradient.advancedMode) {
      this.advancedRangeSubscribe(this.settings.gradient.gradientAdvanced);
    } else {
      this.color = this.getProgressColor(this.progress, this.settings.gradient.gradient);
    }
  }

  destroy(): void {
    if (this.advancedGradientSourcesSubscription) {
      this.ctx.subscriptionApi.removeSubscription(this.advancedGradientSourcesSubscription.id);
    }
  }

  getProgressColor(progress: number, levelColors): string {
    const colorsCount = levelColors.length;
    const inc = colorsCount > 1 ? (1 / (colorsCount - 1)) : 1;
    const colorsRange = [];
    for (let i = 0; i < levelColors.length; i++) {
      const levelColor: any = this.settings.gradient.advancedMode ? levelColors[i].color : levelColors[i];
      if (levelColor !== null) {
        const tColor = tinycolor(levelColor);
        colorsRange.push({
          pct: this.settings.gradient.advancedMode ? levelColors[i].source.value / this.maxValue : inc * i,
          color: tColor.toRgb(),
          alpha: tColor.getAlpha(),
          rgbString: tColor.toRgbString()
        });
      }
    }

    if (progress === 0 || colorsRange.length === 1) {
      return colorsRange[0].rgbString;
    }

    for (let j = 1; j < colorsRange.length; j++) {
      if (progress <= colorsRange[j].pct) {
        const lower = colorsRange[j - 1];
        const upper = colorsRange[j];
        const range = upper.pct - lower.pct;
        const rangePct = (progress - lower.pct) / range;
        const pctLower = 1 - rangePct;
        const pctUpper = rangePct;
        const color = tinycolor({
          r: Math.floor(lower.color.r * pctLower + upper.color.r * pctUpper),
          g: Math.floor(lower.color.g * pctLower + upper.color.g * pctUpper),
          b: Math.floor(lower.color.b * pctLower + upper.color.b * pctUpper),
          a: (lower.alpha + upper.alpha)/2
        });
        return color.toRgbString();
      }
    }
    return colorsRange[colorsRange.length - 1].rgbString;
  }

  advancedRangeSubscribe(options: Array<AdvancedGradient>){
    let advancedGradientDatasource: Datasource[] = [];
    let index = 0;

    function setGradientColor(gradientSetting: ValueSourceWithDataKey) {
      if (gradientSetting.type === ValueSourceDataKeyType.constant && isFinite(gradientSetting.value)) {
        index++;
      } else {
        try {
          advancedGradientDatasource = this.generateDatasource(this.ctx, advancedGradientDatasource,
            gradientSetting, {index});
        } catch (e) {
          return;
        }
        index++;
      }
    }

    for (const gradient of options) {
      setGradientColor.call(this, gradient.source);
    }

    if (advancedGradientDatasource.length) {
      this.subscribeAttributes(advancedGradientDatasource).subscribe((subscription) => {
        this.advancedGradientSourcesSubscription = subscription;
      });
    } else {
      this.color = this.getProgressColor(this.progress, this.settings.gradient.gradientAdvanced);
      this.colorUpdated.emit();
    }
  }

  subscribeAttributes(datasource: Datasource[]): Observable<IWidgetSubscription> {
    if (!datasource.length) {
      return EMPTY;
    }

    const levelColorsSourcesSubscriptionOptions: WidgetSubscriptionOptions = {
      datasources: datasource,
      useDashboardTimewindow: false,
      type: widgetType.latest,
      callbacks: {
        onDataUpdated: (subscription) => {
          this.updateAttribute(subscription.data);
          this.color = this.getProgressColor(this.progress, this.settings.gradient.gradientAdvanced);
          this.colorUpdated.emit();
        }
      }
    };

    return this.ctx.subscriptionApi.createSubscription(levelColorsSourcesSubscriptionOptions, true);
  }

  updateAttribute(data: Array<DatasourceData>) {
    for (const keyData of data) {
      if (keyData && keyData.data && keyData.data[0]) {
        const attrValue = keyData.data[0][1];
        if (isFinite(attrValue)) {
          for (const setting of keyData.dataKey.settings.dataKeySettings) {
            this.settings.gradient.gradientAdvanced[setting.index].source.value = attrValue;
          }
        }
      }
    }
  }
}

export type FormatTimeUnit = 'millisecond' | 'second' | 'minute' | 'hour'
  | 'day' | 'month' | 'year';


export const formatTimeUnits: FormatTimeUnit[] = [
  'year', 'month', 'day', 'hour', 'minute', 'second', 'millisecond'
];

export const formatTimeUnitTranslations = new Map<FormatTimeUnit, string>(
  [
    ['year', 'date.unit-year'],
    ['month', 'date.unit-month'],
    ['day', 'date.unit-day'],
    ['hour', 'date.unit-hour'],
    ['minute', 'date.unit-minute'],
    ['second', 'date.unit-second'],
    ['millisecond', 'date.unit-millisecond']
  ]
);

export type AutoDateFormatSettings = {
  [unit in FormatTimeUnit]?: string;
};

export interface DateFormatSettings {
  format?: string;
  lastUpdateAgo?: boolean;
  custom?: boolean;
  auto?: boolean;
  hideLastUpdatePrefix?: boolean;
  autoDateFormatSettings?: AutoDateFormatSettings;
}

export const simpleDateFormat = (format: string): DateFormatSettings => ({
  format,
  lastUpdateAgo: false,
  custom: false,
  auto: false
});

export const lastUpdateAgoDateFormat = (): DateFormatSettings => ({
  format: null,
  lastUpdateAgo: true,
  custom: false,
  auto: false
});

export const customDateFormat = (format: string): DateFormatSettings => ({
  format,
  lastUpdateAgo: false,
  custom: true,
  auto: false
});

export const defaultAutoDateFormatSettings: AutoDateFormatSettings = {
  millisecond: 'MMM dd yyyy HH:mm:ss.SSS',
  second: 'MMM dd yyyy HH:mm:ss',
  minute: 'MMM dd yyyy HH:mm',
  hour: 'MMM dd yyyy HH:mm',
  day: 'MMM dd yyyy',
  month: 'MMM yyyy',
  year: 'yyyy'
};

export const autoDateFormat = (): DateFormatSettings => ({
  format: null,
  lastUpdateAgo: false,
  custom: false,
  auto: true,
  autoDateFormatSettings: {}
});

export const dateFormats = ['MMM yyyy', 'MMM dd yyyy', 'MMM dd yyyy HH:mm', 'dd MMM yyyy HH:mm', 'dd MMM yyyy HH:mm:ss',
  'yyyy MMM dd HH:mm', 'MM/dd/yyyy HH:mm', 'dd/MM/yyyy HH:mm', 'MMM dd yyyy HH:mm:ss', 'yyyy/MM/dd HH:mm:ss', 'yyyy-MM-dd HH:mm:ss',
  'MMM dd yyyy HH:mm:ss.SSS', 'yyyy-MM-dd HH:mm:ss.SSS']
  .map(f => simpleDateFormat(f)).concat([lastUpdateAgoDateFormat(), customDateFormat('EEE, MMMM dd, yyyy')]);

export const dateFormatsWithAuto = [autoDateFormat()].concat(dateFormats);

export const compareDateFormats = (df1: DateFormatSettings, df2: DateFormatSettings): boolean => {
  if (df1 === df2) {
    return true;
  } else if (df1 && df2) {
    if (df1.lastUpdateAgo && df2.lastUpdateAgo) {
      return true;
    } else if (df1.custom && df2.custom) {
      return true;
    } else if (df1.auto && df2.auto && !df1.custom && !df2.custom) {
      return true;
    } else if (!df1.lastUpdateAgo && !df2.lastUpdateAgo && !df1.custom && !df2.custom && !df1.auto && !df2.auto) {
      return df1.format === df2.format;
    }
  }
  return false;
};

export abstract class DateFormatProcessor {

  static fromSettings($injector: Injector, settings: DateFormatSettings): DateFormatProcessor {
    if (settings.lastUpdateAgo) {
      return new LastUpdateAgoDateFormatProcessor($injector, settings);
    } else if (settings.auto && !settings.custom) {
      return new AutoDateFormatProcessor($injector, settings);
    } else {
      return new SimpleDateFormatProcessor($injector, settings);
    }
  }

  formatted = '&nbsp;';

  protected constructor(protected $injector: Injector,
                        protected settings: DateFormatSettings) {
  }

  abstract update(ts: string | number | Date, interval?: Interval): string;

}

export class SimpleDateFormatProcessor extends DateFormatProcessor {

  private datePipe: DatePipe;

  constructor(protected $injector: Injector,
              protected settings: DateFormatSettings) {
    super($injector, settings);
    this.datePipe = $injector.get(DatePipe);
  }

  update(ts: string| number | Date): string {
    if (ts) {
      this.formatted = this.datePipe.transform(ts, this.settings.format);
    } else {
      this.formatted = '&nbsp;';
    }
    return this.formatted;
  }

}

export class LastUpdateAgoDateFormatProcessor extends DateFormatProcessor {

  private dateAgoPipe: DateAgoPipe;
  private translate: TranslateService;

  constructor(protected $injector: Injector,
              protected settings: DateFormatSettings) {
    super($injector, settings);
    this.dateAgoPipe = $injector.get(DateAgoPipe);
    this.translate = $injector.get(TranslateService);
  }

  update(ts: string| number | Date): string {
    if (ts) {
      const agoText = this.dateAgoPipe.transform(ts, {applyAgo: true, short: true, textPart: true});
      if (this.settings.hideLastUpdatePrefix) {
        this.formatted = agoText;
      } else {
        this.formatted = this.translate.instant('date.last-update-n-ago-text',
          {agoText});
      }
    } else {
      this.formatted = '&nbsp;';
    }
    return this.formatted;
  }

}

export class AutoDateFormatProcessor extends DateFormatProcessor {

  private datePipe: DatePipe;
  private readonly autoDateFormatSettings: AutoDateFormatSettings;

  constructor(protected $injector: Injector,
              protected settings: DateFormatSettings) {
    super($injector, settings);
    this.datePipe = $injector.get(DatePipe);
    this.autoDateFormatSettings = mergeDeep({} as AutoDateFormatSettings,
      defaultAutoDateFormatSettings, this.settings.autoDateFormatSettings);
  }

  update(ts: string| number | Date, interval?: Interval): string {
    if (ts) {
      const unit = interval ? intervalToFormatTimeUnit(interval) : tsToFormatTimeUnit(ts);
      const format = this.autoDateFormatSettings[unit];
      if (format) {
        this.formatted = this.datePipe.transform(ts, format);
      } else {
        this.formatted = '&nbsp;';
      }
    } else {
      this.formatted = '&nbsp;';
    }
    return this.formatted;
  }
}

const intervalToFormatTimeUnit = (interval: Interval): FormatTimeUnit => {
  const intervalValue = IntervalMath.numberValue(interval);
  if (intervalValue < SECOND) {
    return 'millisecond';
  } else if (intervalValue < MINUTE) {
    return 'second';
  } else if (intervalValue < HOUR) {
    return 'minute';
  } else if (intervalValue < DAY) {
    return 'hour';
  } else if (intervalValue < AVG_MONTH) {
    return 'day';
  } else if (intervalValue < YEAR) {
    return 'month';
  } else {
    return 'year';
  }
};

export const tsToFormatTimeUnit = (ts: string | number | Date): FormatTimeUnit => {
  const date = moment(ts);
  const M = date.month() + 1;
  const d = date.date();
  const h = date.hours();
  const m = date.minutes();
  const s = date.seconds();
  const S = date.milliseconds();
  const isSecond = S === 0;
  const isMinute = isSecond && s === 0;
  const isHour = isMinute && m === 0;
  const isDay = isHour && h === 0;
  const isMonth = isDay && d === 1;
  const isYear = isMonth && M === 1;
  if (isYear) {
    return 'year';
  }
  else if (isMonth) {
    return 'month';
  }
  else if (isDay) {
    return 'day';
  }
  else if (isHour) {
    return 'hour';
  }
  else if (isMinute) {
    return 'minute';
  }
  else if (isSecond) {
    return 'second';
  }
  else {
    return 'millisecond';
  }
};

export enum BackgroundType {
  image = 'image',
  color = 'color'
}

export const backgroundTypeTranslations = new Map<BackgroundType, string>(
  [
    [BackgroundType.image, 'widgets.background.background-type-image'],
    [BackgroundType.color, 'widgets.background.background-type-color']
  ]
);

export interface OverlaySettings {
  enabled: boolean;
  color: string;
  blur: number;
}

export interface BackgroundSettings {
  type: BackgroundType;
  imageUrl?: string;
  color?: string;
  overlay: OverlaySettings;
}

export const iconStyle = (size: number | string, sizeUnit: cssUnit = 'px'): ComponentStyle => {
  const iconSize = typeof size === 'number' ? size + sizeUnit : size;
  return {
    width: iconSize,
    minWidth: iconSize,
    height: iconSize,
    fontSize: iconSize,
    lineHeight: iconSize
  };
};

export const textStyle = (font?: Font, letterSpacing = 'normal'): ComponentStyle => {
  const style: ComponentStyle = {
    letterSpacing
  };
  if (font?.style) {
    style.fontStyle = font.style;
  }
  if (font?.weight) {
    style.fontWeight = font.weight;
  }
  if (font?.lineHeight) {
    style.lineHeight = font.lineHeight;
  }
  if (font?.size) {
    style.fontSize = (font.size + (font.sizeUnit || 'px'));
  }
  if (font?.family) {
    style.fontFamily = font.family +
      (font.family !== 'Roboto' ? ', Roboto' : '');
  }
  return style;
};

export const inlineTextStyle = (font?: Font, letterSpacing = 'normal'): ComponentStyle => {
  const style: ComponentStyle = {
    letterSpacing
  };
  if (font?.style) {
    style['font-style'] = font.style;
  }
  if (font?.weight) {
    style['font-weight'] = font.weight;
  }
  if (font?.lineHeight) {
    style['line-height'] = font.lineHeight;
  }
  if (font?.size) {
    style['font-size'] = (font.size + (font.sizeUnit || 'px'));
  }
  if (font?.family) {
    style['font-family'] = font.family +
      (font.family !== 'Roboto' ? ', Roboto' : '');
  }
  return style;
};

export const cssTextFromInlineStyle = (styleObj: { [key: string]: string | number }): string => Object.entries(styleObj)
  .map(([key, value]) => `${key}: ${value}`)
  .join('; ');

export const isFontSet = (font: Font): boolean => (!!font && !!font.style && !!font.weight && !!font.size && !!font.family);

export const isFontPartiallySet = (font: Font): boolean => (!!font && (!!font.style || !!font.weight || !!font.size || !!font.family));

export const validateAndUpdateBackgroundSettings = (background: BackgroundSettings): BackgroundSettings => {
  if (background) {
    if (background.type === BackgroundType.image && (background as any).imageBase64) {
      background.imageUrl = (background as any).imageBase64;
    }
    if (background.type === 'imageUrl' as any) {
      background.type = BackgroundType.image;
    }
    delete (background as any).imageBase64;
  }
  return background;
};

export const backgroundStyle = (background: BackgroundSettings, imagePipe: ImagePipe,
                                sanitizer: DomSanitizer, preview = false): Observable<ComponentStyle> => {
  background = validateAndUpdateBackgroundSettings(background);
  if (background.type === BackgroundType.color) {
    return of({
      background: background.color
    });
  } else {
    const imageUrl = background.imageUrl;
    if (imageUrl) {
      return imagePipe.transform(imageUrl, {asString: true, ignoreLoadingImage: true, preview}).pipe(
        map((transformedUrl) => ({
            background: sanitizer.bypassSecurityTrustStyle(`url(${transformedUrl}) no-repeat 50% 50% / cover`)
          }))
      );
    } else {
      return of({});
    }
  }
};

export const overlayStyle = (overlay: OverlaySettings): ComponentStyle => (
  {
    display: overlay.enabled ? 'block' : 'none',
    background: overlay.color,
    backdropFilter: `blur(${overlay.blur}px)`
  }
);

export const getDataKey = (datasources?: Datasource[], index = 0): DataKey => {
  if (datasources && datasources.length) {
    const dataKeys = datasources[0].dataKeys;
    if (dataKeys && dataKeys.length > index) {
      return dataKeys[index];
    }
  }
  return null;
};

export const updateDataKeys = (datasources: Datasource[], dataKeys: DataKey[]): void => {
  if (datasources && datasources.length) {
    datasources[0].dataKeys = dataKeys;
  }
};


export const getDataKeyByLabel = (datasources: Datasource[], label: string): DataKey => {
  if (datasources && datasources.length) {
    const dataKeys = datasources[0].dataKeys;
    if (dataKeys && dataKeys.length) {
      return dataKeys.find(k => k.label === label);
    }
  }
  return null;
};

export const updateDataKeyByLabel = (datasources: Datasource[], dataKey: DataKey, label: string): void => {
  if (datasources && datasources.length) {
    let dataKeys = datasources[0].dataKeys;
    if (!dataKeys) {
      dataKeys = [];
      datasources[0].dataKeys = dataKeys;
    }
    const existingIndex = dataKeys.findIndex(k => k.label === label || k === dataKey);
    if (dataKey) {
      dataKey.label = label;
      if (existingIndex > -1) {
        dataKeys[existingIndex] = dataKey;
      } else {
        dataKeys.push(dataKey);
      }
    } else if (existingIndex > -1) {
      dataKeys.splice(existingIndex, 1);
    }
  }
};

export const getTargetDeviceFromDatasources = (datasources?: Datasource[]): TargetDevice => {
  if (datasources && datasources.length) {
    const datasource = datasources[0];
    if (datasource?.type === DatasourceType.device) {
      return {
        type: TargetDeviceType.device,
        deviceId: datasource?.deviceId
      };
    } else if (datasource?.type === DatasourceType.entity) {
      return {
        type: TargetDeviceType.entity,
        entityAliasId: datasource?.entityAliasId
      };
    }
  }
  return null;
};

export const getAlarmFilterConfig = (datasources?: Datasource[]): AlarmFilterConfig => {
  if (datasources && datasources.length) {
    const config = datasources[0].alarmFilterConfig;
    if (config) {
      return config;
    }
  }
  return { statusList: [ AlarmSearchStatus.ACTIVE ] };
};

export const setAlarmFilterConfig = (config: AlarmFilterConfig, datasources?: Datasource[]): void => {
  if (datasources && datasources.length) {
    datasources[0].alarmFilterConfig = config;
  }
};

export const getLabel = (datasources?: Datasource[]): string => {
  const dataKey = getDataKey(datasources);
  if (dataKey) {
    return dataKey.label;
  }
  return '';
};

export const setLabel = (label: string, datasources?: Datasource[]): void => {
  const dataKey = getDataKey(datasources);
  if (dataKey) {
    dataKey.label = label;
  }
};

export const getSingleTsValue = (data: Array<DatasourceData>): DataEntry => {
  if (data.length) {
    const dsData = data[0];
    if (dsData.data.length) {
      return dsData.data[0];
    }
  }
  return null;
};

export const getSingleTsValueByDataKey = (data: Array<DatasourceData>, dataKey: DataKey): DataEntry => {
  if (data.length) {
    const dsData = data.find(d => d.dataKey === dataKey);
    if (dsData?.data?.length) {
      return dsData.data[0];
    }
  }
  return null;
};

export const getLatestSingleTsValue = (data: Array<DatasourceData>): DataEntry => {
  if (data.length) {
    const dsData = data[0];
    if (dsData.data.length) {
      return dsData.data[dsData.data.length - 1];
    }
  }
  return null;
};
