// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
