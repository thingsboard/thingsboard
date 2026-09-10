// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Pipe, PipeTransform } from '@angular/core';
import { isNumber, isObject } from '@core/utils';

@Pipe({
    name: 'tbJson',
    standalone: false
})
export class TbJsonPipe implements PipeTransform {
  transform(value: any, maxLength?: number): string {
    let result: string;
    if (isObject(value)) {
      result = JSON.stringify(value);
    } else if (isNumber(value)) {
      result = value.toString();
    } else {
      result = value;
    }
    if (maxLength != null && maxLength > 0 && result && result.length > maxLength) {
      return result.slice(0, maxLength) + '\u2026';
    }
    return result;
  }
}
