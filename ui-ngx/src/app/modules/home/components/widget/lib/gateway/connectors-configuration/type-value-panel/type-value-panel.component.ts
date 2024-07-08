///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Component, forwardRef, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { isDefinedAndNotNull } from '@core/utils';
import {
  MappingDataKey,
  MappingValueType,
  mappingValueTypesMap,
  noLeadTrailSpacesRegex
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'tb-type-value-panel',
  templateUrl: './type-value-panel.component.html',
  styleUrls: ['./type-value-panel.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TypeValuePanelComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TypeValuePanelComponent),
      multi: true
    }
  ]
})
export class TypeValuePanelComponent implements ControlValueAccessor, Validator, OnInit, OnDestroy {

  valueTypeKeys: MappingValueType[] = Object.values(MappingValueType);
  valueTypes = mappingValueTypesMap;
  valueListFormArray: UntypedFormArray;

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => {};

  constructor(private fb: UntypedFormBuilder) {}

  ngOnInit(): void {
    this.valueListFormArray = this.fb.array([]);
    this.valueListFormArray.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateView(value);
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  trackByKey(_: number, keyControl: AbstractControl): any {
    return keyControl;
  }

  addKey(): void {
    const dataKeyFormGroup = this.fb.group({
      type: [MappingValueType.STRING, []],
      value: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]]
    });
    this.valueListFormArray.push(dataKeyFormGroup);
  }

  deleteKey($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.valueListFormArray.removeAt(index);
    this.valueListFormArray.markAsDirty();
  }

  valueTitle(value: any): string {
    if (isDefinedAndNotNull(value)) {
      if (typeof value === 'object') {
        return JSON.stringify(value);
      }
      return value;
    }
    return '';
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {}

  writeValue(deviceInfoArray: Array<MappingDataKey>): void {
    for (const deviceInfo of deviceInfoArray) {
      const dataKeyFormGroup = this.fb.group({
        type: [deviceInfo.type, []],
        value: [deviceInfo.value, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]]
      });
      this.valueListFormArray.push(dataKeyFormGroup);
    }
  }

  validate(): ValidationErrors | null {
    return this.valueListFormArray.valid ? null : {
      valueListForm: { valid: false }
    };
  }

  private updateView(value: any): void {
    this.propagateChange(value);
  }
}
