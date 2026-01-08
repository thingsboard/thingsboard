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
import { TranslateService } from '@ngx-translate/core';
import { MillisecondsToTimeStringPipe } from './milliseconds-to-time-string.pipe';

@Pipe({
  name: 'durationLeft',
  pure: false,
  standalone: true,
})
export class DurationLeftPipe implements PipeTransform {

  constructor(private translate: TranslateService, private millisecondsToTimeString: MillisecondsToTimeStringPipe) {
  }

  transform(untilTimestamp: number, shortFormat = true, onlyFirstDigit = true): string {
    const time = this.millisecondsToTimeString.transform((untilTimestamp - new Date().getTime()), shortFormat, onlyFirstDigit) ?? 0;
    return this.translate.instant('common.time-left', { time });
  }
}
