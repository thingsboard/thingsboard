// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import moment from 'moment';

export function isTwelveHourTimeFormat(format: string): boolean {
  return /\ba$/i.test(format);
}

export type DateOrder = 'yyyy' | 'MM' | 'dd' | 'month';

export const timeClocks = ['24', '12'] as const;

export type TimeClock = typeof timeClocks[number];

export const dateSeparators = ['/', '-', '.'] as const;
export const timeSeparators = [':', ',', '.'] as const;

export type DateSeparator = typeof dateSeparators[number];
export type TimeSeparator = typeof timeSeparators[number];

export type DateUnit = 'year' | 'month' | 'day';

export const dateOrderSequences: { [order in Exclude<DateOrder, 'month'>]: DateUnit[] } = {
  yyyy: ['year', 'month', 'day'],
  MM: ['month', 'day', 'year'],
  dd: ['day', 'month', 'year']
};

export const dateOrderTranslations: { [order in DateOrder]: string } = {
  yyyy: 'date.order-yyyy-first',
  MM: 'date.order-mm-first',
  dd: 'date.order-dd-first',
  month: 'date.order-month-name'
};

export const dateOrders = Object.keys(dateOrderTranslations) as DateOrder[];

export const fallbackDateOrder: Exclude<DateOrder, 'month'> = 'yyyy';
export const fallbackDateSeparator: DateSeparator = '-';
export const fallbackTimeSeparator: TimeSeparator = ':';
export const fallbackTimeClock: TimeClock = '24';
export const fallbackShowSeconds = true;
export const fallbackShowMilliseconds = false;

export interface DateTimeFormatPickerState {
  auto: boolean;
  dateOrder: DateOrder;
  dateSeparator: DateSeparator;
  timeClock: TimeClock;
  timeSeparator: TimeSeparator;
  showSeconds: boolean;
  showMilliseconds: boolean;
}

export type TbDateFormat = 'tbDateTime' | 'tbDateTimeNoSeconds' | 'tbDateTimeMillis' | 'tbDate' | 'tbTime';

export const exportDateFormat = 'yyyy-MM-dd HH:mm:ss';

const timeSeparatorClass = `[${timeSeparators.join('').replace(/[\\\]^-]/g, '\\$&')}]`;

const SECONDS_TOKEN_REGEX = new RegExp(`${timeSeparatorClass}ss(\\.S+)?`);
const TIME_UNIT_SEPARATOR_REGEX = new RegExp(timeSeparatorClass);

export interface ParsedDateParts {
  year?: number;
  month?: number;
  day?: number;
}

export interface ParsedTimeParts {
  hour?: number;
  minute?: number;
  second?: number;
}

export function timeFormatWithoutSeconds(timeFormat: string): string {
  return timeFormat.replace(SECONDS_TOKEN_REGEX, '');
}

export function timeFormatWithMilliseconds(timeFormat: string): string {
  const secondsMatch = SECONDS_TOKEN_REGEX.exec(timeFormat);
  if (!secondsMatch) {
    const separator = TIME_UNIT_SEPARATOR_REGEX.exec(timeFormat)?.[0] ?? ':';
    return timeFormat.replace('mm', `mm${separator}ss.SSS`);
  }
  if (secondsMatch[1]) {
    return timeFormat;
  }
  return timeFormat.replace(secondsMatch[0], `${secondsMatch[0]}.SSS`);
}

export const tbDateFormats: Record<TbDateFormat, (dateFormat: string, timeFormat: string) => string> = {
  tbDate: (dateFormat) => dateFormat,
  tbTime: (_dateFormat, timeFormat) => timeFormat,
  tbDateTime: (dateFormat, timeFormat) => `${dateFormat} ${timeFormat}`,
  tbDateTimeNoSeconds: (dateFormat, timeFormat) => `${dateFormat} ${timeFormatWithoutSeconds(timeFormat)}`,
  tbDateTimeMillis: (dateFormat, timeFormat) => `${dateFormat} ${timeFormatWithMilliseconds(timeFormat)}`
};

const tbDateFormatNames = Object.keys(tbDateFormats) as TbDateFormat[];

export const isTbDateFormat = (format: string): format is TbDateFormat => tbDateFormatNames.includes(format as TbDateFormat);

export function parseDateByFormat(value: string, momentDateFormat: string, locale = 'en-US'): ParsedDateParts | null {
  if (!value?.trim()) {
    return null;
  }
  const parsed = moment(value.trim(), momentDateFormat, locale, true);
  if (!parsed.isValid()) {
    return null;
  }
  return { year: parsed.year(), month: parsed.month() + 1, day: parsed.date() };
}

function timeFormatCandidates(momentTimeFormat: string): string[] {
  return isTwelveHourTimeFormat(momentTimeFormat) ? [momentTimeFormat, momentTimeFormat.replace(/\s*a$/i, '')] : [momentTimeFormat];
}

export function parseTimeByFormat(value: string, momentTimeFormat: string): ParsedTimeParts | null {
  if (!value?.trim()) {
    return null;
  }
  const parsed = moment(value.trim(), timeFormatCandidates(momentTimeFormat), true);
  if (!parsed.isValid()) {
    return null;
  }
  return { hour: parsed.hour(), minute: parsed.minute(), second: parsed.second() };
}

export function parseDateTimeByFormat(value: string, momentDateFormat: string, momentTimeFormat: string,
                                       locale = 'en-US'): (ParsedDateParts & ParsedTimeParts) | null {
  if (!value?.trim()) {
    return null;
  }
  const formats = timeFormatCandidates(momentTimeFormat).map(timeFormat => `${momentDateFormat} ${timeFormat}`);
  const parsed = moment(value.trim(), formats, locale, true);
  if (!parsed.isValid()) {
    return null;
  }
  return {
    year: parsed.year(), month: parsed.month() + 1, day: parsed.date(),
    hour: parsed.hour(), minute: parsed.minute(), second: parsed.second()
  };
}
