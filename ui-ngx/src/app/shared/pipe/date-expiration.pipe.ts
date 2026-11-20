// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Pipe, PipeTransform } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MillisecondsToTimeStringPipe } from '@shared/pipe/milliseconds-to-time-string.pipe';
import { isDefined } from '@core/utils';

@Pipe({
    name: 'dateExpiration',
    standalone: false
})
export class DateExpirationPipe implements PipeTransform {

  constructor(private millisecondsToTimeString: MillisecondsToTimeStringPipe, private datePipe: DatePipe) {
  }

  transform(expirationMs: number, arg?: any): string {
    const displayDate = isDefined(arg?.displayDate) ? arg.displayDate : true;
    const dateFormat = isDefined(arg?.dateFormat) ? arg.dateFormat : undefined;
    const shortFormat = isDefined(arg?.shortFormat) ? arg.shortFormat : true;
    const onlyFirstDigit = isDefined(arg?.onlyFirstDigit) ? arg.onlyFirstDigit : true;
    let time = this.millisecondsToTimeString.transform(expirationMs, shortFormat, onlyFirstDigit);
    if (displayDate) {
      if (dateFormat) {
        time += this.datePipe.transform(expirationMs + Date.now(), dateFormat);
      } else {
        time += ` (${this.datePipe.transform(expirationMs + Date.now(), 'tbDate')})`;
      }
    }
    return time;
  }
}
