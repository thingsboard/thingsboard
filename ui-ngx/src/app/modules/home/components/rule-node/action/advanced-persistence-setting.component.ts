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

import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { Component, forwardRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdvancedProcessingStrategy } from '@home/components/rule-node/action/timeseries-config.models';

@Component({
  selector: 'tb-advanced-persistence-settings',
  templateUrl: './advanced-persistence-setting.component.html',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AdvancedPersistenceSettingComponent),
    multi: true
  },{
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => AdvancedPersistenceSettingComponent),
    multi: true
  }]
})
export class AdvancedPersistenceSettingComponent implements ControlValueAccessor, Validator {

  persistenceForm = this.fb.group({
    timeseries: [null],
    latest: [null],
    webSockets: [null]
  });

  private propagateChange: (value: any) => void = () => {};

  constructor(private fb: FormBuilder) {
    this.persistenceForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => this.propagateChange(value));
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.persistenceForm.disable({emitEvent: false});
    } else {
      this.persistenceForm.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.persistenceForm.valid ? null : {
      persistenceForm: false
    };
  }

  writeValue(value: AdvancedProcessingStrategy) {
    this.persistenceForm.patchValue(value, {emitEvent: false});
  }
}
