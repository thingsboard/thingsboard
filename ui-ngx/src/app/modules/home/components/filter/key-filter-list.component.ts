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
  FormArray,
  FormBuilder,
  FormControl,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Observable } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ComplexOperation,
  complexOperationTranslationMap,
  EntityKeyType,
  entityKeyTypeTranslationMap,
  KeyFilter,
  KeyFilterInfo,
  keyFilterInfosToKeyFilters
} from '@shared/models/query/query.models';
import { MatDialog } from '@angular/material/dialog';
import { deepClone } from '@core/utils';
import { KeyFilterDialogComponent, KeyFilterDialogData } from '@home/components/filter/key-filter-dialog.component';
import { EntityId } from '@shared/models/id/entity-id';

@Component({
  selector: 'tb-key-filter-list',
  templateUrl: './key-filter-list.component.html',
  styleUrls: ['./key-filter-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => KeyFilterListComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => KeyFilterListComponent),
      multi: true
    }
  ],
  standalone: false
})
export class KeyFilterListComponent implements ControlValueAccessor, Validator, OnInit {

  @Input() disabled: boolean;

  @Input() displayUserParameters = true;

  @Input() allowUserDynamicSource = true;

  @Input() telemetryKeysOnly = false;

  @Input() entityId: EntityId;

  @Input() operation: ComplexOperation = ComplexOperation.AND;

  complexOperationTranslationMap = complexOperationTranslationMap;

  entityKeyTypeTranslations = entityKeyTypeTranslationMap;

  keyFiltersFormArray: FormArray<FormControl<KeyFilterInfo>>;

  keyFiltersControl: FormControl<Array<KeyFilter>>;

  private propagateChange = null;

  constructor(private fb: FormBuilder,
              private dialog: MatDialog,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.keyFiltersFormArray = this.fb.array<FormControl<KeyFilterInfo>>([]);
    this.keyFiltersControl = this.fb.control(null);

    this.keyFiltersFormArray.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateModel());
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.keyFiltersFormArray.disable({emitEvent: false});
      this.keyFiltersControl.disable({emitEvent: false});
    } else {
      this.keyFiltersFormArray.enable({emitEvent: false});
      this.keyFiltersControl.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.keyFiltersFormArray.valid && this.keyFiltersControl.valid ? null : {
      keyFilterList: {valid: false}
    };
  }

  writeValue(keyFilters: Array<KeyFilterInfo>): void {
    if (keyFilters?.length === this.keyFiltersFormArray.length) {
      this.keyFiltersFormArray.patchValue(keyFilters, {emitEvent: false});
    } else {
      this.keyFiltersFormArray.clear({emitEvent: false});
      if (keyFilters) {
        for (const keyFilter of keyFilters) {
          this.keyFiltersFormArray.push(
            this.fb.control(keyFilter, [Validators.required]),
            {emitEvent: false}
          );
        }
      }
      if (this.disabled) {
        this.keyFiltersFormArray.disable({emitEvent: false});
      } else {
        this.keyFiltersFormArray.enable({emitEvent: false});
      }
    }
    const keyFiltersArray = keyFilterInfosToKeyFilters(keyFilters);
    this.keyFiltersControl.patchValue(keyFiltersArray, {emitEvent: false});
  }

  removeKeyFilter(index: number) {
    this.keyFiltersFormArray.removeAt(index);
  }

  addKeyFilter() {
    this.openKeyFilterDialog(null).subscribe((result) => {
      if (result) {
        this.keyFiltersFormArray.push(this.fb.control(result, [Validators.required]));
      }
    });
  }

  editKeyFilter(index: number) {
    const keyFilter = this.keyFiltersFormArray.at(index).value;
    this.openKeyFilterDialog(keyFilter).subscribe(
      (result) => {
        if (result) {
          this.keyFiltersFormArray.at(index).patchValue(result);
        }
      }
    );
  }

  private openKeyFilterDialog(keyFilter?: KeyFilterInfo): Observable<KeyFilterInfo> {
    const isAdd = !keyFilter;
    if (!keyFilter) {
      keyFilter = {
        key: {
          key: '',
          type: EntityKeyType.ATTRIBUTE
        },
        valueType: null,
        value: null,
        predicates: []
      };
    }
    return this.dialog.open<KeyFilterDialogComponent, KeyFilterDialogData,
      KeyFilterInfo>(KeyFilterDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        keyFilter: keyFilter ? (this.disabled ? keyFilter : deepClone(keyFilter)) : null,
        isAdd,
        readonly: this.disabled,
        displayUserParameters: this.displayUserParameters,
        allowUserDynamicSource: this.allowUserDynamicSource,
        telemetryKeysOnly: this.telemetryKeysOnly,
        entityId: this.entityId
      }
    }).afterClosed();
  }

  private updateModel() {
    const keyFilters = this.keyFiltersFormArray.getRawValue();
    this.propagateChange(keyFilters.length ? keyFilters : null);
    const keyFiltersArray = keyFilterInfosToKeyFilters(keyFilters);
    this.keyFiltersControl.patchValue(keyFiltersArray, {emitEvent: false});
  }
}
