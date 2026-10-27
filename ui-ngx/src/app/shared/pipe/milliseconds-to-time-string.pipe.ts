// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DAY, HOUR, MINUTE, SECOND, YEAR } from '@shared/models/time/time.models';

@Pipe({
    name: 'milliSecondsToTimeString',
    standalone: false
})
export class MillisecondsToTimeStringPipe implements PipeTransform {

  constructor(private translate: TranslateService) {
  }

  transform(milliSeconds: number, shortFormat = false, onlyFirstDigit = false): string {
    const { years, days, hours, minutes, seconds } = this.extractTimeUnits(milliSeconds);
    return this.formatTimeString(years, days, hours, minutes, seconds, shortFormat, onlyFirstDigit);
  }

  private extractTimeUnits(milliseconds: number): { years: number; days: number; hours: number; minutes: number; seconds: number } {
    const years = Math.floor(milliseconds / YEAR);
    const days = Math.floor((milliseconds % YEAR) / DAY);
    const hours = Math.floor((milliseconds % DAY) / HOUR);
    const minutes = Math.floor((milliseconds % HOUR) / MINUTE);
    const seconds = Math.floor((milliseconds % MINUTE) / SECOND);
    return { years, days, hours, minutes, seconds };
  }

  private formatTimeString(
    years: number,
    days: number,
    hours: number,
    minutes: number,
    seconds: number,
    shortFormat: boolean,
    onlyFirstDigit: boolean
  ): string {
    const timeUnits = [
      { value: years, key: 'years', shortKey: 'short.years' },
      { value: days, key: 'days', shortKey: 'short.days' },
      { value: hours, key: 'hours', shortKey: 'short.hours' },
      { value: minutes, key: 'minutes', shortKey: 'short.minutes' },
      { value: seconds, key: 'seconds', shortKey: 'short.seconds' }
    ];

    let timeString = '';
    for (const { value, key, shortKey } of timeUnits) {
      if (value > 0) {
        timeString += this.translate.instant(shortFormat ? `timewindow.${shortKey}` : `timewindow.${key}`, { [key]: value });
        if (onlyFirstDigit) {
          return timeString.trim();
        }
      }
    }

    return timeString.length > 0 ? timeString : this.translate.instant('timewindow.short.seconds', { seconds: 0 });
  }
}
