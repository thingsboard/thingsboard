///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import { Inject, Injectable } from '@angular/core';
import { WINDOW } from '@core/services/window.service';
import { ExceptionData } from '@app/shared/models/error.models';
import { isUndefined, isDefined } from '@core/utils';
import { WindowMessage } from '@shared/models/window-message.model';
import { TranslateService } from '@ngx-translate/core';
import { customTranslationsPrefix } from '@app/shared/models/constants';
import { DataKey, Datasource, DatasourceType, KeyInfo } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { DataKeyType } from '@app/shared/models/telemetry/telemetry.models';
import { alarmFields } from '@shared/models/alarm.models';
import { materialColors } from '@app/shared/models/material.models';

@Injectable({
  providedIn: 'root'
})
export class UtilsService {

  iframeMode = false;
  widgetEditMode = false;
  editWidgetInfo: any = null;

  constructor(@Inject(WINDOW) private window: Window,
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

  public hashCode(str: string): number {
    let hash = 0;
    let i: number;
    let char: number;
    if (str.length === 0) {
      return hash;
    }
    for (i = 0; i < str.length; i++) {
      char = str.charCodeAt(i);
      // tslint:disable-next-line:no-bitwise
      hash = ((hash << 5) - hash) + char;
      // tslint:disable-next-line:no-bitwise
      hash = hash & hash; // Convert to 32bit integer
    }
    return hash;
  }

  public objectHashCode(obj: any): number {
    let hash = 0;
    if (obj) {
      const str = JSON.stringify(obj);
      hash = this.hashCode(str);
    }
    return hash;
  }

  public processWidgetException(exception: any): ExceptionData {
    const data = this.parseException(exception, -5);
    if (this.widgetEditMode) {
      const message: WindowMessage = {
        type: 'widgetException',
        data
      };
      this.window.parent.postMessage(message, '*');
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
    let result = '';
    const translationId = customTranslationsPrefix + translationValue;
    const translation = this.translate.instant(translationId);
    if (translation !== translationId) {
      result = translation + '';
    } else {
      result = defaultValue;
    }
    return result;
  }

  public guid(): string {
    function s4(): string {
      return Math.floor((1 + Math.random()) * 0x10000)
        .toString(16)
        .substring(1);
    }
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
      s4() + '-' + s4() + s4() + s4();
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
    });
    return datasources;
  }

  public getMaterialColor(index) {
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

}
