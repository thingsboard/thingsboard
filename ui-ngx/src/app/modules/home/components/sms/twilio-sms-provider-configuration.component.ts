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
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { isDefinedAndNotNull } from '@core/utils';
import {
  phoneNumberPatternTwilio,
  SmsProviderConfiguration,
  SmsProviderType,
  TwilioSmsProviderConfiguration
} from '@shared/models/settings.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-twilio-sms-provider-configuration',
  templateUrl: './twilio-sms-provider-configuration.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => TwilioSmsProviderConfigurationComponent),
    multi: true
  }]
})
export class TwilioSmsProviderConfigurationComponent implements ControlValueAccessor, OnInit {

  twilioSmsProviderConfigurationFormGroup: UntypedFormGroup;

  phoneNumberPatternTwilio = phoneNumberPatternTwilio;

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
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.twilioSmsProviderConfigurationFormGroup = this.fb.group({
      numberFrom: [null, [Validators.required, Validators.pattern(phoneNumberPatternTwilio)]],
      accountSid: [null, Validators.required],
      accountToken: [null, Validators.required]
    });
    this.twilioSmsProviderConfigurationFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.twilioSmsProviderConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.twilioSmsProviderConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: TwilioSmsProviderConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.twilioSmsProviderConfigurationFormGroup.patchValue(value, {emitEvent: false});
    }
  }

  private updateModel() {
    let configuration: TwilioSmsProviderConfiguration = null;
    if (this.twilioSmsProviderConfigurationFormGroup.valid) {
      configuration = this.twilioSmsProviderConfigurationFormGroup.value;
      (configuration as SmsProviderConfiguration).type = SmsProviderType.TWILIO;
    }
    this.propagateChange(configuration);
  }
}
