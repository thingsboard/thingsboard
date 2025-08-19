// jalali-datetime-adapter.ts
import {Injectable, Optional, Inject} from '@angular/core';
import {MAT_DATE_LOCALE} from '@angular/material/core';
import {DatetimeAdapter, NativeDatetimeAdapter} from '@mat-datetimepicker/core';
import {NativeDateAdapter} from '@angular/material/core';
import moment from 'moment-jalaali';

/**
 * Jalali (Persian) calendar adapter for Angular Material Datetime Picker
 * این آداپتر برای نمایش و کار با تاریخ شمسی در دیت‌پیکر متریال طراحی شده
 */
@Injectable()
export class JalaliDatetimeAdapter extends DatetimeAdapter<Date> {

  readonly _delegate: NativeDatetimeAdapter;

  constructor(
    @Optional() @Inject(MAT_DATE_LOCALE) matDateLocale: string,
  ) {
    // ابتدا یک NativeDateAdapter می‌سازیم
    const dateAdapter = new NativeDateAdapter(matDateLocale || 'fa-IR');
    // سپس آن را به NativeDatetimeAdapter پاس می‌دهیم
    const delegate = new NativeDatetimeAdapter(matDateLocale || 'fa-IR', dateAdapter);
    super(delegate);
    this._delegate = delegate;
    this.setLocale(matDateLocale || 'fa-IR');
  }

  /**
   * دریافت سال شمسی از تاریخ
   */
  override getYear(date: Date): number {
    return moment(date).jYear();
  }

  /**
   * دریافت ماه شمسی از تاریخ (0-11)
   */
  override getMonth(date: Date): number {
    console.log("=== تست moment-jalaali ===");
    console.log("تاریخ ورودی:", date);

    const m = moment(date);
    console.log("moment object:", m);

    // تست متدهای جلالی
    console.log("jYear:", m.jYear());
    console.log("jMonth:", m.jMonth());
    console.log("jDate:", m.jDate());

    // مقایسه با تاریخ میلادی
    console.log("year (میلادی):", m.year());
    console.log("month (میلادی):", m.month());
    console.log("date (میلادی):", m.date());

    // تست فرمت جلالی
    console.log("فرمت جلالی:", m.format('jYYYY/jMM/jDD'));
    console.log("فرمت میلادی:", m.format('YYYY/MM/DD'));

    return m.jMonth();
  }

  /**
   * دریافت روز ماه شمسی از تاریخ (1-31)
   */
  override getDate(date: Date): number {
    return moment(date).jDate();
  }

  /**
   * دریافت ساعت از تاریخ (0-23)
   */
  override getHour(date: Date): number {
    return this._delegate.getHour(date);
  }

  /**
   * دریافت دقیقه از تاریخ (0-59)
   */
  override getMinute(date: Date): number {
    return this._delegate.getMinute(date);
  }

  /**
   * دریافت روز هفته (0-6، 0 = یکشنبه)
   */
  override getDayOfWeek(date: Date): number {
    return moment(date).day();
  }

  /**
   * دریافت نام ماه‌های شمسی
   */
  override getMonthNames(style: 'long' | 'short' | 'narrow'): string[] {
    // const months = moment.localeData('fa').jMonths();
    // if (style === 'short' || style === 'narrow') {
    // نام‌های کوتاه ماه‌ها


    moment.loadPersian({dialect: "persian-modern"});

    // Get all Jalali month names
    const jalaliMonth = [];
    for (let i = 0; i < 12; i++) {
      // Create a moment object for the first day of each month
      const monthn = moment().jMonth(i).startOf('month');
      // Get the month name in Persian
      if (style === 'short' || style === 'narrow') {
        jalaliMonth.push(monthn.format('jMMM'));
      } else {
        jalaliMonth.push(monthn.format('jMMMM'));
      }
    }
    return jalaliMonth;
  }

  /**
   * دریافت لیست اعداد روزهای ماه (1-31)
   */
  override getDateNames(): string[] {
    const dateNames = [];
    for (let i = 1; i <= 31; i++) {
      dateNames.push(String(i));
    }
    return dateNames;
  }

  /**
   * دریافت نام روزهای هفته
   */
  override getDayOfWeekNames(style: 'long' | 'short' | 'narrow'): string[] {
    const weekdays = moment.localeData('fa').weekdays();
    if (style === 'short') {
      return ['ی', 'د', 'س', 'چ', 'پ', 'ج', 'ش'];
    }
    if (style === 'narrow') {
      return ['ی', 'د', 'س', 'چ', 'پ', 'ج', 'ش'];
    }
    return weekdays;
  }

  /**
   * دریافت لیست ساعت‌ها (0-23)
   */
  override getHourNames(): string[] {
    return this._delegate.getHourNames();
  }

  /**
   * دریافت لیست دقیقه‌ها (0-59)
   */
  override getMinuteNames(): string[] {
    return this._delegate.getMinuteNames();
  }

  /**
   * دریافت نام سال به صورت رشته
   */
  override getYearName(date: Date): string {
    return String(this.getYear(date));
  }

  /**
   * دریافت اولین روز هفته (برای تقویم فارسی = شنبه = 6)
   */
  override getFirstDayOfWeek(): number {
    return 6; // شنبه
  }

  /**
   * دریافت تعداد روزهای ماه شمسی
   */
  override getNumDaysInMonth(date: Date): number {


    let year = moment(date).jYear();
    let month = moment(date).jMonth();

      year += this.div(month, 12)
      month = this.mod(month, 12)
      if (month < 0) {
        month += 12
        year -= 1
      }
      if (month < 6) {
        return 31
      } else if (month < 11) {
        return 30
      } else if (moment().jMoment.jIsLeapYear(year)) {
        return 30
      } else {
        return 29
      }

  }

  div = function (a, b) {
    return ~~(a / b)
  }

  mod = function (a, b) {
    return a - ~~(a / b) * b
  }


  /**
   * دریافت اولین تاریخ ماه
   */
  override getFirstDateOfMonth(date: Date): Date {
    return moment(date).startOf('jMonth').toDate();
  }

  /**
   * بررسی اینکه آیا تاریخ در ماه بعدی است
   */
  override isInNextMonth(date: Date, currentMonth: Date): boolean {
    const nextMonth = this.addCalendarMonths(currentMonth, 1);
    return this.getMonth(date) === this.getMonth(nextMonth) &&
      this.getYear(date) === this.getYear(nextMonth);
  }

  /**
   * بررسی اینکه آیا تاریخ در ماه قبلی است
   */
  isInPreviousMonth(date: Date, currentMonth: Date): boolean {
    const prevMonth = this.addCalendarMonths(currentMonth, -1);
    return this.getMonth(date) === this.getMonth(prevMonth) &&
      this.getYear(date) === this.getYear(prevMonth);
  }

  /**
   * کپی کردن تاریخ
   */
  override clone(date: Date): Date {
    return new Date(date.getTime());
  }

  /**
   * ایجاد تاریخ جدید با سال، ماه و روز شمسی
   */
  override createDate(year: number, month: number, date: number): Date {
    return moment().jYear(year).jMonth(month).jDate(date).hour(0).minute(0).second(0).millisecond(0).toDate();
  }

  /**
   * ایجاد تاریخ و زمان کامل با تقویم شمسی
   */
  override createDatetime(
    year: number,
    month: number,
    date: number,
    hour: number,
    minute: number
  ): Date {
    return moment()
      .jYear(year)
      .jMonth(month)
      .jDate(date)
      .hour(hour)
      .minute(minute)
      .second(0)
      .millisecond(0)
      .toDate();
  }

  /**
   * دریافت تاریخ امروز
   */
  override today(): Date {
    return new Date();
  }

  /**
   * تبدیل رشته به تاریخ با فرمت مشخص
   */
  override parse(value: any, parseFormat: string | string[]): Date | null {
    if (value && typeof value === 'string') {
      // پشتیبانی از فرمت‌های مختلف
      const formats = Array.isArray(parseFormat) ? parseFormat : [parseFormat];
      const parsed = moment(value, formats, 'fa');
      if (parsed.isValid()) {
        return parsed.toDate();
      }
    }
    return this._delegate.parse(value, parseFormat);
  }

  /**
   * تبدیل تاریخ به رشته با فرمت مشخص
   */
  override format(date: Date, displayFormat: string): string {
    return moment(date).locale('fa').format(displayFormat);
  }

  /**
   * اضافه کردن سال شمسی به تاریخ
   */
  override addCalendarYears(date: Date, years: number): Date {
    return moment(date).add(years, 'jYear').toDate();
  }

  /**
   * اضافه کردن ماه شمسی به تاریخ
   */
  override addCalendarMonths(date: Date, months: number): Date {
    return moment(date).add(months, 'jMonth').toDate();
  }

  /**
   * اضافه کردن روز به تاریخ
   */
  override addCalendarDays(date: Date, days: number): Date {
    return moment(date).add(days, 'days').toDate();
  }

  /**
   * اضافه کردن ساعت به تاریخ
   */
  override addCalendarHours(date: Date, hours: number): Date {
    return moment(date).add(hours, 'hours').toDate();
  }

  /**
   * اضافه کردن دقیقه به تاریخ
   */
  override addCalendarMinutes(date: Date, minutes: number): Date {
    return moment(date).add(minutes, 'minutes').toDate();
  }

  /**
   * تبدیل تاریخ به فرمت ISO 8601
   */
  override toIso8601(date: Date): string {
    return this._delegate.toIso8601(date);
  }

  /**
   * تبدیل ورودی به تاریخ (deserialize)
   */
  override deserialize(value: any): Date | null {
    if (typeof value === 'string') {
      if (!value) {
        return null;
      }
      // امکان پارس کردن فرمت‌های مختلف شمسی و میلادی
      const formats = ['jYYYY/jMM/jDD', 'jYYYY-jMM-jDD', 'YYYY-MM-DD', 'YYYY/MM/DD'];
      const parsed = moment(value, formats, true);
      if (parsed.isValid()) {
        return parsed.toDate();
      }
    }
    return this._delegate.deserialize(value);
  }

  /**
   * بررسی اینکه آیا آبجکت یک instance از Date است
   */
  override isDateInstance(obj: any): boolean {
    return this._delegate.isDateInstance(obj);
  }

  /**
   * بررسی معتبر بودن تاریخ
   */
  override isValid(date: Date): boolean {
    return this._delegate.isValid(date);
  }

  /**
   * ایجاد یک تاریخ نامعتبر
   */
  override invalid(): Date {
    return this._delegate.invalid();
  }

  /**
   * مقایسه دو تاریخ (فقط بخش تاریخ، بدون زمان)
   */
  override compareDate(first: Date, second: Date): number {
    const yearDiff = this.getYear(first) - this.getYear(second);
    if (yearDiff !== 0) return yearDiff;

    const monthDiff = this.getMonth(first) - this.getMonth(second);
    if (monthDiff !== 0) return monthDiff;

    return this.getDate(first) - this.getDate(second);
  }

  /**
   * مقایسه کامل دو تاریخ (شامل زمان)
   */
  override compareDatetime(first: Date, second: Date): number {
    return first.getTime() - second.getTime();
  }

  /**
   * بررسی یکسان بودن دو تاریخ (فقط بخش تاریخ)
   */
  override sameDate(first: Date | null, second: Date | null): boolean {
    if (first && second) {
      return this.compareDate(first, second) === 0;
    }
    return first === second;
  }

  /**
   * بررسی یکسان بودن دو تاریخ و زمان
   */
  override sameDatetime(first: Date | null, second: Date | null): boolean {
    if (first && second) {
      return this.compareDatetime(first, second) === 0;
    }
    return first === second;
  }

  /**
   * محدود کردن تاریخ بین حداقل و حداکثر
   */
  override clampDate(date: Date, min?: Date | null, max?: Date | null): Date {
    if (min && this.compareDate(date, min) < 0) {
      return this.clone(min);
    }
    if (max && this.compareDate(date, max) > 0) {
      return this.clone(max);
    }
    return date;
  }
}
