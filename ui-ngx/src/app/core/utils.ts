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

import _ from 'lodash';
import { Observable, Subject } from 'rxjs';
import { finalize, share } from 'rxjs/operators';
import { Datasource } from '@app/shared/models/widget.models';
import { EntityId } from '@shared/models/id/entity-id';
import { NULL_UUID } from '@shared/models/id/has-uuid';

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
  return value !== null && typeof value === 'string' && value.trim().length > 0;
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
    // tslint:disable-next-line:no-bitwise
    hash = ((hash << 5) - hash) + char;
    // tslint:disable-next-line:no-bitwise
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

export function validateEntityId(entityId: EntityId): boolean {
  return isDefinedAndNotNull(entityId.id) && entityId.id !== NULL_UUID && isDefinedAndNotNull(entityId.entityType);
}
