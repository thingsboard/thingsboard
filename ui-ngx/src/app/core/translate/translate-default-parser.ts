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

import { Injectable } from '@angular/core';
import { InterpolateFunction, InterpolationParameters, TranslateParser } from '@ngx-translate/core';
import { isDefinedAndNotNull } from '@core/utils';

@Injectable({ providedIn: 'root' })
export class TranslateDefaultParser extends TranslateParser {
  templateMatcher: RegExp = /{{\s?([^{}\s]*)\s?}}/g;

  public interpolate(expr: string | InterpolateFunction, params?: InterpolationParameters): string {
    let result: string;

    if (typeof expr === 'string') {
      result = this.interpolateString(expr, params);
    } else if (typeof expr === 'function') {
      result = this.interpolateFunction(expr, params);
      if (typeof result === 'string') {
        result = this.interpolateString(result, params);
      }
    } else {
      result = expr as string;
    }

    return result;
  }

  getValue(target: any, key: string): any {
    const keys = typeof key === 'string' ? key.split('.') : [key];
    key = '';
    do {
      key += keys.shift();
      if (isDefinedAndNotNull(target) && isDefinedAndNotNull(target[key]) && (typeof target[key] === 'object' || !keys.length)) {
        target = target[key];
        key = '';
      } else if (!keys.length) {
        target = undefined;
      } else {
        key += '.';
      }
    } while (keys.length);

    return target;
  }

  private interpolateFunction(fn: InterpolateFunction, params?: InterpolationParameters) {
    return fn(params);
  }

  private interpolateString(expr: string, params?: InterpolationParameters) {
    if (!params) {
      return expr;
    }

    return expr.replace(this.templateMatcher, (substring: string, b: string) => {
      const r = this.getValue(params, b);
      return isDefinedAndNotNull(r) ? r : substring;
    });
  }
}
