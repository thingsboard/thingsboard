// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectAuth, selectIsAuthenticated } from '@core/auth/auth.selectors';
import { Subject } from 'rxjs';
import { filter, switchMap, take } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  DateOrder,
  dateOrders,
  dateOrderSequences,
  DateSeparator,
  dateSeparators,
  DateTimeFormatPickerState,
  DateUnit,
  fallbackDateOrder,
  fallbackDateSeparator,
  fallbackShowMilliseconds,
  fallbackShowSeconds,
  fallbackTimeClock,
  fallbackTimeSeparator,
  isTwelveHourTimeFormat,
  TbDateFormat,
  tbDateFormats,
  TimeClock,
  timeClocks,
  timeFormatWithoutSeconds,
  TimeSeparator,
  timeSeparators
} from '@shared/models/date-time-format.models';

@Injectable({
  providedIn: 'root'
})
export class DateTimeFormatService {

  private currentDateFormat: string = this.buildDateFormat(fallbackDateOrder, fallbackDateSeparator);
  private currentTimeFormat: string = this.buildTimeFormat(fallbackTimeClock, fallbackTimeSeparator, fallbackShowSeconds, fallbackShowMilliseconds);
  private currentMomentDateFormat: string = this.buildMomentDateFormat(fallbackDateOrder, fallbackDateSeparator);
  private currentMomentTimeFormat: string = this.buildMomentTimeFormat(fallbackTimeClock, fallbackTimeSeparator, fallbackShowSeconds, fallbackShowMilliseconds);

  private readonly changedSubject = new Subject<void>();
  readonly changed$ = this.changedSubject.asObservable();

  private reportView = false;

  constructor(private store: Store<AppState>) {
    this.store.select(selectIsAuthenticated).pipe(
      filter((data) => data),
      switchMap(() => this.store.select(selectAuth).pipe(take(1))),
      takeUntilDestroyed()
    ).subscribe((data) => {
      this.setPickerState(data.userDetails?.additionalInfo?.dateTimeFormat);
    });
  }

  setReportView(reportView: boolean): void {
    this.reportView = reportView;
  }

  setPickerState(state?: DateTimeFormatPickerState): void {
    const resolved = this.resolvePickerState(state);
    this.currentDateFormat = this.buildDateFormat(resolved.dateOrder, resolved.dateSeparator);
    this.currentTimeFormat = this.buildTimeFormat(resolved.timeClock, resolved.timeSeparator, resolved.showSeconds, resolved.showMilliseconds);
    this.currentMomentDateFormat = this.buildMomentDateFormat(resolved.dateOrder, resolved.dateSeparator);
    this.currentMomentTimeFormat = this.buildMomentTimeFormat(resolved.timeClock, resolved.timeSeparator, resolved.showSeconds, resolved.showMilliseconds);
    this.changedSubject.next();
  }

  getDateFormat(): string {
    return this.currentDateFormat;
  }

  getTimeFormat(): string {
    return this.currentTimeFormat;
  }

  getMomentDateFormat(): string {
    return this.currentMomentDateFormat;
  }

  getTimeFormatWithoutSeconds(): string {
    return timeFormatWithoutSeconds(this.currentTimeFormat);
  }

  getMomentTimeFormatWithoutSeconds(): string {
    return timeFormatWithoutSeconds(this.currentMomentTimeFormat);
  }

  isTwelveHourFormat(): boolean {
    return isTwelveHourTimeFormat(this.currentTimeFormat);
  }

  buildCombinedFormat(dateFormat: string, timeFormat: string): string {
    return tbDateFormats.tbDateTime(dateFormat, timeFormat);
  }

  resolveFormat(format: TbDateFormat): string {
    return tbDateFormats[format](this.currentDateFormat, this.currentTimeFormat);
  }

  resolvePickerState(state?: DateTimeFormatPickerState): DateTimeFormatPickerState {
    const showSeconds = state?.showSeconds ?? fallbackShowSeconds;
    const showMilliseconds = state?.showMilliseconds ?? fallbackShowMilliseconds;
    if (state && !state.auto) {
      return {
        ...state,
        dateOrder: dateOrders.includes(state.dateOrder) ? state.dateOrder : fallbackDateOrder,
        dateSeparator: dateSeparators.includes(state.dateSeparator) ? state.dateSeparator : fallbackDateSeparator,
        timeClock: timeClocks.includes(state.timeClock) ? state.timeClock : fallbackTimeClock,
        timeSeparator: timeSeparators.includes(state.timeSeparator) ? state.timeSeparator : fallbackTimeSeparator,
        showSeconds,
        showMilliseconds
      };
    }
    if (this.reportView) {
      return {
        auto: true,
        dateOrder: fallbackDateOrder,
        dateSeparator: fallbackDateSeparator,
        timeClock: fallbackTimeClock,
        timeSeparator: fallbackTimeSeparator,
        showSeconds,
        showMilliseconds
      };
    }
    return {
      auto: true,
      ...this.detectAutoDateOrder(),
      ...this.detectAutoTimeClock(),
      showSeconds,
      showMilliseconds
    };
  }

  buildDateFormat(order: DateOrder, separator: DateSeparator): string {
    if (order === 'month') {
      return 'MMM d, yyyy';
    }
    const angularUnitTokens = { year: 'yyyy', month: 'MM', day: 'dd' };
    return dateOrderSequences[order].map(unit => angularUnitTokens[unit]).join(separator);
  }

  buildTimeFormat(clock: TimeClock, separator: TimeSeparator, showSeconds: boolean, showMilliseconds: boolean): string {
    const tokens = [clock === '12' ? 'h' : 'HH', 'mm'];
    if (showSeconds || showMilliseconds) {
      tokens.push('ss');
    }
    let format = tokens.join(separator);
    if (showMilliseconds) {
      format += '.SSS';
    }
    return clock === '12' ? `${format} a` : format;
  }

  private buildMomentTimeFormat(clock: TimeClock, separator: TimeSeparator, showSeconds: boolean, showMilliseconds: boolean): string {
    const format = this.buildTimeFormat(clock, separator, showSeconds, showMilliseconds);
    return clock === '12' ? format.replace(/a$/, 'A') : format;
  }

  private buildMomentDateFormat(order: DateOrder, separator: DateSeparator): string {
    return this.buildDateFormat(order, separator).replace(/y+|d+/g, token => token.toUpperCase());
  }

  private detectAutoDateOrder(): { dateOrder: DateOrder; dateSeparator: DateSeparator } {
    const parts = new Intl.DateTimeFormat(navigator.language, { year: 'numeric', month: '2-digit', day: '2-digit' })
      .formatToParts(new Date());
    const firstUnit = parts.find(p => p.type === 'year' || p.type === 'month' || p.type === 'day')?.type as DateUnit;
    const literal = parts.find(p => p.type === 'literal')?.value as DateSeparator;
    const dateOrder = (Object.keys(dateOrderSequences) as Exclude<DateOrder, 'month'>[])
      .find(order => dateOrderSequences[order][0] === firstUnit) ?? fallbackDateOrder;
    return {
      dateOrder,
      dateSeparator: dateSeparators.includes(literal) ? literal : fallbackDateSeparator
    };
  }

  private detectAutoTimeClock(): { timeClock: TimeClock; timeSeparator: TimeSeparator } {
    const hour12 = new Intl.DateTimeFormat(navigator.language, { hour: 'numeric' }).resolvedOptions().hour12;
    const parts = new Intl.DateTimeFormat(navigator.language, { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12 })
      .formatToParts(new Date());
    const literal = parts.find(p => p.type === 'literal')?.value as TimeSeparator;
    return {
      timeClock: hour12 ? '12' : '24',
      timeSeparator: timeSeparators.includes(literal) ? literal : fallbackTimeSeparator
    };
  }

}
