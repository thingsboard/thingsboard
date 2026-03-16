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

import {
  isDefinedAndNotNull,
  isNotEmptyStr,
  isNumber,
  isNumeric,
  isUndefinedOrNull,
  mergeDeep,
  parseFunction
} from '@core/utils';
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
import { EventEmitter, Injector } from '@angular/core';
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
import {
  IWidgetSubscription,
  WidgetSubscriptionCallbacks,
  WidgetSubscriptionOptions
} from '@core/api/widget-api.models';
import { UnitService } from '@core/services/unit.service';
import { TbUnit, TbUnitConverter } from '@shared/models/unit.models';

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

export enum ValueSourceType {
  constant = 'constant',
  latestKey = 'latestKey',
  entity = 'entity'
}

export const ValueSourceTypes = Object.keys(ValueSourceType) as ValueSourceType[];

export const ValueSourceTypeTranslation = new Map<ValueSourceType, string>(
  [
    [ValueSourceType.constant, 'widgets.value-source.type-constant'],
    [ValueSourceType.latestKey, 'widgets.value-source.type-latest-key'],
    [ValueSourceType.entity, 'widgets.value-source.type-entity']
  ]
);

export interface ValueSourceConfig {
  type: ValueSourceType;
  value?: number;
  latestKeyType?: DataKeyType.attribute | DataKeyType.timeseries;
  latestKey?: string;
  entityKeyType?: DataKeyType.attribute | DataKeyType.timeseries;
  entityAlias?: string;
  entityKey?: string;
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

interface AdvancedColorMode {
  advancedMode?: boolean;
}

export interface ColorRange {
  from?: number;
  to?: number;
  color: string;
}

export interface AdvancedColorRange {
  from?: ValueSourceConfig;
  to?: ValueSourceConfig;
  color: string;
}

export interface ColorRangeSettings extends AdvancedColorMode {
  range?: ColorRange[];
  rangeAdvanced?: AdvancedColorRange[];
}

export interface ColorGradientSettings extends AdvancedColorMode {
  gradient?: string[];
  gradientAdvanced?: AdvancedGradient[];
  minValue?: number;
  maxValue?: number;
}

export interface AdvancedGradient {
  source: ValueSourceConfig;
  color: string;
}

export interface ColorSettings {
  type: ColorType;
  color: string;
  rangeList?: ColorRangeSettings;
  gradient?: ColorGradientSettings;
  colorFunction?: string;
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

export const filterIncludingColorRanges = (ranges: Array<ColorRange> | ColorRangeSettings): Array<ColorRange> => {
  const result = [...(Array.isArray(ranges) ? ranges : ranges.range)];
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

export const gradientColor = (defaultColor: string, colors: string[], minValue?: number, maxValue?: number): ColorSettings => ({
  type: ColorType.gradient,
  color: defaultColor,
  colorFunction: defaultColorFunction,
  gradient: {
    advancedMode: false,
    gradient: colors,
    minValue: isDefinedAndNotNull(minValue) ? minValue : 0,
    maxValue: isDefinedAndNotNull(maxValue) ? maxValue : 100
  }
});

export const defaultGradient = (minValue?: number, maxValue?: number): ColorGradientSettings => ({
  advancedMode: false,
  gradient: ['rgba(0, 255, 0, 1)', 'rgba(255, 0, 0, 1)'],
  gradientAdvanced: [
    {
      source: {type: ValueSourceType.constant},
      color: 'rgba(0, 255, 0, 1)'
    },
    {
      source: {type: ValueSourceType.constant},
      color: 'rgba(255, 0, 0, 1)'
    }
  ],
  minValue: isDefinedAndNotNull(minValue) ? minValue : 0,
  maxValue: isDefinedAndNotNull(maxValue) ? maxValue : 100
});

export const defaultRange = (): ColorRangeSettings => ({
  advancedMode: false,
  range: [],
  rangeAdvanced: []
});

const updateGradientMinMaxValues = (colorSettings: ColorSettings, minValue?: number, maxValue?: number): void => {
  if (isDefinedAndNotNull(colorSettings.gradient)) {
    if (isDefinedAndNotNull(minValue)) {
      colorSettings.gradient.minValue = minValue;
    }
    if (isDefinedAndNotNull(maxValue)) {
      colorSettings.gradient.maxValue = maxValue;
    }
  }
};

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
  const unitMatch = strSize.match(new RegExp(`(${cssUnits.join('|')})$`));
  if (unitMatch) {
    resolvedUnit = unitMatch[0] as cssUnit;
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

export interface ColorProcessorSettings {
  settings: ColorSettings;
  ctx?: WidgetContext;
  minGradientValue?: number;
  maxGradientValue?: number;
}

export abstract class ColorProcessor {

  static fromSettings(color: ColorSettings, ctx?: WidgetContext): ColorProcessor {
    return ColorProcessor.fromColorProcessorSettings({
      settings: color,
      ctx
    });
  }

  static fromColorProcessorSettings(setting: ColorProcessorSettings): ColorProcessor {
    const settings = setting.settings || constantColor(null);
    switch (settings.type) {
      case ColorType.constant:
        return new ConstantColorProcessor(settings);
      case ColorType.range:
        return new RangeColorProcessor(settings, setting.ctx);
      case ColorType.function:
        return new FunctionColorProcessor(settings);
      case ColorType.gradient:
        updateGradientMinMaxValues(settings, setting.minGradientValue, setting.maxGradientValue);
        return new GradientColorProcessor(settings, setting.ctx);
      default:
        return new ConstantColorProcessor(settings);
    }
  }

  color: string;

  colorUpdated?: EventEmitter<void>;

  protected constructor(protected settings: ColorSettings) {
    this.color = settings.color;
  }

  abstract update(value: any): void;
  destroy(): void {};
}

class ConstantColorProcessor extends ColorProcessor {
  constructor(protected settings: ColorSettings) {
    super(settings);
  }

  update(value: any): void {}
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
}

export abstract class AdvancedModeColorProcessor extends ColorProcessor {

  protected sourcesSubscription: IWidgetSubscription;
  protected advancedMode: boolean;
  private currentValue: number;

  colorUpdated = new EventEmitter<void>();

  protected constructor(protected settings: ColorSettings,
                        protected ctx: WidgetContext) {
    super(settings);
    this.advancedMode = this.getCurrentConfig()?.advancedMode;
    if (this.advancedMode) {
      createValueSubscription(
        this.ctx,
        this.datasourceConfigs(),
        this.onDataUpdated.bind(this)
      ).subscribe((subscription) => {
        this.sourcesSubscription = subscription;
      });
    }
  }

  abstract updatedAdvancedData(data: Array<DatasourceData>): void;
  abstract datasourceConfigs(): Array<ValueSourceConfig>;
  abstract getCurrentConfig(): AdvancedColorMode;

  update(value: any) {
    if (this.advancedMode) {
      this.currentValue = value;
    }
  }

  destroy(): void {
    if (this.sourcesSubscription) {
      this.ctx.subscriptionApi.removeSubscription(this.sourcesSubscription.id);
    }
    this.colorUpdated.complete();
    this.colorUpdated.unsubscribe();
    this.colorUpdated = null;
  }

  private onDataUpdated(subscription: IWidgetSubscription) {
    this.updatedAdvancedData(subscription.data);
    if (this.currentValue) {
      this.update(this.currentValue);
      this.colorUpdated.emit();
    }
  }
}

class RangeColorProcessor extends AdvancedModeColorProcessor {

  constructor(protected settings: ColorSettings,
              protected ctx: WidgetContext) {
    super(settings, ctx);
  }

  update(value: any): void {
    super.update(value);
    this.color = this.computeFromRange(value);
  }

  updatedAdvancedData(data: Array<DatasourceData>) {
    for (const keyData of data) {
      if (keyData && keyData.data && keyData.data[0]) {
        const attrValue = keyData.data[0][1];
        if (isFinite(attrValue)) {
          for (const currentIndex of keyData.dataKey.settings.indexes) {
            const index = Math.floor(currentIndex / 2);
            if (index === currentIndex / 2) {
              this.settings.rangeList.rangeAdvanced[index].from.value = attrValue;
            } else {
              this.settings.rangeList.rangeAdvanced[index].to.value = attrValue;
            }
          }
        }
      }
    }
  }

  datasourceConfigs(): Array<ValueSourceConfig> {
    const configs: Array<ValueSourceConfig> = [];
    for (const range of this.settings.rangeList.rangeAdvanced) {
      if (range.from) {
        configs.push(range.from);
      }
      if (range.to) {
        configs.push(range.to);
      }
    }
    return configs;
  }

  getCurrentConfig(): AdvancedColorMode {
    return this.settings.rangeList;
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

    if (rangeList?.length && isDefinedAndNotNull(value) && isNumeric(value)) {
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

  public static constantRange(range: ColorRange): boolean {
    return isNumber(range.from) && isNumber(range.to) && range.from === range.to;
  }
  public static constantAdvancedRange(range: AdvancedColorRange): boolean {
    return isNumber(range.from.value) && isNumber(range.to.value) && range.from.value === range.to.value;
  }
}

class GradientColorProcessor extends AdvancedModeColorProcessor {

  private readonly minValue: number;
  private readonly maxValue: number;

  constructor(protected settings: ColorSettings,
              protected ctx: WidgetContext) {
    super(settings, ctx);
    this.minValue = isDefinedAndNotNull(settings.gradient.minValue) ? settings.gradient.minValue : 0;
    this.maxValue = isDefinedAndNotNull(settings.gradient.maxValue) ? settings.gradient.maxValue : 100;
  }

  update(value: any): void {
    const progress = this.calculateProgress(+value, this.minValue, this.maxValue);
    super.update(progress);
    this.color = this.getGradientColor(progress,
      this.advancedMode ? this.settings.gradient.gradientAdvanced : this.settings.gradient.gradient);
  }

  updatedAdvancedData(data: Array<DatasourceData>) {
    for (const keyData of data) {
      if (keyData && keyData.data && keyData.data[0]) {
        const attrValue = keyData.data[0][1];
        if (isFinite(attrValue)) {
          for (const index of keyData.dataKey.settings.indexes) {
            this.settings.gradient.gradientAdvanced[index].source.value = attrValue;
          }
        }
      }
    }
  }

  datasourceConfigs(): Array<ValueSourceConfig> {
    const configs: Array<ValueSourceConfig> = [];
    for (const gradient of this.settings.gradient.gradientAdvanced) {
      configs.push(gradient.source);
    }
    return configs;
  }

  getCurrentConfig(): AdvancedColorMode {
    return this.settings.gradient;
  }

  private calculateProgress(current: number, min: number, max: number) {
    if (current < min) {
      return 0;
    } else if (current > max) {
      return 1;
    }
    return (current - min) / (max - min);
  }

  private getGradientColor(progress: number, levelColors: Array<AdvancedGradient | string>): string {
    const colorsCount = levelColors.length;
    const inc = colorsCount > 1 ? (1 / (colorsCount - 1)) : 1;
    const colorsRange = [];
    levelColors.forEach((levelColor, i) => {
      const color = typeof levelColor === 'string' ? levelColor : levelColor.color;
      if (color !== null) {
        const tColor = tinycolor(color);
        colorsRange.push({
          pct:  typeof levelColor !== 'string' ?
            this.calculateProgress(+levelColor.source.value, this.minValue, this.maxValue) : inc * i,
          color: tColor.toRgb(),
          alpha: tColor.getAlpha(),
          rgbString: tColor.toRgbString()
        });
      }
    });
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

export interface ValueFormatSettings {
  decimals?: number;
  units?: TbUnit;
  showZeroDecimals?: boolean;
  ignoreUnitSymbol?: boolean;
}

export abstract class ValueFormatProcessor {

  protected isDefinedDecimals: boolean;
  protected hideZeroDecimals: boolean;
  protected unitSymbol: string;

  static fromSettings($injector: Injector, settings: ValueFormatSettings): ValueFormatProcessor;
  static fromSettings(unitService: UnitService, settings: ValueFormatSettings): ValueFormatProcessor;
  static fromSettings(unitServiceOrInjector: Injector | UnitService, settings: ValueFormatSettings): ValueFormatProcessor {
    if (settings.units !== null && typeof settings.units === 'object') {
      return new UnitConverterValueFormatProcessor(unitServiceOrInjector, settings)
    }
    return new SimpleValueFormatProcessor(settings);
  }

  protected constructor(protected settings: ValueFormatSettings) {
  }

  abstract format(value: any): string;

  protected formatValue(value: number): string {
    let formatted: number | string = value;
    if (this.isDefinedDecimals) {
      formatted = formatted.toFixed(this.settings.decimals);
    }
    if (this.hideZeroDecimals) {
      formatted = Number(formatted);
    }
    formatted = formatted.toString();
    if (this.unitSymbol) {
      formatted += ` ${this.unitSymbol}`;
    }
    return formatted;
  }
}

export class SimpleValueFormatProcessor extends ValueFormatProcessor {

  constructor(protected settings: ValueFormatSettings) {
    super(settings);
    this.unitSymbol = !settings.ignoreUnitSymbol && isNotEmptyStr(settings.units) ? (settings.units as string) : null;
    this.isDefinedDecimals = isDefinedAndNotNull(settings.decimals);
    this.hideZeroDecimals = !settings.showZeroDecimals;
  }

  format(value: any): string {
    if (isDefinedAndNotNull(value) && isNumeric(value) && (this.isDefinedDecimals || this.unitSymbol || Number(value).toString() === value)) {
      return this.formatValue(Number(value));
    }
    return value ?? '';
  }
}

export class UnitConverterValueFormatProcessor extends ValueFormatProcessor {

  private readonly unitConverter: TbUnitConverter;

  constructor(protected unitServiceOrInjector: Injector | UnitService,
              protected settings: ValueFormatSettings) {
    super(settings);
    const unitService = this.unitServiceOrInjector instanceof UnitService ? this.unitServiceOrInjector : this.unitServiceOrInjector.get(UnitService);
    const unit = settings.units;
    this.unitSymbol = settings.ignoreUnitSymbol ? null : unitService.getTargetUnitSymbol(unit);
    this.unitConverter = unitService.geUnitConverter(unit);

    this.isDefinedDecimals = isDefinedAndNotNull(settings.decimals);
    this.hideZeroDecimals = !settings.showZeroDecimals;
  }

  format(value: any): string {
    if (isDefinedAndNotNull(value) && isNumeric(value)) {
      const formatted = this.unitConverter(Number(value));
      return this.formatValue(formatted);
    }
    return value ?? '';
  }
}

export const createValueFormatterFromSettings = (ctx: WidgetContext): ValueFormatProcessor => {
  let decimals = ctx.decimals;
  let units = ctx.units;
  const dataKey = getDataKey(ctx.datasources);
  if (isDefinedAndNotNull(dataKey?.decimals)) {
    decimals = dataKey.decimals;
  }
  if (dataKey?.units) {
    units = dataKey.units;
  }
  return ValueFormatProcessor.fromSettings(ctx.$injector, {units: units, decimals: decimals});
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

export const isBackgroundSettings = (background: any): background is BackgroundSettings => {
  if (background && background.type && background.overlay && background.overlay.color) {
    return true;
  } else {
    return false;
  }
};

export const colorBackground = (color: string): BackgroundSettings => ({
  type: BackgroundType.color,
  color,
  overlay: {
    enabled: false,
    color: 'rgba(255,255,255,0.72)',
    blur: 3
  }
});

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

export const createValueSubscription = (ctx: WidgetContext,
                                        datasourceConfigs: ValueSourceConfig[],
                                        onDataUpdated: WidgetSubscriptionCallbacks['onDataUpdated']): Observable<IWidgetSubscription> => {
  let datasources: Datasource[] = [];
  let index = 0;

  datasourceConfigs.forEach(config => {
    if (config.type !== ValueSourceType.constant) {
      try {
        datasources = generateDatasource(ctx, datasources, config, index);
      } catch (e) {
        return;
      }
    }
    index++;
  });

  if (datasources.length) {
    return subscribeForDatasource(ctx, datasources, onDataUpdated);
  }
  return EMPTY;
};

const generateDatasource = (ctx: WidgetContext,
                            datasources: Datasource[],
                            valueSource: ValueSourceConfig,
                            index: number): Datasource[] => {
  if (valueSource.type === ValueSourceType.latestKey) {
    if (ctx.datasources.length && isDefinedAndNotNull(ctx.datasources[0].aliasName)) {
      datasources = configureDatasource(ctx, valueSource, ctx.datasources[0].aliasName, datasources, index, true);
    } else {
      datasources = configureDatasource(ctx, valueSource, '', datasources, index, true);
    }
  } else if (valueSource.type === ValueSourceType.entity) {
    datasources = configureDatasource(ctx, valueSource, valueSource.entityAlias, datasources, index, false);
  }
  return datasources;
};

const configureDatasource = (ctx: WidgetContext,
                             valueSource: ValueSourceConfig,
                             entityAlias: string,
                             datasources: Datasource[],
                             index: number,
                             isLatest: boolean): Datasource[] => {
  const indexes = [index];
  const entityAliasId = ctx.aliasController.getEntityAliasId(entityAlias);
  let datasource = datasources.find(d =>
    entityAlias ? (d.entityAliasId === entityAliasId) : (d.deviceId === ctx.datasources[0].deviceId)
  );
  const dataKey: DataKey = {
    type: isLatest ? valueSource.latestKeyType : valueSource.entityKeyType,
    name: isLatest ? valueSource.latestKey : valueSource.entityKey,
    label: isLatest ? valueSource.latestKey : valueSource.entityKey,
    settings: {indexes}
  };
  if (datasource) {
    const findDataKey = datasource.dataKeys.find(dataKeyIteration =>
      dataKeyIteration.name === (isLatest ? valueSource.latestKey : valueSource.entityKey));
    if (findDataKey) {
      findDataKey.settings.indexes.push(index);
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
        dataKeys: [dataKey]
      };
    } else {
      datasource = {
        type: DatasourceType.device,
        name: ctx.datasources[0].name,
        deviceName: ctx.datasources[0].name,
        deviceId: ctx.datasources[0].deviceId,
        dataKeys: [dataKey]
      };
    }
    datasources.push(datasource);
  }
  return datasources;
};


const subscribeForDatasource = (ctx: WidgetContext,
                                datasource: Datasource[],
                                onDataUpdated: WidgetSubscriptionCallbacks['onDataUpdated']): Observable<IWidgetSubscription> => {
  if (!datasource.length) {
    return EMPTY;
  }

  const subscriptionOptions: WidgetSubscriptionOptions = {
    datasources: datasource,
    useDashboardTimewindow: false,
    type: widgetType.latest,
    callbacks: {
      onDataUpdated
    }
  };

  return ctx.subscriptionApi.createSubscription(subscriptionOptions, true);
};
