///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { CommonTransportConfiguration, createCommonTransportConfiguration } from '@shared/models/device.models';
import { isDefinedAndNotNull } from '@core/utils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-common-transport-configuration',
  templateUrl: './common-transport-configuration.component.html',
  styleUrls: ['./common-transport-configuration.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => CommonTransportConfigurationComponent),
    multi: true
  }, {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => CommonTransportConfigurationComponent),
    multi: true
  }]
})
export class CommonTransportConfigurationComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {

  commonTransportConfigurationFormGroup: FormGroup;

  @Input()
  disabled: boolean;

  private destroy$ = new Subject();
  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {
  }

  ngOnInit() {
    this.commonTransportConfigurationFormGroup = this.fb.group({
      sequentialRpc: [false]
    });
    this.commonTransportConfigurationFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateModel(value);
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.commonTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.commonTransportConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.commonTransportConfigurationFormGroup.valid ? null : {
      commonTransportConfigurationFormGroup: false
    };
  }

  writeValue(value: CommonTransportConfiguration) {
    if (isDefinedAndNotNull(value)) {
      this.commonTransportConfigurationFormGroup.patchValue(value, {emitEvent: false});
    } else {
      this.commonTransportConfigurationFormGroup.patchValue(createCommonTransportConfiguration(), {emitEvent: false});
    }
  }

  private updateModel(value: CommonTransportConfiguration) {
    this.propagateChange(value);
  }

}
