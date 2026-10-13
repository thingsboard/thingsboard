// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef, Input, OnDestroy, OnChanges, SimpleChanges } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { MqttQos, MqttQosTranslation, MqttTopicFilter } from '@shared/models/integration.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { isNumber } from '@core/utils';

@Component({
    selector: 'tb-mqtt-topic-filters',
    templateUrl: './mqtt-topic-filters.component.html',
    styleUrls: ['./mqtt-topic-filters.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MqttTopicFiltersComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => MqttTopicFiltersComponent),
            multi: true,
        }],
    standalone: false
})
export class MqttTopicFiltersComponent implements ControlValueAccessor, Validator, OnDestroy, OnChanges {

  mqttTopicFiltersForm: UntypedFormGroup;
  mqttQosTypes = Object.values(MqttQos).filter((v): v is MqttQos => isNumber(v));
  MqttQosTranslation = MqttQosTranslation;

  @Input()
  disabled: boolean;

  @Input()
  excludeQos: MqttQos[];

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    this.mqttTopicFiltersForm = this.fb.group({
      filters: this.fb.array([], Validators.required)
    });
    this.mqttTopicFiltersForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateModel(value.filters);
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (propName === 'excludeQos' && change.currentValue !== change.previousValue) {
        const excludeQos = change.currentValue;
        if (excludeQos?.length) {
          this.mqttQosTypes = Object.values(MqttQos).filter((v): v is MqttQos  => !excludeQos.includes(v) && isNumber(v));
        } else {
          this.mqttQosTypes = Object.values(MqttQos).filter((v): v is MqttQos => isNumber(v));
        }
      }
    }
  }

  writeValue(value: MqttTopicFilter[]) {
    if (this.mqttFiltersFromArray.length === value?.length) {
      this.mqttTopicFiltersForm.get('filters').patchValue(value, {emitEvent: false});
    } else {
      const filtersControls: Array<AbstractControl> = [];
      if (value) {
        value.forEach((filter) => {
          filtersControls.push(this.fb.group({
            filter: [filter.filter, [Validators.required]],
            qos: [filter.qos, [Validators.required]]
          }));
        });
      }
      this.mqttTopicFiltersForm.setControl('filters', this.fb.array(filtersControls), {emitEvent: false});
      if (this.disabled) {
        this.mqttTopicFiltersForm.disable({emitEvent: false});
      } else {
        this.mqttTopicFiltersForm.enable({emitEvent: false});
      }
    }
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.mqttTopicFiltersForm.disable({emitEvent: false});
    } else {
      this.mqttTopicFiltersForm.enable({emitEvent: false});
    }
  }

  get mqttFiltersFromArray(): UntypedFormArray {
    return this.mqttTopicFiltersForm.get('filters') as UntypedFormArray;
  }

  addTopicFilter() {
    this.mqttFiltersFromArray.push(this.fb.group({
      filter: ['', [Validators.required]],
      qos: [0, [Validators.required]]
    }));
  }

  private updateModel(value: MqttTopicFilter[]) {
    this.propagateChange(value);
  }

  validate(): ValidationErrors | null {
    return this.mqttTopicFiltersForm.valid ? null : {
      mqttTopicFilters: {valid: false}
    };
  }

  getMqttQosTranslation(mqttQoS: number) {
    return this.MqttQosTranslation.get(mqttQoS as MqttQos);
  }
}
