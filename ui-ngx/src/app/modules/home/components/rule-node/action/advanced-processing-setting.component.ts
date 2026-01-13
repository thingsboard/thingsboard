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

import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdvancedProcessingStrategy } from '@home/components/rule-node/action/timeseries-config.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { AttributeAdvancedProcessingStrategy } from '@home/components/rule-node/action/attributes-config.model';

@Component({
  selector: 'tb-advanced-processing-settings',
  templateUrl: './advanced-processing-setting.component.html',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AdvancedProcessingSettingComponent),
    multi: true
  },{
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => AdvancedProcessingSettingComponent),
    multi: true
  }]
})
export class AdvancedProcessingSettingComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  @coerceBoolean()
  timeseries = false;

  @Input()
  @coerceBoolean()
  attributes = false;

  @Input()
  @coerceBoolean()
  latest = false;

  @Input()
  @coerceBoolean()
  webSockets = false;

  @Input()
  @coerceBoolean()
  calculatedFields = false;

  processingForm: UntypedFormGroup;

  private propagateChange: (value: any) => void = () => {};

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.processingForm = this.fb.group({});
    if (this.timeseries) {
      this.processingForm.addControl('timeseries', this.fb.control(null, []));
    }
    if (this.attributes) {
      this.processingForm.addControl('attributes', this.fb.control(null, []));
    }
    if (this.latest) {
      this.processingForm.addControl('latest', this.fb.control(null, []));
    }
    if (this.webSockets) {
      this.processingForm.addControl('webSockets', this.fb.control(null, []));
    }
    if (this.calculatedFields) {
      this.processingForm.addControl('calculatedFields', this.fb.control(null, []));
    }
    this.processingForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => this.propagateChange(value));
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.processingForm.disable({emitEvent: false});
    } else {
      this.processingForm.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.processingForm.valid ? null : {
      processingForm: false
    };
  }

  writeValue(value: AdvancedProcessingStrategy | AttributeAdvancedProcessingStrategy) {
    this.processingForm.patchValue(value, {emitEvent: false});
  }
}
