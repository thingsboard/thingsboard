// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectorRef, Inject, Injectable, LOCALE_ID, Optional, Pipe, PipeTransform } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DateTimeFormatService } from '@core/services/date-time-format.service';
import { isTbDateFormat, TbDateFormat } from '@shared/models/date-time-format.models';

@Injectable({
  providedIn: 'root'
})
@Pipe({
  name: 'tbDate',
  standalone: false,
  pure: false
})
export class TbDatePipe extends DatePipe implements PipeTransform {

  private lastKey: string;
  private lastResult: string | null;

  constructor(@Inject(LOCALE_ID) locale: string,
              @Optional() private cdr: ChangeDetectorRef,
              private formatService: DateTimeFormatService) {
    super(locale);
    this.formatService.changed$.pipe(
      takeUntilDestroyed()
    ).subscribe(() => {
      this.cdr?.markForCheck();
    });
  }

  transform(value: Date | string | number, format?: TbDateFormat | (string & {}), timezone?: string, locale?: string): string | null;
  transform(value: null | undefined, format?: TbDateFormat | (string & {}), timezone?: string, locale?: string): null;
  transform(value: Date | string | number | null | undefined, format?: TbDateFormat | (string & {}), timezone?: string,
            locale?: string): string | null {
    const resolvedFormat = this.resolveNamedFormat(format);
    const valueKey = value instanceof Date ? value.getTime() : value;
    const key = `${valueKey}|${resolvedFormat}|${timezone}|${locale}`;
    if (key === this.lastKey) {
      return this.lastResult;
    }
    this.lastResult = super.transform(value, resolvedFormat, timezone, locale);
    this.lastKey = key;
    return this.lastResult;
  }

  private resolveNamedFormat(format: string = 'tbDateTime'): string {
    return isTbDateFormat(format) ? this.formatService.resolveFormat(format) : format;
  }
}
