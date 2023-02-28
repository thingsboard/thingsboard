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

import _ from 'lodash';
import { Observable, Subject } from 'rxjs';
import { finalize, share } from 'rxjs/operators';
import { Datasource, DatasourceData, FormattedData, ReplaceInfo } from '@app/shared/models/widget.models';
import { EntityId } from '@shared/models/id/entity-id';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { EntityType, baseDetailsPageByEntityType } from '@shared/models/entity-type.models';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';
import { serverErrorCodesTranslations } from '@shared/models/constants';

const varsRegex = /\${([^}]*)}/g;

export function onParentScrollOrWindowResize(el: Node): Observable<Event> {
  const scrollSubject = new Subject<Event>();
  const scrollParentNodes = scrollParents(el);
  const eventListenerObject: EventListenerObject = {
    handleEvent(evt: Event) {
      scrollSubject.next(evt);
    }
  };
  scrollParentNodes.forEach((scrollParentNode) => {
    scrollParentNode.addEventListener('scroll', eventListenerObject);
  });
  window.addEventListener('resize', eventListenerObject);
  return scrollSubject.pipe(
    finalize(() => {
      scrollParentNodes.forEach((scrollParentNode) => {
        scrollParentNode.removeEventListener('scroll', eventListenerObject);
      });
      window.removeEventListener('resize', eventListenerObject);
    }),
    share()
  );
}

export function isLocalUrl(url: string): boolean {
  const parser = document.createElement('a');
  parser.href = url;
  const host = parser.hostname;
  return host === 'localhost' || host === '127.0.0.1';
}

export function animatedScroll(element: HTMLElement, scrollTop: number, delay?: number) {
  let currentTime = 0;
  const increment = 20;
  const start = element.scrollTop;
  const to = scrollTop;
  const duration = delay ? delay : 0;
  const remaining = to - start;
  const animateScroll = () => {
    if (duration === 0) {
      element.scrollTop = to;
    } else {
      currentTime += increment;
      element.scrollTop = easeInOut(currentTime, start, remaining, duration);
      if (currentTime < duration) {
        setTimeout(animateScroll, increment);
      }
    }
  };
  animateScroll();
}

export function isUndefined(value: any): boolean {
  return typeof value === 'undefined';
}

export function isUndefinedOrNull(value: any): boolean {
  return typeof value === 'undefined' || value === null;
}

export function isDefined(value: any): boolean {
  return typeof value !== 'undefined';
}

export function isDefinedAndNotNull(value: any): boolean {
  return typeof value !== 'undefined' && value !== null;
}

export function isEmptyStr(value: any): boolean {
  return value === '';
}

export function isNotEmptyStr(value: any): boolean {
  return typeof value === 'string' && value.trim().length > 0;
}

export function isFunction(value: any): boolean {
  return typeof value === 'function';
}

export function isObject(value: any): boolean {
  return value !== null && typeof value === 'object';
}

export function isNumber(value: any): boolean {
  return typeof value === 'number';
}

export function isNumeric(value: any): boolean {
  return (value - parseFloat(value) + 1) >= 0;
}

export function isBoolean(value: any): boolean {
  return typeof value === 'boolean';
}

export function isString(value: any): boolean {
  return typeof value === 'string';
}

export function isEmpty(obj: any): boolean {
  for (const key of Object.keys(obj)) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) {
      return false;
    }
  }
  return true;
}

export function isLiteralObject(value: any) {
  return (!!value) && (value.constructor === Object);
}

export function formatValue(value: any, dec?: number, units?: string, showZeroDecimals?: boolean): string | undefined {
  if (isDefinedAndNotNull(value) && isNumeric(value) &&
    (isDefinedAndNotNull(dec) || isDefinedAndNotNull(units) || Number(value).toString() === value)) {
    let formatted: string | number = Number(value);
    if (isDefinedAndNotNull(dec)) {
      formatted = formatted.toFixed(dec);
    }
    if (!showZeroDecimals) {
      formatted = (Number(formatted));
    }
    formatted = formatted.toString();
    if (isDefinedAndNotNull(units) && units.length > 0) {
      formatted += ' ' + units;
    }
    return formatted;
  } else {
    return value !== null ? value : '';
  }
}

export function objectValues(obj: any): any[] {
  return Object.keys(obj).map(e => obj[e]);
}

export function deleteNullProperties(obj: any) {
  if (isUndefined(obj) || obj == null) {
    return;
  }
  Object.keys(obj).forEach((propName) => {
    if (obj[propName] === null || isUndefined(obj[propName])) {
      delete obj[propName];
    } else if (isObject(obj[propName])) {
      deleteNullProperties(obj[propName]);
    } else if (obj[propName] instanceof Array) {
      (obj[propName] as any[]).forEach((elem) => {
        deleteNullProperties(elem);
      });
    }
  });
}

export function objToBase64(obj: any): string {
  const json = JSON.stringify(obj);
  return btoa(encodeURIComponent(json).replace(/%([0-9A-F]{2})/g,
    function toSolidBytes(match, p1) {
      return String.fromCharCode(Number('0x' + p1));
    }));
}

export function base64toString(b64Encoded: string): string {
  return decodeURIComponent(atob(b64Encoded).split('').map((c) => {
    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
  }).join(''));
}

export function objToBase64URI(obj: any): string {
  return encodeURIComponent(objToBase64(obj));
}

export function base64toObj(b64Encoded: string): any {
  const json = decodeURIComponent(atob(b64Encoded).split('').map((c) => {
    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
  }).join(''));
  return JSON.parse(json);
}

const scrollRegex = /(auto|scroll)/;

function parentNodes(node: Node, nodes: Node[]): Node[] {
  if (node.parentNode === null) {
    return nodes;
  }
  return parentNodes(node.parentNode, nodes.concat([node]));
}

function style(el: Element, prop: string): string {
  return getComputedStyle(el, null).getPropertyValue(prop);
}

function overflow(el: Element): string {
  return style(el, 'overflow') + style(el, 'overflow-y') + style(el, 'overflow-x');
}

function isScrollNode(node: Node): boolean {
  if (node instanceof Element) {
    return scrollRegex.test(overflow(node));
  } else {
    return false;
  }
}

function scrollParents(node: Node): Node[] {
  if (!(node instanceof HTMLElement || node instanceof SVGElement)) {
    return [];
  }
  const scrollParentNodes = [];
  const nodeParents = parentNodes(node, []);
  nodeParents.forEach((nodeParent) => {
    if (isScrollNode(nodeParent)) {
      scrollParentNodes.push(nodeParent);
    }
  });
  if (document.scrollingElement) {
    scrollParentNodes.push(document.scrollingElement);
  } else if (document.documentElement) {
    scrollParentNodes.push(document.documentElement);
  }
  return scrollParentNodes;
}

export function hashCode(str: string): number {
  let hash = 0;
  let i: number;
  let char: number;
  if (str.length === 0) {
    return hash;
  }
  for (i = 0; i < str.length; i++) {
    char = str.charCodeAt(i);
    // eslint-disable-next-line no-bitwise
    hash = ((hash << 5) - hash) + char;
    // eslint-disable-next-line no-bitwise
    hash = hash & hash; // Convert to 32bit integer
  }
  return hash;
}

export function objectHashCode(obj: any): number {
  let hash = 0;
  if (obj) {
    const str = JSON.stringify(obj);
    hash = hashCode(str);
  }
  return hash;
}

function easeInOut(
  currentTime: number,
  startTime: number,
  remainingTime: number,
  duration: number) {
  currentTime /= duration / 2;

  if (currentTime < 1) {
    return (remainingTime / 2) * currentTime * currentTime + startTime;
  }

  currentTime--;
  return (
    (-remainingTime / 2) * (currentTime * (currentTime - 2) - 1) + startTime
  );
}

export function deepClone<T>(target: T, ignoreFields?: string[]): T {
  if (target === null) {
    return target;
  }
  if (target instanceof Date) {
    return new Date(target.getTime()) as any;
  }
  if (target instanceof Array) {
    const cp = [] as any[];
    (target as any[]).forEach((v) => { cp.push(v); });
    return cp.map((n: any) => deepClone<any>(n)) as any;
  }
  if (typeof target === 'object' && target !== {}) {
    const cp = {...(target as { [key: string]: any })} as { [key: string]: any };
    Object.keys(cp).forEach(k => {
      if (!ignoreFields || ignoreFields.indexOf(k) === -1) {
        cp[k] = deepClone<any>(cp[k]);
      }
    });
    return cp as T;
  }
  return target;
}

export function extractType<T extends object>(target: any, keysOfProps: (keyof T)[]): T {
  return _.pick(target, keysOfProps);
}

export function isEqual(a: any, b: any): boolean {
  return _.isEqual(a, b);
}

export function mergeDeep<T>(target: T, ...sources: T[]): T {
  return _.merge(target, ...sources);
}

export function guid(): string {
  function s4(): string {
    return Math.floor((1 + Math.random()) * 0x10000)
      .toString(16)
      .substring(1);
  }
  return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
    s4() + '-' + s4() + s4() + s4();
}

const SNAKE_CASE_REGEXP = /[A-Z]/g;

export function snakeCase(name: string, separator: string): string {
  separator = separator || '_';
  return name.replace(SNAKE_CASE_REGEXP, (letter, pos) => {
    return (pos ? separator : '') + letter.toLowerCase();
  });
}

export function getDescendantProp(obj: any, path: string): any {
  if (obj.hasOwnProperty(path)) {
    return obj[path];
  }
  return path.split('.').reduce((acc, part) => acc && acc[part], obj);
}

export function insertVariable(pattern: string, name: string, value: any): string {
  let result = pattern;
  let match = varsRegex.exec(pattern);
  while (match !== null) {
    const variable = match[0];
    const variableName = match[1];
    if (variableName === name) {
      result = result.replace(variable, value);
    }
    match = varsRegex.exec(pattern);
  }
  return result;
}

export function createLabelFromDatasource(datasource: Datasource, pattern: string): string {
  let label = pattern;
  if (!datasource) {
    return label;
  }
  let match = varsRegex.exec(pattern);
  while (match !== null) {
    const variable = match[0];
    const variableName = match[1];
    if (variableName === 'dsName') {
      label = label.replace(variable, datasource.name);
    } else if (variableName === 'entityName') {
      label = label.replace(variable, datasource.entityName);
    } else if (variableName === 'deviceName') {
      label = label.replace(variable, datasource.entityName);
    } else if (variableName === 'entityLabel') {
      label = label.replace(variable, datasource.entityLabel || datasource.entityName);
    } else if (variableName === 'aliasName') {
      label = label.replace(variable, datasource.aliasName);
    } else if (variableName === 'entityDescription') {
      label = label.replace(variable, datasource.entityDescription);
    }
    match = varsRegex.exec(pattern);
  }
  return label;
}

export function formattedDataFormDatasourceData(input: DatasourceData[], dataIndex?: number): FormattedData[] {
  return _(input).groupBy(el => el.datasource.entityName + el.datasource.entityType)
    .values().value().map((entityArray, i) => {
      const datasource = entityArray[0].datasource;
      const obj = formattedDataFromDatasource(datasource, i);
      entityArray.filter(el => el.data.length).forEach(el => {
        const index = isDefined(dataIndex) ? dataIndex : el.data.length - 1;
        if (!obj.hasOwnProperty(el.dataKey.label) || el.data[index][1] !== '') {
          obj[el.dataKey.label] = el.data[index][1];
          obj[el.dataKey.label + '|ts'] = el.data[index][0];
          if (el.dataKey.label.toLowerCase() === 'type') {
            obj.deviceType = el.data[index][1];
          }
        }
      });
      return obj;
    });
}

export function formattedDataArrayFromDatasourceData(input: DatasourceData[]): FormattedData[][] {
  return _(input).groupBy(el => el.datasource.entityName)
    .values().value().map((entityArray, dsIndex) => {
      const timeDataMap: {[time: number]: FormattedData} = {};
      entityArray.filter(e => e.data.length).forEach(entity => {
        entity.data.forEach(tsData => {
          const time = tsData[0];
          const value = tsData[1];
          let data = timeDataMap[time];
          if (!data) {
            const datasource = entity.datasource;
            data = formattedDataFromDatasource(datasource, dsIndex);
            data.time = time;
            timeDataMap[time] = data;
          }
          data[entity.dataKey.label] = value;
          data[entity.dataKey.label + '|ts'] = time;
          if (entity.dataKey.label.toLowerCase() === 'type') {
            data.deviceType = value;
          }
        });
      });
      return _.values(timeDataMap);
    });
}

export function formattedDataFromDatasource(datasource: Datasource, dsIndex: number): FormattedData {
  return {
    entityName: datasource.entityName,
    deviceName: datasource.entityName,
    entityId: datasource.entityId,
    entityType: datasource.entityType,
    entityLabel: datasource.entityLabel || datasource.entityName,
    entityDescription: datasource.entityDescription,
    aliasName: datasource.aliasName,
    $datasource: datasource,
    dsIndex,
    dsName: datasource.name,
    deviceType: null
  };
}

export function flatFormattedData(input: FormattedData[]): FormattedData {
  let result: FormattedData = {} as FormattedData;
  if (input.length) {
    for (const toMerge of input) {
      result = {...result, ...toMerge};
    }
    const sourceData = input[0];
    result.entityName =  sourceData.entityName;
    result.deviceName = sourceData.deviceName;
    result.entityId =  sourceData.entityId;
    result.entityType =  sourceData.entityType;
    result.entityLabel = sourceData.entityLabel;
    result.entityDescription = sourceData.entityDescription;
    result.aliasName = sourceData.aliasName;
    result.$datasource =  sourceData.$datasource;
    result.dsIndex =  sourceData.dsIndex;
    result.dsName = sourceData.dsName;
    result.deviceType =  sourceData.deviceType;
  }
  return result;
}

export function flatDataWithoutOverride(input: FormattedData[]): FormattedData {
  const result: FormattedData = {} as FormattedData;
  input.forEach((data) => {
    Object.keys(data).forEach((key) => {
      if (!isDefinedAndNotNull(result[key]) || isEmptyStr(result[key])) {
        result[key] = data[key];
      }
    });
  });
  return result;
}

export function mergeFormattedData(first: FormattedData[], second: FormattedData[]): FormattedData[] {
  const merged = first.concat(second);
  return _(merged).groupBy(el => el.$datasource)
    .values().value().map((formattedDataArray, i) => {
      let res = formattedDataArray[0];
      if (formattedDataArray.length > 1) {
        const toMerge = formattedDataArray[1];
        res = {...res, ...toMerge};
      }
      return res;
    });
}

export function processDataPattern(pattern: string, data: FormattedData): Array<ReplaceInfo> {
  const replaceInfo: Array<ReplaceInfo> = [];
  try {
    const reg = /\${([^}]*)}/g;
    let match = reg.exec(pattern);
    while (match !== null) {
      const variableInfo: ReplaceInfo = {
        dataKeyName: '',
        valDec: 2,
        variable: ''
      };
      const variable = match[0];
      let label = match[1];
      let valDec = 2;
      const splitValues = label.split(':');
      if (splitValues.length > 1) {
        label = splitValues[0];
        valDec = parseFloat(splitValues[1]);
      }

      variableInfo.variable = variable;
      variableInfo.valDec = valDec;

      if (label.startsWith('#')) {
        const keyIndexStr = label.substring(1);
        const n = Math.floor(Number(keyIndexStr));
        if (String(n) === keyIndexStr && n >= 0) {
          variableInfo.dataKeyName = data.$datasource.dataKeys[n].label;
        }
      } else {
        variableInfo.dataKeyName = label;
      }
      replaceInfo.push(variableInfo);

      match = reg.exec(pattern);
    }
  } catch (ex) {
    console.log(ex, pattern);
  }
  return replaceInfo;
}

export function fillDataPattern(pattern: string, replaceInfo: Array<ReplaceInfo>, data: FormattedData) {
  let text = createLabelFromDatasource(data.$datasource, pattern);
  if (replaceInfo) {
    for (const variableInfo of replaceInfo) {
      let txtVal = '';
      if (variableInfo.dataKeyName && isDefinedAndNotNull(data[variableInfo.dataKeyName])) {
        const varData = data[variableInfo.dataKeyName];
        if (isNumber(varData)) {
          txtVal = padValue(varData, variableInfo.valDec);
        } else {
          txtVal = varData;
        }
      }
      text = text.replace(variableInfo.variable, txtVal);
    }
  }
  return text;
}

export function createLabelFromPattern(pattern: string, data: FormattedData): string {
  const replaceInfo = processDataPattern(pattern, data);
  return fillDataPattern(pattern, replaceInfo, data);
}

export function parseFunction(source: any, params: string[] = ['def']): (...args: any[]) => any {
  let res = null;
  if (source?.length) {
    try {
      res = new Function(...params, source);
    }
    catch (err) {
      res = null;
    }
  }
  return res;
}

export function safeExecute(func: (...args: any[]) => any, params = []) {
  let res = null;
  if (func && typeof (func) === 'function') {
    try {
      res = func(...params);
    }
    catch (err) {
      console.log('error in external function:', err);
      res = null;
    }
  }
  return res;
}

export function padValue(val: any, dec: number): string {
  let strVal;
  let n;

  val = parseFloat(val);
  n = (val < 0);
  val = Math.abs(val);

  if (dec > 0) {
    strVal = val.toFixed(dec);
  } else {
    strVal = Math.round(val).toString();
  }
  strVal = (n ? '-' : '') + strVal;
  return strVal;
}

export function baseUrl(): string {
  let url = window.location.protocol + '//' + window.location.hostname;
  const port = window.location.port;
  if (port && port.length > 0 && port !== '80' && port !== '443') {
    url += ':' + port;
  }
  return url;
}

export function sortObjectKeys<T>(obj: T): T {
  return Object.keys(obj).sort().reduce((acc, key) => {
    acc[key] = obj[key];
    return acc;
  }, {} as T);
}

export function deepTrim<T>(obj: T): T {
  if (isNumber(obj) || isUndefined(obj) || isString(obj) || obj === null || obj instanceof File) {
    return obj;
  }
  return Object.keys(obj).reduce((acc, curr) => {
    if (isString(obj[curr])) {
      acc[curr] = obj[curr].trim();
    } else if (isObject(obj[curr])) {
      acc[curr] = deepTrim(obj[curr]);
    } else {
      acc[curr] = obj[curr];
    }
    return acc;
  }, (Array.isArray(obj) ? [] : {}) as T);
}

export function generateSecret(length?: number): string {
  if (isUndefined(length) || length == null) {
    length = 1;
  }
  const l = length > 10 ? 10 : length;
  const str = Math.random().toString(36).substr(2, l);
  if (str.length >= length) {
    return str;
  }
  return str.concat(generateSecret(length - str.length));
}

export function validateEntityId(entityId: EntityId | null): boolean {
    return isDefinedAndNotNull(entityId?.id) && entityId.id !== NULL_UUID && isDefinedAndNotNull(entityId?.entityType);
}

export function isMobileApp(): boolean {
  return isDefined((window as any).flutter_inappwebview);
}

const alphanumericCharacters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
const alphanumericCharactersLength = alphanumericCharacters.length;

export function randomAlphanumeric(length: number): string {
  let result = '';
  for ( let i = 0; i < length; i++ ) {
    result += alphanumericCharacters.charAt(Math.floor(Math.random() * alphanumericCharactersLength));
  }
  return result;
}

export function getEntityDetailsPageURL(id: string, entityType: EntityType): string {
  return `${baseDetailsPageByEntityType.get(entityType)}/${id}`;
}

export function parseHttpErrorMessage(errorResponse: HttpErrorResponse,
                                      translate: TranslateService, responseType?: string): {message: string, timeout: number} {
  let error = null;
  let errorMessage: string;
  let timeout = 0;
  if (responseType === 'text') {
    try {
      error = errorResponse.error ? JSON.parse(errorResponse.error) : null;
    } catch (e) {}
  } else {
    error = errorResponse.error;
  }
  if (error && !error.message) {
    errorMessage = prepareMessageFromData(error);
  } else if (error && error.message) {
    errorMessage = error.message;
    timeout = error.timeout ? error.timeout : 0;
  } else {
    errorMessage = `Unhandled error code ${error ? error.status : '\'Unknown\''}`;
  }
  if (isObject(errorMessage)) {
    let errorText = `${errorResponse.status}: `;
    let errorKey = null;
    if ((errorMessage as any).errorCode) {
      errorKey = serverErrorCodesTranslations.get((errorMessage as any).errorCode);
    }
    errorText += errorKey ? translate.instant(errorKey) : errorResponse.statusText;
    errorMessage = errorText;
  }
  return {message: errorMessage, timeout};
}

function prepareMessageFromData(data): string {
  if (typeof data === 'object' && data.constructor === ArrayBuffer) {
    const msg = String.fromCharCode.apply(null, new Uint8Array(data));
    try {
      const msgObj = JSON.parse(msg);
      if (msgObj.message) {
        return msgObj.message;
      } else {
        return msg;
      }
    } catch (e) {
      return msg;
    }
  } else {
    return data;
  }
}

export function genNextLabel(name: string, datasources: Datasource[]): string {
  let label = name;
  let i = 1;
  let matches = false;
  if (datasources) {
    do {
      matches = false;
      datasources.forEach((datasource) => {
        if (datasource) {
          if (datasource.dataKeys) {
            datasource.dataKeys.forEach((dataKey) => {
              if (dataKey.label === label) {
                i++;
                label = name + ' ' + i;
                matches = true;
              }
            });
          }
          if (datasource.latestDataKeys) {
            datasource.latestDataKeys.forEach((dataKey) => {
              if (dataKey.label === label) {
                i++;
                label = name + ' ' + i;
                matches = true;
              }
            });
          }
        }
      });
    } while (matches);
  }
  return label;
}
