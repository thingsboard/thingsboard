// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Inject, Injectable, LOCALE_ID } from '@angular/core';
import { formatDate } from '@angular/common';
import { MAT_NATIVE_DATETIME_FORMATS, MatDatetimeFormats, NativeDatetimeAdapter } from '@mat-datetimepicker/core';
import { DateTimeFormatService } from '@core/services/date-time-format.service';
import {
  ParsedDateParts,
  ParsedTimeParts,
  parseDateByFormat,
  parseDateTimeByFormat,
  parseTimeByFormat
} from '@shared/models/date-time-format.models';

type CustomDatetimeInputFormat = 'date' | 'time' | 'datetime';

export const CUSTOM_DATETIME_FORMATS: MatDatetimeFormats = {
  parse: {
    ...MAT_NATIVE_DATETIME_FORMATS.parse,
    dateInput: 'date',
    timeInput: 'time',
    datetimeInput: 'datetime'
  },
  display: {
    ...MAT_NATIVE_DATETIME_FORMATS.display,
    dateInput: 'date',
    timeInput: 'time',
    datetimeInput: 'datetime'
  }
};

@Injectable()
export class CustomDateAdapter extends NativeDatetimeAdapter {

  constructor(private dateTimeFormatService: DateTimeFormatService, @Inject(LOCALE_ID) private appLocale: string) {
    super();
  }

  format(date: Date, displayFormat: any): string {
    switch (displayFormat as CustomDatetimeInputFormat) {
      case 'date':
        return formatDate(date, this.dateTimeFormatService.getDateFormat(), this.appLocale);
      case 'time':
        return formatDate(date, this.dateTimeFormatService.getTimeFormatWithoutSeconds(), this.appLocale);
      case 'datetime':
        return formatDate(date,
          `${this.dateTimeFormatService.getDateFormat()} ${this.dateTimeFormatService.getTimeFormatWithoutSeconds()}`,
          this.appLocale);
      default:
        return super.format(date, displayFormat);
    }
  }

  parse(value: string | number, parseFormat?: any): Date {
    if (typeof value === 'number') {
      return new Date(value);
    }
    if (!value) {
      return null;
    }
    return this.parseWithCustomFormat(value, parseFormat as CustomDatetimeInputFormat) ?? this.invalid();
  }

  private parseWithCustomFormat(value: string, parseFormat: CustomDatetimeInputFormat): Date | null {
    const timeFormatNoSeconds = this.dateTimeFormatService.getMomentTimeFormatWithoutSeconds();
    let dateParts: ParsedDateParts = null;
    let timeParts: ParsedTimeParts = null;
    if (parseFormat === 'date') {
      dateParts = parseDateByFormat(value, this.dateTimeFormatService.getMomentDateFormat(), this.appLocale);
      if (!dateParts) {
        return null;
      }
    } else if (parseFormat === 'time') {
      timeParts = parseTimeByFormat(value, timeFormatNoSeconds);
      if (!timeParts) {
        return null;
      }
    } else {
      const combined = parseDateTimeByFormat(value, this.dateTimeFormatService.getMomentDateFormat(), timeFormatNoSeconds, this.appLocale);
      if (!combined) {
        return null;
      }
      dateParts = combined;
      timeParts = combined;
    }
    const now = new Date();
    const year = dateParts?.year ?? now.getFullYear();
    const month = (dateParts?.month ?? now.getMonth() + 1) - 1;
    const day = dateParts?.day ?? now.getDate();
    const hour = timeParts?.hour ?? 0;
    const minute = timeParts?.minute ?? 0;
    const second = timeParts?.second ?? 0;
    const result = new Date(year, month, day, hour, minute, second);
    return isNaN(result.getTime()) ? null : result;
  }

}
