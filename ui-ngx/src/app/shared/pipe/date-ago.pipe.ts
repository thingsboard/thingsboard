// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    name: 'dateAgo',
    standalone: false
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
