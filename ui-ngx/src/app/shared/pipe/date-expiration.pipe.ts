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

import { Pipe, PipeTransform } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MillisecondsToTimeStringPipe } from '@shared/pipe/milliseconds-to-time-string.pipe';
import { isDefined } from '@core/utils';

@Pipe({
  name: 'dateExpiration'
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
