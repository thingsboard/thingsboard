// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Pipe, PipeTransform } from '@angular/core';
import { isString } from '@core/utils';

@Pipe({
    name: 'truncate',
    standalone: false
})
export class TruncatePipe implements PipeTransform {
  transform(text: string, wordwise: boolean, max: any, tail: string): string {
    if (!text) { return ''; }
    if (isString(max)) {
      max = parseInt(max, 10);
    }
    if (!max) { return text; }
    if (text.length <= max) { return text; }

    text = text.substr(0, max);
    if (wordwise) {
      let lastspace = text.lastIndexOf(' ');
      if (lastspace !== -1) {
        if (text.charAt(lastspace - 1) === '.' || text.charAt(lastspace - 1) === ',') {
          lastspace = lastspace - 1;
        }
        text = text.substr(0, lastspace);
      }
    }
    return text + (tail || ' …');
  }
}
