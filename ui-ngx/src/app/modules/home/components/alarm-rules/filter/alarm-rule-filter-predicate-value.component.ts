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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  ValidationErrors,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { EntityKeyValueType } from '@shared/models/query/query.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";
import { AlarmRuleValue } from "@shared/models/alarm-rule.models";
import { isDefinedAndNotNull } from "@core/utils";
import { FormControlsFrom } from "@shared/models/tenant.model";

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
export class AlarmRuleFilterPredicateValueComponent implements ControlValueAccessor, Validator, OnInit {

  @Input()
  arguments: Record<string, CalculatedFieldArgument>;

  @Input()
  valueType: EntityKeyValueType;

  valueTypeEnum = EntityKeyValueType;

  filterPredicateValueFormGroup: FormGroup<FormControlsFrom<AlarmRuleValue<string | number | boolean>>>;

  mode: 'static' | 'dynamic' = 'static';

  private propagateChange = null;
  private propagateChangePending = false;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
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
      dynamicValueArgument: [null, Validators.required]
    });
    this.filterPredicateValueFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  private updateValueModeValidators(mode: 'static' | 'dynamic'): void {
    if (mode === 'static') {
      this.filterPredicateValueFormGroup.get('staticValue').enable({emitEvent: false});
      this.filterPredicateValueFormGroup.get('dynamicValueArgument').disable({emitEvent: false});
    } else {
      this.filterPredicateValueFormGroup.get('staticValue').disable({emitEvent: false});
      this.filterPredicateValueFormGroup.get('dynamicValueArgument').enable({emitEvent: false});
    }
  }

  get argumentsList(): Array<string> {
    return this.arguments ? Object.keys(this.arguments): [];
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.updateModel();
      }, 0);
    }
  }

  onModeChange(mode: 'static' | 'dynamic') {
    this.mode = mode;
    this.updateValueModeValidators(mode);
  }

  registerOnTouched(fn: any): void {
  }

  validate(): ValidationErrors | null {
    return this.filterPredicateValueFormGroup.valid ? null : {
      filterPredicateValue: {valid: false}
    };
  }

  writeValue(predicateValue: AlarmRuleValue<string | number | boolean>): void {
    this.propagateChangePending = false;
    this.filterPredicateValueFormGroup.patchValue(predicateValue, {emitEvent: false});
    this.mode = isDefinedAndNotNull(predicateValue.dynamicValueArgument) ? 'dynamic' : 'static';
    this.updateValueModeValidators(this.mode);
  }

  private updateModel() {
    const predicateValue: AlarmRuleValue<string | number | boolean> = this.filterPredicateValueFormGroup.value;
    if (this.propagateChange) {
      this.propagateChange(predicateValue);
    } else {
      this.propagateChangePending = true;
    }
  }
}
