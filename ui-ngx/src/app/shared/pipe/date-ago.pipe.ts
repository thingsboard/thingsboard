///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import {Pipe, PipeTransform} from '@angular/core';
import { DAY, HOUR, MINUTE, SECOND, WEEK, YEAR } from '@shared/models/time/time.models';
import { TranslateService } from '@ngx-translate/core';

interface DateAgoInterval {
  singular: string;
  plural: string;
  value: number;
}

@Pipe({
  name: 'dateAgo'
})
export class DateAgoPipe implements PipeTransform {

  constructor(private translate: TranslateService) {
  }

  transform(timeStamp: number): string {
    if (timeStamp) {
      const secondsPassed = Math.floor((+new Date() - +new Date(timeStamp)) / 1000);
      if (secondsPassed < 60)
        return 'recently';
      const intervalsInSeconds: Array<DateAgoInterval> = [
        {
          singular: 'alarm-comment.year',
          plural: 'alarm-comment.years',
          value: YEAR / SECOND
        },
        {
          singular: 'alarm-comment.month',
          plural: 'alarm-comment.months',
          value: DAY / SECOND * 30
        },
        {
          singular: 'alarm-comment.week',
          plural: 'alarm-comment.weeks',
          value: WEEK / SECOND
        },
        {
          singular: 'alarm-comment.day',
          plural: 'alarm-comment.days',
          value: DAY / SECOND
        },
        {
          singular: 'alarm-comment.hour',
          plural: 'alarm-comment.hours',
          value: HOUR / SECOND
        },
        {
          singular: 'alarm-comment.minute',
          plural: 'alarm-comment.minutes',
          value: MINUTE / SECOND
        },
      ]
      let counter: number;
      for (const interval of intervalsInSeconds) {
        counter = Math.floor(secondsPassed / interval.value);
        if (counter > 0)
          if (counter === 1) {
            return counter + ' ' + this.translate.instant(interval.singular);
          } else {
            return counter + ' ' + this.translate.instant(interval.plural);
          }
      }
    }
    return '';
  }

}
