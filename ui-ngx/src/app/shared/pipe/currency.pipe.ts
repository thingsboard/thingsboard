// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'tbCurrency',
    standalone: false
})
export class TbCurrencyPipe implements PipeTransform {

  constructor() {
  }

  transform(amount: number, args?: any): string {
    if (args?.short) {
      return (amount >= 0 ? '$' : '-$') + (Math.abs(amount) / 100).toFixed(2);
    } else {
      return (amount >= 0 ? '' : '-') + (Math.abs(amount) / 100).toFixed(2) + ' USD';
    }
  }
}
