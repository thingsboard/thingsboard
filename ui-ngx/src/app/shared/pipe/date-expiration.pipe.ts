// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    const dateFormat = isDefined(arg?.dateFormat) ? arg.dateFormat : ' (dd/MM/yyyy)';
    const shortFormat = isDefined(arg?.shortFormat) ? arg.shortFormat : true;
    const onlyFirstDigit = isDefined(arg?.onlyFirstDigit) ? arg.onlyFirstDigit : true;
    let time = this.millisecondsToTimeString.transform(expirationMs, shortFormat, onlyFirstDigit);
    if (displayDate) {
      const exactDate = this.datePipe.transform(expirationMs + Date.now(), dateFormat);
      time += exactDate;
    }
    return time;
  }
}
