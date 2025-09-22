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

import _ from 'lodash';
import { from, isObservable, Observable, of, ReplaySubject, Subject } from 'rxjs';
import { catchError, finalize, share } from 'rxjs/operators';
import { DataKey, Datasource, DatasourceData, FormattedData, ReplaceInfo } from '@app/shared/models/widget.models';
import { EntityId } from '@shared/models/id/entity-id';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { baseDetailsPageByEntityType, EntityType } from '@shared/models/entity-type.models';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';
import { serverErrorCodesTranslations } from '@shared/models/constants';
import { SubscriptionEntityInfo } from '@core/api/widget-api.models';
import {
  CompiledTbFunction,
  compileTbFunction, GenericFunction,
  isNotEmptyTbFunction,
  TbFunction
} from '@shared/models/js-function.models';
import { DomSanitizer } from '@angular/platform-browser';
import { SecurityContext } from '@angular/core';
import { AbstractControl, ValidationErrors, Validators } from '@angular/forms';

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

export function isLiteralObject(value: any) {
  return (!!value) && (value.constructor === Object);
}

export const isDate = (obj: any): boolean => {
  return Object.prototype.toString.call(obj) === "[object Date]";
}

export const isFile = (obj: any): boolean => {
  return Object.prototype.toString.call(obj) === "[object File]";
}

export const formatValue = (value: any, dec?: number, units?: string, showZeroDecimals?: boolean): string | undefined => {
  if (isDefinedAndNotNull(value) && isNumeric(value) &&
    (isDefinedAndNotNull(dec) || isNotEmptyStr(units) || Number(value).toString() === value)) {
    let formatted = value;
    if (isDefinedAndNotNull(dec)) {
      formatted = Number(formatted).toFixed(dec);
    }
    if (!showZeroDecimals) {
      formatted = (Number(formatted));
    }
    formatted = formatted.toString();
    if (isNotEmptyStr(units)) {
      formatted += ' ' + units;
    }
    return formatted;
  } else {
    return value !== null ? value : '';
  }
}

export const formatNumberValue = (value: any, dec?: number): number | undefined => {
  if (isDefinedAndNotNull(value) && isNumeric(value)) {
    let formatted: string | number = Number(value);
    if (isDefinedAndNotNull(dec)) {
      formatted = formatted.toFixed(dec);
    }
    return Number(formatted);
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
    } else if (Array.isArray(obj[propName])) {
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
  return decodeURIComponent(atob(b64Encoded).split('').map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''));
}

export function objToBase64URI(obj: any): string {
  return encodeURIComponent(objToBase64(obj));
}

export function base64toObj(b64Encoded: string): any {
  const json = decodeURIComponent(atob(b64Encoded).split('').map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''));
  return JSON.parse(json);
}

export function stringToBase64(value: string): string {
  return btoa(encodeURIComponent(value).replace(/%([0-9A-F]{2})/g,
    function toSolidBytes(match, p1) {
      return String.fromCharCode(Number('0x' + p1));
    }));
}

export const blobToBase64 = (blob: Blob): Observable<string> => from(new Promise<string>((resolve) => {
      const reader = new FileReader();
      reader.onloadend = () => resolve(reader.result as string);
      reader.readAsDataURL(blob);
    }
  ));

export const blobToText = (blob: Blob): Observable<string> => from(new Promise<string>((resolve) => {
    const reader = new FileReader();
    reader.onloadend = () => resolve(reader.result as string);
    reader.readAsText(blob);
  }
));

export const updateFileContent = (file: File, newContent: string): File => {
  const blob = new Blob([newContent], { type: file.type });
  return new File([blob], file.name, {type: file.type});
};

export const createFileFromContent = (content: string, name: string, type: string): File => {
  const blob = new Blob([content], { type });
  return new File([blob], name, { type });
};

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
  // Observables can't be cloned using the spread operator, because they have non-enumerable methods (like .pipe).
  if (isObservable(target)) {
    return target;
  }
  if (isDate(target)) {
    return new Date((target as Date).getTime()) as T;
  }
  if (Array.isArray(target)) {
    return (target as any[]).map((item) => deepClone(item)) as any;
  }
  if (typeof target === 'object') {
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

export const isEqual = (a: any, b: any): boolean => _.isEqual(a, b);

export const isEmpty = (a: any): boolean => _.isEmpty(a);

export const unset = (object: any, path: string | symbol): boolean => _.unset(object, path);

export const setByPath = <T extends object>(object: T, path: string | number | symbol, value: any): T => _.set(object, path, value);

export const isEqualIgnoreUndefined = (a: any, b: any): boolean => {
  if (a === b) {
    return true;
  }
  if (isDefinedAndNotNull(a) && isDefinedAndNotNull(b)) {
    return isEqual(a, b);
  } else {
    return (isUndefinedOrNull(a) || !a) && (isUndefinedOrNull(b) || !b);
  }
};

export const isArraysEqualIgnoreUndefined = (a: any[], b: any[]): boolean => {
  const res = isEqualIgnoreUndefined(a, b);
  if (!res) {
    return (isUndefinedOrNull(a) || !a?.length) && (isUndefinedOrNull(b) || !b?.length);
  } else {
    return res;
  }
};

export function mergeDeep<T>(target: T, ...sources: T[]): T {
  return _.merge(target, ...sources);
}

function ignoreArrayMergeFunc(target: any, sources: any) {
  if (_.isArray(target)) {
    return sources;
  }
}

export function mergeDeepIgnoreArray<T>(target: T, ...sources: T[]): T {
  return _.mergeWith(target, ...sources, ignoreArrayMergeFunc);
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
  return name.replace(SNAKE_CASE_REGEXP, (letter, pos) => (pos ? separator : '') + letter.toLowerCase());
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

export const createLabelFromDatasource = (datasource: Datasource, pattern: string): string => {
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
};

export const createLabelFromSubscriptionEntityInfo = (entityInfo: SubscriptionEntityInfo, pattern: string): string => {
  let label = pattern;
  if (!entityInfo) {
    return label;
  }
  let match = varsRegex.exec(pattern);
  while (match !== null) {
    const variable = match[0];
    const variableName = match[1];
    if (variableName === 'dsName') {
      label = label.replace(variable, entityInfo.entityName);
    } else if (variableName === 'entityName') {
      label = label.replace(variable, entityInfo.entityName);
    } else if (variableName === 'deviceName') {
      label = label.replace(variable, entityInfo.entityName);
    } else if (variableName === 'entityLabel') {
      label = label.replace(variable, entityInfo.entityLabel || entityInfo.entityName);
    } else if (variableName === 'aliasName') {
      label = label.replace(variable, entityInfo.entityName);
    } else if (variableName === 'entityDescription') {
      label = label.replace(variable, entityInfo.entityDescription);
    }
    match = varsRegex.exec(pattern);
  }
  return label;
};

export const hasDatasourceLabelsVariables = (pattern: string): boolean => varsRegex.test(pattern) !== null;

export function formattedDataFormDatasourceData<D extends Datasource = Datasource>(input: DatasourceData[], dataIndex?: number, ts?: number,
                                                groupFunction: (el: DatasourceData) => any = (el) => el.datasource.entityName + el.datasource.entityType): FormattedData<D>[] {
  return _(input).groupBy(groupFunction)
    .values().value().map((entityArray, i) => {
      const datasource = entityArray[0].datasource as D;
      const obj = formattedDataFromDatasource<D>(datasource, i);
      entityArray.filter(el => el.data.length).forEach(el => {
        const index = isDefined(dataIndex) ? dataIndex : el.data.length - 1;
        const dataSet = isDefined(ts) ? el.data.find(data => data[0] === ts) : el.data[index];
        if (dataSet !== undefined && (!obj.hasOwnProperty(el.dataKey.label) || dataSet[1] !== '')) {
          obj[el.dataKey.label] = dataSet[1];
          obj[el.dataKey.label + '|ts'] = dataSet[0];
          if (el.dataKey.label.toLowerCase() === 'type') {
            obj.deviceType = dataSet[1];
          }
        }
      });
      return obj;
    });
}

export function formattedDataArrayFromDatasourceData<D extends Datasource = Datasource>(input: DatasourceData[],
                                                                                        groupFunction: (el: DatasourceData) => any =
                                                                                        (el) => el.datasource.entityName + el.datasource.entityType): FormattedData<D>[][] {
  return _(input).groupBy(groupFunction)
    .values().value().map((entityArray, dsIndex) => {
      const timeDataMap: {[time: number]: FormattedData<D>} = {};
      entityArray.filter(e => e.data.length).forEach(entity => {
        entity.data.forEach(tsData => {
          const time = tsData[0];
          const value = tsData[1];
          let data = timeDataMap[time];
          if (!data) {
            const datasource = entity.datasource as D;
            data = formattedDataFromDatasource<D>(datasource, dsIndex);
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

export function formattedDataFromDatasource<D extends Datasource = Datasource>(datasource: D, dsIndex: number): FormattedData<D> {
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

export function parseTbFunction<T extends GenericFunction>(http: HttpClient, source: TbFunction, params: string[] = ['def']): Observable<CompiledTbFunction<T>> {
  if (isNotEmptyTbFunction(source)) {
    return compileTbFunction<T>(http, source, ...params).pipe(
      catchError(() => {
        return of(null);
      }),
      share({
        connector: () => new ReplaySubject(1),
        resetOnError: false,
        resetOnComplete: false,
        resetOnRefCountZero: false
      })
    );
  } else {
    return of(null);
  }
}

export function safeExecuteTbFunction<T extends GenericFunction>(func: CompiledTbFunction<T>, params = []) {
  let res = null;
  if (func) {
    try {
      res = func.execute(...params);
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
  if (isNumber(obj) || isUndefined(obj) || isString(obj) || obj === null || isFile(obj)) {
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
  return baseDetailsPageByEntityType.has(entityType) ? `${baseDetailsPageByEntityType.get(entityType)}/${id}` : '';
}

export function parseHttpErrorMessage(errorResponse: HttpErrorResponse,
                                      translate: TranslateService, responseType?: string, sanitizer?:DomSanitizer): {message: string; timeout: number} {
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
  if(sanitizer) {
    errorMessage = sanitizer.sanitize(SecurityContext.HTML,errorMessage);
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

export const genNextLabel = (name: string, datasources: Datasource[]): string => {
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
              if (dataKey?.label === label) {
                i++;
                label = name + ' ' + i;
                matches = true;
              }
            });
          }
          if (datasource.latestDataKeys) {
            datasource.latestDataKeys.forEach((dataKey) => {
              if (dataKey?.label === label) {
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

export const genNextLabelForDataKeys = (name: string, dataKeys: DataKey[]): string => {
  let label = name;
  let i = 1;
  let matches = false;
  if (dataKeys) {
    do {
      matches = false;
      dataKeys.forEach((dataKey) => {
        if (dataKey?.label === label) {
          i++;
          label = name + ' ' + i;
          matches = true;
        }
      });
    } while (matches)
  }
  return label;
}

export const getOS = (): string => {
  const userAgent = window.navigator.userAgent.toLowerCase();
  const macosPlatforms = /(macintosh|macintel|macppc|mac68k|macos|mac_powerpc)/i;
  const windowsPlatforms = /(win32|win64|windows|wince)/i;
  const iosPlatforms = /(iphone|ipad|ipod|darwin|ios)/i;
  let os = null;

  if (macosPlatforms.test(userAgent)) {
    os = 'macos';
  } else if (iosPlatforms.test(userAgent)) {
    os = 'ios';
  } else if (windowsPlatforms.test(userAgent)) {
    os = 'windows';
  } else if (/android/.test(userAgent)) {
    os = 'android';
  } else if (/linux/.test(userAgent)) {
    os = 'linux';
  }

  return os;
};

export const isSafari = (): boolean => {
  const userAgent = window.navigator.userAgent.toLowerCase();
  return /^((?!chrome|android).)*safari/i.test(userAgent);
};

export const isFirefox = (): boolean => {
  const userAgent = window.navigator.userAgent.toLowerCase();
  return /^((?!seamonkey).)*firefox/i.test(userAgent);
};

export const camelCase = (str: string): string => {
  return _.camelCase(str);
};

export const convertKeysToCamelCase = (obj: Record<string, any>): Record<string, any> => {
  return _.mapKeys(obj, (value, key) => _.camelCase(key));
};

export const unwrapModule = (module: any) : any => {
  if ('default' in module && Object.keys(module).length === 1) {
    return module.default;
  } else {
    return module;
  }
};

export const trimDefaultValues = (input: Record<string, any>, defaults: Record<string, any>): Record<string, any> => {
  const result: Record<string, any> = {};

  for (const key in input) {
    if (!(key in defaults)) {
      result[key] = input[key];
    } else if (typeof defaults[key] === 'object' && defaults[key] !== null && typeof input[key] === 'object' && input[key] !== null) {
      const subPatch = trimDefaultValues(input[key], defaults[key]);
      if (Object.keys(subPatch).length > 0) {
        result[key] = subPatch;
      }
    } else if (defaults[key] !== input[key]) {
      result[key] = input[key];
    }
  }

  for (const key in defaults) {
    if (!(key in input)) {
      delete result[key];
    }
  }

  return result;
}

export const validateEmail = (control: AbstractControl): ValidationErrors | null => {
  const email = control.value;
  const nativeEmailError = Validators.email(control);
  if (nativeEmailError !== null) {
    return nativeEmailError;
  }
  const passesDomainCheck = /\.[^.\s]{2,}$/.test(email);
  return passesDomainCheck ? null : {email: true};
};

