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
