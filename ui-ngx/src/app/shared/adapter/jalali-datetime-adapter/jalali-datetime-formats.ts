/**
 * فرمت‌های تاریخ و زمان جلالی برای mat-datetimepicker
 * این فرمت‌ها برای نمایش و پارس کردن تاریخ‌های جلالی استفاده می‌شوند
 */
export const JALALI_DATETIME_FORMATS = {
  parse: {
    // فرمت‌های ورودی قابل قبول برای پارس کردن
    dateInput: 'jYYYY/jMM/jDD',
    monthInput: 'jMMMM',
    yearInput: 'jYYYY',
    timeInput: 'HH:mm',
    datetimeInput: 'jYYYY/jMM/jDD HH:mm'
  },
  display: {
    // فرمت‌های نمایش در بخش‌های مختلف
    dateInput: 'jYYYY/jMM/jDD',
    monthInput: 'jMMMM jYYYY',
    yearInput: 'jYYYY',
    timeInput: 'HH:mm',
    datetimeInput: 'jYYYY/jMM/jDD HH:mm',

    // فرمت‌های نمایش در تقویم
    monthLabel: 'jMMMM jYYYY',
    yearLabel: 'jYYYY',

    // فرمت‌های نمایش در header های مختلف
    dateA11yLabel: 'jYYYY/jMM/jDD',
    monthYearA11yLabel: 'jMMMM jYYYY',
    popupHeaderDateLabel: 'jDD jMMMM'
  }
};
