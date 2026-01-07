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

import { Inject, Pipe, PipeTransform } from '@angular/core';
import { DAY, HOUR, MINUTE, SECOND, WEEK, YEAR } from '@shared/models/time/time.models';
import { TranslateService } from '@ngx-translate/core';

const intervals = {
  years: YEAR,
  months: DAY * 30,
  weeks: WEEK,
  days: DAY,
  hr: HOUR,
  min: MINUTE,
  sec: SECOND
};

@Pipe({
  name: 'dateAgo'
})
export class DateAgoPipe implements PipeTransform {

  constructor(@Inject(TranslateService) private translate: TranslateService) {

  }

  transform(value: string| number | Date, args?: any): string {
    if (value) {
      const applyAgo = !!args?.applyAgo;
      const short = !!args?.short;
      const textPart = !!args?.textPart;
      const ms = Math.floor((+new Date() - +new Date(value)));
      if (ms < 29 * SECOND) { // less than 30 seconds ago will show as 'Just now'
        return this.translate.instant(textPart ? 'timewindow.just-now-lower' : 'timewindow.just-now');
      }
      let counter;
      // eslint-disable-next-line guard-for-in
      for (const i in intervals) {
        counter = Math.floor(ms / intervals[i]);
        if (counter > 0) {
          let res = this.translate.instant(`timewindow.${i+(short ? '-short' : '')}`, {[i]: counter});
          if (applyAgo) {
            res += ' ' + this.translate.instant('timewindow.ago');
          }
          return res;
        }
      }
    }
    return '';
  }

}
