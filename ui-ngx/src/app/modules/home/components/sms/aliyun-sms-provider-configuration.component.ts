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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { isDefinedAndNotNull } from '@core/utils';
import {
  SmsProviderConfiguration, SmsProviderType,
  AliyunSmsProviderConfiguration
} from '@shared/models/settings.models';

@Component({
  selector: 'tb-aliyun-sms-provider-configuration',
  templateUrl: './aliyun-sms-provider-configuration.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AliyunSmsProviderConfigurationComponent),
    multi: true
  }]
})
export class AliyunSmsProviderConfigurationComponent implements ControlValueAccessor, OnInit {

  aliyunSmsProviderConfigurationFormGroup: FormGroup;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.aliyunSmsProviderConfigurationFormGroup = this.fb.group({
      accessKeyId: [null, [Validators.required]],
      accessKeySecret: [null, [Validators.required]],
      signName: [null, [Validators.required]],
      templateCode: [null, [Validators.required]]
    });
    this.aliyunSmsProviderConfigurationFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.aliyunSmsProviderConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.aliyunSmsProviderConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: AliyunSmsProviderConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.aliyunSmsProviderConfigurationFormGroup.patchValue(value, {emitEvent: false});
    }
  }

  private updateModel() {
    let configuration: AliyunSmsProviderConfiguration = null;
    if (this.aliyunSmsProviderConfigurationFormGroup.valid) {
      configuration = this.aliyunSmsProviderConfigurationFormGroup.value;
      (configuration as SmsProviderConfiguration).type = SmsProviderType.ALIYUN;
    }
    this.propagateChange(configuration);
  }
}
