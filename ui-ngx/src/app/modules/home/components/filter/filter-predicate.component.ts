// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { EntityKeyValueType, FilterPredicateType, KeyFilterPredicateInfo } from '@shared/models/query/query.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-filter-predicate',
    templateUrl: './filter-predicate.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => FilterPredicateComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => FilterPredicateComponent),
            multi: true
        }
    ],
    standalone: false
})
export class FilterPredicateComponent implements ControlValueAccessor, Validator, OnInit {

  @Input() disabled: boolean;

  @Input() valueType: EntityKeyValueType;

  @Input() key: string;

  @Input() displayUserParameters = true;

  @Input() allowUserDynamicSource = true;

  @Input() onlyUserDynamicSource = false;

  filterPredicateFormGroup: UntypedFormGroup;

  type: FilterPredicateType;

  filterPredicateType = FilterPredicateType;

  private propagateChange = null;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.filterPredicateFormGroup = this.fb.group({
      predicate: [null, [Validators.required]],
      userInfo: [null, []]
    });
    this.filterPredicateFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.filterPredicateFormGroup.disable({emitEvent: false});
    } else {
      this.filterPredicateFormGroup.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.filterPredicateFormGroup.valid ? null : {
      filterPredicate: {valid: false}
    };
  }

  writeValue(predicate: KeyFilterPredicateInfo): void {
    this.type = predicate.keyFilterPredicate.type;
    this.filterPredicateFormGroup.get('predicate').patchValue(predicate.keyFilterPredicate, {emitEvent: false});
    this.filterPredicateFormGroup.get('userInfo').patchValue(predicate.userInfo, {emitEvent: false});
  }

  private updateModel() {
    let predicate: KeyFilterPredicateInfo = null;
    if (this.filterPredicateFormGroup.valid) {
      predicate = {
        keyFilterPredicate: this.filterPredicateFormGroup.getRawValue().predicate,
        userInfo: this.filterPredicateFormGroup.getRawValue().userInfo
      };
    }
    this.propagateChange(predicate);
  }

}
