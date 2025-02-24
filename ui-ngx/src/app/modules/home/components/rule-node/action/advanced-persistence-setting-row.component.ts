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

import { Component, forwardRef, Input } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import {
  AdvancedProcessingConfig,
  defaultAdvancedProcessingConfig,
  maxDeduplicateTimeSecs,
  ProcessingType,
  ProcessingTypeTranslationMap
} from '@home/components/rule-node/action/timeseries-config.models';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-advanced-persistence-setting-row',
  templateUrl: './advanced-persistence-setting-row.component.html',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AdvancedPersistenceSettingRowComponent),
    multi: true
  },{
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => AdvancedPersistenceSettingRowComponent),
    multi: true
  }]
})
export class AdvancedPersistenceSettingRowComponent implements ControlValueAccessor, Validator {

  @Input()
  title: string;

  persistenceSettingRowForm = this.fb.group({
    type: [defaultAdvancedProcessingConfig.type],
    deduplicationIntervalSecs: [{value: 60, disabled: true}]
  });

  PersistenceType = ProcessingType;
  persistenceStrategies = [ProcessingType.ON_EVERY_MESSAGE, ProcessingType.DEDUPLICATE, ProcessingType.SKIP];
  PersistenceTypeTranslationMap = ProcessingTypeTranslationMap;

  maxDeduplicateTime = maxDeduplicateTimeSecs;

  private propagateChange: (value: any) => void = () => {};

  constructor(private fb: FormBuilder) {
    this.persistenceSettingRowForm.get('type').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(() => this.updatedValidation());

    this.persistenceSettingRowForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value) => this.propagateChange(value));
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.persistenceSettingRowForm.disable({emitEvent: false});
    } else {
      this.persistenceSettingRowForm.enable({emitEvent: false});
      this.updatedValidation();
    }
  }

  validate(): ValidationErrors | null {
    return this.persistenceSettingRowForm.valid ? null : {
      persistenceSettingRow: false
    };
  }

  writeValue(value: AdvancedProcessingConfig) {
    if (isDefinedAndNotNull(value)) {
      this.persistenceSettingRowForm.patchValue(value, {emitEvent: false});
    } else {
      this.persistenceSettingRowForm.patchValue(defaultAdvancedProcessingConfig);
    }
  }

  private updatedValidation() {
    if (this.persistenceSettingRowForm.get('type').value === ProcessingType.DEDUPLICATE) {
      this.persistenceSettingRowForm.get('deduplicationIntervalSecs').enable({emitEvent: false});
    } else {
      this.persistenceSettingRowForm.get('deduplicationIntervalSecs').disable({emitEvent: false})
    }
  }
}
