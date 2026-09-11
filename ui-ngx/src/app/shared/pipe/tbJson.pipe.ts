// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Pipe, PipeTransform } from '@angular/core';
import { isNumber, isObject } from '@core/utils';

@Pipe({
    name: 'tbJson',
    standalone: false
})
export class TbJsonPipe implements PipeTransform {
  transform(value: any): string {
    if (isObject(value)) {
      return JSON.stringify(value);
    } else if (isNumber(value)) {
      return value.toString();
    }
    return value;
  }
}
