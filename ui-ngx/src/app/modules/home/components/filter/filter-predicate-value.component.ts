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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import {
  DynamicValueSourceType,
  dynamicValueSourceTypeTranslationMap,
  EntityKeyValueType,
  FilterPredicateValue,
  getDynamicSourcesForAllowUser,
  inheritModeForDynamicValueSourceType,
  StringOperation
} from '@shared/models/query/query.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-filter-predicate-value',
    templateUrl: './filter-predicate-value.component.html',
    styleUrls: ['./filter-predicate.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => FilterPredicateValueComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => FilterPredicateValueComponent),
            multi: true
        }
    ],
    standalone: false
})
export class FilterPredicateValueComponent implements ControlValueAccessor, Validator, OnInit {

  private readonly inheritModeForSources: DynamicValueSourceType[] = inheritModeForDynamicValueSourceType;

  @Input() disabled: boolean;

  @Input()
  set allowUserDynamicSource(allow: boolean) {
    this.dynamicValueSourceTypes = getDynamicSourcesForAllowUser(allow);
    this.allow = allow;
  }

  private onlyUserDynamicSourceValue = false;

  @Input()
  set onlyUserDynamicSource(dynamicMode: boolean) {
    this.onlyUserDynamicSourceValue = dynamicMode;
    if (this.filterPredicateValueFormGroup) {
      this.updateValidationDynamicMode();
      setTimeout(() => {
        this.updateModel();
      }, 0);
    }
  }

  get onlyUserDynamicSource(): boolean {
    return this.onlyUserDynamicSourceValue;
  }

  @Input()
  set operation(operation: StringOperation) {
    if (operation && (operation === StringOperation.IN || operation === StringOperation.NOT_IN)) {
      this.hintText = 'filter.default-comma-separated-values';
    } else {
      this.hintText = 'filter.default-value';
    }
  }

  @Input()
  valueType: EntityKeyValueType;

  valueTypeEnum = EntityKeyValueType;

  allow = true;

  dynamicValueSourceTypes: DynamicValueSourceType[] = getDynamicSourcesForAllowUser(this.allow);

  dynamicValueSourceTypeTranslations = dynamicValueSourceTypeTranslationMap;

  filterPredicateValueFormGroup: UntypedFormGroup;

  dynamicMode = false;

  inheritMode = false;

  hintText = 'filter.default-value';

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
      defaultValue: [defaultValue, defaultValueValidators],
      dynamicValue: this.fb.group(
        {
          sourceType: [null],
          sourceAttribute: [null],
          inherit: [false]
        }
      )
    });
    this.filterPredicateValueFormGroup.get('dynamicValue').get('sourceType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      (sourceType) => {
        if (!sourceType) {
          this.filterPredicateValueFormGroup.get('dynamicValue').get('sourceAttribute').patchValue(null, {emitEvent: false});
        }
        this.updateShowInheritMode(sourceType);
      }
    );
    this.updateValidationDynamicMode();
    this.filterPredicateValueFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
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

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.filterPredicateValueFormGroup.disable({emitEvent: false});
    } else {
      this.filterPredicateValueFormGroup.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.filterPredicateValueFormGroup.valid ? null : {
      filterPredicateValue: {valid: false}
    };
  }

  writeValue(predicateValue: FilterPredicateValue<string | number | boolean>): void {
    this.propagateChangePending = false;
    this.filterPredicateValueFormGroup.get('defaultValue').patchValue(predicateValue.defaultValue, {emitEvent: false});
    this.filterPredicateValueFormGroup.get('dynamicValue').patchValue({
      sourceType: predicateValue.dynamicValue ? predicateValue.dynamicValue.sourceType : null,
      sourceAttribute: predicateValue.dynamicValue ? predicateValue.dynamicValue.sourceAttribute : null,
      inherit: predicateValue.dynamicValue ? predicateValue.dynamicValue.inherit : false
    }, {emitEvent: this.onlyUserDynamicSource});
    this.updateShowInheritMode(predicateValue?.dynamicValue?.sourceType);
  }

  private updateModel() {
    const predicateValue: FilterPredicateValue<string | number | boolean> = this.filterPredicateValueFormGroup.getRawValue();
    if (predicateValue.dynamicValue) {
      if (!predicateValue.dynamicValue.sourceType || !predicateValue.dynamicValue.sourceAttribute) {
        predicateValue.dynamicValue = null;
      }
    }
    if (this.propagateChange) {
      this.propagateChange(predicateValue);
    } else {
      this.propagateChangePending = true;
    }
  }

  private updateShowInheritMode(sourceType: DynamicValueSourceType) {
    if (this.inheritModeForSources.includes(sourceType)) {
      this.inheritMode = true;
    } else {
      this.filterPredicateValueFormGroup.get('dynamicValue.inherit').patchValue(false, {emitEvent: false});
      this.inheritMode = false;
    }
  }

  private updateValidationDynamicMode() {
    if (this.onlyUserDynamicSource) {
      this.filterPredicateValueFormGroup.get('dynamicValue.sourceType').setValidators(Validators.required);
      this.filterPredicateValueFormGroup.get('dynamicValue.sourceAttribute').setValidators(Validators.required);
    } else {
      this.filterPredicateValueFormGroup.get('dynamicValue.sourceType').clearValidators();
      this.filterPredicateValueFormGroup.get('dynamicValue.sourceAttribute').clearValidators();
    }
    this.filterPredicateValueFormGroup.get('dynamicValue.sourceType').updateValueAndValidity({emitEvent: false});
    this.filterPredicateValueFormGroup.get('dynamicValue.sourceAttribute').updateValueAndValidity({emitEvent: false});
  }
}
