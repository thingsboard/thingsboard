// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
