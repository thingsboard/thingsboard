// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'shortNumber',
    standalone: false
})
export class ShortNumberPipe implements PipeTransform {

  transform(number: number, args?: any): any {
    if (isNaN(number)) return 0;
    if (number === null) return 0;
    if (number === 0) return 0;
    let abs = Math.abs(number);
    const rounder = Math.pow(10, 1);
    const isNegative = number < 0;
    const isLong = args && args.long;
    let key = '';

    const powers = [
      {key: 'Q', longKey: ' quadrillion', value: Math.pow(10, 15)},
      {key: 'T', longKey: ' trillion', value: Math.pow(10, 12)},
      {key: 'B', longKey: ' billion', value: Math.pow(10, 9)},
      {key: 'M', longKey: ' million', value: Math.pow(10, 6)},
      {key: 'K', longKey: ' thousand', value: 1000}
    ];

    for (let i = 0; i < powers.length; i++) {
      let reduced = abs / powers[i].value;
      reduced = Math.round(reduced * rounder) / rounder;
      if (reduced >= 1) {
        abs = reduced;
        key = isLong ? powers[i].longKey : powers[i].key;
        break;
      }
    }
    return (isNegative ? '-' : '') + abs + key;
  }
}
