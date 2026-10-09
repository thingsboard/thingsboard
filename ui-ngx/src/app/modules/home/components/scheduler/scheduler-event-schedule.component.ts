// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, ChangeDetectorRef, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALUE_ACCESSOR,
  Validators,
  NG_VALIDATORS,
  Validator,
  AbstractControl,
  ValidationErrors
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { PageComponent } from '@shared/components/page.component';
import {
  SchedulerEventSchedule,
  SchedulerRepeatType,
  schedulerRepeatTypeTranslationMap,
  SchedulerTimeUnit,
  schedulerTimeUnitTranslationMap
} from '@shared/models/scheduler-event.models';
import _moment from 'moment';
import * as momentTz from 'moment-timezone';
import { isDefined } from '@core/utils';
import { ErrorStateMatcher } from '@angular/material/core';
import { scheduleWeekDays } from '@home/components/scheduler/scheduler-events.models';
import { MatChipListbox } from '@angular/material/chips';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

interface SchedulerEventScheduleConfig {
  timezone: string;
  startDate?: Date;
  repeat?: boolean;
  repeatType?: SchedulerRepeatType;
  weeklyRepeat?: boolean[];
  endsOnDate?: Date;
  days?: number;
  weeks?: number;
  timerRepeat?: {
    repeatInterval?: number;
    timeUnit?: SchedulerTimeUnit;
  };
}

export class EndsOnDateErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(control: UntypedFormControl | null): boolean {
    const invalidCtrl = !!(control?.invalid);
    const invalidParent = !!(control?.parent &&
      control?.parent.invalid && control?.parent.hasError('endsOnDateValidator'));
    return (invalidCtrl || invalidParent);
  }
}

@Component({
    selector: 'tb-scheduler-event-schedule',
    templateUrl: './scheduler-event-schedule.component.html',
    styleUrls: ['./scheduler-event-schedule.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SchedulerEventScheduleComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => SchedulerEventScheduleComponent),
            multi: true
        }],
    standalone: false
})
export class SchedulerEventScheduleComponent extends PageComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy, Validator {

  modelValue: SchedulerEventScheduleConfig | null;

  endsOnDateMatcher = new EndsOnDateErrorStateMatcher();

  scheduleConfigFormGroup: UntypedFormGroup;

  schedulerRepeatTypes = Object.keys(SchedulerRepeatType);

  schedulerRepeatType = SchedulerRepeatType;

  schedulerRepeatTypeTranslations = schedulerRepeatTypeTranslationMap;

  schedulerTimeUnits = Object.keys(SchedulerTimeUnit);

  schedulerTimeUnitTranslations = schedulerTimeUnitTranslationMap;

  weekDays = scheduleWeekDays;

  @Input()
  disabled: boolean;

  private lastAppliedTimezone: string;

  private destroy$ = new Subject<void>();

  private propagateChange = (v: any) => {};

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef) {
    super(store);
    this.scheduleConfigFormGroup = this.fb.group({
      timezone: [null, [Validators.required]],
      startDate: [null, [Validators.required]],
      repeat: [null, []],
      repeatType: [null, [Validators.required]],
      weeklyRepeat: this.fb.array(this.createDefaultWeeklyRepeat()),
      endsOnDate: [null, [Validators.required]],
      days: [null, [Validators.min(1), Validators.required]],
      weeks: [null, [Validators.min(1), Validators.required]],
      timerRepeat: this.fb.group(
        {
          repeatInterval: [null, [Validators.required, Validators.min(0)]],
          timeUnit: [null, [Validators.required]]
        }
      )
    }, {validator: this.endsOnDateValidator('startDate', 'endsOnDate')});

    this.scheduleConfigFormGroup.get('timezone').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((timezone: string) => {
      if (timezone !== this.lastAppliedTimezone && timezone) {
        let startDate: Date = this.scheduleConfigFormGroup.get('startDate').value;
        const startTime = this.dateTimeToUtcTime(startDate, this.lastAppliedTimezone);
        startDate = this.dateFromUtcTime(startTime, timezone);
        this.scheduleConfigFormGroup.get('startDate').patchValue(startDate, {emitEvent: false});
        let endsOnDate: Date = this.scheduleConfigFormGroup.get('endsOnDate').value;
        if (endsOnDate) {
          const endsOnTime = this.dateToUtcTime(endsOnDate, this.lastAppliedTimezone);
          endsOnDate = this.dateFromUtcTime(endsOnTime, timezone);
          this.scheduleConfigFormGroup.get('endsOnDate').patchValue(endsOnDate, {emitEvent: false});
        }
        this.lastAppliedTimezone = timezone;
      }
    });

    this.scheduleConfigFormGroup.get('repeat').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((repeat: boolean) => {
      if (repeat) {
        this.scheduleConfigFormGroup.get('repeatType').patchValue(SchedulerRepeatType.DAILY, {emitEvent: false});
        const startDate: Date | null = this.scheduleConfigFormGroup.get('startDate').value;
        if (startDate) {
          const endsOnDate = new Date(
            startDate.getFullYear(),
            startDate.getMonth(),
            startDate.getDate() + 5);
          this.scheduleConfigFormGroup.get('endsOnDate').patchValue(endsOnDate, {emitEvent: false});
        }
      }
      this.updateEnabledState();
    });

    this.scheduleConfigFormGroup.get('repeatType').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((repeatType: SchedulerRepeatType) => {
      if (repeatType === SchedulerRepeatType.WEEKLY) {
        const startDate: Date = this.scheduleConfigFormGroup.get('startDate').value;
        this.scheduleConfigFormGroup.get('weeklyRepeat').patchValue(this.createDefaultWeeklyRepeat(startDate), {emitEvent: false});
      }
      this.updateEnabledState();
    });

    this.scheduleConfigFormGroup.get('weeklyRepeat').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((weeklyRepeat: boolean[]) => {
      const startDate: Date = this.scheduleConfigFormGroup.get('startDate').value;
      this.scheduleConfigFormGroup.get('weeklyRepeat').patchValue(this.createDefaultWeeklyRepeat(startDate, weeklyRepeat),
        {emitEvent: false});
    });

    this.scheduleConfigFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  private endsOnDateValidator(startDate: string | null, endsOnDate: string | null) {
    return (group: UntypedFormGroup): {[key: string]: any} => {
      if (group.controls[startDate].valid && group.controls[endsOnDate].valid &&
        (group.controls[startDate].value.getTime() > group.controls[endsOnDate].value.getTime())) {
        return { endsOnDateValidator: true };
      }
      return null;
    };
  }

  weeklyRepeatControl(index: number): UntypedFormControl {
    return (this.scheduleConfigFormGroup.get('weeklyRepeat') as UntypedFormArray).at(index) as UntypedFormControl;
  }

  onChipClicked(index: number, chipListBox: MatChipListbox): void {
    const control = this.weeklyRepeatControl(index);
    const isSelected = control.value;
    const options = chipListBox._chips.toArray();
    const selectedOptions = options.filter(chip => chip.selected);

    if (isSelected && !selectedOptions.length) {
      options[index]._setSelectedState(true, false, false);
    } else {
      control.setValue(!isSelected);
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.updateEnabledState();
  }

  private updateEnabledState() {
    if (this.disabled) {
      this.scheduleConfigFormGroup.disable({emitEvent: false});
    } else {
      this.scheduleConfigFormGroup.enable({emitEvent: false});
      const repeat: boolean = this.scheduleConfigFormGroup.get('repeat').value;
      if (!repeat) {
        this.scheduleConfigFormGroup.get('repeatType').disable({emitEvent: false});
        this.scheduleConfigFormGroup.get('weeklyRepeat').disable({emitEvent: false});
        this.scheduleConfigFormGroup.get('endsOnDate').disable({emitEvent: false});
        this.scheduleConfigFormGroup.get('timerRepeat').disable({emitEvent: false});
        this.scheduleConfigFormGroup.get('days').disable({emitEvent: false});
        this.scheduleConfigFormGroup.get('weeks').disable({emitEvent: false});
      } else {
        const repeatType: SchedulerRepeatType = this.scheduleConfigFormGroup.get('repeatType').value;
        if (repeatType !== SchedulerRepeatType.WEEKLY) {
          this.scheduleConfigFormGroup.get('weeklyRepeat').disable({emitEvent: false});
        }
        if (repeatType !== SchedulerRepeatType.TIMER) {
          this.scheduleConfigFormGroup.get('timerRepeat').disable({emitEvent: false});
        }
        if (repeatType !== SchedulerRepeatType.EVERY_N_DAYS) {
          this.scheduleConfigFormGroup.get('days').disable({emitEvent: false});
        }
        if (repeatType !== SchedulerRepeatType.EVERY_N_WEEKS) {
          this.scheduleConfigFormGroup.get('weeks').disable({emitEvent: false});
        }
      }
    }
    this.cd.detectChanges();
  }

  writeValue(value: SchedulerEventSchedule | null): void {
    this.modelValue = this.toSchedulerEventScheduleConfig(value);
    let doUpdate = false;
    if (!this.modelValue) {
      this.modelValue = this.createDefaultSchedulerEventScheduleConfig();
      doUpdate = true;
    } else if (this.modelValue.timezone !== value.timezone) {
      doUpdate = true;
    }
    this.lastAppliedTimezone = this.modelValue.timezone;
    this.scheduleConfigFormGroup.reset(this.modelValue, {emitEvent: false});
    this.updateEnabledState();
    if (doUpdate) {
      setTimeout(() => {
        this.updateModel();
      }, 0);
    }
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (!this.scheduleConfigFormGroup.valid && !this.disabled) {
      return {
        scheduleConfigForm: {
          valid: false
        }
      };
    }

    return null;
  }

  getSchedulerTimeUnitTranslations(timeUnit: string): string {
    return this.schedulerTimeUnitTranslations.get(timeUnit as SchedulerTimeUnit);
  }
  getSchedulerRepeatTypeTranslations(repeatType: string): string {
    return this.schedulerRepeatTypeTranslations.get(repeatType as SchedulerRepeatType);
  }

  private toSchedulerEventScheduleConfig(value: SchedulerEventSchedule): SchedulerEventScheduleConfig {
    if (value) {
      const timezone = value.timezone || momentTz.tz.guess();
      const config: SchedulerEventScheduleConfig = {
        timezone,
        startDate: this.dateFromUtcTime(value.startTime, timezone)
      };
      if (value.repeat) {
        config.repeat = true;
        config.repeatType = value.repeat.type;
        if (value.repeat.type === SchedulerRepeatType.WEEKLY && value.repeat.repeatOn) {
          config.weeklyRepeat = this.createDefaultWeeklyRepeat();
          value.repeat.repeatOn.forEach((repeatOn) => {
            config.weeklyRepeat[repeatOn] = true;
          });
        } else if (value.repeat.type === SchedulerRepeatType.TIMER) {
          config.timerRepeat = {
            repeatInterval: value.repeat.repeatInterval,
            timeUnit: value.repeat.timeUnit
          };
        } else if (value.repeat.type === SchedulerRepeatType.EVERY_N_DAYS) {
          config.days = value.repeat.days;
        } else if (value.repeat.type === SchedulerRepeatType.EVERY_N_WEEKS) {
          config.weeks = value.repeat.weeks;
        }
        config.endsOnDate = this.dateFromUtcTime(value.repeat.endsOn, timezone);
      } else {
        config.repeat = false;
      }
      return config;
    }
    return null;
  }

  private fromSchedulerEventScheduleConfig(value: SchedulerEventScheduleConfig): SchedulerEventSchedule {
    if (value) {
      const schedule: SchedulerEventSchedule = {
        timezone: value.timezone,
        startTime: this.dateTimeToUtcTime(value.startDate, value.timezone)
      };
      if (value.repeat) {
        schedule.repeat = {
          type: value.repeatType,
          endsOn: this.dateToUtcTime(value.endsOnDate, value.timezone)
        };
        if (value.repeatType === SchedulerRepeatType.WEEKLY) {
          schedule.repeat.repeatOn = [];
          for (let i = 0; i < 7; i++) {
            if (value.weeklyRepeat[i]) {
              schedule.repeat.repeatOn.push(i);
            }
          }
        } else if (value.repeatType === SchedulerRepeatType.TIMER) {
          schedule.repeat.repeatInterval = value.timerRepeat.repeatInterval;
          schedule.repeat.timeUnit = value.timerRepeat.timeUnit;
        } else if (value.repeatType === SchedulerRepeatType.EVERY_N_DAYS) {
          schedule.repeat.days = value.days;
        } else if (value.repeatType === SchedulerRepeatType.EVERY_N_WEEKS) {
          schedule.repeat.weeks = value.weeks;
        }
      }
      return schedule;
    }
    return null;
  }

  private createDefaultSchedulerEventScheduleConfig(): SchedulerEventScheduleConfig {
    const scheduleConfig: SchedulerEventScheduleConfig = {
      timezone: momentTz.tz.guess()
    };
    const date = new Date();
    scheduleConfig.startDate = new Date(
      date.getFullYear(),
      date.getMonth(),
      date.getDate());
    scheduleConfig.repeat = false;
    return scheduleConfig;
  }

  private createDefaultWeeklyRepeat(startDate?: Date, weeklyRepeat?: boolean[]): boolean[] {
    if (!weeklyRepeat) {
      weeklyRepeat = [];
      for (let i = 0; i < 7; i++) {
        weeklyRepeat[i] = false;
      }
    }
    if (isDefined(startDate) && startDate !== null) {
      const setCurrentDate = weeklyRepeat.filter(repeat => repeat).length === 0;
      if (setCurrentDate) {
        const day = _moment(startDate).day();
        weeklyRepeat[day] = true;
      }
    }
    return weeklyRepeat;
  }

  private updateModel() {
    if (this.scheduleConfigFormGroup.valid) {
      this.modelValue = this.scheduleConfigFormGroup.value;
      this.propagateChange(this.fromSchedulerEventScheduleConfig(this.modelValue));
    } else {
      this.propagateChange(null);
    }
  }

  private dateFromUtcTime(time: number, timezone: string): Date {
    const offset = momentTz.tz.zone(timezone).utcOffset(time) * 60 * 1000;
    return new Date(time - offset + new Date(time).getTimezoneOffset() * 60 * 1000);
  }

  private dateTimeToUtcTime(date: Date, timezone: string): number {
    const ts = new Date(
      date.getFullYear(),
      date.getMonth(),
      date.getDate(),
      date.getHours(),
      date.getMinutes(),
      date.getSeconds(),
      date.getMilliseconds()
    ).getTime();
    const offset = momentTz.tz.zone(timezone).utcOffset(ts) * 60 * 1000;
    return ts + offset - new Date(ts).getTimezoneOffset() * 60 * 1000;
  }

  private dateToUtcTime(date: Date, timezone: string): number {
    const ts = new Date(
      date.getFullYear(),
      date.getMonth(),
      date.getDate()
    ).getTime();
    const offset = momentTz.tz.zone(timezone).utcOffset(ts) * 60 * 1000;
    return ts + offset - new Date(ts).getTimezoneOffset() * 60 * 1000;
  }

}
