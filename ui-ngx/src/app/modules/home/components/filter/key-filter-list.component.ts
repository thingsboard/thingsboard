///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR,
  Validators
} from '@angular/forms';
import { Observable, Subscription } from 'rxjs';
import { EntityKeyType, entityKeyTypeTranslationMap, KeyFilterInfo } from '@shared/models/query/query.models';
import { MatDialog } from '@angular/material/dialog';
import { deepClone } from '@core/utils';
import { KeyFilterDialogComponent, KeyFilterDialogData } from '@home/components/filter/key-filter-dialog.component';

@Component({
  selector: 'tb-key-filter-list',
  templateUrl: './key-filter-list.component.html',
  styleUrls: ['./key-filter-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => KeyFilterListComponent),
      multi: true
    }
  ]
})
export class KeyFilterListComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  keyFilterListFormGroup: FormGroup;

  entityKeyTypeTranslations = entityKeyTypeTranslationMap;

  private propagateChange = null;

  private valueChangeSubscription: Subscription = null;

  constructor(private fb: FormBuilder,
              private dialog: MatDialog) {
  }

  ngOnInit(): void {
    this.keyFilterListFormGroup = this.fb.group({});
    this.keyFilterListFormGroup.addControl('keyFilters',
      this.fb.array([]));
  }

  keyFiltersFormArray(): FormArray {
    return this.keyFilterListFormGroup.get('keyFilters') as FormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.keyFilterListFormGroup.disable({emitEvent: false});
    } else {
      this.keyFilterListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(keyFilters: Array<KeyFilterInfo>): void {
    if (this.valueChangeSubscription) {
      this.valueChangeSubscription.unsubscribe();
    }
    const keyFilterControls: Array<AbstractControl> = [];
    if (keyFilters) {
      for (const keyFilter of keyFilters) {
        keyFilterControls.push(this.fb.control(keyFilter, [Validators.required]));
      }
    }
    this.keyFilterListFormGroup.setControl('keyFilters', this.fb.array(keyFilterControls));
    this.valueChangeSubscription = this.keyFilterListFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    if (this.disabled) {
      this.keyFilterListFormGroup.disable({emitEvent: false});
    } else {
      this.keyFilterListFormGroup.enable({emitEvent: false});
    }
  }

  public removeKeyFilter(index: number) {
    (this.keyFilterListFormGroup.get('keyFilters') as FormArray).removeAt(index);
  }

  public addKeyFilter() {
    const keyFiltersFormArray = this.keyFilterListFormGroup.get('keyFilters') as FormArray;
    this.openKeyFilterDialog(null).subscribe((result) => {
      if (result) {
        keyFiltersFormArray.push(this.fb.control(result, [Validators.required]));
      }
    });
  }

  public editKeyFilter(index: number) {
    const keyFilter: KeyFilterInfo =
      (this.keyFilterListFormGroup.get('keyFilters') as FormArray).at(index).value;
    this.openKeyFilterDialog(keyFilter).subscribe(
      (result) => {
        if (result) {
          (this.keyFilterListFormGroup.get('keyFilters') as FormArray).at(index).patchValue(result);
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
        predicates: []
      };
    }
    return this.dialog.open<KeyFilterDialogComponent, KeyFilterDialogData,
      KeyFilterInfo>(KeyFilterDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        keyFilter: keyFilter ? deepClone(keyFilter): null,
        isAdd
      }
    }).afterClosed();
  }

  private updateModel() {
    const keyFilters: Array<KeyFilterInfo> = this.keyFilterListFormGroup.getRawValue().keyFilters;
    if (keyFilters.length) {
      this.propagateChange(keyFilters);
    } else {
      this.propagateChange(null);
    }
  }
}
