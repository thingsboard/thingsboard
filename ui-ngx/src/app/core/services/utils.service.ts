///
/// Copyright © 2016-2021 The Thingsboard Authors
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

// tslint:disable-next-line:no-reference
/// <reference path="../../../../src/typings/rawloader.typings.d.ts" />

import { Inject, Injectable, NgZone } from '@angular/core';
import { WINDOW } from '@core/services/window.service';
import { ExceptionData } from '@app/shared/models/error.models';
import {
  baseUrl,
  createLabelFromDatasource,
  deepClone,
  deleteNullProperties,
  guid,
  isDefined,
  isDefinedAndNotNull, isString,
  isUndefined
} from '@core/utils';
import { WindowMessage } from '@shared/models/window-message.model';
import { TranslateService } from '@ngx-translate/core';
import { customTranslationsPrefix, i18nPrefix } from '@app/shared/models/constants';
import { DataKey, Datasource, DatasourceType, KeyInfo } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { DataKeyType } from '@app/shared/models/telemetry/telemetry.models';
import { alarmFields } from '@shared/models/alarm.models';
import { materialColors } from '@app/shared/models/material.models';
import { WidgetInfo } from '@home/models/widget-component.models';
import jsonSchemaDefaults from 'json-schema-defaults';
import materialIconsCodepoints from '!raw-loader!material-design-icons/iconfont/codepoints';
import { Observable, of, ReplaySubject } from 'rxjs';

const i18nRegExp = new RegExp(`{${i18nPrefix}:[^{}]+}`, 'g');

const predefinedFunctions: { [func: string]: string } = {
  Sin: 'return Math.round(1000*Math.sin(time/5000));',
  Cos: 'return Math.round(1000*Math.cos(time/5000));',
  Random: 'var value = prevValue + Math.random() * 100 - 50;\n' +
    'var multiplier = Math.pow(10, 2 || 0);\n' +
    'var value = Math.round(value * multiplier) / multiplier;\n' +
    'if (value < -1000) {\n' +
    '	value = -1000;\n' +
    '} else if (value > 1000) {\n' +
    '	value = 1000;\n' +
    '}\n' +
    'return value;'
};

const predefinedFunctionsList: Array<string> = [];
for (const func of Object.keys(predefinedFunctions)) {
  predefinedFunctionsList.push(func);
}

const defaultAlarmFields: Array<string> = [
  alarmFields.createdTime.keyName,
  alarmFields.originator.keyName,
  alarmFields.type.keyName,
  alarmFields.severity.keyName,
  alarmFields.status.keyName
];

const commonMaterialIcons: Array<string> = ['more_horiz', 'more_vert', 'open_in_new',
  'visibility', 'play_arrow', 'arrow_back', 'arrow_downward',
  'arrow_forward', 'arrow_upwards', 'close', 'refresh', 'menu', 'show_chart', 'multiline_chart', 'pie_chart', 'insert_chart', 'people',
  'person', 'domain', 'devices_other', 'now_widgets', 'dashboards', 'map', 'pin_drop', 'my_location', 'extension', 'search',
  'settings', 'notifications', 'notifications_active', 'info', 'info_outline', 'warning', 'list', 'file_download', 'import_export',
  'share', 'add', 'edit', 'done'];

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class UtilsService {

  iframeMode = false;
  widgetEditMode = false;
  editWidgetInfo: WidgetInfo = null;

  defaultDataKey: DataKey = {
    name: 'f(x)',
    type: DataKeyType.function,
    label: 'Sin',
    color: this.getMaterialColor(0),
    funcBody: this.getPredefinedFunctionBody('Sin'),
    settings: {},
    _hash: Math.random()
  };

  defaultDatasource: Datasource = {
    type: DatasourceType.function,
    name: DatasourceType.function,
    dataKeys: [deepClone(this.defaultDataKey)]
  };

  defaultAlarmDataKeys: Array<DataKey> = [];

  materialIcons: Array<string> = [];

  constructor(@Inject(WINDOW) private window: Window,
              private zone: NgZone,
              private translate: TranslateService) {
    let frame: Element = null;
    try {
      frame = window.frameElement;
    } catch (e) {
      // ie11 fix
    }
    if (frame) {
      this.iframeMode = true;
      const dataWidgetAttr = frame.getAttribute('data-widget');
      if (dataWidgetAttr && dataWidgetAttr.length) {
        this.editWidgetInfo = JSON.parse(dataWidgetAttr);
        this.widgetEditMode = true;
      }
    }
  }

  public getPredefinedFunctionsList(): Array<string> {
    return predefinedFunctionsList;
  }

  public getPredefinedFunctionBody(func: string): string {
    return predefinedFunctions[func];
  }

  public getDefaultDatasource(dataKeySchema: any): Datasource {
    const datasource = deepClone(this.defaultDatasource);
    if (isDefined(dataKeySchema)) {
      datasource.dataKeys[0].settings = this.generateObjectFromJsonSchema(dataKeySchema);
    }
    return datasource;
  }

  private initDefaultAlarmDataKeys() {
    for (let i = 0; i < defaultAlarmFields.length; i++) {
      const name = defaultAlarmFields[i];
      const dataKey: DataKey = {
        name,
        type: DataKeyType.alarm,
        label: this.translate.instant(alarmFields[name].name),
        color: this.getMaterialColor(i),
        settings: {},
        _hash: Math.random()
      };
      this.defaultAlarmDataKeys.push(dataKey);
    }
  }

  public getDefaultAlarmDataKeys(): Array<DataKey> {
    if (!this.defaultAlarmDataKeys.length) {
      this.initDefaultAlarmDataKeys();
    }
    return deepClone(this.defaultAlarmDataKeys);
  }

  public generateObjectFromJsonSchema(schema: any): any {
    const obj = jsonSchemaDefaults(schema);
    deleteNullProperties(obj);
    return obj;
  }

  public processWidgetException(exception: any): ExceptionData {
    const data = this.parseException(exception, -6);
    if (this.widgetEditMode) {
      const message: WindowMessage = {
        type: 'widgetException',
        data
      };
      this.window.parent.postMessage(JSON.stringify(message), '*');
    }
    return data;
  }

  public parseException(exception: any, lineOffset?: number): ExceptionData {
    const data: ExceptionData = {};
    if (exception) {
      if (typeof exception === 'string') {
        data.message = exception;
      } else if (exception instanceof String) {
        data.message = exception.toString();
      } else {
        if (exception.name) {
          data.name = exception.name;
        } else {
          data.name = 'UnknownError';
        }
        if (exception.message) {
          data.message = exception.message;
        }
        if (exception.lineNumber) {
          data.lineNumber = exception.lineNumber;
          if (exception.columnNumber) {
            data.columnNumber = exception.columnNumber;
          }
        } else if (exception.stack) {
          const lineInfoRegexp = /(.*<anonymous>):(\d*)(:)?(\d*)?/g;
          const lineInfoGroups = lineInfoRegexp.exec(exception.stack);
          if (lineInfoGroups != null && lineInfoGroups.length >= 3) {
            if (isUndefined(lineOffset)) {
              lineOffset = -2;
            }
            data.lineNumber = Number(lineInfoGroups[2]) + lineOffset;
            if (lineInfoGroups.length >= 5) {
              data.columnNumber = Number(lineInfoGroups[4]);
            }
          }
        }
      }
    }
    return data;
  }

  public customTranslation(translationValue: string, defaultValue: string): string {
    if (translationValue && isString(translationValue)) {
      if (translationValue.includes(`{${i18nPrefix}`)) {
        const matches = translationValue.match(i18nRegExp);
        let result = translationValue;
        for (const match of matches) {
          const translationId = match.substring(6, match.length - 1);
          result = result.replace(match, this.doTranslate(translationId, match));
        }
        return result;
      } else {
        return this.doTranslate(translationValue, defaultValue, customTranslationsPrefix);
      }
    } else {
      return translationValue;
    }
  }

  private doTranslate(translationValue: string, defaultValue: string, prefix?: string): string {
    let result: string;
    let translationId;
    if (prefix) {
      translationId = prefix + translationValue;
    } else {
      translationId = translationValue;
    }
    const translation = this.translate.instant(translationId);
    if (translation !== translationId) {
      result = translation + '';
    } else {
      result = defaultValue;
    }
    return result;
  }

  public guid(): string {
    return guid();
  }

  public validateDatasources(datasources: Array<Datasource>): Array<Datasource> {
    datasources.forEach((datasource) => {
      // @ts-ignore
      if (datasource.type === 'device') {
        datasource.type = DatasourceType.entity;
        datasource.entityType = EntityType.DEVICE;
        if (datasource.deviceId) {
          datasource.entityId = datasource.deviceId;
        } else if (datasource.deviceAliasId) {
          datasource.entityAliasId = datasource.deviceAliasId;
        }
        if (datasource.deviceName) {
          datasource.entityName = datasource.deviceName;
        }
      }
      if (datasource.type === DatasourceType.entity && datasource.entityId) {
        datasource.name = datasource.entityName;
      }
      if (!datasource.dataKeys) {
        datasource.dataKeys = [];
      }
    });
    return datasources;
  }

  public getMaterialIcons(): Observable<Array<string>> {
    if (this.materialIcons.length) {
      return of(this.materialIcons);
    } else {
      const materialIconsSubject = new ReplaySubject<Array<string>>();
      this.zone.runOutsideAngular(() => {
        const codepointsArray = materialIconsCodepoints
          .split('\n')
          .filter((codepoint) => codepoint && codepoint.length);
        codepointsArray.forEach((codepoint) => {
          const values = codepoint.split(' ');
          if (values && values.length === 2) {
            this.materialIcons.push(values[0]);
          }
        });
        materialIconsSubject.next(this.materialIcons);
      });
      return materialIconsSubject.asObservable();
    }
  }

  public getCommonMaterialIcons(): Array<string> {
    return commonMaterialIcons;
  }

  public getMaterialColor(index: number) {
    const colorIndex = index % materialColors.length;
    return materialColors[colorIndex].value;
  }

  public createKey(keyInfo: KeyInfo, type: DataKeyType, index: number = -1): DataKey {
    let label;
    if (type === DataKeyType.alarm && !keyInfo.label) {
      const alarmField = alarmFields[keyInfo.name];
      if (alarmField) {
        label = this.translate.instant(alarmField.name);
      }
    }
    if (!label) {
      label = keyInfo.label || keyInfo.name;
    }
    const dataKey: DataKey = {
      name: keyInfo.name,
      type,
      label,
      funcBody: keyInfo.funcBody,
      settings: {},
      _hash: Math.random()
    };
    if (keyInfo.units) {
      dataKey.units = keyInfo.units;
    }
    if (isDefined(keyInfo.decimals)) {
      dataKey.decimals = keyInfo.decimals;
    }
    if (keyInfo.color) {
      dataKey.color = keyInfo.color;
    } else if (index > -1) {
      dataKey.color = this.getMaterialColor(index);
    }
    if (keyInfo.postFuncBody && keyInfo.postFuncBody.length) {
      dataKey.usePostProcessing = true;
      dataKey.postFuncBody = keyInfo.postFuncBody;
    }
    return dataKey;
  }

  /* public createAdditionalDataKey(dataKey: DataKey, datasource: Datasource, timeUnit: string,
    datasources: Datasource[], additionalKeysNumber: number): DataKey {
    const additionalDataKey = deepClone(dataKey);
    if (dataKey.settings.comparisonSettings.comparisonValuesLabel) {
      additionalDataKey.label = createLabelFromDatasource(datasource, dataKey.settings.comparisonSettings.comparisonValuesLabel);
    } else {
      additionalDataKey.label = dataKey.label + ' ' + this.translate.instant('legend.comparison-time-ago.' + timeUnit);
    }
    additionalDataKey.pattern = additionalDataKey.label;
    if (dataKey.settings.comparisonSettings.color) {
      additionalDataKey.color = dataKey.settings.comparisonSettings.color;
    } else {
      const index = datasources.map((_datasource) => datasource.dataKeys.length)
        .reduce((previousValue, currentValue) => previousValue + currentValue, 0);
      additionalDataKey.color = this.getMaterialColor(index + additionalKeysNumber);
    }
    additionalDataKey._hash = Math.random();
    return additionalDataKey;
  }*/

  public createLabelFromDatasource(datasource: Datasource, pattern: string): string {
    return createLabelFromDatasource(datasource, pattern);
  }

  public generateColors(datasources: Array<Datasource>) {
    let index = 0;
    datasources.forEach((datasource) => {
      datasource.dataKeys.forEach((dataKey) => {
        if (!dataKey.color) {
          dataKey.color = this.getMaterialColor(index);
        }
        index++;
      });
    });
  }

  public currentPerfTime(): number {
    return this.window.performance && this.window.performance.now ?
      this.window.performance.now() : Date.now();
  }

  public getQueryParam(name: string): string {
    const url = this.window.location.href;
    name = name.replace(/[\[\]]/g, '\\$&');
    const regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)');
    const results = regex.exec(url);
    if (!results) {
      return null;
    }
    if (!results[2]) {
      return '';
    }
    return decodeURIComponent(results[2].replace(/\+/g, ' '));
  }

  public updateQueryParam(name: string, value: string | null) {
    const baseUrlPart = [baseUrl(), this.window.location.pathname].join('');
    const urlQueryString = this.window.location.search;
    let newParam = '';
    let params = '';
    if (value !== null) {
      newParam = name + '=' + value;
    }
    if (urlQueryString) {
      const keyRegex = new RegExp('([\?&])' + name + '[^&]*');
      if (urlQueryString.match(keyRegex) !== null) {
        if (newParam) {
          newParam = '$1' + newParam;
        }
        params = urlQueryString.replace(keyRegex, newParam);
      } else if (newParam) {
        params = urlQueryString + '&' + newParam;
      }
    } else if (newParam) {
      params = '?' + newParam;
    }
    this.window.history.replaceState({}, '', baseUrlPart + params);
  }

  public baseUrl(): string {
    return baseUrl();
  }

  public deepClone<T>(target: T, ignoreFields?: string[]): T {
    return deepClone(target, ignoreFields);
  }

  public isUndefined(value: any): boolean {
    return isUndefined(value);
  }

  public isDefined(value: any): boolean {
    return isDefined(value);
  }

  public defaultValue(value: any, defaultValue: any): any {
    if (isDefinedAndNotNull(value)) {
      return value;
    } else {
      return defaultValue;
    }
  }
}
