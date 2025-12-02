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

import { Component, DestroyRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import { TimeUnit, timeUnitTranslations } from "@home/components/rule-node/rule-node-config.models";
import { AlarmRuleFilterPredicateType, NoDataAlarmRuleFilterPredicate } from "@shared/models/alarm-rule.models";
import { isDefinedAndNotNull } from "@core/utils";

@Component({
  selector: 'tb-alarm-rule-filter-predicate-no-data-value',
  templateUrl: './alarm-rule-filter-predicate-no-data-value.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmRuleFilterPredicateNoDataValueComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => AlarmRuleFilterPredicateNoDataValueComponent),
      multi: true
    }
  ]
})
export class AlarmRuleFilterPredicateNoDataValueComponent implements ControlValueAccessor, Validator, OnInit, OnChanges {

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  @Input()
  valueType: AlarmRuleFilterPredicateType;

  @Input()
  argumentInUse: string;

  valueTypeEnum = AlarmRuleFilterPredicateType;

  filterPredicateValueNoDataFormGroup = this.fb.group({
    type: ['NO_DATA'],
    unit: [TimeUnit.MINUTES, Validators.required],
    duration: this.fb.group({
      staticValue: [null as null | number, [Validators.required, Validators.min(1)]],
      dynamicValueArgument: ['', Validators.required]
    })
  });

  timeUnits = [TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS];
  timeUnitsTranslationMap = timeUnitTranslations;


  dynamicModeControl = this.fb.control(false);

  argumentsList: Array<string>;

  private propagateChange= (v: any) => { };

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.argumentsList = this.arguments ? Object.keys(this.arguments): [];
    this.filterPredicateValueNoDataFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.dynamicModeControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => this.updateValueModeValidators(value));
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.argumentInUse) {
      const argumentInUseChanges = changes.argumentInUse;
      if (!argumentInUseChanges.firstChange && argumentInUseChanges.currentValue !== argumentInUseChanges.previousValue) {
        if (this.dynamicModeControl.value) {
          if (this.argumentInUse === this.filterPredicateValueNoDataFormGroup.get('duration.dynamicValueArgument').value) {
            this.filterPredicateValueNoDataFormGroup.get('duration.dynamicValueArgument').setErrors({argumentInUse: true});
            this.filterPredicateValueNoDataFormGroup.updateValueAndValidity();
          }
        }
      }
    }
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.filterPredicateValueNoDataFormGroup.disable({emitEvent: false});
      this.dynamicModeControl.disable({emitEvent: false});
    } else {
      this.filterPredicateValueNoDataFormGroup.enable({emitEvent: false});
      this.dynamicModeControl.enable({emitEvent: false});
      this.updateValueModeValidators(this.dynamicModeControl.value);
    }
  }

  private updateValueModeValidators(isDynamicMode: boolean): void {
    if (isDynamicMode) {
      this.filterPredicateValueNoDataFormGroup.get('duration.staticValue').disable({emitEvent: false});
      this.filterPredicateValueNoDataFormGroup.get('duration.dynamicValueArgument').enable();
      setTimeout(()=> {
        if (this.filterPredicateValueNoDataFormGroup.get('duration.dynamicValueArgument').value && this.argumentInUse === this.filterPredicateValueNoDataFormGroup.get('dynamicValueArgument').value) {
          this.filterPredicateValueNoDataFormGroup.get('duration.dynamicValueArgument').setErrors({argumentInUse: true});
          this.filterPredicateValueNoDataFormGroup.updateValueAndValidity();
        }
      }, 0);
    } else {
      this.filterPredicateValueNoDataFormGroup.get('duration.dynamicValueArgument').disable({emitEvent: false});
      this.filterPredicateValueNoDataFormGroup.get('duration.staticValue').enable();
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  validate(): ValidationErrors | null {
    return this.filterPredicateValueNoDataFormGroup.valid ? null : {
      filterPredicateValue: {valid: false}
    };
  }

  writeValue(predicateValue: NoDataAlarmRuleFilterPredicate): void {
    if (isDefinedAndNotNull(predicateValue.duration.dynamicValueArgument)) {
      const availableArgument = this.argumentsList.filter(arg => arg !== this.argumentInUse);
      if (!availableArgument.includes(predicateValue.duration.dynamicValueArgument)) {
        predicateValue.duration.dynamicValueArgument = '';
      }
      this.dynamicModeControl.patchValue(true, {emitEvent: false});
    }
    this.filterPredicateValueNoDataFormGroup.patchValue(predicateValue, {emitEvent: false});
  }

  private updateModel() {
    this.propagateChange(this.filterPredicateValueNoDataFormGroup.value);
  }
}
