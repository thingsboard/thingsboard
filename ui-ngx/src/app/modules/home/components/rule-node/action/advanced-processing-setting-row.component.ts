// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    selector: 'tb-advanced-processing-setting-row',
    templateUrl: './advanced-processing-setting-row.component.html',
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AdvancedProcessingSettingRowComponent),
            multi: true
        }, {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => AdvancedProcessingSettingRowComponent),
            multi: true
        }],
    standalone: false
})
export class AdvancedProcessingSettingRowComponent implements ControlValueAccessor, Validator {

  @Input()
  title: string;

  processingSettingRowForm = this.fb.group({
    type: [defaultAdvancedProcessingConfig.type],
    deduplicationIntervalSecs: [{value: 60, disabled: true}]
  });

  ProcessingType = ProcessingType;
  processingStrategies = [ProcessingType.ON_EVERY_MESSAGE, ProcessingType.DEDUPLICATE, ProcessingType.SKIP];
  ProcessingTypeTranslationMap = ProcessingTypeTranslationMap;

  maxDeduplicateTime = maxDeduplicateTimeSecs;

  private propagateChange: (value: any) => void = () => {};

  constructor(private fb: FormBuilder) {
    this.processingSettingRowForm.get('type').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(() => this.updatedValidation());

    this.processingSettingRowForm.valueChanges.pipe(
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
      this.processingSettingRowForm.disable({emitEvent: false});
    } else {
      this.processingSettingRowForm.enable({emitEvent: false});
      this.updatedValidation();
    }
  }

  validate(): ValidationErrors | null {
    return this.processingSettingRowForm.valid ? null : {
      processingSettingRow: false
    };
  }

  writeValue(value: AdvancedProcessingConfig) {
    if (isDefinedAndNotNull(value)) {
      this.processingSettingRowForm.patchValue(value, {emitEvent: false});
    } else {
      this.processingSettingRowForm.patchValue(defaultAdvancedProcessingConfig);
    }
  }

  private updatedValidation() {
    if (this.processingSettingRowForm.get('type').value === ProcessingType.DEDUPLICATE) {
      this.processingSettingRowForm.get('deduplicationIntervalSecs').enable({emitEvent: false});
    } else {
      this.processingSettingRowForm.get('deduplicationIntervalSecs').disable({emitEvent: false})
    }
  }
}
