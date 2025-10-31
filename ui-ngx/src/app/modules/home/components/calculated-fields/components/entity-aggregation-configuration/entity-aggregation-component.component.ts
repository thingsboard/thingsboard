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
  notEmptyObjectValidator,
  OutputType
} from '@shared/models/calculated-field.models';
import { map } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HOUR, MINUTE, SECOND } from '@shared/models/time/time.models';
import { isDefinedAndNotNull } from '@core/utils';

interface CalculatedFieldEntityAggregationConfigurationValue extends CalculatedFieldEntityAggregationConfiguration {
  interval: AggInterval & {allowOffsetMillis?: boolean};
  allowWatermark: boolean;
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


  entityAggregationConfiguration = this.fb.group({
    arguments: this.fb.control({}, notEmptyObjectValidator()),
    metrics: this.fb.control({}, notEmptyObjectValidator()),
    interval: this.fb.group({
      type: [AggIntervalType.HOUR],
      tz: ['', Validators.required],
      multiplier: [HOUR/SECOND, Validators.required],
      allowOffsetMillis: [false],
      offsetMillis: [MINUTE/SECOND, Validators.required],
    }),
    allowWatermark: [false],
    watermark: this.fb.group({
      duration: [6 * MINUTE / SECOND, Validators.required],
      checkInterval: [MINUTE / SECOND, Validators.required],
    }),
    output: this.fb.control<CalculatedFieldOutput>({
      type: OutputType.Timeseries,
    }),
  });

  arguments$ = this.entityAggregationConfiguration.get('arguments').valueChanges.pipe(
    map(argumentsObj => Object.keys(argumentsObj))
  );

  AggIntervalType = AggIntervalType;
  AggIntervalTypes = Object.values(AggIntervalType) as AggIntervalType[];
  AggIntervalTypeTranslations = AggIntervalTypeTranslations;

  private propagateChange: (config: CalculatedFieldEntityAggregationConfiguration) => void = () => { };

  constructor(private fb: FormBuilder) {

    this.entityAggregationConfiguration.get('interval.type').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((type: AggIntervalType) => {
      this.checkAggIntervalType(type);
    });

    this.entityAggregationConfiguration.get('interval.allowOffsetMillis').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((allow: boolean) => {
      this.checkIntervalDuration(allow);
    });

    this.entityAggregationConfiguration.get('allowWatermark').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((allow: boolean) => {
      this.checkWatermark(allow);
    });

    this.entityAggregationConfiguration.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value: CalculatedFieldEntityAggregationConfigurationValue) => {
      this.updatedModel(value);
    });
  }

  validate(): ValidationErrors | null {
    return this.entityAggregationConfiguration.valid || this.entityAggregationConfiguration.disabled ? null : {invalidPropagateConfig: false};
  }

  writeValue(value: CalculatedFieldEntityAggregationConfiguration): void {
    const data: CalculatedFieldEntityAggregationConfigurationValue = {
      ...value,
      allowWatermark: isDefinedAndNotNull(value.watermark),
      interval: {...value.interval, allowOffsetMillis: isDefinedAndNotNull(value?.interval?.offsetMillis)}
    }
    this.entityAggregationConfiguration.patchValue(data, {emitEvent: false});
    this.checkAggIntervalType(this.entityAggregationConfiguration.get('interval.type').value);
    this.checkIntervalDuration(this.entityAggregationConfiguration.get('interval.allowOffsetMillis').value);
    this.checkWatermark(this.entityAggregationConfiguration.get('allowWatermark').value);
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
      this.checkIntervalDuration(this.entityAggregationConfiguration.get('interval.allowOffsetMillis').value);
      this.checkWatermark(this.entityAggregationConfiguration.get('allowWatermark').value);
    }
  }

  private updatedModel(value: CalculatedFieldEntityAggregationConfigurationValue): void {
    value.type = CalculatedFieldType.ENTITY_AGGREGATION;
    if (!value.interval.allowOffsetMillis) {
      delete value.interval.offsetMillis;
    }
    delete value.interval.offsetMillis;
    if (!value.allowWatermark) {
      delete value.watermark;
    }
    delete value.allowWatermark;
    this.propagateChange(value);
  }

  private checkAggIntervalType(type: AggIntervalType) {
    if (type === AggIntervalType.CUSTOM) {
      this.entityAggregationConfiguration.get('interval.multiplier').enable({emitEvent: false});
    } else {
      this.entityAggregationConfiguration.get('interval.multiplier').disable({emitEvent: false});
    }
  }

  private checkIntervalDuration(allow: boolean) {
    if (allow) {
      this.entityAggregationConfiguration.get('interval.offsetMillis').enable({emitEvent: false});
    } else {
      this.entityAggregationConfiguration.get('interval.offsetMillis').disable({emitEvent: false});
    }
  }

  private checkWatermark(allow: boolean) {
    if (allow) {
      this.entityAggregationConfiguration.get('watermark').enable({emitEvent: false});
    } else {
      this.entityAggregationConfiguration.get('watermark').disable({emitEvent: false});
    }
  }
}
