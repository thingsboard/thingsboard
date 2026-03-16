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
  NG_VALUE_ACCESSOR,
} from '@angular/forms';
import {
  DynamicValueSourceType,
  dynamicValueSourceTypeTranslationMap,
  getDynamicSourcesForAllowUser
} from '@shared/models/query/query.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-alarm-dynamic-value',
    templateUrl: './alarm-dynamic-value.component.html',
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AlarmDynamicValue),
            multi: true
        }],
    standalone: false
})

export class AlarmDynamicValue implements ControlValueAccessor, OnInit{
  public dynamicValue: UntypedFormGroup;
  public dynamicValueSourceTypes: DynamicValueSourceType[] = getDynamicSourcesForAllowUser(false);
  public dynamicValueSourceTypeTranslations = dynamicValueSourceTypeTranslationMap;
  private propagateChange = (v: any) => { };

  @Input()
  helpId: string;

  @Input()
  disabled: boolean;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.dynamicValue = this.fb.group({
      sourceType: [null, []],
      sourceAttribute: [null]
    })

    this.dynamicValue.get('sourceType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      (sourceType) => {
        if (!sourceType) {
          this.dynamicValue.get('sourceAttribute').patchValue(null, {emitEvent: false});
        }
      }
    );

    this.dynamicValue.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    })
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(dynamicValue: {sourceType: string, sourceAttribute: string}): void {
    if(dynamicValue) {
      this.dynamicValue.patchValue(dynamicValue, {emitEvent: false});
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.dynamicValue.disable({emitEvent: false});
    } else {
      this.dynamicValue.enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.propagateChange(this.dynamicValue.value);
  }
}
