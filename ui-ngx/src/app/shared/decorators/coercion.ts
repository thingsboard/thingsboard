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

import {
  coerceArray,
  coerceBooleanProperty,
  coerceCssPixelValue,
  coerceNumberProperty,
  coerceStringArray
} from '@angular/cdk/coercion';

export const coerceBoolean = () => (target: any, key: string): void => {
  const getter = function() {
    return this['__' + key];
  };

  const setter = function(next: any) {
    this['__' + key] = coerceBooleanProperty(next);
  };

  Object.defineProperty(target, key, {
    get: getter,
    set: setter,
    enumerable: true,
    configurable: true,
  });
};

export const coerceNumber = () => (target: any, key: string): void => {
  const getter = function(): number {
    return this['__' + key];
  };

  const setter = function(next: any) {
    this['__' + key] = coerceNumberProperty(next);
  };

  Object.defineProperty(target, key, {
    get: getter,
    set: setter,
    enumerable: true,
    configurable: true,
  });
};

export const coerceCssPixelProperty = () => (target: any, key: string): void => {
  const getter = function(): string {
    return this['__' + key];
  };

  const setter = function(next: any) {
    this['__' + key] = coerceCssPixelValue(next);
  };

  Object.defineProperty(target, key, {
    get: getter,
    set: setter,
    enumerable: true,
    configurable: true,
  });
};

export const coerceArrayProperty = () => (target: any, key: string): void => {
  const getter = function(): any[] {
    return this['__' + key];
  };

  const setter = function(next: any) {
    this['__' + key] = coerceArray(next);
  };

  Object.defineProperty(target, key, {
    get: getter,
    set: setter,
    enumerable: true,
    configurable: true,
  });
};

export const coerceStringArrayProperty = (separator?: string | RegExp) => (target: any, key: string): void => {
  const getter = function(): string[] {
    return this['__' + key];
  };

  const setter = function(next: any) {
    this['__' + key] = coerceStringArray(next, separator);
  };

  Object.defineProperty(target, key, {
    get: getter,
    set: setter,
    enumerable: true,
    configurable: true,
  });
};
