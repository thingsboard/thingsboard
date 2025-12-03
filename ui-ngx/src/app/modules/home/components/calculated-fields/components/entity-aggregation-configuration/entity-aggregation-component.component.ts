///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Component, forwardRef, Input } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AggInterval,
  AggIntervalType,
  AggIntervalTypeTranslations,
  CalculatedFieldEntityAggregationConfiguration,
  CalculatedFieldOutput,
  CalculatedFieldType,
  defaultCalculatedFieldOutput,
  notEmptyObjectValidator,
} from '@shared/models/calculated-field.models';
import { filter, map } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AVG_MONTH, AVG_QUARTER, DAY, HOUR, MINUTE, SECOND, YEAR } from '@shared/models/time/time.models';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { merge, Observable } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import _moment from 'moment';

interface CalculatedFieldEntityAggregationConfigurationValue extends CalculatedFieldEntityAggregationConfiguration {
  interval: AggInterval & {allowOffsetSec?: boolean};
  allowWatermark: boolean;
}

enum TimeCategory {
  SECONDS = 'SECONDS',
  MINUTES = 'MINUTES',
  HOURS = 'HOURS',
  DAYS = 'DAYS'
}

@Component({
  selector: 'tb-entity-aggregation-component',
  templateUrl: './entity-aggregation-component.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityAggregationComponentComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => EntityAggregationComponentComponent),
      multi: true
    }
  ],
})
export class EntityAggregationComponentComponent implements ControlValueAccessor, Validator {

  @Input({required: true})
  entityId: EntityId;

  @Input({required: true})
  tenantId: string;

  @Input({required: true})
  entityName: string;

  @Input() testScript: (expression?: string) => Observable<string>;

  readonly minAllowedAggregationIntervalInSecForCF = getCurrentAuthState(this.store).minAllowedAggregationIntervalInSecForCF;
  readonly DayInSec = DAY / SECOND;

  entityAggregationConfiguration = this.fb.group({
    arguments: this.fb.control({}, notEmptyObjectValidator()),
    metrics: this.fb.control({}, notEmptyObjectValidator()),
    interval: this.fb.group({
      type: [AggIntervalType.HOUR],
      tz: ['', Validators.required],
      durationSec: [this.minAllowedAggregationIntervalInSecForCF, Validators.required],
      allowOffsetSec: [false],
      offsetSec: [this.minAllowedAggregationIntervalInSecForCF > 60 ? MINUTE / SECOND : 1, Validators.required],
    }),
    allowWatermark: [false],
    watermark: this.fb.group({
      duration: [HOUR/SECOND, Validators.required],
    }),
    output: this.fb.control<CalculatedFieldOutput>(defaultCalculatedFieldOutput),
  });

  arguments$ = this.entityAggregationConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => Object.keys(argumentsObj))
  );

  AggIntervalType = AggIntervalType;
  AggIntervalTypes = Object.values(AggIntervalType) as AggIntervalType[];
  AggIntervalTypeTranslations = AggIntervalTypeTranslations;

  hint: string;

  private propagateChange: (config: CalculatedFieldEntityAggregationConfiguration) => void = () => { };

  constructor(private fb: FormBuilder,
              private store: Store<AppState>,
              private translate: TranslateService,) {

    this.entityAggregationConfiguration.get('interval.type').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((type: AggIntervalType) => {
      this.checkAggIntervalType(type);
    });

    this.entityAggregationConfiguration.get('interval.allowOffsetSec').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((allow: boolean) => {
      this.checkIntervalDuration(allow);
    });

    this.entityAggregationConfiguration.get('allowWatermark').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((allow: boolean) => {
      this.checkWatermark(allow);
    });

    merge(
      this.entityAggregationConfiguration.get('interval.type').valueChanges,
      this.entityAggregationConfiguration.get('interval.durationSec').valueChanges,
      this.entityAggregationConfiguration.get('interval.offsetSec').valueChanges,
      this.entityAggregationConfiguration.get('interval.allowOffsetSec').valueChanges,
    ).pipe(
      filter(() =>  this.entityAggregationConfiguration.get('interval.allowOffsetSec').value),
      takeUntilDestroyed()
    ).subscribe(() => {
      this.updatedOffsetHint();
    });

    this.entityAggregationConfiguration.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value: CalculatedFieldEntityAggregationConfigurationValue) => {
      this.updatedModel(deepClone(value));
    });
  }

  validate(): ValidationErrors | null {
    return this.entityAggregationConfiguration.valid || this.entityAggregationConfiguration.disabled ? null : {invalidPropagateConfig: false};
  }

  writeValue(value: CalculatedFieldEntityAggregationConfiguration): void {
    const data: CalculatedFieldEntityAggregationConfigurationValue = {
      ...value,
      allowWatermark: isDefinedAndNotNull(value.watermark),
      interval: {...value.interval, allowOffsetSec: isDefinedAndNotNull(value?.interval?.offsetSec)}
    }
    this.entityAggregationConfiguration.patchValue(data, {emitEvent: false});
    this.checkAggIntervalType(this.entityAggregationConfiguration.get('interval.type').value);
    this.checkIntervalDuration(this.entityAggregationConfiguration.get('interval.allowOffsetSec').value);
    this.checkWatermark(this.entityAggregationConfiguration.get('allowWatermark').value);
    this.updatedOffsetHint();
    setTimeout(() => {
      this.entityAggregationConfiguration.get('arguments').updateValueAndValidity({onlySelf: true});
    });
  }

  registerOnChange(fn: (config: CalculatedFieldEntityAggregationConfiguration) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_: any): void { }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.entityAggregationConfiguration.disable({emitEvent: false});
    } else {
      this.entityAggregationConfiguration.enable({emitEvent: false});
      this.checkAggIntervalType(this.entityAggregationConfiguration.get('interval.type').value);
      this.checkIntervalDuration(this.entityAggregationConfiguration.get('interval.allowOffsetSec').value);
      this.checkWatermark(this.entityAggregationConfiguration.get('allowWatermark').value);
    }
  }

  get maxOffsetTime(): number {
    switch (this.entityAggregationConfiguration.get('interval.type').value as AggIntervalType) {
      case AggIntervalType.HOUR:
        return HOUR / SECOND - 1;
      case AggIntervalType.DAY:
        return DAY / SECOND - 1;
      case AggIntervalType.WEEK:
      case AggIntervalType.WEEK_SUN_SAT:
        return 7 * DAY / SECOND - 1;
      case AggIntervalType.MONTH:
        return AVG_MONTH / SECOND;
      case AggIntervalType.QUARTER:
        return AVG_QUARTER / SECOND - 1;
      case AggIntervalType.YEAR:
        return YEAR / SECOND - 1;
      case AggIntervalType.CUSTOM:
        return this.entityAggregationConfiguration.get('interval.durationSec').value - 1;
    }
  }

  private updatedModel(value: CalculatedFieldEntityAggregationConfigurationValue): void {
    value.type = CalculatedFieldType.ENTITY_AGGREGATION;
    if (!value.interval.allowOffsetSec) {
      delete value.interval.offsetSec;
    }
    delete value.interval.allowOffsetSec;
    if (!value.allowWatermark) {
      delete value.watermark;
    }
    delete value.allowWatermark;
    this.propagateChange(value);
  }

  private checkAggIntervalType(type: AggIntervalType) {
    if (type === AggIntervalType.CUSTOM) {
      this.entityAggregationConfiguration.get('interval.durationSec').enable({emitEvent: false});
    } else {
      this.entityAggregationConfiguration.get('interval.durationSec').disable({emitEvent: false});
    }
  }

  private checkIntervalDuration(allow: boolean) {
    if (allow) {
      this.entityAggregationConfiguration.get('interval.offsetSec').enable({emitEvent: false});
    } else {
      this.entityAggregationConfiguration.get('interval.offsetSec').disable({emitEvent: false});
      this.hint = '';
    }
  }

  private checkWatermark(allow: boolean) {
    if (allow) {
      this.entityAggregationConfiguration.get('watermark').enable({emitEvent: false});
    } else {
      this.entityAggregationConfiguration.get('watermark').disable({emitEvent: false});
    }
  }

  private updatedOffsetHint(): void {
    const offset = this.entityAggregationConfiguration.get('interval.offsetSec').value;
    const intervalType = this.entityAggregationConfiguration.get('interval.type').value as AggIntervalType;
    const durationSec = this.entityAggregationConfiguration.get('interval.durationSec').value;
    const offsetCategory = this.getTimeCategory(offset);
    const now = _moment.utc();
    let interval: string = '';
    if (intervalType === AggIntervalType.CUSTOM) {
      const durationSecCategory = this.getTimeCategory(durationSec);
      const formatString = this.getCustomFormatString(offsetCategory, durationSecCategory);
      const intervals: string[] = [];
      let allInterval = durationSec >= HOUR*6/SECOND && durationSec < DAY/SECOND;
      now.startOf('year').add(offset, 'seconds');

      let repeat = 2;
      if (allInterval) {
        repeat = Math.floor(DAY/SECOND/durationSec);
        if (repeat > 4) {
          repeat = 2;
          allInterval = false;
        }
      }

      for (let i = 0; i < repeat; i++) {
        const s1 = now.clone().add(i * durationSec, 'seconds').format(formatString);
        const s2 = now.clone().add((i + 1) * durationSec, 'seconds').format(formatString);
        intervals.push(`${s1} - ${s2}`);
      }
      interval = intervals.join('; ');

      if (allInterval) {
        this.hint = this.translate.instant('calculated-fields.aggregate-period-hint-offset', {interval});
      } else {
        interval += '…'
        this.hint = this.translate.instant('calculated-fields.aggregate-period-hint-offset-and-so-on', {interval});
      }
    } else {
      interval = this.buildStandardIntervalString(now, intervalType, offset, offsetCategory);
      this.hint = this.translate.instant('calculated-fields.aggregate-period-hint-offset-and-so-on', { interval });
    }
  }

  private getTimeCategory(seconds: number): TimeCategory {
    if (seconds % (DAY / SECOND) === 0) {
      return TimeCategory.DAYS;
    }
    if (seconds % (HOUR / SECOND) === 0) {
      return TimeCategory.HOURS;
    }
    if (seconds % (MINUTE / SECOND) === 0) {
      return TimeCategory.MINUTES;
    }
    return TimeCategory.SECONDS;
  }

  private getCustomFormatString(offsetCat: TimeCategory, durationCat: TimeCategory): string {
    if (durationCat === TimeCategory.DAYS) {
      if (offsetCat === TimeCategory.SECONDS) {
        return '[Day] D, HH:mm:ss';
      }
      if (offsetCat === TimeCategory.MINUTES || offsetCat === TimeCategory.HOURS) {
        return '[Day] D, HH:mm';
      }
      return '[Day] D';
    } else {
      if (offsetCat === TimeCategory.SECONDS) {
        return 'HH:mm:ss';
      }
      return 'HH:mm';
    }
  }

  private formatAdditiveInterval(now: _moment.Moment, addUnit: 'hour' | 'day' | 'month' | 'quarter', offsetCat: TimeCategory,
                                 formats: { [key in TimeCategory]?: { s1: string, s2: string, s3: string } }): string {
    const formatTs = formats[offsetCat] || formats[TimeCategory.SECONDS];

    if (!formatTs) {
      return '';
    }

    const s1 = now.format(formatTs.s1);
    const s2 = now.clone().add(1, addUnit).format(formatTs.s2);
    const s3 = now.clone().add(2, addUnit).format(formatTs.s3);

    return `${s1} - ${s2}; ${s2} - ${s3}…`;
  }

  private formatNextInterval(now: _moment.Moment, offsetCat: TimeCategory, secFmt: string, minHourFmt: string, dayFmt: string): string {
    let s1: string;
    if (offsetCat === TimeCategory.SECONDS) {
      s1 = now.format(secFmt);
    } else if (offsetCat === TimeCategory.MINUTES || offsetCat === TimeCategory.HOURS) {
      s1 = now.format(minHourFmt);
    } else {
      s1 = now.format(dayFmt);
    }

    const s2 = `Next ${s1}`;
    const s3 = `Following ${s1}`;
    return `${s1} - ${s2}; ${s2} - ${s3}… `;
  }

  private buildStandardIntervalString(now: _moment.Moment, type: AggIntervalType, offset: number, offsetCat: TimeCategory): string {
    switch (type) {
      case AggIntervalType.HOUR:
        now.startOf('day').add(offset, 'seconds');
        return this.formatAdditiveInterval(now, 'hour', offsetCat, {
          [TimeCategory.SECONDS]: { s1: 'HH:mm:ss', s2: 'HH:mm:ss', s3: 'HH:mm:ss' },
          [TimeCategory.MINUTES]: { s1: 'HH:mm:ss', s2: 'HH:mm', s3: 'HH:mm' }
        });

      case AggIntervalType.DAY:
        now.startOf('month').add(offset, 'seconds');
        return this.formatAdditiveInterval(now, 'day', offsetCat, {
          [TimeCategory.SECONDS]: { s1: '[Day] D, HH:mm:ss', s2: '[Day] D, HH:mm:ss', s3: '[Day] D, HH:mm:ss' },
          [TimeCategory.MINUTES]: { s1: '[Day] D, HH:mm:ss', s2: '[Day] D, HH:mm', s3: '[Day] D, HH:mm' },
          [TimeCategory.HOURS]: { s1: 'HH:mm:ss', s2: '[Day] D, HH:mm', s3: '[Day] D, HH:mm' } // Note: Original logic, s1 format is different
        });

      case AggIntervalType.WEEK:
        now.isoWeekday(1).startOf('isoWeek').add(offset, 'seconds');
        return this.formatNextInterval(now, offsetCat, 'ddd, HH:mm:ss', 'ddd, HH:mm', 'ddd');

      case AggIntervalType.WEEK_SUN_SAT:
        now.startOf('week').add(offset, 'seconds');
        return this.formatNextInterval(now, offsetCat, 'ddd, HH:mm:ss', 'ddd, HH:mm', 'ddd');

      case AggIntervalType.MONTH:
        now.startOf('year').add(offset, 'seconds');
        return this.formatAdditiveInterval(now, 'month', offsetCat, {
          [TimeCategory.SECONDS]: { s1: 'Do [of month], HH:mm:ss', s2: '[Next] Do, HH:mm:ss', s3: '[Following] Do, HH:mm:ss' },
          [TimeCategory.MINUTES]: { s1: 'Do [of month], HH:mm', s2: '[Next] Do, HH:mm', s3: '[Following] Do, HH:mm' },
          [TimeCategory.HOURS]: { s1: 'Do [of month], HH:mm', s2: '[Next] Do, HH:mm', s3: '[Following] Do, HH:mm' },
          [TimeCategory.DAYS]: { s1: 'Do [of month]', s2: '[Next] Do', s3: '[Following] Do' }
        });

      case AggIntervalType.QUARTER:
        now.startOf('year').add(offset, 'seconds');
        return this.formatAdditiveInterval(now, 'quarter', offsetCat, {
          [TimeCategory.SECONDS]: { s1: 'MMM Do, HH:mm:ss', s2: 'MMM Do, HH:mm:ss', s3: 'MMM Do, HH:mm:ss' },
          [TimeCategory.MINUTES]: { s1: 'MMM Do, HH:mm', s2: 'MMM Do, HH:mm', s3: 'MMM Do, HH:mm' },
          [TimeCategory.HOURS]: { s1: 'MMM Do, HH:mm', s2: 'MMM Do, HH:mm', s3: 'MMM Do, HH:mm' },
          [TimeCategory.DAYS]: { s1: 'MMM Do', s2: 'MMM Do', s3: 'MMM Do' }
        });

      case AggIntervalType.YEAR:
        now.startOf('year').add(offset, 'seconds');
        return this.formatNextInterval(now, offsetCat, 'MMM Do, HH:mm:ss', 'MMM Do, HH:mm', 'MMM Do');

      default:
        return '';
    }
  }
}
