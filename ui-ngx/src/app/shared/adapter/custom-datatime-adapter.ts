// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { NativeDatetimeAdapter } from '@mat-datetimepicker/core';

@Injectable()
export class CustomDateAdapter extends NativeDatetimeAdapter {

  parse(value: string | number): Date {
    if (typeof value === 'number') {
      return new Date(value);
    }
    let newDate = value;
    const formatToParts = Intl.DateTimeFormat(this.locale).formatToParts();
    if (formatToParts[0].type.toLowerCase() === 'day') {
      const literal = formatToParts[1].value;
      newDate = newDate.replace(new RegExp(`(\\d+[${literal}])(\\d+[${literal}])`), '$2$1');
    }
    return newDate ? new Date(Date.parse(newDate)) : null;
  }

}
