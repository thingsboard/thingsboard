// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
        }],
    standalone: false
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
