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

import { Injectable } from '@angular/core';

const APP_PREFIX = 'TB-';

// @dynamic
@Injectable(
  {
    providedIn: 'root'
  }
)
export class LocalStorageService {
  constructor() {}

  static loadInitialState() {
    return Object.keys(localStorage).reduce((state: any, storageKey) => {
      if (storageKey.includes(APP_PREFIX)) {
        const stateKeys = storageKey
          .replace(APP_PREFIX, '')
          .toLowerCase()
          .split('.')
          .map(key =>
            key
              .split('-')
              .map(
                (token, index) =>
                  index === 0
                    ? token
                    : token.charAt(0).toUpperCase() + token.slice(1)
              )
              .join('')
          );
        let currentStateRef = state;
        stateKeys.forEach((key, index) => {
          if (index === stateKeys.length - 1) {
            currentStateRef[key] = JSON.parse(localStorage.getItem(storageKey));
            return;
          }
          currentStateRef[key] = currentStateRef[key] || {};
          currentStateRef = currentStateRef[key];
        });
      }
      return state;
    }, {});
  }

  setItem(key: string, value: any) {
    localStorage.setItem(`${APP_PREFIX}${key}`, JSON.stringify(value));
  }

  getItem(key: string) {
    return JSON.parse(localStorage.getItem(`${APP_PREFIX}${key}`));
  }

  removeItem(key: string) {
    localStorage.removeItem(`${APP_PREFIX}${key}`);
  }
  /** Tests that localStorage exists, can be written to, and read from. */
  testLocalStorage() {
    const testValue = 'testValue';
    const testKey = 'testKey';
    let retrievedValue: string;
    const errorMessage = 'localStorage did not return expected value';

    this.setItem(testKey, testValue);
    retrievedValue = this.getItem(testKey);
    this.removeItem(testKey);

    if (retrievedValue !== testValue) {
      throw new Error(errorMessage);
    }
  }
}
