// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import {
  AwsSnsSmsProviderConfiguration,
  BindTypes,
  bindTypesTranslationMap,
  CodingSchemes,
  codingSchemesMap,
  NumberingPlanIdentification,
  numberingPlanIdentificationMap,
  SmppSmsProviderConfiguration,
  smppVersions,
  SmsProviderConfiguration,
  SmsProviderType,
  TypeOfNumber,
  typeOfNumberMap
} from '@shared/models/settings.models';
import { isDefinedAndNotNull } from '@core/utils';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-smpp-sms-provider-configuration',
    templateUrl: './smpp-sms-provider-configuration.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SmppSmsProviderConfigurationComponent),
            multi: true
        }],
    standalone: false
})

export class SmppSmsProviderConfigurationComponent  implements ControlValueAccessor, OnInit{
  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }
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

  smppSmsProviderConfigurationFormGroup: UntypedFormGroup;

  smppVersions = smppVersions;

  bindTypes = Object.keys(BindTypes);
  bindTypesTranslation = bindTypesTranslationMap;

  typeOfNumber = Object.keys(TypeOfNumber);
  typeOfNumberMap = typeOfNumberMap;

  numberingPlanIdentification = Object.keys(NumberingPlanIdentification);
  numberingPlanIdentificationMap = numberingPlanIdentificationMap;

  codingSchemes = Object.keys(CodingSchemes);
  codingSchemesMap = codingSchemesMap;

  private propagateChange = (v: any) => { };

  ngOnInit(): void {
    this.smppSmsProviderConfigurationFormGroup = this.fb.group({
      protocolVersion: [null, [Validators.required]],
      host: [null, [Validators.required]],
      port: [null, [Validators.required]],
      systemId: [null, [Validators.required]],
      password: [null, [Validators.required]],
      systemType: [null],
      bindType: [null, []],
      serviceType: [null, []],
      sourceAddress: [null, []],
      sourceTon: [null, []],
      sourceNpi: [null, []],
      destinationTon: [null, []],
      destinationNpi: [null, []],
      addressRange: [null, []],
      codingScheme: [null, []],
    });

    this.smppSmsProviderConfigurationFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValue();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.smppSmsProviderConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.smppSmsProviderConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: AwsSnsSmsProviderConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.smppSmsProviderConfigurationFormGroup.patchValue(value, {emitEvent: false});
    }
  }

  private updateValue() {
    let configuration: SmppSmsProviderConfiguration = null;
    if (this.smppSmsProviderConfigurationFormGroup.valid) {
      configuration = this.smppSmsProviderConfigurationFormGroup.value;
      (configuration as SmsProviderConfiguration).type = SmsProviderType.SMPP;
    }
    this.propagateChange(configuration);
  }

}
