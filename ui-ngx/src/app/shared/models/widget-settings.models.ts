///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { isDefinedAndNotNull, isNumber, isNumeric, parseFunction } from '@core/utils';
import { DataKey, Datasource, DatasourceData } from '@shared/models/widget.models';
import { Injector } from '@angular/core';
import { DatePipe } from '@angular/common';
import { DateAgoPipe } from '@shared/pipe/date-ago.pipe';
import { TranslateService } from '@ngx-translate/core';
import { AlarmFilterConfig } from '@shared/models/query/query.models';
import { AlarmSearchStatus } from '@shared/models/alarm.models';

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
  range = 'range',
  function = 'function'
}

export const colorTypeTranslations = new Map<ColorType, string>(
  [
    [ColorType.constant, 'widgets.color.color-type-constant'],
    [ColorType.range, 'widgets.color.color-type-range'],
    [ColorType.function, 'widgets.color.color-type-function']
  ]
);

export interface ColorRange {
  from?: number;
  to?: number;
  color: string;
}

export interface ColorSettings {
  type: ColorType;
  color: string;
  rangeList?: ColorRange[];
  colorFunction?: string;
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

export const defaultColorFunction = 'var temperature = value;\n' +
    'if (typeof temperature !== undefined) {\n' +
    '  var percent = (temperature + 60)/120 * 100;\n' +
    '  return tinycolor.mix(\'blue\', \'red\', percent).toHexString();\n' +
    '}\n' +
    'return \'blue\';';

export const cssSizeToStrSize = (size?: number, unit?: cssUnit): string => (isDefinedAndNotNull(size) ? size + '' : '0') + (unit || 'px');

export const resolveCssSize = (strSize?: string): [number, cssUnit] => {
  if (!strSize || !strSize.trim().length) {
    return [0, 'px'];
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
  let numericSize = 0;
  if (isNumeric(resolvedSize)) {
    numericSize = Number(resolvedSize);
  }
  return [numericSize, resolvedUnit];
};

type ValueColorFunction = (value: any) => string;

export abstract class ColorProcessor {

  static fromSettings(color: ColorSettings): ColorProcessor {
    const settings = color || constantColor(null);
    switch (settings.type) {
      case ColorType.constant:
        return new ConstantColorProcessor(settings);
      case ColorType.range:
        return new RangeColorProcessor(settings);
      case ColorType.function:
        return new FunctionColorProcessor(settings);
    }
  }

  color: string;

  protected constructor(protected settings: ColorSettings) {
    this.color = settings.color;
  }

  abstract update(value: any): void;

}

class ConstantColorProcessor extends ColorProcessor {
  constructor(protected settings: ColorSettings) {
    super(settings);
  }

  update(value: any): void {}
}

class RangeColorProcessor extends ColorProcessor {

  constructor(protected settings: ColorSettings) {
    super(settings);
  }

  update(value: any): void {
    this.color = this.computeFromRange(value);
  }

  private computeFromRange(value: any): string {
    if (this.settings.rangeList?.length && isDefinedAndNotNull(value) && isNumeric(value)) {
      const num = Number(value);
      for (const range of this.settings.rangeList) {
        if (RangeColorProcessor.constantRange(range) && range.from === num) {
          return range.color;
        } else if ((!isNumber(range.from) || num >= range.from) && (!isNumber(range.to) || num < range.to)) {
          return range.color;
        }
      }
    }
    return this.settings.color;
  }

  private static constantRange(range: ColorRange): boolean {
    return isNumber(range.from) && isNumber(range.to) && range.from === range.to;
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
}

export interface DateFormatSettings {
  format?: string;
  lastUpdateAgo?: boolean;
  custom?: boolean;
}

export const simpleDateFormat = (format: string): DateFormatSettings => ({
  format,
  lastUpdateAgo: false,
  custom: false
});

export const lastUpdateAgoDateFormat = (): DateFormatSettings => ({
  format: null,
  lastUpdateAgo: true,
  custom: false
});

export const customDateFormat = (format: string): DateFormatSettings => ({
  format,
  lastUpdateAgo: false,
  custom: true
});

export const dateFormats = ['MMM dd yyyy HH:mm', 'dd MMM yyyy HH:mm', 'yyyy MMM dd HH:mm',
  'MM/dd/yyyy HH:mm', 'dd/MM/yyyy HH:mm', 'yyyy/MM/dd HH:mm:ss']
  .map(f => simpleDateFormat(f)).concat([lastUpdateAgoDateFormat(), customDateFormat('EEE, MMMM dd, yyyy')]);

export const compareDateFormats = (df1: DateFormatSettings, df2: DateFormatSettings): boolean => {
  if (df1 === df2) {
    return true;
  } else if (df1 && df2) {
    if (df1.lastUpdateAgo && df2.lastUpdateAgo) {
      return true;
    } else if (df1.custom && df2.custom) {
      return true;
    } else if (!df1.lastUpdateAgo && !df2.lastUpdateAgo && !df1.custom && !df2.custom) {
      return df1.format === df2.format;
    }
  }
  return false;
};

export abstract class DateFormatProcessor {

  static fromSettings($injector: Injector, settings: DateFormatSettings): DateFormatProcessor {
    if (settings.lastUpdateAgo) {
      return new LastUpdateAgoDateFormatProcessor($injector, settings);
    } else {
      return new SimpleDateFormatProcessor($injector, settings);
    }
  }

  formatted = '&nbsp;';

  protected constructor(protected $injector: Injector,
                        protected settings: DateFormatSettings) {
  }

  abstract update(ts: string | number | Date): void;

}

export class SimpleDateFormatProcessor extends DateFormatProcessor {

  private datePipe: DatePipe;

  constructor(protected $injector: Injector,
              protected settings: DateFormatSettings) {
    super($injector, settings);
    this.datePipe = $injector.get(DatePipe);
  }

  update(ts: string| number | Date): void {
    this.formatted = this.datePipe.transform(ts, this.settings.format);
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

  update(ts: string| number | Date): void {
    this.formatted = this.translate.instant('date.last-update-n-ago-text',
      {agoText: this.dateAgoPipe.transform(ts, {applyAgo: true, short: true, textPart: true})});
  }

}

export enum BackgroundType {
  image = 'image',
  imageUrl = 'imageUrl',
  color = 'color'
}

export const backgroundTypeTranslations = new Map<BackgroundType, string>(
  [
    [BackgroundType.image, 'widgets.background.background-type-image'],
    [BackgroundType.imageUrl, 'widgets.background.background-type-image-url'],
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
  imageBase64?: string;
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

export const isFontSet = (font: Font): boolean => (!!font && !!font.style && !!font.weight && !!font.size && !!font.family);

export const isFontPartiallySet = (font: Font): boolean => (!!font && (!!font.style || !!font.weight || !!font.size || !!font.family));

export const backgroundStyle = (background: BackgroundSettings): ComponentStyle => {
  if (background.type === BackgroundType.color) {
    return {
      background: background.color
    };
  } else {
    const imageUrl = background.type === BackgroundType.image ? background.imageBase64 : background.imageUrl;
    if (imageUrl) {
      return {
        background: `url(${imageUrl}) no-repeat 50% 50% / cover`
      };
    } else {
      return {};
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

export const getDataKey = (datasources?: Datasource[]): DataKey => {
  if (datasources && datasources.length) {
    const dataKeys = datasources[0].dataKeys;
    if (dataKeys && dataKeys.length) {
      return dataKeys[0];
    }
  }
  return null;
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

export const getSingleTsValue = (data: Array<DatasourceData>): [number, any] => {
  if (data.length) {
    const dsData = data[0];
    if (dsData.data.length) {
      return dsData.data[0];
    }
  }
  return null;
};

export const getSingleTsValueByDataKey = (data: Array<DatasourceData>, dataKey: DataKey): [number, any] => {
  if (data.length) {
    const dsData = data.find(d => d.dataKey === dataKey);
    if (dsData?.data?.length) {
      return dsData.data[0];
    }
  }
  return null;
};

export const getLatestSingleTsValue = (data: Array<DatasourceData>): [number, any] => {
  if (data.length) {
    const dsData = data[0];
    if (dsData.data.length) {
      return dsData.data[dsData.data.length - 1];
    }
  }
  return null;
};
