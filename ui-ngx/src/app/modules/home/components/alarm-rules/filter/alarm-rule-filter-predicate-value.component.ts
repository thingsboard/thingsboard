///
/// Copyright © 2016-2026 The Thingsboard Authors
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
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { EntityKeyValueType } from '@shared/models/query/query.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import { AlarmRuleValue } from "@shared/models/alarm-rule.models";
import { FormControlsFrom } from "@shared/models/tenant.model";
import { isDefinedAndNotNull } from "@core/utils";

@Component({
  selector: 'tb-alarm-rule-filter-predicate-value',
  templateUrl: './alarm-rule-filter-predicate-value.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmRuleFilterPredicateValueComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => AlarmRuleFilterPredicateValueComponent),
      multi: true
    }
  ]
})
export class AlarmRuleFilterPredicateValueComponent implements ControlValueAccessor, Validator, OnInit, OnChanges {

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  @Input()
  valueType: EntityKeyValueType;

  @Input()
  argumentInUse: string;

  valueTypeEnum = EntityKeyValueType;

  filterPredicateValueFormGroup: FormGroup<FormControlsFrom<AlarmRuleValue<string | number | boolean>>>;

  dynamicModeControl = this.fb.control(false);

  argumentsList: Array<string>;

  private propagateChange= (v: any) => { };

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.argumentsList = this.arguments ? Object.keys(this.arguments): [];
    let defaultValue: string | number | boolean;
    let defaultValueValidators: ValidatorFn[];
    switch (this.valueType) {
      case EntityKeyValueType.STRING:
        defaultValue = '';
        defaultValueValidators = [];
        break;
      case EntityKeyValueType.NUMERIC:
        defaultValue = 0;
        defaultValueValidators = [Validators.required];
        break;
      case EntityKeyValueType.BOOLEAN:
        defaultValue = false;
        defaultValueValidators = [];
        break;
      case EntityKeyValueType.DATE_TIME:
        defaultValue = Date.now();
        defaultValueValidators = [Validators.required];
        break;
    }
    this.filterPredicateValueFormGroup = this.fb.group({
      staticValue: [defaultValue, defaultValueValidators],
      dynamicValueArgument: ['', Validators.required]
    });
    this.filterPredicateValueFormGroup.valueChanges.pipe(
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
          if (this.argumentInUse === this.filterPredicateValueFormGroup.get('dynamicValueArgument').value) {
            this.filterPredicateValueFormGroup.get('dynamicValueArgument').setErrors({argumentInUse: true});
            this.filterPredicateValueFormGroup.updateValueAndValidity();
          }
        }
      }
    }
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.filterPredicateValueFormGroup.disable({emitEvent: false});
      this.dynamicModeControl.disable({emitEvent: false});
    } else {
      this.filterPredicateValueFormGroup.enable({emitEvent: false});
      this.dynamicModeControl.enable({emitEvent: false});
      this.updateValueModeValidators(this.dynamicModeControl.value);
    }
  }

  private updateValueModeValidators(isDynamicMode: boolean): void {
    if (isDynamicMode) {
      this.filterPredicateValueFormGroup.get('staticValue').disable({emitEvent: false});
      this.filterPredicateValueFormGroup.get('dynamicValueArgument').enable();
      setTimeout(()=> {
        if (this.filterPredicateValueFormGroup.get('dynamicValueArgument').value && this.argumentInUse === this.filterPredicateValueFormGroup.get('dynamicValueArgument').value) {
          this.filterPredicateValueFormGroup.get('dynamicValueArgument').setErrors({argumentInUse: true});
          this.filterPredicateValueFormGroup.updateValueAndValidity();
        }
      }, 0);
    } else {
      this.filterPredicateValueFormGroup.get('dynamicValueArgument').disable({emitEvent: false});
      this.filterPredicateValueFormGroup.get('staticValue').enable();
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  validate(): ValidationErrors | null {
    return this.filterPredicateValueFormGroup.valid ? null : {
      filterPredicateValue: {valid: false}
    };
  }

  writeValue(predicateValue: AlarmRuleValue<string | number | boolean>): void {
    if (isDefinedAndNotNull(predicateValue?.dynamicValueArgument)) {
      const availableArgument = this.argumentsList.filter(arg => arg !== this.argumentInUse);
      if (!availableArgument.includes(predicateValue.dynamicValueArgument)) {
        predicateValue.dynamicValueArgument = '';
      }
      this.dynamicModeControl.patchValue(true, {emitEvent: false});
    }
    this.filterPredicateValueFormGroup.patchValue(predicateValue, {emitEvent: false});
  }

  private updateModel() {
    this.propagateChange(this.filterPredicateValueFormGroup.value);
  }
}
